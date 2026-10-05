# 003 — Tareas

## Hecho
- [x] T1 `TeamBalancer` puro con tests.
- [x] T1b Rating Elo automático (`PlayerRatingCalculator`, `PlayerRatingService`) aplicado al cerrar y revertido al reabrir.
- [x] T2 Integración en `MatchTeamService` usando snapshots del puerto de membresía.
- [x] T3 Validaciones del modo manual (solo inscritos) y estado `SCHEDULED`.
- [x] T4 Corrección del fallo al regenerar equipos (borrado masivo).
- [x] T5 Permisos de gestor.

## Pendiente
- [x] T6 Visibilidad: cada jugador ve su propio rating.
- [x] T7 `GET /api/player-ratings/club/{clubId}/me`.
- [x] T9 Tests de integración (regenerar equipos, permisos, cierre y rating) — `MatchFlowIntegrationTest`.
- [ ] T8 (siguiente rama, mejora) Devolver el nivel total de cada equipo a los gestores.
- [ ] T10 Pendiente de producto: ¿los equipos se publican automáticamente al cerrar la convocatoria o los confirma el gestor?
