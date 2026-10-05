---
name: verify-backend
description: Cómo compilar, testear y verificar el backend Squadfy con Gradle. Usar después de cualquier cambio de código antes de dar una tarea por terminada, o cuando el usuario pida compilar, pasar tests o comprobar que algo funciona.
---

# Verificar Squadfy Backend

Ejecuta Gradle desde la raíz del repo con `./gradlew`. **Docker debe estar arrancado**: los tests de integración usan Testcontainers (PostgreSQL, RabbitMQ, Redis, Mailpit).

## Comandos
| Objetivo | Comando |
|---|---|
| Compilar todo (main + test) | `./gradlew compileKotlin compileTestKotlin --console=plain` |
| Tests de un módulo | `./gradlew :match:test --console=plain` |
| Un test concreto | `./gradlew :app:test --tests '*EventMessaging*' --console=plain` |
| Todo (lo mismo que la CI) | `./gradlew build --console=plain` |
| Imagen Docker | `docker build -t squadfy-backend .` |

- Infraestructura de test compartida en `common/src/testFixtures` (`testImplementation(testFixtures(projects.common))`):
  - `PostgresTestContainerConfiguration` para tests `@DataJpaTest` de un módulo (con `ddl-auto=create-drop` + `create_namespaces=true`, como `match/.../MatchFlowIntegrationTest.kt`).
  - `InfrastructureTestContainersConfiguration` para `@SpringBootTest` de la app completa (como `app/.../SquadfyApplicationTests.kt`).
- `app` valida migraciones Flyway vs entidades: `EmptyDatabaseMigrationTest` y `ExistingDatabaseMigrationTest`. Si cambias una entidad sin migración, fallan.
- No filtres la salida con `grep` sin comprobar también `BUILD SUCCESSFUL`/`BUILD FAILED`: un error de configuración de Gradle no contiene `e:`.

## Comprobar resultados
- Resumen de tests: `grep -ho 'testsuite name="[^"]*" tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' <modulo>/build/test-results/test/*.xml`
- Informe HTML: `<modulo>/build/reports/tests/test/index.html`.

## Arrancar la app
`docker compose up -d` y `JWT_SECRET_BASE64=$(openssl rand -base64 32) FIREBASE_ENABLED=false ./gradlew :app:bootRun --args='--spring.profiles.active=dev'`. Variables: `.env.example`.
Comprobación rápida: `curl localhost:8080/actuator/health`.
