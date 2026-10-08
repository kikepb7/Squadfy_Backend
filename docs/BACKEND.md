# Squadfy Backend — Documentación completa

> **Para quien lea esto desde otro proyecto (p. ej. la app KMP):** este documento describe el backend tal y como está en `master` (octubre 2026). Es autocontenido: arquitectura, cómo arrancarlo, configuración, contrato REST completo, WebSocket, notificaciones push, reglas de negocio y flujos recomendados para la app. Cuando haya dudas sobre un campo concreto, la fuente de verdad ejecutable es el OpenAPI (`/v3/api-docs`, perfil `dev`).

---

## Índice

1. [Qué es Squadfy y estado actual](#1-qué-es-squadfy-y-estado-actual)
2. [Arquitectura](#2-arquitectura)
3. [Puesta en marcha y comandos](#3-puesta-en-marcha-y-comandos)
4. [Configuración (variables de entorno)](#4-configuración-variables-de-entorno)
5. [Convenciones de la API](#5-convenciones-de-la-api)
6. [Autenticación y sesión](#6-autenticación-y-sesión)
7. [Reglas de negocio por dominio](#7-reglas-de-negocio-por-dominio)
8. [Referencia de endpoints](#8-referencia-de-endpoints)
9. [Modelos (DTOs)](#9-modelos-dtos)
10. [Enumerados](#10-enumerados)
11. [Errores](#11-errores)
12. [Chat en tiempo real (WebSocket)](#12-chat-en-tiempo-real-websocket)
13. [Notificaciones push (Firebase)](#13-notificaciones-push-firebase)
14. [Guía de integración para la app KMP](#14-guía-de-integración-para-la-app-kmp)
15. [Cambios respecto a la versión anterior de la app](#15-cambios-respecto-a-la-versión-anterior-de-la-app)
16. [Limitaciones conocidas y pendiente](#16-limitaciones-conocidas-y-pendiente)

---

## 1. Qué es Squadfy y estado actual

Backend para gestionar **clubes de fútbol amateur**:

- Un usuario crea un club y comparte un **código de invitación**; otros se unen con él. Un usuario puede estar en varios clubes, con una ficha distinta en cada uno (dorsal, posición, rol).
- Cada club juega **un partido a la semana** (mismo día y hora). El sistema crea solo el siguiente partido y su **convocatoria**.
- La convocatoria **se abre el día después del partido anterior** y **se cierra a la hora de cierre del club** (por defecto **22:00 del día anterior**, configurable). Los miembros se apuntan y desapuntan; con el cupo lleno (10/14/22 según 5v5/7v7/11v11) entran en **lista de espera**, que sube automáticamente si alguien se desapunta.
- Cada miembro puede añadir hasta **2 invitados** por convocatoria; los invitados solo ocupan plazas que sobren (los miembros tienen prioridad).
- A la **hora del sorteo** (por defecto, la misma que el cierre; configurable) se **publican automáticamente dos equipos equilibrados** (posición + nivel), con los invitados confirmados. Los gestores pueden rectificarlos.
- Los gestores pueden **cancelar o mover una semana** del calendario (excepciones) y cada miembro puede registrar sus **ausencias**.
- El nivel de cada jugador es un **rating tipo Elo** calculado automáticamente de los resultados y estadísticas; es público dentro del club (clasificación).
- Tras el partido, los gestores registran goles, asistencias, tarjetas y minutos, pueden fijar el **marcador oficial** a mano y lo **cierran**; las **estadísticas**, el **ranking** y la **variación de rating de cada jugador en el partido** se calculan de los partidos cerrados.
- **Chat** en tiempo real (WebSocket) y **notificaciones push** (Firebase) del ciclo de partido.

### Estado

| Área | Estado |
|---|---|
| Autenticación (registro, verificación por email opcional, login, refresh, reset/cambio de contraseña) | ✅ |
| Feature flags por entorno (`FEATURE_*`, `GET /features`; spec 011) | ✅ |
| Borrado de cuenta desde la app y desde la web (`/account/delete`), rate limit por cuenta (spec 010) | ✅ |
| Clubes: crear, unirse por código, miembros, roles, expulsar, vetar, transferir propiedad, editar | ✅ |
| Horario semanal, planificación automática, convocatoria con ventana y lista de espera | ✅ |
| Sorteo equilibrado automático a la hora del sorteo + rectificación manual | ✅ |
| Invitados, excepciones del calendario, ausencias, cierre/sorteo configurables, marcador manual (spec 008) | ✅ |
| Rating Elo público + equilibrio de equipos para gestores | ✅ |
| Cerrar/reabrir partido, eventos, minutos, estadísticas y ranking | ✅ |
| Notificaciones push del ciclo de partido + silenciar club | ✅ |
| Chat (REST + WebSocket) y foto de perfil | ✅ |
| API versionada `/api/v1`, OpenAPI/Swagger (solo `dev`) | ✅ |
| Docker, Flyway, CI (GitHub Actions), health checks | ✅ |
| **Despliegue** (elegir hosting, registro de imágenes, CD) | ⏳ pendiente |

Calidad: `./gradlew build` ejecuta ~135 tests (unitarios + integración con PostgreSQL, RabbitMQ, Redis y Mailpit reales vía Testcontainers).

---

## 2. Arquitectura

**Stack**: Kotlin 2.3 · JVM 21 · Spring Boot 4.1 (Spring MVC, Security, Data JPA, AMQP, WebSocket) · PostgreSQL + Flyway · RabbitMQ · Redis · Supabase Storage · Firebase Cloud Messaging · Docker.

**Monolito modular** (un solo despliegue, módulos Gradle con fronteras estrictas):

| Módulo | Responsabilidad | Esquema PostgreSQL |
|---|---|---|
| `app` | Ensamblado: arranque, seguridad, configuración, migraciones, OpenAPI | — |
| `common` | Tipos, eventos entre módulos, puertos (interfaces), JWT, storage, errores comunes | — |
| `user` | Registro, login, tokens, verificación de email, contraseñas, `/me` | `user_service` |
| `club` | Clubes, miembros, roles, vetos | `club_service` |
| `match` | Horario, partidos, convocatorias, equipos, rating, estadísticas | `match_service` |
| `chat` | Chats, mensajes, WebSocket, perfiles públicos y foto | `chat_service` |
| `notification` | Emails, push (Firebase), dispositivos, preferencias de silencio | `notification_service` |

Comunicación entre módulos:
- **Síncrona** por puertos en `common` (p. ej. `ClubMembershipProvider`, `UserDirectory`); ningún módulo lee tablas de otro.
- **Asíncrona** por **RabbitMQ** (exchanges `user.events`, `chat.events`, `club.events`, `match.events`), publicando solo tras confirmar la transacción.

Procesos programados (UTC):

| Proceso | Frecuencia | Qué hace |
|---|---|---|
| Planificar partidos | cada hora (min 5) | Crea el siguiente partido + convocatoria de cada club con horario activo (idempotente) |
| Cerrar convocatorias | cada 5 min | Cierra las que pasaron `closesAt` y publica equipos |
| Notificaciones de convocatoria | cada 5 min (seg 30) | Push de apertura y recordatorio de cierre (una vez) |
| Reintentos de push | cada 15 s | Reintenta envíos fallidos temporalmente |
| Ping WebSocket | cada 30 s | Mantiene vivas las conexiones de chat |
| Limpieza de tokens | diario 03:00 | Borra tokens de verificación/reset caducados |

---

## 3. Puesta en marcha y comandos

Requisitos: **JDK 21** y **Docker**.

```bash
# Infraestructura local: PostgreSQL, RabbitMQ, Redis y Mailpit
docker compose up -d

# API en http://localhost:8080 (perfil dev: Swagger + logs SQL)
JWT_SECRET_BASE64=$(openssl rand -base64 32) FIREBASE_ENABLED=false \
  ./gradlew :app:bootRun --args='--spring.profiles.active=dev'
```

- **Swagger UI** (solo `dev`): http://localhost:8080/swagger-ui.html · **OpenAPI JSON**: http://localhost:8080/v3/api-docs (útil para generar el cliente).
- **Emails** capturados en local (verificación, reset): Mailpit http://localhost:8025.
- **RabbitMQ** UI: http://localhost:15672 (guest/guest).
- Si un puerto está ocupado: `POSTGRES_PORT=55432 docker compose up -d` (también `RABBITMQ_PORT`, `REDIS_PORT`, `MAIL_PORT`, `APP_PORT`) y ajustar `DB_URL`.
- Todo en contenedores (incluida la API): `docker compose --profile app up --build`.

Otros comandos:

| Objetivo | Comando |
|---|---|
| Compilar | `./gradlew compileKotlin compileTestKotlin --console=plain` |
| Tests + build (igual que CI, requiere Docker) | `./gradlew build --console=plain` |
| Tests de un módulo | `./gradlew :match:test` |
| Imagen Docker | `docker build -t squadfy-backend .` |
| Salud | `curl localhost:8080/actuator/health` |

Base de datos: el esquema lo crean las migraciones **Flyway** (`app/src/main/resources/db/migration`, V1–V7) al arrancar; Hibernate solo valida.

---

## 4. Configuración (variables de entorno)

Toda la infraestructura se configura por variables (plantilla en `.env.example`). Los valores por defecto apuntan al `docker compose` local. En desarrollo, un fichero `.env` en la raíz del repo se **carga automáticamente** al arrancar desde IntelliJ o con `./gradlew :app:bootRun` (las variables de entorno reales tienen prioridad; la imagen Docker nunca lo incluye). Por compatibilidad, `POSTGRES_PASSWORD` y `MAILGUN_PASSWORD` siguen aceptándose como alternativa a `DB_PASSWORD` y `MAIL_PASSWORD`.

| Variable | Defecto | Uso |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | — (`prod` en la imagen Docker) | `dev`: Swagger, logs SQL, tokens de 1000 min, sin proxy |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/squadfy` / `squadfy` / `squadfy` | PostgreSQL (Supabase en remoto: `...?sslmode=require`) |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_USERNAME` / `REDIS_PASSWORD` | `localhost` / `6379` / — / — | Caché de mensajes y rate limiting |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` / `RABBITMQ_VHOST` / `RABBITMQ_SSL_ENABLED` | `localhost` / `5672` / `guest` / `guest` / `/` / `false` | Eventos. CloudAMQP: SSL `true` y puerto **5671** |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` / `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` / `MAIL_FROM` | `localhost` / `1025` / — / — / `false` / `false` / `no-reply@squadfy.local` | SMTP (Mailgun en remoto: 587, auth + starttls) |
| `JWT_SECRET_BASE64` | **obligatoria** | Secreto HMAC (≥256 bits, `openssl rand -base64 32`) |
| `JWT_EXPIRATION_MINUTES` | `15` (`1000` en `dev`) | Vida del access token |
| `APP_PUBLIC_URL` | `http://localhost:8080` | Base del enlace de verificación de email |
| `RESET_PASSWORD_URL` | `squadfy://reset-password` | Enlace del email de reset (`?token=...`): deep link de la app |
| `WEBSOCKET_ALLOWED_ORIGIN` | `http://localhost:8080` | Origen permitido en el WebSocket (solo afecta a navegadores) |
| `SUPABASE_PROJECT_URL` / `SUPABASE_SERVICE_KEY` | — | Storage de fotos y logos |
| `FIREBASE_ENABLED` | `true` | `false` = no se envían push (útil en local) |
| `FIREBASE_CREDENTIALS_PATH` | `classpath:firebase-credentials/squadfy-backend-firebase-adminsdk.json` | Service account (en Docker: `file:/run/secrets/...`) |
| `FIREBASE_ANDROID_PACKAGE` | vacío | Si se define, solo ese `applicationId` de Android recibe push |
| `FEATURE_EMAIL_VERIFICATION` | `false` (también en `prod`) | Feature flag: exigir verificar el email antes de entrar (antes `EMAIL_VERIFICATION_ENABLED`, que se sigue leyendo) |
| `FEATURE_RATE_LIMIT` | `false` (`true` en `prod`) | Feature flag: límites por cuenta y por IP de los endpoints de auth, spec 010 (antes `RATE_LIMIT_ENABLED`) |
| `NGINX_REQUIRE_PROXY` | `true` (`false` en `dev`) | Exigir IP real vía proxy de confianza |
| `DB_POOL_SIZE` | `10` | Conexiones a la BD |

### Feature flags (spec 011)
Cada función desactivable es un flag `squadfy.features.<nombre>`, configurable con `FEATURE_<NOMBRE>=true|false`. Un nombre desconocido impide arrancar (evita erratas). La app lee el estado efectivo con `GET /api/v1/features` (público).

| Flag | Defecto | Efecto |
|---|---|---|
| `email-verification` | apagado (también en `prod`) | Exige verificar el email antes de entrar |
| `rate-limit` | apagado; encendido en `prod` | Límites por cuenta y por IP de auth (spec 010) |

Para añadir uno: nueva entrada en el enum `Feature` (`common`), declararlo en `application.yml` y consultarlo con `FeatureFlags.isEnabled(...)`.

---

## 5. Convenciones de la API

- **Base**: `http(s)://<host>/api/v1`. Todo endpoint REST vive bajo `/api/v1` (un test lo garantiza). Cambios incompatibles futuros irán a `/api/v2`.
- **Autenticación**: cabecera `Authorization: Bearer <accessToken>` en todo salvo `/api/v1/auth/*` (excepto `change-password`) y `/actuator/health`.
- **Formato**: JSON UTF-8, `Content-Type: application/json` (salvo el logo del club, multipart).
- **Fechas e instantes**: cadenas **ISO-8601 en UTC** con fracción de segundo opcional, p. ej. `"2026-10-07T20:00:00Z"`, `"2026-10-07T00:13:51.413967Z"`. Las horas locales (`LocalTime`) como `"20:00:00"`. Las fechas a mostrar deben convertirse a la **zona del club** (`timeZone` del horario, p. ej. `Europe/Madrid`).
- **Identificadores**: UUID en texto. Hay que distinguir:
  - `userId`: el usuario (global).
  - `clubMemberId` / `memberId`: la ficha de ese usuario **en un club concreto** (equipos, convocatorias, estadísticas y ratings usan `clubMemberId`). Para mostrar nombre y foto, cruzar con `GET /clubs/{clubId}/members` (`id` = `clubMemberId`, `userId`, `username`, `profilePictureUrl`).
- **Nulos**: los campos opcionales se envían como `null` (no se omiten).
- **Enumerados**: por nombre (`"FIVE_A_SIDE"`, `"THURSDAY"`). Recomendado tolerar valores desconocidos en el cliente.
- **Mapas**: `minutesPlayed` es un objeto `{ "<clubMemberId>": minutos }`.
- **Códigos de estado**: `200` OK, `201` creación (`clubs`, `schedule`, `matches`, `chats`, `devices`, `events`), `204` sin contenido (salir, expulsar, vetar, borrar cuenta), `400` petición o regla inválida, `401` sin token o token inválido, `403` sin permiso, `404` no encontrado, `409` conflicto, `429` límite de peticiones.
- **Errores**: `{ "code": "...", "message": "..." }`; validación `{ "code": "VALIDATION_ERROR", "errors": ["..."] }`. Un `401` por falta de token **no tiene cuerpo**. Ver [§11](#11-errores).
- **Paginación**: solo en mensajes de chat (`before` + `pageSize`). El resto de listados son completos (tamaños de club pequeños).

---

## 6. Autenticación y sesión

### Tokens
- **Access token** (JWT): `Authorization: Bearer ...`. Vida: **15 min** (1000 min en `dev`).
- **Refresh token** (JWT): **30 días**. Se **rota** en cada `refresh` (el anterior deja de valer). `logout` lo invalida. Cambiar o restablecer la contraseña **cierra todas las sesiones** (invalida todos los refresh tokens).

### Flujos
1. **Registro**: `POST /auth/register {email, username, password}` → `UserDto`.
   - **Por defecto (también en producción) no hay que verificar el email** (feature flag `email-verification` desactivado): el usuario queda con `hasVerifiedEmail=true`, no se envía correo y puede entrar al momento. La app debe consultar `GET /features` y, con el flag apagado, ir directamente al login (o loguear tras registrar) sin mostrar «revisa tu email».
   - Con `FEATURE_EMAIL_VERIFICATION=true`: `hasVerifiedEmail=false` y se envía un correo con el enlace `{APP_PUBLIC_URL}/account/verify-email?token=...` (24 h). Es una página web que verifica el email y dice si se ha verificado, si el enlace caducó o si ya se usó.
2. **Login**: `POST /auth/login {email, password}` → `{user, accessToken, refreshToken}`. Solo con `email-verification` activado y el email sin verificar → **403 `EMAIL_NOT_VERIFIED`** (ofrecer "reenviar email": `POST /auth/resend-verification {email}`).
3. **Refresh**: `POST /auth/refresh {refreshToken}` → nuevo par de tokens. Token inválido/caducado/ya usado → **401 `INVALID_TOKEN`** (mandar al login).
4. **Logout**: `POST /auth/logout {refreshToken}` (y `DELETE /devices/{token}` para dejar de recibir push).
5. **Olvidé la contraseña**: `POST /auth/forgot-password {email}` (responde 200 aunque el email no exista). El email abre `RESET_PASSWORD_URL?token=...` (por defecto el deep link **`squadfy://reset-password?token=...`**, 30 min de validez). La app captura el deep link, pide la nueva contraseña y llama `POST /auth/reset-password {token, newPassword}`.
6. **Cambiar contraseña** (con sesión): `POST /auth/change-password {oldPassword, newPassword}`. Antigua incorrecta → 401 `INVALID_CREDENTIALS`; igual a la nueva → 409 `SAME_PASSWORD`.
7. **Mi perfil**: `GET /me` → `UserDto` (incluye el email; es el único sitio donde se expone).
8. **Borrar mi cuenta** (spec 010; obligatorio en App Store y Google Play):
   - Desde la app: `DELETE /me {password}` → **204**. Contraseña incorrecta → 401 `INVALID_CREDENTIALS`. Tras el 204, borrar los tokens locales y volver al inicio.
   - Desde la web (sin la app): **`/account/delete`**, página pública con formulario (email + contraseña + confirmación) que llama a `POST /auth/delete-account {email, password}`. **Esta es la URL que hay que dar a Google Play** (`https://<dominio>/account/delete`).
   - Es **inmediato e irreversible**: se borran email, nombre, contraseña, foto, sesiones (también los access tokens ya emitidos, que pasan a dar 401), dispositivos, preferencias de notificación, ausencias y todos sus mensajes. El email y el nombre de usuario quedan libres.
   - Se **conserva anonimizado** su historial de club («Usuario eliminado …») para que marcadores y estadísticas de los demás cuadren. Sale de todos sus clubes (y de las convocatorias abiertas, con sus invitados) y de todos sus chats.
   - Clubes de los que era **OWNER**: pasan al ADMIN activo más antiguo o, si no hay, al miembro activo más antiguo. Si era el único miembro, el club se elimina con todos sus datos.

### Validaciones
- `email` válido; `username` 3–20 caracteres (único); `password` ≥ 8 caracteres con al menos un dígito o carácter especial.
- **Rate limiting** (feature flag `rate-limit`, activo en `prod`; spec 010). Los límites son **por cuenta**, así que varios jugadores en el mismo wifi no se bloquean entre sí; el límite por IP es solo una red de seguridad:

  | Operación | Por cuenta | Por IP |
  |---|---|---|
  | `refresh` | 60/h por usuario del token | 300/h; tokens inválidos o caducados 30/h |
  | `login` | 10/h por email | 300/h |
  | `register` | — | 50/h |
  | `DELETE /me` · `POST /auth/delete-account` | 5/h por usuario · por email | — · 50/h |
  | `resend-verification` · `forgot-password` | por email | 10/h |

  Al superarlo → **429 `RATE_LIMIT_EXCEEDED`** con la cabecera **`Retry-After`** (segundos). La app debe esperar ese tiempo y no reintentar en bucle.

---

## 7. Reglas de negocio por dominio

### 7.1 Clubes y miembros
- Quien crea el club es su **OWNER** (uno por club). Roles: `OWNER`, `ADMIN`, `CAPTAIN`, `PLAYER`. **Gestor = OWNER o ADMIN**. El capitán **no** tiene permisos de gestión.
- Código de invitación: 8 caracteres alfanuméricos en mayúsculas, visible para todos los miembros; los gestores pueden regenerarlo (el anterior deja de funcionar). Al unirse no se distinguen mayúsculas.
- Unirse: con código válido; no dos veces (409); si el club tiene `maxMembers` y está lleno → 409.
- Posición opcional: `GOALKEEPER`, `DEFENDER`, `MIDFIELDER`, `FORWARD`. Cada miembro edita su dorsal y posición.
- **Jerarquía**: el OWNER actúa sobre cualquiera y asigna `ADMIN`/`CAPTAIN`/`PLAYER`; un ADMIN solo actúa sobre `CAPTAIN`/`PLAYER` y solo alterna entre esos dos (no concede ni quita `ADMIN`). Nadie se expulsa ni cambia su propio rol. El rol `OWNER` solo cambia con **transferencia** (el antiguo owner pasa a `ADMIN`).
- **Salir**: cualquier miembro salvo el owner (409 `CONFLICT` "transfer ownership first").
- **Expulsar**: según la jerarquía. Puede volver con el código.
- **Vetar**: como expulsar, pero **no puede volver** (403 `BANNED_FROM_CLUB` al unirse) hasta que un gestor levante el veto. También se puede vetar a quien ya salió.
- Al salir/ser expulsado/vetado se **conserva su historial y rating**; deja de verse en el club y **sale de las convocatorias abiertas** (si estaba confirmado, sube el primero de la lista de espera). Si vuelve, recupera la misma ficha (`clubMemberId`) con rol `PLAYER`.
- Privacidad: el **email no se muestra** al resto de miembros.

### 7.2 Horario semanal y planificación
- Un horario por club: día de la semana, hora local, **zona horaria IANA** (defecto `Europe/Madrid`), **formato** (`FIVE_A_SIDE`=10 plazas, `SEVEN_A_SIDE`=14, `ELEVEN_A_SIDE`=22) y **duración** (10–180 min, defecto 60).
- Al crear (o reactivar) el horario se planifica **inmediatamente** el primer partido con su convocatoria (abierta ya).
- Después, cada hora: si el club no tiene un partido programado futuro y no existe partido (ni cancelado) en la siguiente fecha que toca, se crea. Un partido cancelado **no se recrea**. Cambiar el día/hora aplica desde el siguiente partido planificado.
- Los gestores pueden crear **partidos extra** manuales (fecha futura, formato y duración opcionales) y **cancelar** partidos (no los ya cerrados).
- **Hora de cierre y hora del sorteo**: `closeDaysBefore` + `closeTime` (defecto 1 día antes a las 22:00) y `drawDaysBefore` + `drawTime` (defecto: igual que el cierre), en hora local del club. El cierre debe ser anterior al inicio y el sorteo igual o posterior al cierre y anterior al inicio; si no → 400. Aplica a los partidos que se planifiquen después del cambio; los partidos extra usan la configuración del horario.
- **Excepciones del calendario** (una por fecha; las ven todos los miembros, las crean/borran los gestores). `date` es el día de partido de esa semana (debe coincidir con `matchDayOfWeek` y ser futuro):
  - `CANCELLED`: esa semana no hay partido. Si aún no estaba planificado, la planificación la salta y crea el de la semana siguiente; si ya lo estaba, se cancela como una cancelación normal (push `match.cancelled` a los apuntados) y se planifica la siguiente semana.
  - `RESCHEDULED` + `newScheduledAt` (futuro): el partido de esa semana se juega en otra fecha/hora. Si ya estaba planificado se **mueve** conservando inscripciones, se recalculan cierre y sorteo (una convocatoria ya cerrada se reabre si el nuevo cierre es futuro) y se avisa a todos los miembros (push `match.rescheduled`).
  - **Borrar** una excepción la deshace mientras la semana no se haya jugado: un partido cancelado por ella se reactiva con sus inscripciones; uno movido vuelve a su fecha habitual.
- `MatchDto.scheduleDate` indica a qué semana del horario pertenece un partido (`null` en partidos extra).

### 7.3 Convocatoria (announcement) y lista de espera
- `opensAt`: inicio del día siguiente al último partido no cancelado (o "ahora" si no hay o ya pasó).
- `closesAt`: la **hora de cierre** del horario (por defecto **22:00 del día anterior al partido**, hora del club). Si el partido se crea después de ese corte, cierra a la hora del partido.
- `drawAt`: la **hora del sorteo** (nunca antes de `closesAt` ni después del inicio).
- Solo miembros, solo con estado `OPEN` y dentro de `[opensAt, closesAt)`; fuera de la ventana → 400 `BAD_REQUEST`. **Tras el cierre nadie puede apuntarse ni desapuntarse.**
- Apuntarse: `CONFIRMED` si hay plazas, si no `WAITLISTED` (por orden de inscripción). No dos veces (409).
- Desapuntarse un confirmado → **el primero en espera pasa a confirmado** automáticamente (y recibe push). Desapuntarse de la espera no promociona a nadie.
- El estado `status` puede seguir `OPEN` hasta 5 min después de `closesAt` (lo cierra un proceso): **la app debe considerar abierta la convocatoria solo si `status == OPEN && opensAt <= ahora < closesAt`**.
- Cancelar el partido cancela su convocatoria (`CANCELLED`).
- **Invitados**: cualquier miembro añade invitados (nombre obligatorio ≤ 80, posición opcional) dentro de la ventana de inscripción, **máximo 2 por miembro** y convocatoria (409). El invitado no depende de que su anfitrión esté apuntado. Los quita quien lo invitó o un gestor (403 si no). **Los miembros tienen prioridad**: las plazas se asignan primero a los miembros por orden de inscripción y después a los invitados por orden de alta; si se apunta un miembro sin hueco, el último invitado confirmado pasa a la espera. La espera se ordena igual (miembros primero). Si el anfitrión sale del club, sus invitados se retiran de las convocatorias abiertas. Los invitados no reciben push y su anfitrión no recibe avisos de sus cambios de estado.
- **Ausencias**: cada miembro registra periodos `fromDate`–`toDate` (fechas locales del club, hasta 1 año, que acaben hoy o después) y los borra. Al registrarla, se le **desapunta** de las convocatorias **abiertas** de partidos en ese periodo (sube la espera); no recibe la push de apertura ni el recordatorio de esos partidos. La ausencia es informativa: **puede volver a apuntarse** si al final viene. Todos los miembros ven las ausencias del club.

### 7.4 Equipos
- A la **hora del sorteo** (`drawAt`) se publican **automáticamente** dos equipos con los `CONFIRMED`, miembros e **invitados** (la lista de espera no juega). Si un gestor ya había sorteado exactamente con esos participantes, se respetan.
- Los invitados juegan el sorteo con nivel neutro (1000) y su posición si la indicaron; en `MatchDto` aparecen en `teamAGuests`/`teamBGuests`.
- Algoritmo: tamaños con diferencia ≤ 1; porteros repartidos; cada posición repartida; se minimiza la diferencia de nivel (rating) con intercambios; con niveles iguales el sorteo es aleatorio.
- Los gestores pueden **rectificar** en cualquier momento antes de cerrar el partido: `AUTO` (nuevo sorteo) o `MANUAL` (equipos indicados con `clubMemberId` de miembros y/o `guestId` de invitados, solo confirmados, disjuntos, diferencia ≤ 1).
- Los gestores ven el **equilibrio** de los equipos: rating medio/total por equipo, rating de cada jugador (los invitados con `isGuest = true` y 1000) y resultado esperado del equipo A (0–1; 0,5 = igualado).

### 7.5 Resultado, minutos, rating y estadísticas
- El gestor registra **eventos** (gol, asistencia, amarilla, roja; minuto opcional 1–120) solo de jugadores de los equipos y solo con el partido `SCHEDULED`.
- **Minutos**: cada jugador suma la duración del partido salvo que un gestor fije sus minutos (0–duración) antes de cerrar.
- **Marcador manual**: el gestor puede fijar el marcador final (`PUT /matches/{id}/score`, 0–99) o quitarlo (`DELETE`) mientras el partido está `SCHEDULED`. Si existe, **es el oficial** (`isManualScore = true`): decide victoria/empate/derrota en rating y estadísticas. Los goles registrados siguen contando para el goleador aunque no cuadren con el marcador (los de invitados solo se reflejan en el marcador).
- **Cerrar** (`complete`): solo `SCHEDULED`, con la hora de inicio pasada y con equipos. El marcador = el manual si existe; si no, goles registrados por equipo. Actualiza el rating.
- **Reabrir** (`reopen`): solo el **último** partido cerrado del club; revierte sus cambios de rating y permite corregir eventos y minutos; luego se vuelve a cerrar.
- **Rating** (Elo): empieza en 1000; sube/baja según el resultado esperado vs real del equipo, la diferencia de goles y la aportación individual (goles, asistencias, tarjetas, suma cero). Provisional hasta 10 partidos. **Público en el club** (clasificación); cada jugador ve su posición. No hay valoración manual: la "valoración del partido" de cada miembro es su variación de rating, en `MatchDto.ratingChanges` (partidos cerrados). Los invitados cuentan en la media de su equipo con 1000, pero no tienen rating ni estadísticas.
- **Estadísticas**: partidos, victorias, empates, derrotas, goles, asistencias, amarillas, rojas y minutos, calculadas de los partidos **cerrados**. Clasificación ordenable por `GOALS`, `ASSISTS`, `MATCHES`, `MINUTES`, `WINS`; incluye a todos los miembros (con ceros). Los empates comparten posición (1, 2, 2, 4).

### 7.6 Notificaciones
Solo **push** (FCM). Detalle en [§13](#13-notificaciones-push-firebase). Cada usuario puede **silenciar un club** (salvo el aviso de "tienes plaza").

### 7.7 Chat
- Los participantes de chat se crean al **verificar el email**; solo usuarios verificados pueden chatear.
- Un chat tiene ≥ 2 participantes; cualquier participante puede añadir a otros; cada uno puede salir.
- Los mensajes se **envían por WebSocket**; el historial se pide por REST (paginado). Cada usuario solo borra sus propios mensajes.

---

## 8. Referencia de endpoints

Permisos: **público** (sin token), **auth** (cualquier usuario autenticado), **miembro** (del club), **gestor** (OWNER/ADMIN del club), **owner**, **participante** (del chat). Todos los paths cuelgan de `/api/v1`.

### 8.1 Auth y perfil
| Método | Ruta | Permiso | Cuerpo | Respuesta |
|---|---|---|---|---|
| POST | `/auth/register` | público | `{email, username, password}` | `UserDto` |
| POST | `/auth/login` | público | `{email, password}` | `AuthenticatedUserDto` |
| POST | `/auth/refresh` | público | `{refreshToken}` | `AuthenticatedUserDto` |
| POST | `/auth/logout` | público | `{refreshToken}` | 200 vacío |
| POST | `/auth/resend-verification` | público | `{email}` | 200 vacío |
| GET | `/auth/verify?token=` | público | — | 200 vacío (lo llama la página `/account/verify-email`; enlaces antiguos) |
| GET | `/features` | público | — | `{"email-verification": false, "rate-limit": true}` (feature flags del entorno) |
| POST | `/auth/forgot-password` | público | `{email}` | 200 vacío |
| POST | `/auth/reset-password` | público | `{token, newPassword}` | 200 vacío |
| POST | `/auth/change-password` | auth | `{oldPassword, newPassword}` | 200 vacío |
| POST | `/auth/delete-account` | público | `{email, password}` | **204** (formulario web de borrado) |
| GET | `/me` | auth | — | `UserDto` (con email) |
| DELETE | `/me` | auth | `{password}` | **204** (borra la cuenta) |
| GET | `/users?query=` | auth | — | `ChatParticipantDto` (búsqueda exacta por username o email; 404 si no existe) |
| GET | `/users/{userId}` | auth | — | `ChatParticipantDto` (perfil público: nombre y foto) |
| POST | `/me/profile-picture/upload-url?mimeType=` | auth | — | `PictureUploadResponse` |
| PUT | `/me/profile-picture` | auth | `{publicUrl}` | 200 |
| DELETE | `/me/profile-picture` | auth | — | 200 |

Foto de perfil: `mimeType` ∈ `image/jpeg`, `image/png`, `image/webp`. 1) pedir `upload-url`; 2) subir los bytes a `uploadUrl` (PUT) con las `headers` indicadas antes de `expiresAt` (5 min); 3) confirmar con `PUT /me/profile-picture {publicUrl}`. Los contactos de chat reciben `PROFILE_PICTURE_UPDATED` por WebSocket.

### 8.2 Clubes y miembros
| Método | Ruta | Permiso | Cuerpo | Respuesta |
|---|---|---|---|---|
| GET | `/clubs` | auth | — | `ClubDto[]` (mis clubes) |
| POST | `/clubs` | auth | `{name, description?, maxMembers?, clubLogoUrl?}` | **201** `ClubDto` |
| POST | `/clubs/join` | auth | `{invitationCode, shirtNumber?, position?}` | `ClubDto` |
| GET | `/clubs/{clubId}` | miembro | — | `ClubDto` |
| PATCH | `/clubs/{clubId}` | gestor | `{name?, description?, maxMembers?}` | `ClubDto` |
| PUT | `/clubs/{clubId}/logo` | gestor | multipart, parte `clubLogo` (jpeg/png/webp) | `ClubDto` |
| POST | `/clubs/{clubId}/invitation-code` | gestor | — | `{invitationCode}` |
| POST | `/clubs/{clubId}/transfer-ownership` | owner | `{memberId}` | `ClubDto` |
| GET | `/clubs/{clubId}/members` | miembro | — | `ClubMemberDto[]` (activos) |
| PATCH | `/clubs/{clubId}/members/me` | miembro | `{shirtNumber?, position?}` | `ClubMemberDto` |
| DELETE | `/clubs/{clubId}/members/me` | miembro (no owner) | — | **204** |
| DELETE | `/clubs/{clubId}/members/{memberId}` | gestor (jerarquía) | — | **204** |
| PATCH | `/clubs/{clubId}/members/{memberId}/role` | gestor (jerarquía) | `{role}` | `ClubMemberDto` |
| GET | `/clubs/{clubId}/bans` | gestor | — | `ClubBanDto[]` |
| POST | `/clubs/{clubId}/members/{memberId}/ban` | gestor (jerarquía) | — | **204** |
| DELETE | `/clubs/{clubId}/members/{memberId}/ban` | gestor (jerarquía) | — | **204** |
| GET | `/clubs/{clubId}/absences?from=&to=` | miembro | — | `MemberAbsenceDto[]` (que se solapan con `from`–`to`; ambos opcionales, `YYYY-MM-DD`) |
| POST | `/clubs/{clubId}/members/me/absences` | miembro | `{fromDate, toDate, reason?}` | **201** `MemberAbsenceDto` |
| DELETE | `/clubs/{clubId}/members/me/absences/{absenceId}` | miembro (propia) | — | **204** (404 si no es suya) |

### 8.3 Horario y partidos
| Método | Ruta | Permiso | Cuerpo | Respuesta |
|---|---|---|---|---|
| GET | `/clubs/{clubId}/schedule` | miembro | — | `ClubMatchScheduleDto` (404 si no hay) |
| POST | `/clubs/{clubId}/schedule` | gestor | `{matchDayOfWeek, matchTime, timeZone?, format?, matchDurationMinutes?, closeDaysBefore?, closeTime?, drawDaysBefore?, drawTime?}` | **201** `ClubMatchScheduleDto` |
| PATCH | `/clubs/{clubId}/schedule` | gestor | `{matchDayOfWeek?, matchTime?, timeZone?, format?, matchDurationMinutes?, isActive?, closeDaysBefore?, closeTime?, drawDaysBefore?, drawTime?}` | `ClubMatchScheduleDto` |
| GET | `/clubs/{clubId}/schedule/exceptions` | miembro | — | `ScheduleExceptionDto[]` (por fecha) |
| POST | `/clubs/{clubId}/schedule/exceptions` | gestor | `{date, type: CANCELLED\|RESCHEDULED, newScheduledAt?, reason?}` | **201** `ScheduleExceptionDto` (409 si la fecha ya tiene una) |
| DELETE | `/clubs/{clubId}/schedule/exceptions/{exceptionId}` | gestor | — | **204** (409 si la semana ya se jugó) |
| GET | `/clubs/{clubId}/matches?status=` | miembro | — | `MatchDto[]` (más reciente primero; `status` opcional) |
| POST | `/clubs/{clubId}/matches` | gestor | `{scheduledAt, format?, durationMinutes?}` | **201** `MatchDto` |
| GET | `/matches/{matchId}` | miembro | — | `MatchDto` |
| POST | `/matches/{matchId}/cancel` | gestor | — | `MatchDto` |
| PUT | `/matches/{matchId}/score` | gestor | `{teamAScore, teamBScore}` | `MatchDto` (solo `SCHEDULED`) |
| DELETE | `/matches/{matchId}/score` | gestor | — | `MatchDto` (vuelve al marcador de los eventos) |
| POST | `/matches/{matchId}/complete` | gestor | — | `MatchDto` |
| POST | `/matches/{matchId}/reopen` | gestor | — | `MatchDto` |
| POST | `/matches/{matchId}/teams` | gestor | `{mode: AUTO\|MANUAL, manualTeamA?, manualTeamB?}` (ids de miembro o de invitado) | `MatchDto` |
| GET | `/matches/{matchId}/team-balance` | gestor | — | `TeamBalanceDto` (409 sin equipos) |
| PUT | `/matches/{matchId}/players/{memberId}/minutes` | gestor | `{minutes}` | `MatchDto` |
| POST | `/matches/{matchId}/events` | gestor | `{clubMemberId, type, minute?}` | **201** `MatchDto` |
| DELETE | `/matches/{matchId}/events/{eventId}` | gestor | — | `MatchDto` |

### 8.4 Convocatorias
| Método | Ruta | Permiso | Cuerpo | Respuesta |
|---|---|---|---|---|
| GET | `/clubs/{clubId}/announcements/current` | miembro | — | `CurrentMatchAnnouncementDto` (404 si no hay partido programado) |
| GET | `/clubs/{clubId}/announcements` | miembro | — | `MatchAnnouncementDto[]` (más reciente primero) |
| GET | `/announcements/{announcementId}` | miembro | — | `MatchAnnouncementDto` |
| GET | `/matches/{matchId}/announcement` | miembro | — | `MatchAnnouncementDto` |
| POST | `/announcements/{announcementId}/enrollment` | miembro | — | `MatchAnnouncementDto` |
| DELETE | `/announcements/{announcementId}/enrollment` | miembro | — | `MatchAnnouncementDto` |
| POST | `/announcements/{announcementId}/guests` | miembro | `{name, position?}` | `MatchAnnouncementDto` (409 si ya tiene 2) |
| DELETE | `/announcements/{announcementId}/guests/{guestId}` | anfitrión o gestor | — | `MatchAnnouncementDto` |

### 8.5 Rating y estadísticas
| Método | Ruta | Permiso | Respuesta |
|---|---|---|---|
| GET | `/clubs/{clubId}/ratings` | miembro | `RatingLeaderboardEntryDto[]` (clasificación por rating) |
| GET | `/clubs/{clubId}/ratings/me` | miembro | `PlayerRatingDto` |
| GET | `/clubs/{clubId}/stats?sortBy=GOALS\|ASSISTS\|MATCHES\|MINUTES\|WINS` | miembro | `ClubStatsEntryDto[]` (defecto `GOALS`) |
| GET | `/clubs/{clubId}/stats/me` | miembro | `PlayerStatsDto` |

### 8.6 Chat
| Método | Ruta | Permiso | Cuerpo | Respuesta |
|---|---|---|---|---|
| GET | `/chats` | auth | — | `ChatDto[]` (mis chats) |
| POST | `/chats` | auth | `{otherUserIds: [userId]}` | **201** `ChatDto` |
| GET | `/chats/{chatId}` | participante | — | `ChatDto` (404 si no participas) |
| GET | `/chats/{chatId}/messages?before=&pageSize=` | participante | — | `ChatMessageDto[]` (más antiguo primero; `before` = instante ISO; `pageSize` 1–100, defecto 20; 403 si no participas) |
| POST | `/chats/{chatId}/participants` | participante | `{userIds: [userId]}` | `ChatDto` |
| DELETE | `/chats/{chatId}/participants/me` | participante | — | 200 |
| DELETE | `/messages/{messageId}` | autor | — | 200 |

### 8.7 Notificaciones y dispositivos
| Método | Ruta | Permiso | Cuerpo | Respuesta |
|---|---|---|---|---|
| POST | `/devices` | auth | `{token, platform: ANDROID\|IOS}` | **201** `DeviceTokenDto` |
| DELETE | `/devices/{token}` | auth (propio) | — | **204**; 404 si no existe o es de otro usuario |
| GET | `/clubs/{clubId}/notification-settings` | miembro | — | `{clubId, muted}` |
| PUT | `/clubs/{clubId}/notification-settings` | miembro | `{muted}` | `{clubId, muted}` |

### 8.8 Operación
| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` | público | Salud (`{"status":"UP"}`) |
| GET | `/actuator/info` | público | Versión y fecha de build |
| GET | `/v3/api-docs`, `/swagger-ui.html` | público, **solo `dev`** | Contrato OpenAPI |

---

## 9. Modelos (DTOs)

Notación: `?` = puede ser `null`. `Instant` = ISO-8601 UTC. IDs = UUID en texto.

### Usuario y sesión
```kotlin
UserDto(id: UserId, email: String, username: String, hasVerifiedEmail: Boolean)
AuthenticatedUserDto(user: UserDto, accessToken: String, refreshToken: String)
ChatParticipantDto(userId: UserId, username: String, profilePictureUrl: String?)   // perfil público
PictureUploadResponse(uploadUrl: String, publicUrl: String, headers: Map<String, String>, expiresAt: Instant)
```

### Club
```kotlin
ClubDto(id, name, description?, clubLogoUrl?, ownerId: UserId, invitationCode, maxMembers?, membersCount, createdAt, updatedAt)
ClubMemberDto(id: ClubMemberId, clubId, userId, username, profilePictureUrl?, shirtNumber?, position: String?, role: ClubMemberRole, createdAt, updatedAt)
ClubBanDto(clubMemberId, userId, username, bannedAt: Instant)
InvitationCodeDto(invitationCode: String)
```
`position` es el nombre del enum (`"FORWARD"`…) o `null`; datos antiguos podrían contener texto libre: tratar valores desconocidos como "sin posición".

### Horario y partido
```kotlin
ClubMatchScheduleDto(id, clubId, matchDayOfWeek: DayOfWeek, matchTime: "HH:mm:ss", timeZone: String, format: MatchFormat,
                     maxPlayers: Int, matchDurationMinutes: Int,
                     closeDaysBefore: Int, closeTime: "HH:mm:ss", drawDaysBefore: Int, drawTime: "HH:mm:ss",
                     isActive: Boolean, createdAt, updatedAt)
ScheduleExceptionDto(id, clubId, date: LocalDate, type: ExceptionType, newScheduledAt: Instant?, reason: String?, createdAt)
MemberAbsenceDto(id, clubId, clubMemberId, fromDate: LocalDate, toDate: LocalDate, reason: String?, createdAt)

MatchDto(
  id, clubId, scheduledAt: Instant, status: MatchStatus,
  enrolledPlayers: List<ClubMemberId>,           // confirmados de la convocatoria
  teamA: List<ClubMemberId>, teamB: List<ClubMemberId>,   // vacíos hasta publicar equipos
  durationMinutes: Int, minutesPlayed: Map<ClubMemberId, Int>,  // minutos efectivos de cada jugador de los equipos
  enrolledGuests: List<MatchGuestDto>,           // invitados confirmados
  teamAGuests: List<MatchGuestDto>, teamBGuests: List<MatchGuestDto>,
  teamAScore: Int, teamBScore: Int,              // marcador oficial: manual si isManualScore, si no goles registrados
  isManualScore: Boolean,
  ratingChanges: Map<ClubMemberId, Int>,         // variación de rating de cada miembro (vacío hasta cerrar)
  scheduleDate: LocalDate?,                      // semana del horario; null en partidos extra
  goals: List<MatchEventDto>, assists: List<MatchEventDto>, yellowCards: List<MatchEventDto>, redCards: List<MatchEventDto>,
  createdAt, updatedAt
)
MatchGuestDto(guestId: UUID, name: String, position: PlayerPosition?, invitedByMemberId: ClubMemberId?)
MatchEventDto(id, matchId, clubMemberId, type: MatchEventType, minute: Int?, createdAt)
TeamBalanceDto(matchId, teamA: TeamStrengthDto, teamB: TeamStrengthDto, averageRatingDifference: Int, teamAExpectedScore: Double)
TeamStrengthDto(players: Int, averageRating: Int, totalRating: Int, playerRatings: List<TeamPlayerRatingDto>)  // mayor rating primero
TeamPlayerRatingDto(clubMemberId, rating: Int, isGuest: Boolean)   // en invitados clubMemberId = guestId
```

### Convocatoria
```kotlin
MatchAnnouncementDto(
  id, matchId, clubId, maxPlayers: Int, confirmedCount: Int, waitlistCount: Int,
  opensAt: Instant, closesAt: Instant, drawAt: Instant, status: MatchAnnouncementStatus,
  entries: List<MatchAnnouncementEntryDto>,   // confirmados: miembros y después invitados, por orden de inscripción
  waitlist: List<MatchAnnouncementEntryDto>,  // en espera, por orden de promoción (miembros primero)
  createdAt, updatedAt
)
MatchAnnouncementEntryDto(
  id,                                  // en invitados es el guestId
  matchAnnouncementId, participantType: ParticipantType,
  clubMemberId: ClubMemberId?,         // null en invitados
  guestName: String?, guestPosition: PlayerPosition?, invitedByMemberId: ClubMemberId?,
  status: EntryStatus, enrolledAt: Instant
)
CurrentMatchAnnouncementDto(announcement: MatchAnnouncementDto, matchScheduledAt: Instant, myStatus: MyEnrollmentStatus, myWaitlistPosition: Int?)
```

### Rating y estadísticas
```kotlin
RatingLeaderboardEntryDto(rank, clubMemberId, rating: Int, matchesRated: Int, isProvisional: Boolean)
PlayerRatingDto(clubId, clubMemberId, rating: Int, matchesRated: Int, isProvisional: Boolean, rank: Int, totalPlayers: Int)
ClubStatsEntryDto(rank, clubMemberId, matchesPlayed, wins, draws, losses, goals, assists, yellowCards, redCards, minutesPlayed)
PlayerStatsDto(clubMemberId, matchesPlayed, wins, draws, losses, goals, assists, yellowCards, redCards, minutesPlayed)
```

### Chat y dispositivos
```kotlin
ChatDto(id: ChatId, participants: List<ChatParticipantDto>, lastActivityAt: Instant, lastMessage: ChatMessageDto?, creator: ChatParticipantDto)
ChatMessageDto(id: ChatMessageId, chatId, content: String, createdAt: Instant, senderId: UserId)
DeviceTokenDto(userId, token, createdAt)
ClubNotificationSettingsDto(clubId, muted: Boolean)
```

### Validaciones de peticiones
| Campo | Regla |
|---|---|
| `CreateClubRequest.name` | obligatorio, ≤ 120 |
| `description` | ≤ 2000 |
| `maxMembers` | > 0 (al editar, ≥ miembros actuales) |
| `invitationCode` | alfanumérico 6–12 |
| `shirtNumber` | 1–999 |
| `matchDurationMinutes` / `durationMinutes` | 10–180 |
| `timeZone` | zona IANA válida (`Europe/Madrid`) |
| `scheduledAt` (partido manual) | futuro |
| `closeDaysBefore` / `drawDaysBefore` | 0–6 (y reglas de §7.2) |
| `AddGuestRequest.name` | obligatorio, ≤ 80 |
| `teamAScore` / `teamBScore` | 0–99 |
| `reason` (excepción / ausencia) | ≤ 200 |
| `fromDate` ≤ `toDate` (ausencia) | `toDate` ≥ hoy; ≤ 1 año |
| `minute` (evento) | 1–120 |
| `minutes` (jugador) | 0–duración del partido |
| `CreateChatRequest.otherUserIds` | ≥ 1 (chat de ≥ 2 participantes) |

### Ejemplos reales
```json
// POST /api/v1/clubs/{clubId}/schedule  →  201
{"id":"40117700-cc62-45b2-a6d3-50c0b633c6dc","clubId":"c9080e51-2da6-43bb-acb5-c0271b4dd1c2",
 "matchDayOfWeek":"THURSDAY","matchTime":"20:00:00","timeZone":"Europe/Madrid","format":"FIVE_A_SIDE",
 "maxPlayers":10,"matchDurationMinutes":60,"isActive":true,
 "createdAt":"2026-10-07T00:13:51.435223Z","updatedAt":"2026-10-07T00:13:51.435225Z"}

// GET /api/v1/clubs/{clubId}/announcements/current  →  200   (closesAt 20:00Z = 22:00 en Madrid)
{"announcement":{"id":"90dd41a6-b0e5-4243-a735-c79d7226ede2","matchId":"d38f9fd7-447e-4e10-bfd4-395bfbead889",
  "clubId":"c9080e51-2da6-43bb-acb5-c0271b4dd1c2","maxPlayers":10,"confirmedCount":0,"waitlistCount":0,
  "opensAt":"2026-10-07T00:13:51.446611Z","closesAt":"2026-10-07T20:00:00Z","status":"OPEN",
  "entries":[],"waitlist":[],"createdAt":"2026-10-07T00:13:51.447442Z","updatedAt":"2026-10-07T00:13:51.447444Z"},
 "matchScheduledAt":"2026-10-08T18:00:00Z","myStatus":"NOT_ENROLLED","myWaitlistPosition":null}

// Error de validación  →  400
{"code":"VALIDATION_ERROR","errors":["matchDurationMinutes cannot exceed 180"]}
```

---

## 10. Enumerados

| Enum | Valores |
|---|---|
| `ClubMemberRole` | `OWNER`, `ADMIN`, `CAPTAIN`, `PLAYER` |
| `PlayerPosition` | `GOALKEEPER`, `DEFENDER`, `MIDFIELDER`, `FORWARD` |
| `MatchFormat` | `FIVE_A_SIDE` (10 plazas), `SEVEN_A_SIDE` (14), `ELEVEN_A_SIDE` (22) |
| `DayOfWeek` | `MONDAY` … `SUNDAY` |
| `MatchStatus` | `SCHEDULED`, `IN_PROGRESS` (no se usa), `COMPLETED`, `CANCELLED` |
| `MatchAnnouncementStatus` | `OPEN`, `CLOSED`, `CANCELLED` |
| `EntryStatus` | `CONFIRMED`, `WAITLISTED` |
| `ParticipantType` | `MEMBER`, `GUEST` |
| `ExceptionType` | `CANCELLED`, `RESCHEDULED` |
| `MyEnrollmentStatus` | `NOT_ENROLLED`, `CONFIRMED`, `WAITLISTED` |
| `MatchEventType` | `GOAL`, `ASSIST`, `YELLOW_CARD`, `RED_CARD` |
| `TeamGenerationMode` | `AUTO`, `MANUAL` |
| `StatsSortBy` | `GOALS`, `ASSISTS`, `MATCHES`, `MINUTES`, `WINS` |
| `Platform` | `ANDROID`, `IOS` |

---

## 11. Errores

Formato: `{ "code": "...", "message": "..." }`. El `message` está en inglés y es orientativo; **la app debe decidir por `code` + estado HTTP** y mostrar sus propios textos.

| HTTP | `code` | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Campos inválidos (`errors: [...]`) |
| 400 | `INVALID_REQUEST` | JSON mal formado, enum o UUID inválido |
| 400 | `BAD_REQUEST` | Regla de negocio: convocatoria cerrada o no abierta, sorteo inválido, evento/minutos inválidos, zona horaria desconocida, acción sobre uno mismo, `maxMembers` menor que los miembros |
| 400 | `INVALID_INVITATION_CODE` | Código de invitación inexistente |
| 400 | `INVALID_CHAT_SIZE` / `INVALID_PROFILE_PICTURE` / `INVALID_DEVICE_TOKEN` | Chat < 2 participantes / tipo de imagen / token FCM inválido |
| 401 | *(sin cuerpo)* | Falta el token o es inválido/caducado → refrescar o login |
| 401 | `INVALID_CREDENTIALS` | Login o contraseña actual incorrectos |
| 401 | `INVALID_TOKEN` | Refresh, verificación o reset con token inválido/caducado/usado |
| 403 | `FORBIDDEN` | Sin permiso (no gestor, jerarquía, no participante del chat) |
| 403 | `NOT_CLUB_MEMBER` | Operación de partidos/convocatorias en un club del que no eres miembro |
| 403 | `BANNED_FROM_CLUB` | Unirse a un club que te ha vetado |
| 403 | `EMAIL_NOT_VERIFIED` | Login sin verificar el email (solo con el flag `email-verification` activado) |
| 404 | `NOT_FOUND` / `USER_NOT_FOUND` | Recurso inexistente |
| 409 | `CONFLICT` | Ya apuntado, ya miembro, club lleno, horario ya existe, estado del partido no permite la acción, owner que intenta salir, datos duplicados |
| 409 | `USER_EXITS` | Registro con email o username ya usados (sic, ver §16) |
| 409 | `SAME_PASSWORD` | La nueva contraseña es igual a la anterior |
| 429 | `RATE_LIMIT_EXCEEDED` | Demasiadas peticiones de auth para esa cuenta (o IP); cabecera `Retry-After` |
| 500 | `STORAGE_ERROR` | Fallo subiendo/borrando en Supabase Storage |

---

## 12. Chat en tiempo real (WebSocket)

- **URL**: `ws://<host>/ws/chat` (`wss://` en producción).
- **Autenticación**: cabecera **`Authorization: Bearer <accessToken>`** en el handshake (no hay token por query string). Sin token válido el handshake se rechaza (401). Al caducar el access token, la conexión abierta sigue viva; al reconectar hay que usar un token válido.
- **Keep-alive**: el servidor envía *ping* cada 30 s y cierra la conexión si no recibe *pong* en 60 s (los clientes WebSocket estándar, incluido Ktor, responden *pong* automáticamente).
- Al conectar, el servidor suscribe la sesión a todos los chats del usuario.

### Envoltorio
Todos los mensajes (en ambos sentidos) son JSON con un `payload` que es **una cadena con JSON dentro**:
```json
{ "type": "NEW_MESSAGE", "payload": "{\"chatId\":\"...\",\"content\":\"Hola\"}" }
```

### Cliente → servidor
| `type` | `payload` | Efecto |
|---|---|---|
| `NEW_MESSAGE` | `{chatId, content, messageId?}` | Guarda y difunde el mensaje. `messageId` opcional (UUID generado por el cliente, útil para la UI optimista). Si no participas en el chat, se ignora |

### Servidor → cliente
| `type` | `payload` | Cuándo |
|---|---|---|
| `NEW_MESSAGE` | `ChatMessageDto` | Mensaje nuevo en uno de tus chats (también el tuyo, como confirmación) |
| `MESSAGE_DELETED` | `{chatId, messageId}` | Un mensaje se borró (vía `DELETE /messages/{id}`) |
| `CHAT_PARTICIPANTS_CHANGED` | `{chatId}` | Alguien entró/salió: recargar `GET /chats/{chatId}` |
| `PROFILE_PICTURE_UPDATED` | `{userId, newUrl?}` | Un contacto cambió o quitó su foto |
| `ERROR` | `{code, message}` | P. ej. `INVALID_JSON` si el mensaje no es válido (la conexión sigue abierta) |

Las fechas dentro del `payload` siguen el mismo formato ISO-8601 que la API REST.

---

## 13. Notificaciones push (Firebase)

El backend envía push con **Firebase Cloud Messaging** (Admin SDK). Requisitos:
- La app usa la configuración (`google-services.json` / `GoogleService-Info.plist`) del **mismo proyecto Firebase** que las credenciales del backend.
- iOS: clave APNs (`.p8`) subida en Firebase Console → Cloud Messaging.
- Si el backend define `FIREBASE_ANDROID_PACKAGE`, debe coincidir exactamente con el `applicationId` (incluidas variantes `.debug`).

### Ciclo en la app
1. Pedir permiso de notificaciones (iOS; Android 13+).
2. Tras login: obtener el token FCM y `POST /api/v1/devices {token, platform}`. El backend valida el token con Firebase.
3. Si FCM renueva el token (`onNewToken`): volver a registrarlo.
4. En logout: `DELETE /api/v1/devices/{token}`.
5. Tokens que Firebase da por inválidos se eliminan solos.

### Mensaje
Cada push lleva `notification` (`title`, `body`, en español, fechas en la zona del club) y `data` (todos los valores son strings). En Android usa `collapseKey` y prioridad alta; en iOS sonido por defecto y `threadId`. Con la app en segundo plano el sistema muestra la notificación y entrega `data` al abrirla; en primer plano llega al callback de mensajes de la app.

| `data.type` | Cuándo | Destinatarios | `data` adicional | Ejemplo de texto |
|---|---|---|---|---|
| `match.announcement.opened` | Se abre la convocatoria | Todos los miembros salvo los ausentes ese día | `clubId`, `matchId`, `announcementId` | "Squadfy FC: convocatoria abierta" / "Partido el jueves 15 de octubre a las 20:00. ¡Apúntate!" |
| `match.announcement.closing_soon` | 24 h antes del cierre, si quedan plazas (no si abrió en esas 24 h) | Miembros no apuntados ni ausentes | `clubId`, `matchId`, `announcementId` | "Squadfy FC: quedan 3 plazas" / "La convocatoria cierra el miércoles 14 a las 22:00. …" |
| `match.teams.published` | Equipos publicados a la hora del sorteo o tras rectificar | Jugadores de los equipos | `clubId`, `matchId`, `team` (`A`/`B`) | "Squadfy FC: equipos publicados" / "Juegas en el equipo A · …" |
| `match.cancelled` | Partido cancelado (también por una excepción) | Apuntados (confirmados y espera) | `clubId`, `matchId` | "Squadfy FC: partido cancelado" |
| `match.rescheduled` | Partido movido por una excepción (o devuelto a su fecha al borrarla) | Todos los miembros | `clubId`, `matchId` | "Squadfy FC: partido cambiado" / "El partido del jueves 8 de octubre a las 20:00 pasa al viernes 9 de octubre a las 21:00." |
| `match.waitlist.promoted` | Pasas de la espera a confirmado | El promocionado (**aunque haya silenciado el club**) | `clubId`, `matchId`, `announcementId` | "Squadfy FC: ¡tienes plaza!" |
| `new_message` | Mensaje de chat | Participantes salvo el remitente | `chatId` | título = nombre del remitente, cuerpo = mensaje |

Apertura y recordatorio se envían **una sola vez** por convocatoria. Silenciar un club: `PUT /clubs/{clubId}/notification-settings {"muted": true}`.

---

## 14. Guía de integración para la app KMP

### Cliente HTTP (Ktor + kotlinx.serialization)
- `Json { ignoreUnknownKeys = true; coerceInputValues = true }` para tolerar campos y enums nuevos.
- `Instant`: `kotlinx.datetime.Instant` (o `kotlin.time.Instant`) parsea ISO-8601 con microsegundos. `LocalTime`: `"HH:mm:ss"`. `DayOfWeek`/enums como `String` o enums con valor por defecto.
- UUID como `String` (o `kotlin.uuid.Uuid`). `minutesPlayed`: `Map<String, Int>`.
- Plugin `Auth` con `bearer { loadTokens; refreshTokens { POST /auth/refresh } }`: ante 401 refrescar una vez; si el refresh da 401 `INVALID_TOKEN`, cerrar sesión. Guardar el **nuevo** refresh token tras cada refresh (rotación).
- Mostrar fechas en la zona del club (`schedule.timeZone`), no en la del dispositivo, para coherencia con "cierra a las 22:00".

### Pantallas y llamadas sugeridas
| Pantalla | Llamadas |
|---|---|
| Arranque | `GET /features` (sin sesión) para saber qué funciones están activas en este entorno |
| Registro / verificación | `POST /auth/register`; si `email-verification` está activado, aviso "revisa tu email", el login devuelve 403 `EMAIL_NOT_VERIFIED` hasta verificar y hay botón `POST /auth/resend-verification`; si no, entrar directamente |
| Login / sesión | `POST /auth/login`, guardar tokens, `GET /me`, registrar dispositivo FCM |
| Reset de contraseña | `POST /auth/forgot-password`; deep link `squadfy://reset-password?token=` → `POST /auth/reset-password` |
| Mis clubes | `GET /clubs`; crear `POST /clubs`; unirse `POST /clubs/join` (manejar 400 código inválido, 403 vetado, 409 ya miembro/lleno) |
| Inicio del club | `GET /clubs/{id}/announcements/current` (404 = sin partido programado) + `GET /clubs/{id}/members` para nombres. Botón apuntarse/desapuntarse según `myStatus` y si está abierta (`status == OPEN && opensAt <= now < closesAt`); mostrar "estás en espera (n.º X)" |
| Invitados | En la convocatoria: `POST /announcements/{id}/guests` y `DELETE .../guests/{guestId}`; mostrar `entries`/`waitlist` con `participantType` (nombre del invitado = `guestName`) |
| Detalle de partido | `GET /matches/{id}` (equipos con invitados, marcador oficial, eventos, minutos, `ratingChanges` como "valoración del partido") |
| Gestión del partido (gestores) | `POST /matches/{id}/teams` (AUTO/MANUAL), `GET /matches/{id}/team-balance`, eventos, `PUT .../minutes`, `PUT/DELETE .../score`, `POST .../complete`, `POST .../reopen`, `POST .../cancel` |
| Horario (gestores) | `GET/POST/PATCH /clubs/{id}/schedule` (incluye hora de cierre y de sorteo); excepciones `GET/POST/DELETE /clubs/{id}/schedule/exceptions` |
| Ausencias | `GET /clubs/{id}/absences?from=&to=`; las mías: `POST/DELETE /clubs/{id}/members/me/absences` |
| Miembros y roles | `GET /clubs/{id}/members`; gestores: rol, expulsar, vetar, `GET /clubs/{id}/bans`; owner: transferir; todos: `PATCH /members/me`, salir |
| Clasificaciones | `GET /clubs/{id}/ratings`, `/ratings/me`, `GET /clubs/{id}/stats?sortBy=…`, `/stats/me` (cruzar `clubMemberId` con miembros) |
| Ajustes del club | `GET/PUT /clubs/{id}/notification-settings`; gestores: `PATCH /clubs/{id}`, logo, regenerar código |
| Chat | `GET /chats`, `GET /chats/{id}/messages?before=` para scroll hacia atrás, WebSocket para enviar/recibir, `GET /users?query=` para buscar a quién escribir |
| Perfil | `GET /me`, `GET /users/{myId}` (foto), flujo de foto (`upload-url` → subir → `PUT /me/profile-picture`), **borrar cuenta** (`DELETE /me {password}` tras pedir la contraseña y confirmar) |

### Actualización de datos
- Convocatorias, equipos y partidos **no llegan por WebSocket**: refrescar al abrir la pantalla, con *pull-to-refresh* y **al recibir una push** del tipo correspondiente (usar `clubId`/`matchId` de `data` para navegar y recargar).
- El chat sí es en tiempo real (WebSocket); reconectar con backoff si se cae.

### Permisos en la UI
Ocultar acciones de gestión si el rol del usuario en ese club (de `GET /clubs/{id}/members`, buscando su `userId`) no es `OWNER`/`ADMIN`; aun así el backend es quien decide (403).

---

## 15. Cambios respecto a la versión anterior de la app

La app aún consume las rutas antiguas. Resumen de lo que cambia (detalle completo con la tabla ruta antigua → nueva en [`docs/api/migracion-v1.md`](api/migracion-v1.md)):

1. **Todas las rutas** pasan a `/api/v1` con nombres nuevos (`/api/club` → `/api/v1/clubs`, `/api/matchAnnouncements/{id}/enroll` → `POST /api/v1/announcements/{id}/enrollment`, `/api/chat/create-chat` → `POST /api/v1/chats`, `/api/notification/register` → `POST /api/v1/devices`, etc.). Las antiguas ya no existen.
2. **Horario**: `format` + `timeZone` + `matchDurationMinutes`; sin `matchAnnouncementOpenDaysBeforeMatch` ni `maxPlayers` de entrada; el `clubId` va en la ruta.
3. **Convocatoria**: `entries` (confirmados) + `waitlist`; `confirmedCount`/`waitlistCount` en vez de `enrolledCount`; cierre a las 22:00 del día anterior; nuevo `announcements/current` con `myStatus`.
4. **Posición** como enum cerrado.
5. **Privacidad**: sin `email` en miembros ni usuarios de chat; el propio email en `GET /me`.
6. **Estadísticas** fuera de `ClubMemberDto` (antes siempre 0) → `/stats`.
7. **Nuevas funciones**: lista de espera, equipos automáticos, rating y clasificación, equilibrio de equipos, minutos, estadísticas y ranking, salir/expulsar/vetar/roles/transferir, editar club, push del ciclo de partido, silenciar club, perfil público `GET /users/{id}`.
8. **Errores**: 403 `NOT_CLUB_MEMBER` (antes 400); creaciones devuelven **201**; salir/expulsar/vetar devuelven **204**.
9. **Reset de contraseña** vía deep link `squadfy://reset-password?token=`.
10. **Push de chat**: el título es ahora el nombre del remitente.
11. **Spec 008 (paridad con la app)**: invitados en la convocatoria (entradas con `participantType`), excepciones del calendario, ausencias, `closeTime`/`drawTime` configurables (`drawAt` en la convocatoria), marcador manual oficial y `ratingChanges` por partido. **Se retira la valoración manual** (el nivel es el rating automático). La foto por club queda en backlog.
12. **Spec 010**: borrado de cuenta (`DELETE /me {password}` y página web `/account/delete`) y rate limit por cuenta con `Retry-After`.
13. **Spec 011**: feature flags (`GET /features`); **la verificación de email deja de ser obligatoria** por defecto; el correo de verificación (si se activa) abre la página `/account/verify-email`; `DELETE /devices/{token}` responde 204 y solo borra dispositivos propios.

---

## 16. Limitaciones conocidas y pendiente

| Tema | Detalle |
|---|---|
| Despliegue | Falta elegir hosting; la imagen Docker y la CI están listas. Servicios gestionados: Supabase (PostgreSQL + Storage), CloudAMQP, Redis Cloud, Mailgun, Firebase |
| Código `USER_EXITS` | Errata histórica de `USER_EXISTS`; se mantiene por compatibilidad hasta acordar el cambio |
| Tiempo real | Convocatorias y partidos no se emiten por WebSocket; la app refresca al abrir o al recibir push |
| Búsqueda de usuarios | `GET /users?query=` busca coincidencia exacta de username o email |
| Estadísticas | Sin filtros por temporada/fechas |
| Foto por club | En backlog (spec 008): la foto es la del perfil del usuario |
| Configuración de cierre/sorteo | Un cambio en el horario aplica a los partidos planificados después; la convocatoria ya abierta mantiene sus horas |
| Deuda técnica | RabbitMQ y Redis aún serializan con Jackson 2; *open-in-view* activo; algún aviso de deprecación |

Documentos relacionados: [`specs/README.md`](../specs/README.md) (specs por funcionalidad, SDD) · [`docs/api/migracion-v1.md`](api/migracion-v1.md) (migración de la app) · [`README.md`](../README.md).
