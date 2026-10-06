# 001 — Clubes y membresía

- **Estado**: En curso (núcleo hecho; gestión de miembros pendiente)
- **Módulos**: club, common
- **Dependencias**: autenticación (user)

## Contexto y objetivo
Un usuario crea un club deportivo y comparte un código para que otros se unan. Un usuario puede pertenecer a varios clubes, con una ficha de jugador distinta en cada uno (dorsal, posición, estadísticas, rol).

## Historias de usuario
- Como usuario, quiero crear un club para organizar los partidos de mi grupo.
- Como gestor, quiero compartir un código de invitación y poder regenerarlo si se filtra.
- Como usuario, quiero unirme a un club con un código indicando mi dorsal y posición.
- Como miembro, quiero ver mis clubes y los miembros de cada uno.
- Como miembro, quiero editar mi dorsal y posición en un club.
- Como miembro, quiero salir de un club.
- Como gestor, quiero expulsar a un miembro y nombrar administradores o capitanes.
- Como owner, quiero editar los datos del club (nombre, descripción, logo, límite de miembros).

## Reglas de negocio
- **RN-1**: Quien crea el club es su `OWNER`. Solo hay un owner por club.
- **RN-2**: Roles: `OWNER`, `ADMIN`, `CAPTAIN`, `PLAYER`. "Gestor" = `OWNER` o `ADMIN`.
- **RN-3**: El código de invitación es alfanumérico de 8 caracteres en mayúsculas, único globalmente; la búsqueda no distingue mayúsculas.
- **RN-4**: Un usuario no puede unirse dos veces al mismo club.
- **RN-5**: Si el club tiene `maxMembers`, no se admiten más miembros al alcanzarlo.
- **RN-6**: La posición es una de `GOALKEEPER`, `DEFENDER`, `MIDFIELDER`, `FORWARD` (opcional).
- **RN-7**: Solo los miembros pueden ver el club y sus miembros. Solo los gestores pueden cambiar logo, código, roles o expulsar.
- **RN-8**: El owner no puede salir del club ni ser expulsado sin transferir antes la propiedad.
- **RN-9**: Un admin no puede expulsar ni degradar al owner ni a otro admin; el owner puede con todos.
- **RN-10**: Al salir o ser expulsado, se conservan sus estadísticas y su participación histórica en partidos (borrado lógico de la membresía), pero se le elimina de convocatorias abiertas.

## Criterios de aceptación
- **CA-1** (RN-3/4): Dado un código válido, cuando un usuario no miembro se une, entonces es `PLAYER` y `membersCount` aumenta en 1; si ya era miembro → 409.
- **CA-2** (RN-5): Dado un club lleno, cuando alguien intenta unirse → 409 `CONFLICT`.
- **CA-3** (RN-6): Una posición fuera del enum → 400 `INVALID_REQUEST`.
- **CA-4** (RN-7): Un no miembro que consulta un club o sus miembros → 403.
- **CA-5** (RN-7): Un `PLAYER` que intenta subir logo o regenerar código → 403 y **no** se sube ningún fichero.
- **CA-6** (RN-8): El owner que intenta salir → 409 con mensaje que indique transferir la propiedad.
- **CA-7** (RN-10): Un miembro que sale deja de aparecer en el listado de miembros y en convocatorias abiertas, pero sus eventos de partidos pasados se mantienen.

## API (contrato)
| Método | Ruta | Permiso | Estado |
|---|---|---|---|
| GET | `/api/club` | autenticado | ✅ |
| POST | `/api/club/create` | autenticado | ✅ |
| GET | `/api/club/{clubId}` | miembro | ✅ |
| POST | `/api/club/join` | autenticado | ✅ |
| GET | `/api/club/{clubId}/members` | miembro | ✅ |
| PATCH | `/api/club/{clubId}/members/me` | miembro | ✅ |
| POST | `/api/club/{clubId}/logo` (multipart `clubLogo`) | gestor | ✅ |
| POST | `/api/club/{clubId}/regenerate-invitation-code` | gestor | ✅ |
| DELETE | `/api/club/{clubId}/members/me` | miembro | ❌ |
| DELETE | `/api/club/{clubId}/members/{memberId}` | gestor | ❌ |
| PATCH | `/api/club/{clubId}/members/{memberId}/role` | gestor (RN-9) | ❌ |
| POST | `/api/club/{clubId}/transfer-ownership` | owner | ❌ |
| PATCH | `/api/club/{clubId}` | gestor | ❌ |

## Fuera de alcance
- Solicitudes de unión con aprobación (los eventos `JoinRequest*` existen pero no son MVP).
- Borrado definitivo de clubes.

## Preguntas abiertas
- [x] El código de invitación lo ve cualquier miembro (decidido 2026-10-06).
- [x] El email de un miembro **no** se muestra al resto; solo username y foto (decidido 2026-10-06). Pendiente de implementar (T16).
- [x] Un `CAPTAIN` **no** gestiona convocatorias ni equipos (decidido 2026-10-06); gestor = `OWNER` o `ADMIN`.
