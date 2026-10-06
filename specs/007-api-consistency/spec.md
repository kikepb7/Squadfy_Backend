# 007 — API coherente y documentada (v1)

- **Estado**: Hecha (rama `api-consistency-feature`). Guía para la app: [`docs/api/migracion-v1.md`](../../docs/api/migracion-v1.md)
- **Módulos**: todos los que exponen API (user, club, match, chat, notification), app
- **Dependencias**: 001–006

## Contexto y objetivo
La app móvil consume las rutas actuales, que mezclan estilos (`/api/club` en singular, `/api/club/create`, `/api/matchAnnouncements` en camelCase, `DELETE .../cancel`, rutas `/club/{clubId}` colgando de otros recursos) y no tienen documentación. Además la app ya no está alineada con los cambios de `match-feature`. Objetivo: un contrato **versionado (`/api/v1`), coherente y documentado con OpenAPI** al que la app migre de una vez. La app aún no está publicada, así que las rutas antiguas se sustituyen sin periodo de convivencia.

## Historias de usuario
- Como desarrollador de la app, quiero una especificación OpenAPI fiable para generar o actualizar el cliente sin adivinar campos.
- Como desarrollador de la app, quiero rutas predecibles orientadas a recursos (`/clubs/{id}/matches`, `/matches/{id}`) para no consultar cada endpoint.
- Como usuario de la app, quiero ver en la pantalla principal la convocatoria vigente de mi club y si estoy apuntado o en lista de espera.
- Como usuario, no quiero que el resto de miembros vea mi email.

## Reglas
- **R-1 Versionado**: todo endpoint REST nuevo vive bajo `/api/v1`. Los cambios incompatibles futuros irán a `/api/v2`.
- **R-2 Convenciones**: recursos en plural y kebab-case; jerarquía por pertenencia (`/clubs/{clubId}/...` para colecciones de un club, `/matches/{matchId}` para un recurso concreto); verbos HTTP con semántica (sin `/create`, sin `DELETE` para acciones que no borran); acciones de dominio como `POST /recurso/{id}/<accion>` (`cancel`, `complete`, `reopen`).
- **R-3 Sin convivencia**: las rutas actuales se **sustituyen** por las v1 (la app no está publicada). A partir de la publicación, cualquier cambio incompatible requerirá `/api/v2` con convivencia.
- **R-4 Documentación**: OpenAPI 3 generado del código (springdoc) con esquema de seguridad Bearer JWT, descripciones de cada endpoint, códigos de error y enums. **Swagger UI y `/v3/api-docs` solo en el perfil `dev`** (en el resto, desactivados).
- **R-5 Errores**: formato único `{ "code", "message" }` (o `errors[]` en validación) documentado en OpenAPI; mismos códigos HTTP que hoy (400/401/403/404/409).
- **R-6 Privacidad**: el email de un miembro no aparece en respuestas que vea otro usuario (listado de miembros, participantes de chat). Cada usuario consulta sus propios datos en `GET /api/v1/me`.
- **R-7 Convocatoria vigente**: `GET /api/v1/clubs/{clubId}/announcements/current` devuelve la convocatoria del próximo partido programado con `myStatus` (`NOT_ENROLLED`, `CONFIRMED`, `WAITLISTED`) y, si está en espera, `myWaitlistPosition`. 404 si no hay partido programado.

