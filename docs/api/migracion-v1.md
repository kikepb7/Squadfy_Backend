# Migración de la app móvil a la API v1

Guía para actualizar la app al contrato de la [spec 007](../../specs/007-api-consistency/spec.md). Las rutas antiguas **ya no existen** (la app no está publicada, no hay periodo de convivencia).

**Contrato exacto**: arranca el backend con el perfil `dev` y abre **Swagger UI** en `http://localhost:8080/swagger-ui.html` (JSON en `/v3/api-docs`, útil para generar el cliente). En producción no se publica.

## 1. Rutas

| Antes | Ahora |
|---|---|
| `POST /api/auth/{register,login,refresh,logout,resend-verification,forgot-password,reset-password,change-password}` | `POST /api/v1/auth/...` (mismos nombres) |
| `GET /api/auth/verify?token=` | `GET /api/v1/auth/verify?token=` |
| — | `GET /api/v1/me` (mi perfil, con email) |
| `GET /api/participants` (sin `query`, mi perfil de chat) | `GET /api/v1/users/{userId}` con mi id de `/me` |
| `GET /api/participants?query=` | `GET /api/v1/users?query=` (`query` obligatorio) |
| `POST /api/participants/profile-picture-upload?mimeType=` | `POST /api/v1/me/profile-picture/upload-url?mimeType=` |
| `POST /api/participants/confirm-profile-picture` | `PUT /api/v1/me/profile-picture` con cuerpo `{ "publicUrl": "..." }` |
| `DELETE /api/participants/delete-profile-picture` | `DELETE /api/v1/me/profile-picture` |
| `GET /api/club` | `GET /api/v1/clubs` |
| `POST /api/club/create` | `POST /api/v1/clubs` → **201** |
| `POST /api/club/join` | `POST /api/v1/clubs/join` |
| `GET /api/club/{clubId}` | `GET /api/v1/clubs/{clubId}` |
| `POST /api/club/{clubId}/logo` (multipart `clubLogo`) | `PUT /api/v1/clubs/{clubId}/logo` (multipart `clubLogo`) |
| `POST /api/club/{clubId}/regenerate-invitation-code` | `POST /api/v1/clubs/{clubId}/invitation-code` |
| `GET /api/club/{clubId}/members` | `GET /api/v1/clubs/{clubId}/members` |
| `PATCH /api/club/{clubId}/members/me` | `PATCH /api/v1/clubs/{clubId}/members/me` |
| — | `PATCH /api/v1/clubs/{clubId}` `{name?, description?, maxMembers?}` (gestores) |
| — | `DELETE /api/v1/clubs/{clubId}/members/me` → **204** salir del club (el owner debe transferir antes: 409) |
| — | `DELETE /api/v1/clubs/{clubId}/members/{memberId}` → **204** expulsar (owner: cualquiera; admin: capitanes y jugadores) |
| — | `PATCH /api/v1/clubs/{clubId}/members/{memberId}/role` `{role: ADMIN \| CAPTAIN \| PLAYER}` |
| — | `POST /api/v1/clubs/{clubId}/transfer-ownership` `{memberId}` (el owner pasa a ADMIN) |
| — | `POST /api/v1/clubs/{clubId}/members/{memberId}/ban` → **204** vetar (si está activo, también lo expulsa) |
| — | `DELETE /api/v1/clubs/{clubId}/members/{memberId}/ban` → **204** levantar el veto |
| — | `GET /api/v1/clubs/{clubId}/bans` → `[{clubMemberId, userId, username, bannedAt}]` (gestores) |
| `POST /api/match-schedules` (con `clubId` en el cuerpo) | `POST /api/v1/clubs/{clubId}/schedule` → **201** (sin `clubId` en el cuerpo) |
| `GET /api/match-schedules/club/{clubId}` | `GET /api/v1/clubs/{clubId}/schedule` |
| `PATCH /api/match-schedules/{scheduleId}` | `PATCH /api/v1/clubs/{clubId}/schedule` |
| `POST /api/matches` (con `clubId` en el cuerpo) | `POST /api/v1/clubs/{clubId}/matches` → **201** (sin `clubId` en el cuerpo) |
| `GET /api/matches/club/{clubId}` | `GET /api/v1/clubs/{clubId}/matches` |
| `GET /api/matches/club/{clubId}/scheduled` | `GET /api/v1/clubs/{clubId}/matches?status=SCHEDULED` |
| `GET /api/matches/{matchId}` | `GET /api/v1/matches/{matchId}` |
| `DELETE /api/matches/{matchId}/cancel` | `POST /api/v1/matches/{matchId}/cancel` |
| `POST /api/matches/{matchId}/{complete,reopen}` | `POST /api/v1/matches/{matchId}/{complete,reopen}` |
| `POST /api/matches/{matchId}/generate-teams` | `POST /api/v1/matches/{matchId}/teams` |
| `GET /api/matches/{matchId}/team-balance` | `GET /api/v1/matches/{matchId}/team-balance` |
| `POST/DELETE /api/matches/{matchId}/events[/{eventId}]` | `POST/DELETE /api/v1/matches/{matchId}/events[/{eventId}]` |
| `GET /api/matchAnnouncements/club/{clubId}` | `GET /api/v1/clubs/{clubId}/announcements` |
| — | `GET /api/v1/clubs/{clubId}/announcements/current` (pantalla principal) |
| `GET /api/matchAnnouncements/{id}` | `GET /api/v1/announcements/{id}` |
| `GET /api/matchAnnouncements/match/{matchId}` | `GET /api/v1/matches/{matchId}/announcement` |
| `POST /api/matchAnnouncements/{id}/enroll` | `POST /api/v1/announcements/{id}/enrollment` |
| `DELETE /api/matchAnnouncements/{id}/withdraw` | `DELETE /api/v1/announcements/{id}/enrollment` |
| `GET /api/player-ratings/club/{clubId}[/me]` | `GET /api/v1/clubs/{clubId}/ratings[/me]` |
| `GET /api/chat` | `GET /api/v1/chats` |
| `POST /api/chat/create-chat` | `POST /api/v1/chats` → **201** |
| `GET /api/chat/{chatId}`, `GET /api/chat/{chatId}/messages?before=&pageSize=` | `GET /api/v1/chats/{chatId}`, `GET /api/v1/chats/{chatId}/messages?before=&pageSize=` |
| `POST /api/chat/{chatId}/add` | `POST /api/v1/chats/{chatId}/participants` |
| `DELETE /api/chat/{chatId}/leave` | `DELETE /api/v1/chats/{chatId}/participants/me` |
| `DELETE /api/messages/{messageId}` | `DELETE /api/v1/messages/{messageId}` |
| `POST /api/notification/register` | `POST /api/v1/devices` → **201** |
| `DELETE /api/notification/{token}` | `DELETE /api/v1/devices/{token}` |
| — | `GET / PUT /api/v1/clubs/{clubId}/notification-settings` `{muted}` (silenciar un club) |
| WebSocket `/ws/chat` | sin cambios |

