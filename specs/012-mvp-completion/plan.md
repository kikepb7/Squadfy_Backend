# 012 — Plan técnico

- **A. Tiempo real**: evento de aplicación `ClubDataChangedEvent` en `common/domain/events/live`. `match/service/LiveUpdatePublisher` acumula los cambios de la transacción (sin duplicados) y los publica tras el commit; lo llaman los servicios que modifican partidos, convocatorias, horario, excepciones y ausencias. `ChatWebSocketHandler` lo escucha, resuelve los miembros con `ClubMembershipProvider` y envía `CLUB_DATA_CHANGED` a sus sesiones.
- **B. Estadísticas**: `PlayerStatsService` filtra los partidos cerrados por `scheduledAt` dentro de `[from 00:00, to+1 00:00)` en la zona del club; controlador con `from`/`to`.
- **C. Foto por club**: Flyway V9 añade `club_members.club_picture_url`. `ClubService.updateMyClubPicture/deleteMyClubPicture` (subida a Supabase, borra la anterior). Puerto nuevo `common/domain/user/ProfilePictureProvider`, implementado por chat, para la foto de perfil actual. `ClubUserDataEraser` borra las fotos de club.
- **D. Búsqueda**: consulta JPQL con `LIKE` (escapando `%` y `_`), orden por prefijo y nombre, `Pageable` de 20; endpoint `GET /users/search`.
- **E.** `AuthExceptionHandler`: `USER_EXISTS`.
- **F.** `JacksonJsonMessageConverter` + `DefaultJacksonJavaTypeMapper` en RabbitMQ y `GenericJacksonJsonRedisSerializer` en Redis (Jackson 3 con módulo Kotlin); quitar `@JsonSerialize` de Jackson 2 en los eventos (Jackson 3 escribe `Instant` como ISO-8601). `spring.jpa.open-in-view: false`. Revisar avisos de compilación.
- **Tests**: integración WebSocket (CA-1), estadísticas (CA-2), fotos (CA-3/4, storage simulado), búsqueda (CA-5), registro duplicado (CA-6), suite completa (CA-7).
