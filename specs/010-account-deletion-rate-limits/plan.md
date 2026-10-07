# 010 — Plan técnico

## Borrado de cuenta (síncrono y atómico)
- Puerto nuevo `common/domain/user/UserDataEraser { fun eraseUserData(userId) }`, implementado por **club**, **chat**, **notification** y **match** (orden con `@Order`: club → match → chat → notification).
- `user/service/AccountDeletionService`: verifica la contraseña, ejecuta en **una transacción** todos los `UserDataEraser`, borra tokens (refresh, verificación, reset) y el usuario, y marca sus access tokens como revocados.
- Revocación de access tokens: clave Redis `revoked_user:<userId>` con TTL = vida del access token; `JwtAuthFilter` la consulta tras validar la firma (sin consulta a BD por petición).

### Por módulo
- **club** (`ClubUserDataEraser`): por cada ficha activa, si es OWNER aplica `OwnerSuccession` (dominio puro: ADMIN activo más antiguo → miembro activo más antiguo → ninguno) y transfiere o elimina el club; después desactiva la ficha y publica `ClubEvent.MemberLeft` (match ya retira de convocatorias e invitados). Anonimiza `club_participants` (nombre «Usuario eliminado», email `deleted+<id>@squadfy.invalid`, sin foto). Club eliminado: borra sus fichas y el club, borra el logo del almacenamiento (best effort) y publica `ClubEvent.ClubDeleted`.
- **match**: `MatchClubEventListener` atiende `ClubDeleted` y borra los datos del club en `match_service` (horario, excepciones, ausencias, partidos, eventos, equipos, convocatorias, entradas, ratings y variaciones). `MatchUserDataEraser` borra las ausencias de las fichas del usuario (puerto `ClubMembershipProvider.findMemberIdsOfUser`).
- **chat** (`ChatUserDataEraser`): borra sus mensajes (y vacía la caché de esos chats), sale de sus chats (`removeParticipantFromChat`), reasigna el creador de los chats que creó, borra la foto del almacenamiento (best effort) y elimina su `chat_participant`.
- **notification** (`NotificationUserDataEraser`): borra dispositivos y preferencias por club.

### API y web
- `MeController`: `DELETE /api/v1/me {password}`.
- `AuthController`: `POST /api/v1/auth/delete-account {email, password}`.
- Página estática `user/src/main/resources/static/account/delete.html` servida en `/account/delete` (view controller con forward; `SecurityConfig` la permite).

## Rate limit
- `RateLimiter` genérico (Redis + script Lua existente) con claves `rate_limit:<ámbito>:<id>`; `IpRateLimiter` pasa a usarlo.
- `AuthRateLimits` (api) aplica los límites de RN-B1 antes de llamar al servicio: refresh por `sub` del token (si la firma es válida) o por IP, login por email, borrado por usuario/email.
- El interceptor por IP lanza `RateLimitException` en vez de `sendError`, para que el `@ExceptionHandler` devuelva `{code, message}` + `Retry-After`.
- Propiedad `squadfy.rate-limit.enabled` (`RATE_LIMIT_ENABLED`).

## Modelo de datos
Sin migración: se reutilizan las tablas existentes (anonimizar y borrar).

## Tests
- Unitario: `OwnerSuccession`.
- `app` (SpringBootTest + Testcontainers): CA-1..CA-6 por HTTP; `ClubDeleted` → datos de match borrados.
- `app` con rate limit activo: CA-7, CA-8.