## 2. Cambios de modelos desde la última versión de la app

- **Posición** del jugador: valor cerrado `GOALKEEPER | DEFENDER | MIDFIELDER | FORWARD` (antes texto libre).
- **Horario** (`schedule`): `format` (`FIVE_A_SIDE | SEVEN_A_SIDE | ELEVEN_A_SIDE`) y `timeZone` (IANA, por defecto `Europe/Madrid`); `maxPlayers` lo calcula el formato (10/14/22). Desaparece `matchAnnouncementOpenDaysBeforeMatch`.
- **Partido manual**: `format` opcional en lugar de `maxPlayers`.
- **Convocatoria**: `entries` (confirmados) + `waitlist`, cada entrada con `status` (`CONFIRMED | WAITLISTED`); `confirmedCount` y `waitlistCount` sustituyen a `enrolledCount`. Se cierra a las **22:00 del día anterior**; después no se puede apuntar ni desapuntar. Al cerrarse, los equipos se publican solos.
- **Convocatoria vigente** (nuevo): `{ announcement, matchScheduledAt, myStatus: NOT_ENROLLED | CONFIRMED | WAITLISTED, myWaitlistPosition }`; 404 si no hay partido programado.
- **Ratings** (nuevo): clasificación `[{ rank, clubMemberId, rating, matchesRated, isProvisional }]`; `/me` añade `rank` y `totalPlayers`. Nombre y foto se cruzan con `/clubs/{clubId}/members`.
- **Equilibrio de equipos** (gestores): `teamA/teamB.playerRatings` con el rating de cada jugador.
- **Membresía**: quien sale o es expulsado deja de verse en el club y sale de las convocatorias abiertas (su plaza pasa a la lista de espera). Si vuelve a unirse con el código conserva su `clubMemberId` y su rating, con rol `PLAYER`. Un usuario **vetado** recibe 403 `BANNED_FROM_CLUB` al intentar unirse hasta que un gestor levante el veto.
- **Privacidad**: `email` desaparece de los miembros del club y de los participantes/usuarios de chat. El propio email está en `GET /api/v1/me`.

## 3. Comportamiento y errores

- Errores: `{ "code", "message" }`; validación `{ "code": "VALIDATION_ERROR", "errors": [...] }`.
- 403 `NOT_CLUB_MEMBER` (antes 400) al operar sobre un club del que no eres miembro; 403 `FORBIDDEN` si no eres gestor (OWNER/ADMIN).
- Creaciones (`clubs`, `schedule`, `matches`, `chats`, `devices`, `events`) responden **201**.
- **Restablecer contraseña**: el email abre `RESET_PASSWORD_URL?token=...` (por defecto el deep link `squadfy://reset-password?token=...`). La app debe capturar ese enlace, pedir la nueva contraseña y llamar a `POST /api/v1/auth/reset-password` con `{ token, newPassword }`.

## 4. Notificaciones push

Solo push (Firebase). La app debe registrar el dispositivo con `POST /api/v1/devices` tras el login. Cada push lleva en `data` el campo `type` para decidir qué pantalla abrir:

| `type` | Cuándo | Destinatarios | `data` adicional |
|---|---|---|---|
| `match.announcement.opened` | Se abre la convocatoria | Todos los miembros | `clubId`, `matchId`, `announcementId` |
| `match.announcement.closing_soon` | 24 h antes del cierre, si quedan plazas | Miembros no apuntados | `clubId`, `matchId`, `announcementId` |
| `match.teams.published` | Equipos publicados (al cierre o al rectificar) | Jugadores de los equipos | `clubId`, `matchId`, `team` (`A`/`B`) |
| `match.cancelled` | Partido cancelado | Apuntados | `clubId`, `matchId` |
| `match.waitlist.promoted` | Pasas de la lista de espera a convocado | El jugador promocionado | `clubId`, `matchId`, `announcementId` |
| `new_message` | Mensaje de chat | Participantes del chat | `chatId` |

Un usuario que silencia un club (`PUT .../notification-settings {"muted": true}`) no recibe sus push, salvo `match.waitlist.promoted`. El título de las push de chat es ahora el nombre del remitente.
