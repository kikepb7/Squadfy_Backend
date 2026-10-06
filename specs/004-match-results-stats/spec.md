# 004 — Resultado del partido y estadísticas

- **Estado**: En curso (cerrar/reabrir + rating hechos; ranking y estadísticas visibles pendientes)
- **Módulos**: match, club
- **Dependencias**: 002, 003

## Contexto y objetivo
Tras jugar, el gestor registra goles, asistencias y tarjetas y cierra el partido. Las estadísticas de cada jugador se acumulan en su ficha del club, lo que alimenta el nivel del sorteo (003) y un ranking.

## Reglas de negocio
- **RN-1**: Estados del partido: `SCHEDULED → COMPLETED` o `SCHEDULED → CANCELLED`. (`IN_PROGRESS` no es necesario para el MVP.)
- **RN-2**: Solo se registran eventos de jugadores asignados a un equipo del partido (ya implementado).
- **RN-3**: Un gestor puede cerrar (`complete`) un partido `SCHEDULED` cuya hora de inicio ya pasó y que tiene equipos.
- **RN-4**: Al cerrar, el resultado es el número de goles registrados por cada equipo; se actualiza el rating de cada jugador (spec 003 RN-6) y cuenta como partido jugado.
- **RN-5**: Solo el **último** partido cerrado del club se puede reabrir para corregir eventos; al reabrir se revierten exactamente sus cambios de rating. Los eventos solo se editan en partidos `SCHEDULED`.
- **RN-6**: Ranking del club por goles, asistencias, partidos jugados, minutos jugados y victorias/empates/derrotas.

## Criterios de aceptación
- **CA-1**: Cerrar un partido 2-1 → los jugadores del equipo ganador suben de rating y los del perdedor bajan; `matchesRated` +1 para todos.
- **CA-2**: Cerrar dos veces el mismo partido → 409, estadísticas sin duplicar.
- **CA-3**: Reabrir, borrar un gol y cerrar → los ratings son los que habrían resultado de cerrar directamente con el resultado corregido.
- **CA-4**: Cerrar un partido antes de su hora → 409.

## API (contrato propuesto)
| Método | Ruta | Permiso |
|---|---|---|
| POST | `/api/matches/{matchId}/complete` | gestor |
| POST | `/api/matches/{matchId}/reopen` | gestor |
| GET | `/api/club/{clubId}/ranking?by=goals|assists|matches` | miembro |

## Preguntas abiertas
- [x] Se registran **minutos jugados** (decidido 2026-10-06). Propuesta: por defecto la duración completa para cada jugador de los equipos; el gestor puede ajustar los de un jugador concreto.
- [x] Solo los **gestores** registran goles y eventos (decidido 2026-10-06) (ya implementado).
- [x] Victorias, empates y derrotas **cuentan en el ranking** (decidido 2026-10-06) (y ya cuentan en el rating Elo).
