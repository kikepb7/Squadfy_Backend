# Squadfy Backend

Backend para gestionar clubes deportivos (fútbol amateur): clubes con código de invitación, partido semanal con convocatoria (apuntarse/desapuntarse), sorteo de equipos equilibrados, estadísticas, chat y notificaciones.

Kotlin 2.3 · JVM 21 · Spring Boot 4.1 · PostgreSQL (esquema por módulo, Flyway) · RabbitMQ · Redis · Supabase Storage · Firebase.

## Trabajo guiado por specs (SDD)
- Todo cambio funcional parte de una spec en `specs/NNN-*/` (`spec.md` → `plan.md` → `tasks.md`). Usa la skill `sdd`.
- Principios obligatorios: `specs/constitution.md`. Índice y estado: `specs/README.md`.
- Análisis inicial y deuda conocida: `docs/analysis/2026-10-05-analisis-repositorio.md`.

## Módulos
`app` (ensamblado, seguridad, config) · `common` (tipos, eventos, puertos entre módulos, storage, JWT) · `user` (auth) · `club` · `match` · `chat` · `notification`.
Arquitectura, capas y reglas entre módulos: skill `squadfy-conventions`.

## Comandos
Detalle: skill `verify-backend`. Arranque local: `README.md`.
- Compilar: `./gradlew compileKotlin compileTestKotlin --console=plain`
- Build completo (unit + integración con Testcontainers, requiere Docker): `./gradlew build --console=plain`
- Stack local: `docker compose up -d` + `./gradlew :app:bootRun --args='--spring.profiles.active=dev'`

## Reglas rápidas
- Permisos siempre en la capa de servicio y antes de cualquier efecto (`ClubAccessGuard` en `match`).
- Nada de SQL entre esquemas: usa puertos de `common/domain/<área>`.
- Lógica de negocio pura en `domain/model` con `Clock` inyectado y tests unitarios.
- Cambios de esquema solo con migraciones Flyway nuevas (`app/src/main/resources/db/migration/V<n>__*.sql`); nunca editar una ya aplicada.
- Configuración de infraestructura solo por variables de entorno (documentarlas en `.env.example`).
- Specs y docs en español; código en inglés.
