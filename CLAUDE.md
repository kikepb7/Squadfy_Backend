# Squadfy Backend

Backend para gestionar clubes deportivos (fútbol amateur): clubes con código de invitación, partido semanal con convocatoria (apuntarse/desapuntarse), sorteo de equipos equilibrados, estadísticas, chat y notificaciones.

Kotlin 2.2 · JVM 21 · Spring Boot 4 · PostgreSQL (esquema por módulo) · RabbitMQ · Redis · Supabase Storage · Firebase.

## Trabajo guiado por specs (SDD)
- Todo cambio funcional parte de una spec en `specs/NNN-*/` (`spec.md` → `plan.md` → `tasks.md`). Usa la skill `sdd`.
- Principios obligatorios: `specs/constitution.md`. Índice y estado: `specs/README.md`.
- Análisis inicial y deuda conocida: `docs/analysis/2026-10-05-analisis-repositorio.md`.

## Módulos
`app` (ensamblado, seguridad, config) · `common` (tipos, eventos, puertos entre módulos, storage, JWT) · `user` (auth) · `club` · `match` · `chat` · `notification`.
Arquitectura, capas y reglas entre módulos: skill `squadfy-conventions`.

## Comandos
Usa `sh ./gradlew` (el wrapper no es ejecutable en git). Detalle: skill `verify-backend`.
- Compilar: `sh ./gradlew compileKotlin compileTestKotlin --console=plain`
- Tests: `sh ./gradlew build -x :app:test --console=plain`

## Reglas rápidas
- Permisos siempre en la capa de servicio y antes de cualquier efecto (`ClubAccessGuard` en `match`).
- Nada de SQL entre esquemas: usa puertos de `common/domain/<área>`.
- Lógica de negocio pura en `domain/model` con `Clock` inyectado y tests unitarios.
- Specs y docs en español; código en inglés.
