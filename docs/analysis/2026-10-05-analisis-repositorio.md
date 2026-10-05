# Análisis del repositorio — 2026-10-05

Rama analizada: `match-feature` (base `master`). Alcance: todo el backend, con foco en `club` y el nuevo módulo `match`.

## 1. Qué es Squadfy

Backend para gestionar clubes deportivos (inicialmente fútbol amateur):

1. Un usuario crea un club y obtiene un **código de invitación**; otros usuarios se unen con él. Un usuario puede estar en varios clubes.
2. Cada club juega **un partido por semana**, normalmente el mismo día y hora.
3. Para cada partido hay una **convocatoria** que se abre el día después del partido anterior y se cierra el día antes del siguiente. Los miembros se apuntan y desapuntan.
4. Con los inscritos se hace un **sorteo de dos equipos** lo más parejos posible según posición y estadísticas.
5. Además: chat en tiempo real (WebSocket), notificaciones push (Firebase) y emails (Mailgun).

## 2. Arquitectura actual

| Aspecto | Estado |
|---|---|
| Lenguaje / runtime | Kotlin 2.2, JVM 21 |
| Framework | Spring Boot **4.0.0-SNAPSHOT** (⚠️ no apto para producción) |
| Estilo | Monolito modular Gradle: `app`, `common`, `user`, `club`, `match`, `chat`, `notification` + `build-logic` (convention plugins) |
| Capas por módulo | `api` (controllers, dto, mappers, exception_handling) → `service` → `domain` (model, exception) → `infrastructure` (database, messaging, storage) |
| Persistencia | PostgreSQL (Supabase), un esquema por módulo, JPA/Hibernate, `ddl-auto: update` en dev y `validate` en prod, **sin migraciones** |
| Mensajería | RabbitMQ (CloudAMQP), exchanges `user.events`, `chat.events`; `club.events` definido pero sin uso |
| Caché | Redis (chat) |
| Auth | JWT propio (access + refresh), filtro `JwtAuthFilter`, `requestUserId` desde el `SecurityContext` |
| Storage | Supabase Storage (fotos de perfil, logos) |
| Tests | Solo `contextLoads` (necesita infraestructura real) |
| Despliegue | No hay Dockerfile, CI, health checks ni documentación OpenAPI |

## 3. Estado funcional frente al MVP

| Capacidad | Estado | Spec |
|---|---|---|
| Registro, login, verificación email, reset password | ✅ Hecho | — |
| Crear club, unirse por código, listar mis clubes, miembros, regenerar código, logo | ✅ Hecho | 001 |
| Elegir posición tipada y editar mi ficha (dorsal/posición) | ✅ Hecho en esta sesión | 001 |
| Salir del club, expulsar, cambiar roles, editar/borrar club | ❌ Pendiente | 001 |
| Horario semanal del club (día, hora, zona horaria, cupo) | ✅ Hecho (rehecho en esta sesión) | 002 |
| Generación automática de partido + convocatoria | ✅ Hecho (rehecho en esta sesión) | 002 |
| Apuntarse / desapuntarse con ventana temporal y cupo | ✅ Hecho (rehecho en esta sesión) | 002 |
| Lista de espera cuando se llena el cupo | ❌ Pendiente (decisión de producto) | 002 |
| Sorteo equilibrado por posición y estadísticas | ✅ v1 hecho en esta sesión | 003 |
| Valoración manual de nivel por admins | ❌ Pendiente (decisión de producto) | 003 |
| Cerrar partido, resultado, estadísticas acumuladas, ranking | ❌ Pendiente | 004 |
| Notificaciones de convocatoria/equipos | ❌ Pendiente | 005 |
| Migraciones, Boot GA, Docker, CI, health, OpenAPI, tests integración | ❌ Pendiente — **bloquea el despliegue** | 006 |

## 4. Hallazgos en la rama `match-feature`

### 4.1 Corregidos en esta sesión

