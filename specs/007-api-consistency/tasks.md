# 007 — Tareas

## Fase 0 — Decisiones
- [x] T0 Decisiones: sin convivencia, email oculto, Swagger solo en dev.

## Fase 1 — Base
- [x] T1 springdoc compatible con Boot 4.1 + config OpenAPI (Bearer, grupo v1, activación por propiedad) + reglas en `SecurityConfig`.
- [x] T2 ~~Filtro de deprecación~~ — descartado (sin convivencia).
- [x] T3 Tests HTTP en `app` (`ApiV1IntegrationTest`: usuario + JWT generados en el test).

## Fase 2 — Rutas v1 por módulo
- [x] T4 [P] `user`: auth v1 + `GET /api/v1/me` + `users?query` y foto de perfil.
- [x] T5 [P] `club`: clubs, join, logo, invitation-code, members (sin email).
- [x] T6 [P] `match`: schedule anidado en club, matches (`?status`, acciones POST), teams, team-balance, events.
- [x] T7 [P] `match`: announcements, enrollment, ratings; `announcements/current` (R-7).
- [x] T8 [P] `chat`: chats, messages, participants (sin email).
- [x] T9 [P] `notification`: devices.

## Fase 3 — Documentación y verificación
- [x] T10 `@Operation`/`@ApiResponse`/`@Schema` en todos los endpoints v1; ejemplo de errores.
- [x] T11 Tests CA-1..CA-5.
- [x] T12 Guía de migración para la app (`docs/api/migracion-v1.md`): tabla antigua → nueva y cambios de DTO desde `match-feature`.
- [x] T13 `./gradlew build` en verde; actualizar `specs/README.md`.
- [x] T14 (hallazgo) Spring MVC usa Jackson 3 y faltaba su módulo Kotlin: los cuerpos de petición sin `@JsonProperty` fallaban con 500. Añadido `tools.jackson.module:jackson-module-kotlin` + test de regresión.
- [x] T15 (hallazgo) Corregido `PUT /me/profile-picture`, que leía el cuerpo con `@RequestParam`.
- [x] T16 (hallazgo) El enlace del email de restablecer contraseña apuntaba a un endpoint `POST`; ahora abre `RESET_PASSWORD_URL?token=` (deep link de la app por defecto).
- [ ] T17 Pendiente de producto: confirmar el esquema de deep link de la app para el reset (`squadfy://reset-password` por defecto).
