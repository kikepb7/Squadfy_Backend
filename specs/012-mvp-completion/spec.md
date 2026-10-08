# 012 — Cierre del MVP: tiempo real, estadísticas por periodo, foto por club, búsqueda de usuarios y deuda técnica

- **Estado**: Hecha (2026-10-08)
- **Módulos**: match, chat, club, user, common, app
- **Dependencias**: 002–008, 010

## Contexto y objetivo
Últimas mejoras propuestas para cerrar el backend del MVP. Decisiones de producto (2026-10-08): la «foto por club» es la **foto del jugador en cada club**, y las estadísticas por temporada se piden con un **rango de fechas libre**.

## Reglas de negocio

### A. Tiempo real
- **RN-A1**: Cuando cambia algo de un partido de un club (convocatoria, inscripciones, invitados, equipos, eventos, marcador, minutos, estado o fecha), su horario o sus excepciones, o sus ausencias, los miembros del club conectados por WebSocket reciben `CLUB_DATA_CHANGED` con `{clubId, scope: MATCH|SCHEDULE|ABSENCES, matchId?}`.
- **RN-A2**: El mensaje solo avisa de qué recargar; la app pide los datos por REST. Varios cambios de una misma operación generan un único aviso por partido o ámbito.
- **RN-A3**: Se usa la misma conexión WebSocket del chat (`/ws/chat`).

### B. Estadísticas por periodo
- **RN-B1**: La clasificación de estadísticas y mis estadísticas aceptan `from` y `to` opcionales (fechas `YYYY-MM-DD`, inclusivas, en la zona horaria del club) y solo cuentan los partidos cerrados jugados en ese periodo.
- **RN-B2**: `from` posterior a `to` → 400. Sin parámetros, se cuenta todo (comportamiento actual).
- **RN-B3**: El rating no se filtra: es acumulado (Elo).

### C. Foto del jugador por club
- **RN-C1**: Cada miembro puede subir una foto propia para cada club (jpeg, png o webp) y quitarla.
- **RN-C2**: `ClubMemberDto` expone `clubPictureUrl` (la del club), `profilePictureUrl` (la del perfil) y `pictureUrl`, la que hay que mostrar: la del club si existe, si no la del perfil.
- **RN-C3**: La foto de perfil de los miembros es siempre la actual del usuario (antes no se actualizaba en los clubes).
- **RN-C4**: Al borrar la cuenta se borran también sus fotos de club.

### D. Búsqueda de usuarios
- **RN-D1**: `GET /users/search?q=` devuelve hasta 20 perfiles públicos (nombre y foto) cuyo nombre de usuario **contiene** el texto, sin distinguir mayúsculas, primero los que empiezan por él; o el usuario cuyo email coincide **exactamente** (el email nunca se busca por partes).
- **RN-D2**: `q` debe tener al menos 2 caracteres (400). No se devuelve al propio usuario.
- **RN-D3**: `GET /users?query=` (búsqueda exacta) se mantiene.

### E. Código de error
- **RN-E1**: El registro con email o usuario existente responde `409 USER_EXISTS` (antes `USER_EXITS`, errata).

### F. Deuda técnica
- **RN-F1**: Los eventos de RabbitMQ y la caché de Redis se serializan con Jackson 3, como la API. Los mensajes siguen siendo JSON con la cabecera `__TypeId__`.
- **RN-F2**: `spring.jpa.open-in-view` desactivado: ninguna consulta a la base de datos fuera de la capa de servicio.
- **RN-F3**: Sin avisos de deprecación en la compilación del código principal.

## Criterios de aceptación
- **CA-1** (RN-A1/A2): Dado un miembro conectado al WebSocket, cuando otro miembro se apunta a la convocatoria, entonces recibe un único `CLUB_DATA_CHANGED` con `scope=MATCH` y el `matchId`; un usuario de otro club no lo recibe.
- **CA-2** (RN-B1/B2): Con dos partidos cerrados en meses distintos, `from`/`to` del primer mes devuelve solo las estadísticas de ese partido; `from > to` → 400.
- **CA-3** (RN-C1/C2): Un miembro sube su foto de club y `GET /members` devuelve `pictureUrl = clubPictureUrl`; al quitarla vuelve la de perfil. Un tipo de archivo no permitido → 400.
- **CA-4** (RN-C3): Tras cambiar la foto de perfil, `GET /members` devuelve la nueva en `profilePictureUrl`.
- **CA-5** (RN-D1/D2): Con usuarios «carlos», «marcos» y «ana», buscar `ar` devuelve «carlos» y «marcos» (no «ana» ni al que busca); `q=a` → 400; un email completo encuentra a su usuario y uno parcial no.
- **CA-6** (RN-E1): Registrar un email existente devuelve 409 `USER_EXISTS`.
- **CA-7** (RN-F1/F2): Todo el flujo de la API y los eventos entre módulos sigue funcionando (suite completa en verde).

## API (contrato)
| Método | Ruta | Permiso | Respuesta |
|---|---|---|---|
| GET | `/api/v1/clubs/{clubId}/stats?sortBy=&from=&to=` | miembro | `ClubStatsEntryDto[]` |
| GET | `/api/v1/clubs/{clubId}/stats/me?from=&to=` | miembro | `PlayerStatsDto` |
| PUT | `/api/v1/clubs/{clubId}/members/me/picture` (multipart, parte `picture`) | miembro | `ClubMemberDto` |
| DELETE | `/api/v1/clubs/{clubId}/members/me/picture` | miembro | `ClubMemberDto` |
| GET | `/api/v1/users/search?q=` | auth | `ChatParticipantDto[]` |
| WS | `/ws/chat` → `{type: "CLUB_DATA_CHANGED", payload}` | miembro conectado | — |

Cambios: `ClubMemberDto` + `clubPictureUrl`, `pictureUrl`; código `USER_EXITS` → `USER_EXISTS`.

## Fuera de alcance
- Tiempo real con varias instancias del backend (el WebSocket, igual que el chat, es de una instancia).
- Temporadas con nombre configuradas por club.
