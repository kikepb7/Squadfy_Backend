# Constitución de Squadfy Backend

Principios no negociables. Toda spec, plan y PR debe cumplirlos; si una feature necesita romper uno, se discute y se modifica este documento primero.

## 1. Spec antes que código
- Ningún cambio funcional se implementa sin `spec.md` aprobada en `specs/NNN-<feature>/`.
- La spec describe **qué** y **por qué** (reglas de negocio, criterios de aceptación). El **cómo** va en `plan.md`. El trabajo ejecutable va en `tasks.md`.
- Si durante la implementación cambia una regla de negocio, se actualiza la spec en el mismo PR.

## 2. Monolito modular con fronteras explícitas
- Módulos: `user`, `club`, `match`, `chat`, `notification`, `common`, `app` (ensamblado).
- Un módulo **no** lee tablas de otro esquema ni importa clases de otro módulo de feature. La comunicación es:
  - **Síncrona**: puertos (interfaces) en `common/domain/<área>/`, implementados por el módulo dueño (ej. `ClubMembershipProvider`).
  - **Asíncrona**: eventos `SquadfyEvent` por RabbitMQ.
- Cada módulo tiene su esquema Postgres (`user_service`, `club_service`, `match_service`, ...).

## 3. Seguridad por defecto
- Todo endpoint (salvo `/api/auth/**` públicos) requiere JWT.
- Todo caso de uso que toque datos de un club verifica pertenencia (`requireMember`) o permisos de gestión (`requireManager`) **en la capa de servicio**, nunca solo en el controlador.
- Las comprobaciones de permisos ocurren **antes** de cualquier efecto secundario (subidas a storage, escrituras, eventos).
- Ningún secreto en el repositorio: todo por variables de entorno.

## 4. Dominio puro y testeable
- Las reglas de negocio no triviales (fechas, sorteos, cálculos) viven en funciones/clases puras en `domain/model`, sin Spring ni JPA, con tests unitarios.
- El tiempo se obtiene de un `java.time.Clock` inyectado; nunca `Instant.now()` en lógica de negocio.
- Todo cálculo de calendario se hace en la zona horaria del club.

## 5. Datos consistentes
- Las invariantes se protegen en base de datos (índices únicos, NOT NULL) además de en código.
- Operaciones con concurrencia (cupos, inscripciones) usan bloqueo o restricciones, no solo comprobaciones previas.
- Los cambios de esquema se hacen con migraciones versionadas de Flyway (`app/src/main/resources/db/migration`); una migración aplicada no se modifica nunca.

## 6. API coherente
- Errores con formato `{ "code": "...", "message": "..." }` (o `errors: []` en validación) y códigos HTTP correctos: 400 validación/regla, 403 permisos, 404 no encontrado, 409 conflicto.
- DTOs de API separados de modelos de dominio y entidades JPA. Mapeo en `api/mappers` e `infrastructure/database/mappers`.

## 7. Calidad mínima para merge
- `./gradlew build` en verde (compila, tests unitarios y de integración), igual que la CI.
- Tests unitarios para lógica de dominio nueva; tests de integración con Testcontainers (`common/src/testFixtures`) para flujos críticos y para cada nuevo evento entre módulos.
- Sin código muerto ni clases duplicadas.
