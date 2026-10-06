# 005 — Tareas

- [ ] T1 Resolver pregunta abierta restante (¿email además de push?).
- [ ] T1b Preferencia de silencio por usuario y club en `notification` (tabla + endpoint + filtro al enviar).
- [ ] T2 Eventos `match.events` + cola/binding en `RabbitMqConfig`.
- [ ] T3 Ampliar `ClubMembershipProvider` con la lista de miembros (userIds) del club.
- [ ] T4 Publicación tras commit en `MatchAnnouncementService`, `MatchTeamService`, `MatchService.cancelMatch`.
- [ ] T5 Job de apertura y recordatorio con marcas de idempotencia.
- [ ] T6 Listener en `notification` + plantillas de mensaje.
- [ ] T7 Tests: publicación (unitario con mock de `EventPublisher`) y consumo.
