---
name: verify-backend
description: Cómo compilar, testear y verificar el backend Squadfy con Gradle. Usar después de cualquier cambio de código antes de dar una tarea por terminada, o cuando el usuario pida compilar, pasar tests o comprobar que algo funciona.
---

# Verificar Squadfy Backend

`gradlew` no tiene bit de ejecución en git: invócalo siempre como `sh ./gradlew` desde la raíz del repo.

## Comandos
| Objetivo | Comando |
|---|---|
| Compilar todo (main + test) | `sh ./gradlew compileKotlin compileTestKotlin --console=plain` |
| Tests de un módulo | `sh ./gradlew :match:test --console=plain` |
| Todo (compila + tests) | `sh ./gradlew build -x :app:test --console=plain` |

- Los tests `*IntegrationTest` de `match` y `club` levantan Postgres con Testcontainers: **Docker debe estar arrancado**. Patrón a copiar: `match/src/test/.../MatchFlowIntegrationTest.kt` (`@DataJpaTest` + `ddl-auto=create-drop` + `create_namespaces=true` + `@Import` de los servicios + `Clock` mutable).
- `:app:test` (`SquadfyApplicationTests.contextLoads`) necesita Postgres/RabbitMQ/Redis reales y secretos; exclúyelo hasta que exista la base Testcontainers (spec 006 T6).
- No filtres la salida con `grep` sin comprobar también el código de salida y la línea `BUILD SUCCESSFUL`/`BUILD FAILED`: un error de arranque de Gradle no contiene `e:`.

## Comprobar resultados
- Resumen de tests: `grep -ho 'testsuite name="[^"]*" tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' <modulo>/build/test-results/test/*.xml`
- Informe HTML: `<modulo>/build/reports/tests/test/index.html`.

## Arrancar la app (requiere variables de entorno)
`POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `RABBITMQ_PASSWORD`, `MAILGUN_PASSWORD`, `JWT_SECRET_BASE64`, `SUPABASE_SERVICE_KEY` y `firebase-credentials/squadfy-backend-firebase-adminsdk.json` en `app/src/main/resources`.
`sh ./gradlew :app:bootRun --args='--spring.profiles.active=dev'`