| # | Severidad | Hallazgo | Corrección |
|---|---|---|---|
| 1 | 🔴 Crítica | **Ningún endpoint de `match` comprobaba permisos**: cualquier usuario autenticado podía crear horarios/partidos, cancelar, sortear equipos o añadir eventos en cualquier club y leer sus convocatorias. La versión anterior en `club` sí lo hacía. | `ClubAccessGuard` (`requireMember` / `requireManager`) en todos los casos de uso. |
| 2 | 🔴 Crítica | La subida del logo del club se hacía **antes** de comprobar permisos: cualquiera podía subir ficheros al bucket. | La subida se movió a `ClubService.updateClubLogo` tras `ensureCanManageClub`. |
| 3 | 🟠 Alta | **Zona horaria**: `matchTime` se interpretaba en UTC. Un partido a las 20:00 en Madrid se programaba a las 22:00 locales. | `timeZone` por horario (por defecto `Europe/Madrid`); todo el cálculo en `MatchCalendar` con la zona del club. Test con cambio de horario. |
| 4 | 🟠 Alta | **Regenerar equipos fallaba**: `deleteByMatchId` derivado + `saveAll` en la misma transacción; Hibernate ejecuta los INSERT antes que los DELETE y salta el índice único `(match_id, club_member_id)`. | Borrado masivo `@Modifying` que se ejecuta inmediatamente. |
| 5 | 🟠 Alta | **Carrera en inscripciones**: dos usuarios simultáneos podían superar `maxPlayers` (comprobación count + insert sin bloqueo). | `SELECT ... FOR UPDATE` sobre la convocatoria (`findByIdForUpdate`). |
| 6 | 🟠 Alta | No se respetaba la ventana temporal: se aceptaban inscripciones antes de `opensAt` y entre `closesAt` y la ejecución del job de cierre. | `ensureOpen` comprueba estado **y** ventana con `Clock`. |
| 7 | 🟠 Alta | El scheduler solo creaba el partido si se ejecutaba **exactamente** el día de apertura a las 08:00 UTC; un reinicio ese día perdía la semana. Partido y convocatoria se creaban en transacciones separadas. | Planificación horaria e idempotente (`MatchPlanningService.planNextMatch`), partido + convocatoria en una transacción, índice único `(club_id, scheduled_at)` contra ejecuciones duplicadas. |
| 8 | 🟠 Alta | La regla "se abre el día después del partido anterior" no se implementaba (se usaba "N días antes"). | `MatchCalendar.announcementWindow`: abre el día siguiente al último partido no cancelado; cierra al inicio del día del partido. |
| 9 | 🟡 Media | `match` leía `club_service.club_members` con SQL directo (acoplamiento entre módulos). | Puerto `ClubMembershipProvider` en `common`, implementado por `club`. |
| 10 | 🟡 Media | Sorteo = `shuffle` aleatorio, sin posiciones ni estadísticas. | `TeamBalancer` (porteros repartidos, posiciones equilibradas, rating por estadísticas + intercambios) con tests. |
| 11 | 🟡 Media | Posición como texto libre (120 chars) → imposible equilibrar. | Enum `PlayerPosition` en API; parser tolerante para datos antiguos. |
| 12 | 🟡 Media | Sorteo manual aceptaba jugadores no inscritos; eventos aceptaban jugadores que no juegan el partido; borrar un evento no comprobaba que fuera del partido. | Validaciones añadidas. |
| 13 | 🟡 Media | Cancelar un partido dejaba su convocatoria abierta. | `cancelForMatch`. |
| 14 | 🟡 Media | N+1 en listados de partidos y convocatorias (4 consultas por partido). | Consultas por lotes (`...In`). |
| 15 | 🟡 Media | `SecurityConfig`: `/api/auth/**` (permitAll) iba antes que `/api/auth/change-password` (authenticated), por lo que esta regla nunca aplicaba. | Orden corregido. |
| 16 | 🟢 Baja | `ClubStorageService` nuevo y sin uso (duplicaba `SupabaseStorageService`). | Eliminado. |
| 17 | 🟢 Baja | Manejador de validación duplicado en `club` y `match`; `NotClubMemberException` devolvía 400. | Centralizado en `CommonExceptionHandler`; 403 para no-miembro; 409 para violaciones de integridad. |
| 18 | 🟢 Baja | `show_sql: true` en la configuración base (también en prod). | Movido a `application-dev.yml`. |

