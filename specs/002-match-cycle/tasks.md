# 002 — Tareas

## Hecho
- [x] T1 Zona horaria en horario (`time_zone`) y cálculo en zona del club.
- [x] T2 `MatchCalendar` puro + `MatchCalendarTest`.
- [x] T3 `MatchPlanningService` idempotente, partido + convocatoria en una transacción, índice único `(club_id, scheduled_at)`.
- [x] T4 Scheduler horario con recuperación tras reinicios.
- [x] T5 Planificar el primer partido al crear/reactivar el horario.
- [x] T6 Bloqueo pesimista y validación de ventana en enroll/withdraw.
- [x] T7 Permisos en todos los endpoints (`ClubAccessGuard`).
- [x] T8 Cancelar partido cancela convocatoria; `COMPLETED` no cancelable.
- [x] T9 Consultas por lotes en listados (sin N+1).

- [x] T11 Decisiones: cierre 22:00 del día anterior; lista de espera con promoción automática.
- [x] T12 Lista de espera + formato de partido (5v5/7v7/11v11).

## Pendiente
- [ ] T10 (manual, BD de desarrollo) Ejecutar en la BD de desarrollo: `ALTER TABLE match_service.club_match_schedules DROP COLUMN IF EXISTS callup_open_days_before_match;`
- [x] T15 Preguntas abiertas resueltas (sin bajas tras el cierre; cambio de día aplica al siguiente partido).
- [ ] T13 (siguiente rama, mejora) Endpoint `GET /api/matchAnnouncements/club/{clubId}/current` con la convocatoria vigente e `isEnrolled` del usuario.
- [x] T14 Tests de integración (Testcontainers) — `MatchFlowIntegrationTest`: CA-1, CA-2, CA-3, CA-4b, CA-6, CA-7. CA-4 (concurrencia real) cubierto por bloqueo pesimista; test multihilo queda para spec 006.
