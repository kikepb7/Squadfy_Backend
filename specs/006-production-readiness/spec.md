# 006 — Preparación para producción

- **Estado**: Hecha (rama `production-readiness-feature`), salvo R-8 y R-9, movidos a la rama `api-consistency`
- **Módulos**: todos, build-logic, app
- **Dependencias**: ninguna (se puede hacer en paralelo a 001–005)

## Contexto y objetivo
Que el backend se pueda construir de forma reproducible, arrancar contra una base de datos limpia, configurarse por entorno, observarse y desplegarse automáticamente.

## Requisitos
- **R-1 Builds reproducibles**: Spring Boot en versión GA (4.1.1), sin repositorios snapshot/milestone; `gradlew` ejecutable en git.
- **R-2 Migraciones**: Flyway con una migración baseline por esquema (`user_service`, `club_service`, `match_service`, `chat_service`, `notification_service`) generada a partir del esquema actual; `ddl-auto: validate` en todos los perfiles.
- **R-3 Configuración**: todo host, usuario y URL de infraestructura por variable de entorno (con valores por defecto solo para local). Sin direcciones de email de prueba en la config base. Documentado en `.env.example`.
- **R-4 Contenedor**: `Dockerfile` multi-stage (build con JDK 21, runtime JRE 21, usuario no root) y `docker-compose.yml` para desarrollo local (Postgres, RabbitMQ, Redis).
- **R-5 Observabilidad**: Spring Boot Actuator con `health` (liveness/readiness) y `info` expuestos; logs estructurados en prod.
- **R-6 CI**: GitHub Actions que ejecute `./gradlew build` en cada PR y construya la imagen en `master`.
- **R-7 Tests de integración**: base Testcontainers (Postgres + RabbitMQ) reutilizable por los módulos; `SquadfyApplicationTests.contextLoads` funcionando sin infraestructura externa.
- **R-8 API documentada**: springdoc-openapi con esquema de seguridad Bearer.
- **R-9 Coherencia de rutas** (antes de que exista un cliente en producción): `/api/clubs`, `/api/clubs/{clubId}/schedule`, `/api/clubs/{clubId}/matches`, `/api/match-announcements/{id}/entries/me`, `POST .../cancel`.
- **R-10 Higiene de build**: `allOpen` para entidades JPA en el convention plugin de servicios (sin imports de accessors generados); `group = "com.kikepb"` en todos los módulos; `.gitignore` de `**/build/` y `.DS_Store`.

## Criterios de aceptación
- **CA-1**: `docker compose up` + `./gradlew :app:bootRun --args='--spring.profiles.active=dev'` arranca contra una BD vacía y crea el esquema mediante Flyway.
- **CA-2**: `./gradlew build` en CI pasa sin acceso a servicios externos.
- **CA-3**: `GET /actuator/health` responde `UP` sin autenticación; el resto de actuator no está expuesto.
- **CA-4**: Ningún valor de infraestructura real aparece en `application.yml`.

## Preguntas abiertas
- [ ] ¿Dónde se despliega? (Fly.io, Render, Railway, VPS con Docker, AWS...) Condiciona R-6 y el manejo de secretos.
- [ ] ¿Se mantiene Supabase como Postgres gestionado y CloudAMQP/Redis Cloud, o se autoalojan?
- [ ] ¿Hay ya un cliente (app móvil) consumiendo rutas que impida aplicar R-9 sin versionado?