### 4.2 Pendientes (recogidos en specs)

| Severidad | Hallazgo | Spec |
|---|---|---|
| 🔴 | `ddl-auto: validate` en prod **sin migraciones**: un despliegue en base de datos limpia no arranca. | 006 |
| 🔴 | Spring Boot `4.0.0-SNAPSHOT` + repos snapshot/milestone: builds no reproducibles. Fijar la última 4.0.x GA. | 006 |
| 🟠 | `gradlew` versionado sin bit de ejecución (`100644`): CI fallará. | 006 |
| 🟠 | Sin Dockerfile, CI, actuator/health, OpenAPI. | 006 |
| 🟠 | Infraestructura hardcodeada en `application.yml` (hosts de Supabase, Redis, RabbitMQ, usuario Mailgun sandbox, email de pruebas `squadfy.email.to`). | 006 |
| 🟠 | `allOpen` para entidades JPA solo se aplica en `app` (y con imports de accessors generados frágiles); las entidades de los módulos son `final`, así que no hay proxies LAZY. | 006 |
| 🟠 | Las estadísticas de `ClubMemberEntity` (goles, asistencias, partidos...) nunca se actualizan; los partidos nunca pasan a `COMPLETED`. | 004 |
| 🟡 | `ClubParticipantService.findInUserService` lee `user_service.users` con SQL directo. | 001 |
| 🟡 | Eventos `ClubEvent` definidos pero nunca publicados. | 001 / 005 |
| 🟡 | `ClubMemberDto` expone el email de todos los miembros; `ClubDto` expone el código de invitación a cualquier miembro. | 001 |
| 🟡 | Rutas REST inconsistentes (`/api/club` singular, `/api/club/create`, `/api/matchAnnouncements` camelCase, `DELETE .../cancel`). | 006 |
| 🟢 | `group = "org.example"` en los módulos. | 006 |

### 4.3 Acción manual necesaria en la base de datos de desarrollo

La columna `callup_open_days_before_match` se eliminó del modelo y se añadió `time_zone`. Con `ddl-auto: update`, Hibernate añade la nueva columna pero **no borra** la antigua, que es `NOT NULL` sin valor por defecto y hará fallar los INSERT. Si la tabla ya existe:

```sql
ALTER TABLE match_service.club_match_schedules DROP COLUMN IF EXISTS callup_open_days_before_match;
```

Las columnas nuevas (`club_match_schedules.time_zone`, `club_match_schedules.format`, `callup_entries.status`) y las tablas `player_ratings` y `player_rating_changes` se crean solas con `ddl-auto: update` (las columnas llevan valor por defecto).

Si hay datos duplicados en `match_service.matches` para el mismo `(club_id, scheduled_at)`, el nuevo índice único no se podrá crear hasta limpiarlos.

## 5. Recomendación de orden para llegar al MVP desplegable

1. **006 (parte bloqueante)**: Boot GA, Flyway con baseline, configuración por entorno, Dockerfile, health check. Sin esto no se puede desplegar.
2. **002/003 — tests de integración** de convocatoria y sorteo con Testcontainers.
3. **004**: cerrar partido y acumular estadísticas (alimenta el sorteo).
4. **001 (resto)**: salir/expulsar/roles.
5. **005**: notificaciones de convocatoria abierta, recordatorio de cierre y equipos generados.
6. **006 (resto)**: CI, OpenAPI, coherencia de rutas.
