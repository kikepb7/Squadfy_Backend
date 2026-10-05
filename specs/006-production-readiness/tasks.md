# 006 — Tareas

- [ ] T1 Pendiente de producto: hosting y servicios gestionados (condiciona el push de la imagen a un registro y el despliegue continuo).
- [x] T2 Spring Boot 4.1.1 GA + Kotlin 2.3.21 (alineado con el BOM), sin repos snapshot/milestone.
- [x] T3 Flyway: `V1__baseline.sql` + `V2__rename_callups_to_match_announcements.sql`; `ddl-auto: validate`; baseline automático de BBDD existentes. Tests `EmptyDatabaseMigrationTest` / `ExistingDatabaseMigrationTest`.
- [x] T4 Configuración por variables de entorno + `.env.example`; eliminadas propiedades sin uso y hosts reales.
- [x] T5 `gradlew` ejecutable en git.
- [x] T6 Testcontainers en `common/src/testFixtures`; `SquadfyApplicationTests` arranca la app completa sin infraestructura externa.
- [x] T7 Dockerfile multi-stage + `.dockerignore` (sin secretos) + `docker-compose.yml` (Postgres, RabbitMQ, Redis, Mailpit, app). Verificado: arranque en BD vacía y flujo registro → email.
- [x] T8 Actuator: health (liveness/readiness), info con build info; públicos en `SecurityConfig`.
- [x] T9 CI en GitHub Actions (build + tests + imagen).
- [x] T10 `allOpen` de entidades en `squadfy.spring-boot-service`; `group = "com.kikepb"`.
- [x] T14 (hallazgo) Eventos RabbitMQ no se deserializaban (trusted packages): corregido + `EventMessagingIntegrationTest`.
- [x] T15 (hallazgo) `firebase.enabled` para arrancar sin credenciales en local/tests.
- [x] T13 `.gitignore`: `**/build/`, `.DS_Store`, `.kotlin/`, `.env`.
- [ ] T11 springdoc-openapi con Bearer → rama `api-consistency`.
- [ ] T12 Unificar rutas REST (R-9) → rama `api-consistency`, coordinado con el cliente.
- [ ] T16 Publicar la imagen en un registro y desplegar (tras T1).
