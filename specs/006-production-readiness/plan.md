# 006 — Plan técnico

## Orden recomendado
1. **Boot GA** (`gradle/libs.versions.toml`: `spring-boot` → última 4.0.x GA; quitar repos milestone/snapshot de `settings.gradle.kts`, `build-logic` y módulos). Revisar compatibilidad Jackson 2 (`com.fasterxml`) frente a Jackson 3, que es el predeterminado en Boot 4; mantener Jackson 2 explícitamente si se necesita.
2. **Flyway**: añadir `spring-boot-starter-flyway` + `flyway-database-postgresql` en `app`. Generar la baseline arrancando una BD vacía con `ddl-auto: create` en Testcontainers y volcando con `pg_dump --schema-only` por esquema → `app/src/main/resources/db/migration/V1__baseline.sql`. Para la BD existente: `baseline-on-migrate: true`, `baseline-version: 1`.
   Aprovechar la baseline para renombrar `callups` → `match_announcements` y `callup_entries` → `match_announcement_entries` (y sus columnas).
3. **Config**: mover hosts a variables (`DB_URL`, `DB_USERNAME`, `REDIS_HOST`, `RABBITMQ_HOST`, `MAIL_USERNAME`, `SUPABASE_PROJECT_URL`, ...); `.env.example` sin valores reales.
4. **Docker**: multi-stage con `./gradlew :app:bootJar`; `docker-compose.yml` con `postgres:16`, `rabbitmq:3-management`, `redis:7`.
5. **Actuator**: `management.endpoints.web.exposure.include=health,info`, `management.endpoint.health.probes.enabled=true`; permitir `/actuator/health/**` en `SecurityConfig`.
6. **Testcontainers**: `testFixtures` en `common` con `@ServiceConnection` para Postgres y RabbitMQ; `contextLoads` usándolo.
7. **CI**: `.github/workflows/ci.yml` (JDK 21 temurin, cache Gradle, `./gradlew build`).
8. **OpenAPI** y **rutas** (R-8, R-9) al final, coordinando con el cliente.

## Riesgos
- Subir de snapshot a GA puede romper APIs que cambiaron (Jackson, Spring Security, AMQP). Hacerlo primero y en un PR aislado.
- La baseline debe coincidir exactamente con producción: validar con `ddl-auto: validate` contra una copia de la BD.
