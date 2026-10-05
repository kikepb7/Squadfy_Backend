# Squadfy Backend

Backend para gestionar clubes deportivos (fútbol amateur): clubes con código de invitación, partido semanal con convocatoria y lista de espera, sorteo automático de equipos equilibrados con rating Elo, chat en tiempo real y notificaciones.

**Stack**: Kotlin 2.3 · JVM 21 · Spring Boot 4.1 · PostgreSQL + Flyway · RabbitMQ · Redis · Supabase Storage · Firebase Cloud Messaging.

## Arrancar en local

Requisitos: JDK 21 y Docker.

```bash
docker compose up -d                                           # Postgres, RabbitMQ, Redis, Mailpit
./gradlew :app:bootRun --args='--spring.profiles.active=dev'   # API en http://localhost:8080
```

- Emails capturados: http://localhost:8025 (Mailpit) · RabbitMQ: http://localhost:15672 (guest/guest).
- La configuración por defecto apunta a este stack. Para otro entorno copia `.env.example` a `.env` y rellénalo.
- Sin credenciales de Firebase, usa `FIREBASE_ENABLED=false` (las push se omiten).
- Si un puerto está ocupado: `POSTGRES_PORT=55432 docker compose up -d` (también `RABBITMQ_PORT`, `REDIS_PORT`, `MAIL_PORT`, `APP_PORT`...). Ajusta `DB_URL` en consecuencia.

Todo en contenedores (incluida la aplicación):

```bash
docker compose --profile app up --build
```

## Tests

```bash
./gradlew build
```

Compila y ejecuta tests unitarios y de integración. Los de integración levantan PostgreSQL, RabbitMQ, Redis y Mailpit con Testcontainers: **Docker debe estar arrancado**.

## Base de datos

El esquema lo gestiona Flyway (`app/src/main/resources/db/migration`); Hibernate solo valida (`ddl-auto: validate`). Una base de datos creada antes de Flyway se toma como línea base en V1 automáticamente.

## Salud y despliegue

- `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/info` (públicos).
- Imagen Docker: `docker build -t squadfy-backend .` — perfil `prod` por defecto; toda la infraestructura por variables de entorno (`.env.example`). Las credenciales de Firebase se montan como fichero (`FIREBASE_CREDENTIALS_PATH=file:/run/secrets/firebase.json`), nunca se copian en la imagen.
- CI: `.github/workflows/ci.yml` (build + tests + imagen) en cada PR y en `master`.

## Documentación

- Specs (Spec-Driven Development): [`specs/README.md`](specs/README.md) y [`specs/constitution.md`](specs/constitution.md).
- Análisis inicial: [`docs/analysis/2026-10-05-analisis-repositorio.md`](docs/analysis/2026-10-05-analisis-repositorio.md).
