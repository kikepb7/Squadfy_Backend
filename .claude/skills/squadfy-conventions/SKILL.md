---
name: squadfy-conventions
description: Convenciones de arquitectura y código del backend Squadfy (Kotlin + Spring Boot, monolito modular). Usar antes de crear o modificar controladores, servicios, entidades, repositorios, DTOs, excepciones, eventos RabbitMQ o comunicación entre módulos (user, club, match, chat, notification, common).
---

# Convenciones Squadfy Backend

## Módulos y dependencias
- `app` ensambla todo (SecurityConfig, `@EnableScheduling`, `application*.yml`). Los módulos de feature solo dependen de `common`.
- Paquete raíz compartido `com.kikepb.squadfy`: **los nombres de clase deben ser únicos entre módulos** (Spring escanea todo).
- Esquema Postgres por módulo (`club_service`, `match_service`, ...), declarado en `@Table(schema = ...)`.

## Capas dentro de un módulo
```
api/controllers        @RestController, finos: extraen requestUserId y delegan
api/dto                Request/Response con validación jakarta (@field:...)
api/mappers            Model -> Dto (funciones de extensión toXxxDto)
api/exception_handling @RestControllerAdvice del módulo (excepciones de dominio -> HTTP)
domain/model           data classes y lógica pura (sin Spring/JPA), testeable
domain/exception       RuntimeException con mensaje en inglés
infrastructure/database/{entities,repositories,mappers}
service                casos de uso @Service, @Transactional, permisos
```

## Comunicación entre módulos
- **Nunca** SQL a tablas de otro esquema ni imports de otro módulo de feature.
- Consulta síncrona → puerto en `common/domain/<área>/` implementado por el módulo dueño. Ejemplo: `ClubMembershipProvider` (club) usado por `match` vía `ClubAccessGuard`.
- Notificación asíncrona → evento `SquadfyEvent` (sealed class en `common/domain/events/<área>/`), `EventPublisher.publish`, cola + binding en `RabbitMqConfig`, `@RabbitListener` en el consumidor.
  - **Una familia de eventos nueva (paquete nuevo) debe añadirse a `setTrustedPackages` en `RabbitMqConfig`**; si no, los listeners rechazan los mensajes en silencio. Cúbrelo con un test como `app/.../EventMessagingIntegrationTest.kt`.

## Seguridad
- Usuario actual: `requestUserId` (`common/api/util/RequestUserId.kt`), solo en controladores; pásalo como `userId` al servicio.
- En el servicio, antes de cualquier efecto: `clubAccessGuard.requireMember(clubId, userId)` o `requireManager(...)` (gestor = OWNER/ADMIN). En `club`, `ensureIsClubMember` / `ensureCanManageClub`.
- Excepciones: `ForbiddenException` → 403, `NotClubMemberException` → 403, `UnauthorizedException` → 401.

## Persistencia
- Entidades: `class` con `var`, id `UUID` `@GeneratedValue(strategy = GenerationType.UUID)`, `@CreationTimestamp`/`@UpdateTimestamp`, índices declarados en `@Table`.
- IDs con typealias de `common/domain/type` (`ClubId`, `MatchId`, ...).
- Invariantes en BD (índices únicos) además de en código. Concurrencia sobre cupos → `@Lock(PESSIMISTIC_WRITE)`.
- Borrar y reinsertar en la misma transacción con índice único → borrado masivo `@Modifying @Query` (Hibernate hace flush de INSERT antes que DELETE).
- Listados: evita N+1 con consultas `findAllBy...In` y agrupación en memoria.
- Cambios de esquema: **siempre** con una migración Flyway nueva `app/src/main/resources/db/migration/V<n>__<descripcion>.sql` (nunca editar una aplicada). Hibernate solo valida; `EmptyDatabaseMigrationTest` falla si entidad y migraciones divergen.

## Configuración
- Toda la infraestructura por variables de entorno con default local (`application.yml`), documentadas en `.env.example`. Nada de hosts ni secretos en el repo.
- Integraciones externas opcionales en local con un flag (ej. `firebase.enabled`).

## Tiempo y fechas
- Inyecta `java.time.Clock` (bean en `common/infrastructure/time/ClockConfig.kt`); nada de `Instant.now()` en lógica.
- Calendario en la zona del club (`ClubMatchScheduleModel.timeZone`); persistencia en `Instant`.

## Estilo
- Argumentos con nombre en llamadas (`findByClubId(clubId = clubId)`), expresiones `=` para funciones de una línea.
- Errores HTTP: `{ "code", "message" }`; validación centralizada en `CommonExceptionHandler`.
- Tests: JUnit 5 + `kotlin.test`, nombres con backticks en inglés, en `src/test/kotlin` del módulo.
