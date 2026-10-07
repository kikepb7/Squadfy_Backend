# 008 — Funcionalidades de la app: invitados, excepciones, horarios de cierre y sorteo, marcador manual

- **Estado**: Borrador — pendiente de confirmar las decisiones marcadas con ⚠️
- **Módulos**: match (principal), notification, common
- **Dependencias**: 002, 003, 004, 005

## Contexto y objetivo
La app móvil tiene funciones que el backend no soporta. Decisión de producto (2026-10-07):

| Función de la app | Decisión |
|---|---|
| Invitados en la convocatoria | **Se mantiene**: cada jugador añade manualmente a sus invitados |
| Excepciones | **Se mantienen**: del **calendario** (saltar o mover una semana) y **ausencias** de jugadores |
| `drawTime` | **Se mantiene**: hora de **cierre** de la convocatoria y hora del **sorteo** configurables por separado |
| Marcador manual | **Se mantiene**: el marcador que introduce el gestor es el **oficial** |
| Valoración manual | **Se retira**: la valoración es automática (rating Elo de la spec 003, a partir del resultado y las estadísticas de cada partido). Se expone además la variación de rating de cada jugador en cada partido |
| Foto por club | **Backlog** |

## Reglas de negocio

### A. Invitados
- **RN-A1**: Un miembro puede añadir invitados (nombre obligatorio, posición opcional) a una convocatoria abierta. Se pueden añadir y quitar solo dentro de la ventana de inscripción, como las inscripciones.
- **RN-A2**: **Los miembros tienen prioridad**. Las plazas confirmadas se asignan así: primero los miembros por orden de inscripción, después los invitados por orden de alta. Un invitado solo queda confirmado si sobra plaza; si después se apunta un miembro y no hay hueco, **el último invitado confirmado pasa a la lista de espera**. Al cierre queda la asignación vigente.
- **RN-A3**: La lista de espera se ordena igual: miembros primero, después invitados. Cuando se libera una plaza, sube el primero según ese orden.
- **RN-A4**: ⚠️ Máximo **2 invitados por miembro** y convocatoria.
- **RN-A5**: Quitan un invitado quien lo invitó o un gestor. ⚠️ El invitado **no depende de que su anfitrión esté apuntado**; si el anfitrión sale del club, sus invitados se retiran de las convocatorias abiertas.
- **RN-A6**: Los invitados **juegan el sorteo** con nivel neutro (1000) y su posición si la indicaron. **No** acumulan rating ni estadísticas, y no se les registran eventos (sus goles se reflejan en el marcador manual, RN-D).
- **RN-A7**: ⚠️ Los invitados no reciben push. El anfitrión no recibe aviso de los cambios de estado de sus invitados; la app lo ve en la convocatoria.

### B. Excepciones del calendario
- **RN-B1**: Un gestor puede crear una excepción para **una fecha concreta del horario semanal** (fecha futura):
  - `CANCELLED`: esa semana no hay partido.
  - `RESCHEDULED`: esa semana el partido se juega en otra fecha y hora (futura).
- **RN-B2**: Si el partido de esa semana **aún no está planificado**, la planificación lo respeta (no lo crea o lo crea en la nueva fecha). Si una semana está cancelada, se planifica directamente la siguiente.
- **RN-B3**: Si **ya está planificado**:
  - `CANCELLED`: se cancela el partido y su convocatoria, y se avisa a los inscritos (como una cancelación normal).
  - `RESCHEDULED`: se mueve el partido. Se conservan las inscripciones, se recalculan el cierre y el sorteo respecto a la nueva fecha y se avisa a todos los miembros (push nueva `match.rescheduled`).
- **RN-B4**: Borrar una excepción la deshace mientras la fecha sea futura y el partido no se haya cerrado:
  - Una semana cancelada por la excepción vuelve a tener su partido (se reactiva con sus inscripciones).
  - Un partido movido vuelve a su fecha y hora habituales.
- **RN-B5**: Una fecha solo puede tener una excepción. Los miembros ven las excepciones; solo los gestores las crean y borran.

### C. Ausencias de jugadores
- **RN-C1**: Un miembro registra periodos de ausencia (desde–hasta, fechas locales del club, motivo opcional). Puede borrarlos.
- **RN-C2**: Si una ausencia cubre la fecha de un partido con convocatoria **abierta** en la que está inscrito, se le **desapunta** automáticamente (sube la lista de espera). Con la convocatoria ya cerrada no se toca nada (regla de la spec 002).
- **RN-C3**: Durante su ausencia no recibe los avisos de apertura ni el recordatorio de esas convocatorias.
- **RN-C4**: ⚠️ Puede apuntarse igualmente a un partido dentro de su ausencia (la ausencia es informativa; apuntarse indica que sí viene).
- **RN-C5**: Todos los miembros ven las ausencias del club (para organizarse); cada uno gestiona las suyas.

### D. Cierre y sorteo configurables (`drawTime`)
- **RN-D1**: El horario del club define la **hora de cierre** (días antes del partido + hora local; por defecto 1 día antes a las 22:00) y la **hora del sorteo** (días antes + hora; por defecto igual que el cierre).
- **RN-D2**: El cierre debe ser anterior al inicio del partido, y el sorteo igual o posterior al cierre y anterior al inicio; si no, 400.
- **RN-D3**: Los equipos se publican automáticamente al llegar la **hora del sorteo** (no al cierre). Se mantienen las reglas de la spec 003: se respeta un sorteo previo con los mismos confirmados y el gestor puede rectificar.
- **RN-D4**: Los partidos manuales y los movidos usan la configuración del horario del club (o los valores por defecto si no hay horario).

