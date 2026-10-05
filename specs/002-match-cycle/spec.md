# 002 — Ciclo semanal de partido y convocatoria

- **Estado**: Hecha (rama `match-feature`)
- **Módulos**: match, common
- **Dependencias**: 001

## Contexto y objetivo
Los clubes juegan un partido a la semana, casi siempre el mismo día y hora. El sistema debe crear solo el siguiente partido y su convocatoria, abrirla el día después del partido anterior y cerrarla el día antes del siguiente, y permitir a los miembros apuntarse y desapuntarse.

## Historias de usuario
- Como gestor, quiero configurar el día, la hora, la zona horaria y el formato (5v5, 7v7, 11v11) del club una sola vez.
- Como gestor, quiero crear un partido extra o cancelar uno concreto.
- Como miembro, quiero apuntarme a la convocatoria del próximo partido y desapuntarme si no puedo ir.
- Como miembro, quiero ver quién está apuntado, cuántas plazas quedan y mi posición en la lista de espera.
- Como miembro en lista de espera, quiero entrar automáticamente si alguien se desapunta.

## Reglas de negocio
- **RN-1**: Cada club tiene como mucho un horario (`ClubMatchSchedule`): día de la semana, hora local, zona horaria IANA (por defecto `Europe/Madrid`), formato (`FIVE_A_SIDE`, `SEVEN_A_SIDE`, `ELEVEN_A_SIDE`, por defecto 11v11) e `isActive`. El cupo de plazas confirmadas es 2 × jugadores por lado (10, 14 o 22).
- **RN-2**: Todas las fechas se calculan en la zona horaria del club; se almacenan como `Instant` (UTC).
- **RN-3**: Si el club tiene horario activo, **no tiene ningún partido `SCHEDULED` futuro** y no existe ningún partido (ni cancelado) en la próxima fecha que toca, se crea el partido de esa fecha junto con su convocatoria.
- **RN-4**: La convocatoria **se abre al inicio del día siguiente al último partido no cancelado** anterior. Si no hay partido anterior (o esa fecha ya pasó), se abre inmediatamente.
- **RN-5**: La convocatoria **se cierra a las 22:00 (hora del club) del día anterior al partido**. Si el partido se crea después de ese corte, se cierra a la hora de inicio.
- **RN-6**: Solo los miembros del club pueden apuntarse/desapuntarse, y solo con la convocatoria `OPEN` y dentro de `[opensAt, closesAt)`. Tras el cierre de las 22:00 **no** se puede desapuntar nadie.
- **RN-7**: Nadie puede apuntarse dos veces. Mientras haya plazas libres, la inscripción queda `CONFIRMED`; con el cupo lleno queda `WAITLISTED` (lista de espera por orden de inscripción). Se garantiza también con peticiones concurrentes.
- **RN-7b**: Si un jugador `CONFIRMED` se desapunta, el primero de la lista de espera pasa automáticamente a `CONFIRMED`. Desapuntarse de la lista de espera no promociona a nadie.
- **RN-7c**: Solo los `CONFIRMED` participan en el sorteo de equipos.
- **RN-8**: Cancelar un partido cancela su convocatoria. Un partido `COMPLETED` no se puede cancelar.
- **RN-9**: Un partido cancelado no se vuelve a crear automáticamente; el siguiente se planifica cuando pase esa fecha.
- **RN-10**: Solo los gestores configuran el horario, crean y cancelan partidos. Solo los miembros consultan partidos y convocatorias.
- **RN-11**: La planificación es idempotente: puede ejecutarse varias veces o desde varias instancias sin duplicar partidos.

## Criterios de aceptación
- **CA-1** (RN-3/4/5): Dado un club con horario jueves 20:00 `Europe/Madrid` y último partido el jueves 8, cuando se ejecuta la planificación el jueves 8 a las 22:05, entonces existe el partido del jueves 15 a las 20:00 locales con convocatoria `opensAt = viernes 9 00:00` y `closesAt = miércoles 14 22:00` (hora de Madrid).
- **CA-2** (RN-3): Al crear el horario se planifica de inmediato el primer partido, con la convocatoria abierta ya.
- **CA-3** (RN-6): Apuntarse antes de `opensAt` o a partir de las 22:00 del día anterior → 400 `BAD_REQUEST`.
- **CA-4** (RN-7): En un 5v5 con 9/10 confirmados y dos peticiones simultáneas, una queda `CONFIRMED` y la otra `WAITLISTED`.
- **CA-4b** (RN-7b): Con 10/10 confirmados y A, B en espera, si un confirmado se desapunta, A pasa a `CONFIRMED` y B sigue primero en espera.
- **CA-5** (RN-8): Cancelar un partido → su convocatoria queda `CANCELLED` y no admite inscripciones.
- **CA-6** (RN-10): Un no miembro que consulta la convocatoria → 403 `NOT_CLUB_MEMBER`; un `PLAYER` que crea un partido → 403.
- **CA-7** (RN-11): Ejecutar la planificación dos veces seguidas crea un único partido.
- **CA-8** (RN-2): En la semana del cambio de horario, `closesAt` corresponde a las 22:00 locales.

## API (contrato)
| Método | Ruta | Permiso |
|---|---|---|
| POST | `/api/match-schedules` `{clubId, matchDayOfWeek, matchTime, timeZone?, format?}` | gestor |
| GET | `/api/match-schedules/club/{clubId}` | miembro |
| PATCH | `/api/match-schedules/{scheduleId}` `{matchDayOfWeek?, matchTime?, timeZone?, format?, isActive?}` | gestor |
| POST | `/api/matches` `{clubId, scheduledAt, format?}` | gestor |
| GET | `/api/matches/{matchId}` · `/api/matches/club/{clubId}` · `/api/matches/club/{clubId}/scheduled` | miembro |
| DELETE | `/api/matches/{matchId}/cancel` | gestor |
| GET | `/api/matchAnnouncements/{id}` · `/match/{matchId}` · `/club/{clubId}` | miembro |
| POST | `/api/matchAnnouncements/{id}/enroll` | miembro |
| DELETE | `/api/matchAnnouncements/{id}/withdraw` | miembro |

## Fuera de alcance
- Varios partidos por semana en el mismo horario.
- Convocatorias por invitación selectiva (todos los miembros pueden apuntarse).

## Preguntas abiertas
- [x] Cierre: hasta las 22:00 del día anterior (decidido 2026-10-05).
- [x] Lista de espera con promoción automática (decidido 2026-10-05).
- [x] Bajas tras el cierre de las 22:00: **no permitidas** (decidido 2026-10-05).
- [x] Cambio de día/hora del horario: el partido ya planificado se mantiene; el cambio aplica desde la siguiente planificación. Si hace falta mover el partido, el gestor lo cancela y crea uno manual (decisión por defecto).
