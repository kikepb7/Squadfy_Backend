# 002 — Plan técnico

## Enfoque (implementado)
- **Reglas de fechas puras** en `match/domain/model/MatchCalendar.kt` (`nextMatchDate`, `announcementWindow`), sin Spring, con tests.
- **`MatchPlanningService`** crea partido + convocatoria en una única transacción. `planNextMatch(schedule)` aplica RN-3/RN-9 y es idempotente; el índice único `(club_id, scheduled_at)` en `matches` protege contra ejecuciones concurrentes (RN-11).
- **`MatchSchedulerService`**: job horario `planUpcomingMatches` (minuto 5) y `closeExpiredMatchAnnouncements` (minuto 0). Al ser horario, un reinicio no pierde la semana.
- **Inscripción**: `findByIdForUpdate` (`PESSIMISTIC_WRITE`) serializa enroll/withdraw por convocatoria (RN-7). `ensureOpen` valida estado y ventana con `Clock` (RN-6).
- **Permisos**: `ClubAccessGuard` sobre el puerto `ClubMembershipProvider` (implementado por `club`).
- Alternativa descartada: "abrir N días antes" (`callup_open_days_before_match`) — no cumple la regla del producto y se eliminó.

## Modelo de datos (`match_service`)
- `club_match_schedules`: `+ time_zone varchar(64) not null default 'Europe/Madrid'`, `+ format varchar(16) not null default 'ELEVEN_A_SIDE'`, `- callup_open_days_before_match`.
- `callup_entries`: `+ status varchar(16) not null default 'CONFIRMED'`.
- `matches`: `+ unique (club_id, scheduled_at)`.
- `callups` / `callup_entries`: sin cambios (nombres heredados; renombrar a `match_announcements` en la migración baseline de la spec 006).

## Lista de espera (implementada)
- `callup_entries.status` (`CONFIRMED` | `WAITLISTED`, por defecto `CONFIRMED`); el orden es `enrolled_at`.
- Enroll: `CONFIRMED` si `confirmados < maxPlayers`, si no `WAITLISTED`. Withdraw de un `CONFIRMED` → promoción del primer `WAITLISTED` en la misma transacción, bajo el mismo bloqueo de la convocatoria.
- Pendiente: evento de promoción para notificar al jugador (spec 005).

## Formato
- `MatchFormat` (`FIVE_A_SIDE`=10, `SEVEN_A_SIDE`=14, `ELEVEN_A_SIDE`=22 plazas). `club_match_schedules.format` + `max_players` derivado (se conserva la columna para no romper la BD existente; se copia a la convocatoria).

## Riesgos
- Multi-instancia: los jobs se ejecutan en todas las instancias. El índice único evita duplicados; si escala, añadir ShedLock.
- `@Scheduled` + `Clock.systemUTC()`: tests de integración deben inyectar un `Clock` fijo.

## Estrategia de test
- Unitarios (hechos): `MatchCalendarTest` (siguiente fecha, ventana, mismo día, DST).
- Integración (pendiente): CA-1..CA-8 con Testcontainers Postgres y `Clock` fijo, incluido CA-4 con dos hilos.
