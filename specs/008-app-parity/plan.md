# 008 — Plan técnico

## Modelo de datos (Flyway V8)
- `match_announcement_entries`: `participant_type` (`MEMBER`/`GUEST`, defecto `MEMBER`), `club_member_id` pasa a nulo, `guest_name`, `guest_position`, `invited_by_member_id`. Índice único `(match_announcement_id, club_member_id)` se mantiene (los NULL no colisionan).
- `match_team_players`: `club_member_id` nulo + `guest_entry_id` (id de la entrada del invitado); check "uno de los dos"; índice único `(match_id, guest_entry_id)`.
- `club_match_schedules`: `close_days_before` (1), `close_time` (22:00), `draw_days_before` (1), `draw_time` (22:00).
- `match_announcements`: `draw_at`, `teams_published_at`.
- `matches`: `schedule_date` (fecha local de la semana del horario; nulo en partidos extra), `team_a_score` / `team_b_score` (marcador manual, nulos).
- `schedule_exceptions` (`club_id`, `schedule_date`, `type`, `new_scheduled_at`, `reason`, `affected_match_id`), único `(club_id, schedule_date)`.
- `member_absences` (`club_id`, `club_member_id`, `from_date`, `to_date`, `reason`).
- Relleno: `schedule_date` de los partidos existentes = fecha local de `scheduled_at`; `draw_at` = `closes_at`; `teams_published_at` para convocatorias ya cerradas.

## Dominio puro (con tests)
- `EnrollmentAllocator`: dada la lista de entradas (miembros e invitados) y el cupo, devuelve el estado de cada una (RN-A2/A3). Toda inscripción, baja, alta/baja de invitado, ausencia o salida del club recalcula con él y detecta qué miembros han pasado a confirmados (push de promoción).
- `MatchCalendar`: cierre y sorteo a partir de `daysBefore + time` en la zona del club; siguiente fecha del horario saltando semanas `CANCELLED` y aplicando `RESCHEDULED`.
- `MatchScore`: marcador oficial (manual si existe, si no por eventos).

## Servicios
- `MatchAnnouncementService`: invitados, recálculo con `EnrollmentAllocator`, retirada por ausencia.
- `ScheduleExceptionService`: crear/borrar con efectos sobre el partido planificado (cancelar/reactivar/mover) y evento `MatchRescheduled`.
- `MemberAbsenceService`: CRUD + retirada de convocatorias abiertas.
- `MatchPlanningService`: usa `schedule_date` en vez del rango de `scheduled_at` y consulta excepciones.
- Job de sorteo separado del de cierre (`drawAt`); el de notificaciones excluye a los ausentes.
- `TeamBalancer` ya trabaja con ids genéricos: los invitados entran como `PlayerProfile` con rating 1000.
- `PlayerRatingService` / `PlayerStatsService`: marcador oficial; los invitados cuentan en la media del equipo para el resultado esperado, pero no reciben variación.

## Estrategia de test
- Unitarios: `EnrollmentAllocator`, cálculo de cierre/sorteo y excepciones, marcador oficial.
- Integración `match`: CA-1..CA-11.
- `app`: migración V8 sobre BD existente y contrato HTTP de los endpoints nuevos.
