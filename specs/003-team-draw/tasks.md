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
- [x] T8 Equilibrio de equipos para gestores (RN-10):
  - [x] T8a [P] `TeamBalanceModel` puro + `TeamBalanceModelTest` (CA-9, CA-10) — `match/domain/model`
  - [x] T8b `MatchTeamService.getTeamBalance` con permiso de gestor y 409 sin equipos (CA-11)
  - [x] T8c `GET /api/matches/{matchId}/team-balance` + `TeamBalanceDto` + mapper
  - [x] T8d Test de integración en `MatchFlowIntegrationTest` (CA-9 extremo a extremo, CA-11)
- [ ] T11 Pendiente de producto: ¿rating individual visible para gestores?
- [x] T10 Publicación automática de equipos al cerrar la convocatoria (`MatchTeamService.publishTeamsOnAnnouncementClosed`) + tests CA-7, CA-8.
