# 004 — Tareas

## Hecho
- [x] T1 Preguntas abiertas resueltas (minutos jugados sí, solo gestores registran goles, V/E/D en ranking).
- [x] T2 `completeMatch` / `reopenMatch` + endpoints + validaciones + rating.
- [x] T8a Tests de cerrar/reabrir y rating.

## Rama `match-stats-ranking-feature`
- [x] T3 Migración `V6`: duración del partido y del horario, minutos ajustados por jugador, eliminar contadores de `club_members`.
- [x] T4 Duración en horario/partido + `PUT /matches/{id}/players/{memberId}/minutes` (RN-6).
- [x] T5 [P] `PlayerStatsCalculator` + clasificación ordenable, puros y testeados.
- [x] T6 `PlayerStatsService` + `GET /clubs/{id}/stats[?sortBy]` y `/stats/me` (RN-7, RN-8).
- [x] T7 Quitar contadores de `ClubMemberDto`, modelo y entidad.
- [x] T8 Tests de integración (match y app) + guía de la app.
- [x] T9 ~~Evento `MatchCompleted`~~ — fuera de alcance hasta que haya un consumidor.