### E. Marcador manual
- **RN-E1**: El gestor puede fijar el **marcador final** (goles del equipo A y del B) mientras el partido está `SCHEDULED`.
- **RN-E2**: Si hay marcador manual, **es el oficial**: decide victoria, empate o derrota en el rating y en las estadísticas. Si no lo hay, el marcador es la suma de los goles registrados (comportamiento actual).
- **RN-E3**: Los goles registrados por jugador siguen siendo opcionales y solo cuentan para sus estadísticas individuales, aunque no cuadren con el marcador manual.
- **RN-E4**: Se puede quitar el marcador manual (volver al de los eventos) mientras el partido no esté cerrado.

### F. Valoración automática por partido
- **RN-F1**: No hay valoración manual. Para cada partido cerrado se expone la **variación de rating** de cada jugador miembro (la "valoración del partido"), además del rating acumulado (spec 003).

## Criterios de aceptación
- **CA-1** (RN-A2): 5v5 con 8 miembros y 3 invitados apuntados → 8 miembros y 2 invitados confirmados, 1 invitado en espera; si se apunta otro miembro → 9 miembros y 1 invitado confirmados, 2 invitados en espera.
- **CA-2** (RN-A3): Con un miembro y un invitado en espera, si un confirmado se desapunta sube el miembro.
- **CA-3** (RN-A4/A5): Un tercer invitado del mismo miembro → 409. Un jugador que no es el anfitrión ni gestor no puede quitar el invitado → 403.
- **CA-4** (RN-A6): El sorteo incluye a los invitados confirmados. Al cerrar el partido, los invitados no tienen rating ni estadísticas.
- **CA-5** (RN-B2): Con una excepción `CANCELLED` en la semana siguiente sin planificar, la planificación crea el partido de la semana posterior.
- **CA-6** (RN-B3/B4): Cancelar por excepción una semana ya planificada cancela el partido y avisa; borrar la excepción lo reactiva con sus inscripciones.
- **CA-7** (RN-B3): Mover una semana planificada cambia la fecha, recalcula cierre y sorteo, conserva las inscripciones y emite `match.rescheduled`.
- **CA-8** (RN-C2/C3): Registrar una ausencia que cubre un partido abierto en el que estaba apuntado lo desapunta, promociona la espera y lo excluye del recordatorio.
- **CA-9** (RN-D1–D3): Con cierre a las 21:00 del día anterior y sorteo a las 12:00 del día del partido, la convocatoria se cierra a las 21:00 y los equipos se publican a las 12:00. Una configuración con el sorteo antes del cierre → 400.
- **CA-10** (RN-E2): Con marcador manual 3-2 y solo un gol registrado, el resultado oficial es 3-2 (victoria del A en rating y estadísticas) y el goleador suma 1 gol.
- **CA-11** (RN-F1): Tras cerrar un partido, `MatchDto.ratingChanges` contiene la variación de cada miembro de los equipos.

## API (contrato propuesto)
| Método | Ruta | Permiso |
|---|---|---|
| POST | `/api/v1/announcements/{id}/guests` `{name, position?}` → `MatchAnnouncementDto` | miembro |
| DELETE | `/api/v1/announcements/{id}/guests/{guestId}` → `MatchAnnouncementDto` | anfitrión o gestor |
| GET | `/api/v1/clubs/{clubId}/schedule/exceptions` | miembro |
| POST | `/api/v1/clubs/{clubId}/schedule/exceptions` `{date, type: CANCELLED\|RESCHEDULED, newScheduledAt?, reason?}` | gestor |
| DELETE | `/api/v1/clubs/{clubId}/schedule/exceptions/{exceptionId}` | gestor |
| GET | `/api/v1/clubs/{clubId}/absences?from=&to=` | miembro |
| POST | `/api/v1/clubs/{clubId}/members/me/absences` `{fromDate, toDate, reason?}` | miembro |
| DELETE | `/api/v1/clubs/{clubId}/members/me/absences/{absenceId}` | miembro (propia) |
| PUT / DELETE | `/api/v1/matches/{matchId}/score` `{teamAScore, teamBScore}` | gestor |

Cambios en DTOs existentes:
- `ClubMatchScheduleDto` y sus peticiones: `closeDaysBefore`, `closeTime`, `drawDaysBefore`, `drawTime`.
- `MatchAnnouncementDto`: `drawAt`; las entradas (`entries`, `waitlist`) ganan `participantType` (`MEMBER`/`GUEST`), `guestId?`, `guestName?`, `invitedByMemberId?`; `clubMemberId` pasa a ser nulo en invitados.
- `MatchDto`: `teamAGuests` / `teamBGuests` (`[{guestId, name, position?}]`), `isManualScore`, `ratingChanges` (`{clubMemberId: delta}`), `scheduleDate`.
- `GenerateTeamsRequest` (modo manual): `manualTeamA` / `manualTeamB` aceptan ids de miembro **o** de invitado.
- Push nueva: `match.rescheduled`.

## Fuera de alcance
- Foto por club (backlog).
- Invitados recurrentes o con cuenta propia.
- Ausencias que afecten a otros clubes del mismo usuario (son por club).

## Preguntas abiertas (decisiones por defecto marcadas con ⚠️)
- [ ] RN-A4: ¿máximo 2 invitados por miembro y convocatoria?
- [ ] RN-A5: ¿el invitado se mantiene aunque su anfitrión no esté apuntado?
- [ ] RN-A7: ¿sin push para cambios de estado de invitados?
- [ ] RN-C4: ¿se puede apuntar durante una ausencia?
