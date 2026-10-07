# 004 — Resultado del partido y estadísticas

- **Estado**: Hecha (rama `match-stats-ranking-feature`)
- **Módulos**: match, club, common
- **Dependencias**: 002, 003

## Contexto y objetivo
Tras jugar, el gestor registra goles, asistencias, tarjetas y, si hace falta, ajusta los minutos de algún jugador; después cierra el partido. Cada miembro ve sus estadísticas y las del resto en una clasificación del club.

## Reglas de negocio
- **RN-1**: Estados del partido: `SCHEDULED → COMPLETED` o `SCHEDULED → CANCELLED`.
- **RN-2**: Solo se registran eventos de jugadores asignados a un equipo del partido, y solo los gestores los registran.
- **RN-3**: Un gestor puede cerrar un partido `SCHEDULED` cuya hora de inicio ya pasó y que tiene equipos.
- **RN-4**: Al cerrar, el resultado es el número de goles registrados por cada equipo; se actualiza el rating de cada jugador (spec 003).
- **RN-5**: Solo el **último** partido cerrado del club se puede reabrir para corregir eventos o minutos; al reabrir se revierten sus cambios de rating. Eventos y minutos solo se editan en partidos `SCHEDULED`.
- **RN-6**: Cada partido tiene una **duración** (por defecto la del horario del club, 60 min si no se indica; entre 10 y 180). Cada jugador de los equipos suma esa duración como minutos jugados, salvo que un gestor fije sus minutos (entre 0 y la duración).
- **RN-7**: Las **estadísticas** de un miembro se calculan a partir de los partidos `COMPLETED` del club en los que jugó: partidos jugados, victorias, empates, derrotas, goles, asistencias, tarjetas amarillas y rojas y minutos. No se guardan contadores: corregir y volver a cerrar un partido deja siempre las cifras coherentes.
- **RN-8**: **Clasificación de estadísticas** del club, visible para todos los miembros, ordenable por goles, asistencias, partidos, minutos o victorias. Incluye a todos los miembros activos (con ceros si no han jugado). Empates en la métrica comparten posición (1, 2, 2, 4).

## Criterios de aceptación
- **CA-1**: Cerrar un partido 2-1 → los ganadores suben de rating y los perdedores bajan.
- **CA-2**: Cerrar dos veces el mismo partido → 409.
- **CA-3**: Reabrir, borrar un gol y cerrar → ratings y estadísticas reflejan solo el resultado corregido.
- **CA-4**: Cerrar un partido antes de su hora → 409.
- **CA-5** (RN-6): En un partido de 60 min, un jugador sin ajuste suma 60 minutos y uno ajustado a 30 suma 30; ajustar 70 → 400; ajustar en un partido cerrado → 409.
- **CA-6** (RN-7): Tras dos partidos cerrados (uno ganado con 2 goles del jugador y uno perdido), sus estadísticas son 2 partidos, 1 victoria, 0 empates, 1 derrota, 2 goles; los partidos cancelados o sin cerrar no cuentan.
- **CA-7** (RN-8): La clasificación por goles ordena de más a menos goleador, incluye a los miembros sin partidos y es visible para cualquier miembro; un no miembro → 403.

## API (contrato v1)
| Método | Ruta | Permiso |
|---|---|---|
| POST | `/api/v1/matches/{matchId}/complete` | gestor (hecho) |
| POST | `/api/v1/matches/{matchId}/reopen` | gestor (hecho) |
| PUT | `/api/v1/matches/{matchId}/players/{memberId}/minutes` `{minutes}` | gestor |
| GET | `/api/v1/clubs/{clubId}/stats?sortBy=GOALS\|ASSISTS\|MATCHES\|MINUTES\|WINS` → `[{rank, clubMemberId, matchesPlayed, wins, draws, losses, goals, assists, yellowCards, redCards, minutesPlayed}]` | miembro |
| GET | `/api/v1/clubs/{clubId}/stats/me` | miembro |

Cambios de contrato: el horario y los partidos manuales aceptan `matchDurationMinutes` / `durationMinutes`; `MatchDto` añade `durationMinutes` y `minutesPlayed` (minutos efectivos de cada jugador); `ClubMemberDto` **deja de llevar** los contadores (`goalsScored`, `assists`… siempre a 0), que pasan a `/stats`.

## Fuera de alcance
- Evento `MatchCompleted` (no hay consumidor; se añadirá cuando una notificación lo necesite).
- Estadísticas por temporada o por rango de fechas.

## Preguntas abiertas
- [x] Se registran **minutos jugados** (decidido 2026-10-06): duración completa por defecto, ajustable por jugador.
- [x] Solo los **gestores** registran goles y eventos (decidido 2026-10-06).
- [x] Victorias, empates y derrotas **cuentan en el ranking** (decidido 2026-10-06).
