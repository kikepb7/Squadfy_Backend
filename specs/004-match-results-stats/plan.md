# 004 — Plan técnico

## Enfoque recomendado: estadísticas derivadas, no contadores
En vez de sumar/restar contadores en `club_members` (frágil ante reaperturas y reintentos), las estadísticas se **calculan a partir de los eventos y alineaciones de partidos `COMPLETED`**:

- `match` expone el puerto `PlayerStatsProvider` en `common` (`statsFor(clubId, memberIds): Map<ClubMemberId, PlayerStats>`), con una consulta agregada sobre `match_events` + `match_team_players` + `matches.status = COMPLETED`.
- `club` lo usa para `ClubMemberDto` y el ranking; `match` lo usa directamente en el sorteo.
- Las columnas de contadores de `club_members` se eliminan en una migración posterior.

Ventajas: idempotente, reabrir/cerrar es trivial (RN-5), una sola fuente de verdad. Si el rendimiento lo requiere, se añade una vista materializada.

## Cambios
- `MatchService.completeMatch` / `reopenMatch` con validaciones RN-3 (hecho, junto con la actualización de rating).
- El sorteo **no** usa estas estadísticas directamente: usa el rating Elo de `player_ratings` (spec 003), que ya incorpora goles, asistencias, tarjetas y resultados. `PlayerStatsProvider` sirve para mostrar estadísticas y el ranking.
- Evento `MatchCompleted` en `match.events` (para notificaciones, spec 005).

## Estrategia de test
- Unitario: agregación de estadísticas con datos en memoria.
- Integración: CA-1..CA-4.
