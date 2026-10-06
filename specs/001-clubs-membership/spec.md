# 001 — Clubes y membresía

- **Estado**: Hecha (rama `club-management-feature`)
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
- **RN-9**: Jerarquía de roles:
  - El `OWNER` puede expulsar a cualquier miembro y asignarle `ADMIN`, `CAPTAIN` o `PLAYER`.
  - Un `ADMIN` solo actúa sobre `CAPTAIN` y `PLAYER`: puede expulsarlos y alternarlos entre `CAPTAIN` y `PLAYER`; **no** concede ni retira `ADMIN`.
  - Nadie se expulsa ni cambia su propio rol (para irse se usa "salir del club").
  - El rol `OWNER` solo cambia mediante transferencia (RN-11).
- **RN-10**: Al salir o ser expulsado, se conservan sus estadísticas, rating y participación histórica en partidos (borrado lógico de la membresía), deja de contar como miembro y se le retira de las convocatorias abiertas (si estaba confirmado, sube el primero de la lista de espera).
- **RN-11**: El owner transfiere la propiedad a otro miembro activo: ese miembro pasa a `OWNER` y el antiguo owner pasa a `ADMIN`.
- **RN-12**: Quien salió o fue expulsado puede volver a unirse con un código válido; recupera la misma membresía (historial y rating) con rol `PLAYER`.
- **RN-13**: Los gestores editan nombre, descripción y límite de miembros del club; el límite no puede quedar por debajo del número actual de miembros.
- **RN-14**: Los gestores pueden **vetar** a un miembro actual o antiguo, con los mismos permisos que para expulsar (RN-9) y sin vetarse a sí mismos. Si el miembro está activo, el veto lo expulsa (como RN-10). Un miembro vetado **no puede volver a unirse** aunque tenga un código válido. Los gestores pueden **levantar el veto**; a partir de ahí puede volver a unirse con un código válido (RN-12). Los gestores ven el listado de vetados del club.

## Criterios de aceptación
- **CA-1** (RN-3/4): Dado un código válido, cuando un usuario no miembro se une, entonces es `PLAYER` y `membersCount` aumenta en 1; si ya era miembro → 409.
- **CA-2** (RN-5): Dado un club lleno, cuando alguien intenta unirse → 409 `CONFLICT`.
- **CA-3** (RN-6): Una posición fuera del enum → 400 `INVALID_REQUEST`.
- **CA-4** (RN-7): Un no miembro que consulta un club o sus miembros → 403.
- **CA-5** (RN-7): Un `PLAYER` que intenta subir logo o regenerar código → 403 y **no** se sube ningún fichero.
- **CA-6** (RN-8): El owner que intenta salir → 409 con mensaje que indique transferir la propiedad.
- **CA-7** (RN-10): Un miembro que sale deja de aparecer en el listado de miembros y en convocatorias abiertas, pero sus eventos de partidos pasados se mantienen.
- **CA-8** (RN-9): Un `ADMIN` que intenta expulsar a otro `ADMIN` o hacer `ADMIN` a un `PLAYER` → 403; el `OWNER` sí puede.
- **CA-9** (RN-9): Cualquier miembro que intenta expulsarse o cambiar su propio rol → 400.
- **CA-10** (RN-10): En un 5v5 con 10 confirmados y uno en espera, si un confirmado sale del club, el de la lista de espera pasa a confirmado.
- **CA-11** (RN-11): Tras transferir, el antiguo owner es `ADMIN`, el nuevo es `OWNER` y `ownerId` del club cambia; el antiguo owner ya puede salir.
- **CA-12** (RN-12): Un jugador expulsado que vuelve con el código conserva su `clubMemberId` y su rating, con rol `PLAYER`.
- **CA-13** (RN-13): Bajar `maxMembers` por debajo de los miembros actuales → 400.
- **CA-14** (RN-14): Vetar a un miembro activo lo expulsa (sale de convocatorias abiertas); al intentar unirse con el código → 403 `BANNED_FROM_CLUB`.
- **CA-15** (RN-14): Tras levantar el veto, puede volver a unirse con el código y recupera su membresía como `PLAYER`.
- **CA-16** (RN-14): Un `ADMIN` no puede vetar a otro `ADMIN` ni levantar el veto de un admin vetado por el owner → 403; un `PLAYER` no puede ver el listado de vetados → 403.

## API (contrato v1)
| Método | Ruta | Permiso | Estado |
|---|---|---|---|
| GET | `/api/v1/clubs` | autenticado | ✅ |
| POST | `/api/v1/clubs` | autenticado | ✅ |
| POST | `/api/v1/clubs/join` | autenticado | ✅ (+ RN-12) |
| GET | `/api/v1/clubs/{clubId}` | miembro | ✅ |
| PATCH | `/api/v1/clubs/{clubId}` `{name?, description?, maxMembers?}` | gestor | nuevo |
| PUT | `/api/v1/clubs/{clubId}/logo` | gestor | ✅ |
| POST | `/api/v1/clubs/{clubId}/invitation-code` | gestor | ✅ |
| GET | `/api/v1/clubs/{clubId}/members` | miembro | ✅ |
| PATCH | `/api/v1/clubs/{clubId}/members/me` | miembro | ✅ |
| DELETE | `/api/v1/clubs/{clubId}/members/me` → 204 | miembro (no owner) | nuevo |
| DELETE | `/api/v1/clubs/{clubId}/members/{memberId}` → 204 | gestor (RN-9) | nuevo |
| PATCH | `/api/v1/clubs/{clubId}/members/{memberId}/role` `{role}` | gestor (RN-9) | nuevo |
| POST | `/api/v1/clubs/{clubId}/transfer-ownership` `{memberId}` | owner | nuevo |
| GET | `/api/v1/clubs/{clubId}/bans` → `[{clubMemberId, userId, username, bannedAt}]` | gestor | nuevo |
| POST | `/api/v1/clubs/{clubId}/members/{memberId}/ban` → 204 | gestor (RN-9) | nuevo |
| DELETE | `/api/v1/clubs/{clubId}/members/{memberId}/ban` → 204 | gestor (RN-9) | nuevo |

## Fuera de alcance
- Solicitudes de unión con aprobación (los eventos `JoinRequest*` existen pero no son MVP).
- Borrar un club o que el owner abandone un club en el que es el único miembro.
- Borrado definitivo de clubes.

## Preguntas abiertas
- [x] El código de invitación lo ve cualquier miembro (decidido 2026-10-06).
- [x] El email de un miembro **no** se muestra al resto; solo username y foto (decidido 2026-10-06). Pendiente de implementar (T16).
- [x] Un `CAPTAIN` **no** gestiona convocatorias ni equipos (decidido 2026-10-06); gestor = `OWNER` o `ADMIN`.
- [x] Expulsar permite volver; para impedirlo existe el **veto**, que los gestores pueden levantar más adelante (decidido 2026-10-06, RN-14).
