# 005 — Plan técnico

## Flujo
`match` decide **cuándo** y **a quién** (resuelve los `userId` destinatarios) y publica un `MatchEvent` en el exchange `match.events` tras el commit (`EventPublisher.publishAfterCommit`). `notification` consume la cola `notification.match.events`, quita a quien tiene el club silenciado (salvo la promoción), compone el texto y envía la push con los reintentos existentes.

## Eventos (`common/domain/events/match/MatchEvent`)
| Evento | Clave | Origen en `match` | Destinatarios |
|---|---|---|---|
| `AnnouncementOpened` | `match.announcement.opened` | job cada 5 min: `OPEN`, `opensAt <= ahora`, `opened_notified_at` nulo | todos los miembros |
| `AnnouncementClosingSoon` | `match.announcement.closing_soon` | job: `OPEN`, `ahora >= closesAt − 24 h`, `closing_reminder_sent_at` nulo, plazas libres, `opensAt <= closesAt − 24 h` | miembros no inscritos |
| `TeamsPublished` | `match.teams.published` | `MatchTeamService` (cierre y sorteos del gestor) | jugadores de cada equipo |
| `MatchCancelled` | `match.cancelled` | `MatchService.cancelMatch` | inscritos |
| `PromotedFromWaitlist` | `match.waitlist.promoted` | promoción en `MatchAnnouncementService` | el promocionado |

Todos llevan `clubId`, `clubName`, `matchId`, `matchScheduledAt` y `timeZone` para formatear fechas. El paquete del evento se añade a los *trusted packages* de `RabbitMqConfig`.

- `ClubMembershipProvider` gana `findClubName(clubId)`.
- Componente `MatchNotificationPublisher` en `match`: construye los eventos (miembro → usuario, nombre del club, zona) y los publica tras el commit. Depende solo de puertos y repositorios, para no crear ciclos entre servicios.
- Idempotencia (RN-5): migración `V5` con `match_announcements.opened_notified_at` y `closing_reminder_sent_at`, que se marcan en la misma transacción que la publicación (con `findByIdForUpdate`). La migración marca como ya notificadas las convocatorias que existían, para no avisar en bloque al desplegar.

## Notification
- `V5` también crea `notification_service.club_notification_settings (user_id, club_id, muted, updated_at)` con clave única `(user_id, club_id)`.
- `ClubNotificationSettingsService` + controlador en `/api/v1/clubs/{clubId}/notification-settings` (membresía comprobada con `ClubMembershipProvider`).
- `MatchNotificationMessages` (pura, testeada): título y cuerpo en español por tipo de evento, con fecha en la zona del club.
- `PushNotificationModel` se generaliza: `collapseKey` en lugar de `chatId` (chat sigue usando el `chatId`). Se corrige el título de las push de chat (usaba el id del remitente en vez de su nombre).

## Estrategia de test
- Unitarios: `MatchNotificationMessagesTest`.
- Integración `match` (`MatchFlowIntegrationTest`, `EventPublisher` simulado): CA-1..CA-6.
- Integración `app` (RabbitMQ real, Firebase espiado): CA-7 y CA-8 por HTTP.
