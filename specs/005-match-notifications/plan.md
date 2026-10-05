# 005 — Plan técnico

- **Eventos** en `common/domain/events/match/`: `MatchAnnouncementOpened`, `MatchAnnouncementClosingSoon`, `TeamsGenerated`, `MatchCancelled` (exchange `match.events`), siguiendo el patrón de `ClubEvent`/`ChatEvent` y registrando cola + binding en `RabbitMqConfig`.
- **Destinatarios**: el evento lleva los `userId` (resueltos en `match` mediante `ClubMembershipProvider`, ampliado con `findAllMembers(clubId)`), igual que `ChatEvent.NewMessage.recipientIds`.
- **Idempotencia**: columnas `opened_notified_at`, `closing_reminder_sent_at` en `callups`; el job horario de `MatchSchedulerService` las marca en la misma transacción que publica.
- **Publicación tras commit**: usar `@TransactionalEventListener(AFTER_COMMIT)` para publicar en RabbitMQ y no notificar cambios revertidos.
- **Consumo**: `NotificationMatchEventListener` en `notification` → `PushNotificationService` (añadir métodos por tipo con textos en español).
