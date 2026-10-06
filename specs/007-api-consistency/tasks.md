# 007 — Tareas

## Fase 0 — Decisiones
- [ ] T0 Resolver preguntas abiertas (app publicada / convivencia, fecha Sunset, email en rutas deprecadas, Swagger en prod).

## Fase 1 — Base
- [ ] T1 springdoc compatible con Boot 4.1 + config OpenAPI (Bearer, grupo v1, activación por propiedad) + reglas en `SecurityConfig`.
- [ ] T2 `DeprecatedApiFilter` + propiedad `squadfy.api.legacy-sunset` (solo si hay convivencia).
- [ ] T3 Base de tests HTTP (`ApiTestClient` con usuario autenticado y JWT) en `app`.

## Fase 2 — Rutas v1 por módulo
- [ ] T4 [P] `user`: auth v1 + `GET /api/v1/me` + `users?query` y foto de perfil.
- [ ] T5 [P] `club`: clubs, join, logo, invitation-code, members (sin email).
- [ ] T6 [P] `match`: schedule anidado en club, matches (`?status`, acciones POST), teams, team-balance, events.
- [ ] T7 [P] `match`: announcements, enrollment, ratings; `announcements/current` (R-7).
- [ ] T8 [P] `chat`: chats, messages, participants (sin email).
- [ ] T9 [P] `notification`: devices.

## Fase 3 — Documentación y verificación
- [ ] T10 `@Operation`/`@ApiResponse`/`@Schema` en todos los endpoints v1; ejemplo de errores.
- [ ] T11 Tests CA-1..CA-5.
- [ ] T12 Guía de migración para la app (`docs/api/migracion-v1.md`): tabla antigua → nueva y cambios de DTO desde `match-feature`.
- [ ] T13 `./gradlew build` en verde; actualizar `specs/README.md`.