## Contrato v1
| Método | Ruta v1 | Ruta actual (se elimina) | Permiso |
|---|---|---|---|
| POST | `/api/v1/auth/{register,login,refresh,logout,resend-verification,forgot-password,reset-password,change-password}` | `/api/auth/...` | público / autenticado (`change-password`) |
| GET | `/api/v1/auth/verify` | `/api/auth/verify` | público |
| GET | `/api/v1/me` | — (nuevo) | autenticado |
| GET | `/api/v1/users?query=` | `GET /api/participants?query=` | autenticado |
| GET | `/api/v1/users/{userId}` (perfil público: nombre y foto) | `GET /api/participants` (sin `query`) | autenticado |
| POST | `/api/v1/me/profile-picture/upload-url` | `POST /api/participants/profile-picture-upload` | autenticado |
| PUT | `/api/v1/me/profile-picture` | `POST /api/participants/confirm-profile-picture` | autenticado |
| DELETE | `/api/v1/me/profile-picture` | `DELETE /api/participants/delete-profile-picture` | autenticado |
| GET | `/api/v1/clubs` | `GET /api/club` | autenticado |
| POST | `/api/v1/clubs` | `POST /api/club/create` | autenticado |
| POST | `/api/v1/clubs/join` | `POST /api/club/join` | autenticado |
| GET | `/api/v1/clubs/{clubId}` | `GET /api/club/{clubId}` | miembro |
| PUT | `/api/v1/clubs/{clubId}/logo` | `POST /api/club/{clubId}/logo` | gestor |
| POST | `/api/v1/clubs/{clubId}/invitation-code` | `POST /api/club/{clubId}/regenerate-invitation-code` | gestor |
| GET | `/api/v1/clubs/{clubId}/members` | `GET /api/club/{clubId}/members` | miembro |
| PATCH | `/api/v1/clubs/{clubId}/members/me` | `PATCH /api/club/{clubId}/members/me` | miembro |
| GET / POST / PATCH | `/api/v1/clubs/{clubId}/schedule` | `/api/match-schedules/club/{clubId}`, `POST /api/match-schedules`, `PATCH /api/match-schedules/{scheduleId}` | miembro / gestor / gestor |
| GET | `/api/v1/clubs/{clubId}/matches?status=` | `GET /api/matches/club/{clubId}[/scheduled]` | miembro |
| POST | `/api/v1/clubs/{clubId}/matches` | `POST /api/matches` | gestor |
| GET | `/api/v1/matches/{matchId}` | `GET /api/matches/{matchId}` | miembro |
| POST | `/api/v1/matches/{matchId}/{cancel,complete,reopen}` | `DELETE .../cancel`, `POST .../complete`, `POST .../reopen` | gestor |
| POST | `/api/v1/matches/{matchId}/teams` | `POST /api/matches/{matchId}/generate-teams` | gestor |
| GET | `/api/v1/matches/{matchId}/team-balance` | `GET /api/matches/{matchId}/team-balance` | gestor |
| POST / DELETE | `/api/v1/matches/{matchId}/events[/{eventId}]` | igual bajo `/api/matches` | gestor |
| GET | `/api/v1/clubs/{clubId}/announcements` | `GET /api/matchAnnouncements/club/{clubId}` | miembro |
| GET | `/api/v1/clubs/{clubId}/announcements/current` | — (nuevo, R-7) | miembro |
| GET | `/api/v1/announcements/{announcementId}` | `GET /api/matchAnnouncements/{id}` | miembro |
| GET | `/api/v1/matches/{matchId}/announcement` | `GET /api/matchAnnouncements/match/{matchId}` | miembro |
| POST / DELETE | `/api/v1/announcements/{announcementId}/enrollment` | `POST .../{id}/enroll`, `DELETE .../{id}/withdraw` | miembro |
| GET | `/api/v1/clubs/{clubId}/ratings[/me]` | `GET /api/player-ratings/club/{clubId}[/me]` | miembro |
| GET / POST | `/api/v1/chats` | `GET /api/chat`, `POST /api/chat/create-chat` | autenticado |
| GET | `/api/v1/chats/{chatId}`, `/api/v1/chats/{chatId}/messages?before=&pageSize=` | igual bajo `/api/chat` | participante |
| POST | `/api/v1/chats/{chatId}/participants` | `POST /api/chat/{chatId}/add` | participante |
| DELETE | `/api/v1/chats/{chatId}/participants/me` | `DELETE /api/chat/{chatId}/leave` | participante |
| DELETE | `/api/v1/messages/{messageId}` | `DELETE /api/messages/{messageId}` | autor |
| POST / DELETE | `/api/v1/devices[/{token}]` | `POST /api/notification/register`, `DELETE /api/notification/{token}` | autenticado |
| — | WebSocket `/ws/chat` | sin cambios | autenticado |

Cuerpos de petición: iguales a los actuales salvo que el `clubId` del cuerpo pasa a la ruta (`schedule`, `matches`).

## Criterios de aceptación
- **CA-1** (R-1/R-2): cada fila de la tabla responde en su ruta v1 con el mismo comportamiento que la actual (tests de integración HTTP).
- **CA-2** (R-3): las rutas antiguas ya no existen (404 autenticado).
- **CA-3** (R-4): con perfil `dev`, `GET /v3/api-docs` devuelve un OpenAPI con todas las rutas v1 y el esquema Bearer; sin `dev`, no está disponible.
- **CA-4** (R-6): `GET /api/v1/clubs/{clubId}/members` no contiene `email`; `GET /api/v1/me` sí devuelve el email del usuario.
- **CA-5** (R-7): con un usuario en lista de espera en 2.ª posición, `announcements/current` devuelve `myStatus = WAITLISTED` y `myWaitlistPosition = 2`; sin partido programado → 404.

## Fuera de alcance
- Eliminar las rutas deprecadas (cuando venza el `Sunset`).
- Paginación genérica de listados (solo los mensajes de chat la tienen hoy).
- Cambios en el protocolo WebSocket.

## Preguntas abiertas
- [x] La app **no está publicada** → sin convivencia; las rutas antiguas se sustituyen (2026-10-06).
- [x] El email se oculta en las respuestas que ve otro usuario (2026-10-06).
- [x] Swagger UI / OpenAPI solo en desarrollo (2026-10-06).
- [ ] Esquema del deep link de la app para restablecer contraseña (por defecto `squadfy://reset-password`, configurable con `RESET_PASSWORD_URL`).
