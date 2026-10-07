# 004 — Plan técnico

## Estadísticas derivadas (RN-7)
- Dominio puro `PlayerStatsCalculator.aggregate(matches)` en `match/domain/model`: recibe por partido cerrado los jugadores de cada equipo, sus minutos efectivos y los eventos; devuelve `PlayerStatsModel` por miembro (V/E/D según goles de cada equipo).
- `PlayerStatsService` carga los partidos `COMPLETED` del club, sus jugadores y eventos con consultas por lotes y agrega en memoria (un club tiene decenas de partidos al año; si creciera, vista materializada).
- `RankedStatsModel` + ordenación por métrica con posición de competición (pura, testeada).
- Endpoint en `match` (`ClubStatsController`, `/api/v1/clubs/{clubId}/stats`), con permiso de miembro mediante `ClubAccessGuard`; incluye todos los miembros activos (`ClubMembershipProvider.findAllMembers`).

## Minutos (RN-6)
- Flyway `V6`: `club_match_schedules.match_duration_minutes` y `matches.duration_minutes` (`not null default 60`); `match_team_players.minutes_played` (nulo = duración completa).
- `MatchPlanningService` copia la duración del horario al crear el partido; el partido manual acepta `durationMinutes`.
- `MatchService.setPlayerMinutes` (gestor, partido `SCHEDULED`, jugador en un equipo, `0..duración`). Volver a sortear equipos reinicia los ajustes.

## Limpieza
- Misma migración `V6`: elimina los contadores de `club_service.club_members` (`goals`, `assists`, `yellow_cards`, `red_cards`, `minutes_played`, `matches_played`), que nunca se actualizaban; se quitan de entidad, modelo y `ClubMemberDto`.

## Estrategia de test
- Unitarios: `PlayerStatsCalculatorTest` (V/E/D, goles, tarjetas, minutos efectivos, solo partidos cerrados), ordenación de la clasificación.
- Integración `match`: CA-3, CA-5, CA-6, CA-7.
- `app`: HTTP de `/stats` y minutos; migración `V6`.
