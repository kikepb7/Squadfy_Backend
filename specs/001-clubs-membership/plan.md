# 001 — Plan técnico

## Enfoque
Ampliar `ClubService` con casos de uso de gestión de miembros manteniendo las comprobaciones de permisos en servicio. La membresía pasa a tener borrado lógico (`left_at`) para conservar el histórico que referencian `match_service` (eventos, equipos).

## Cambios por capa
- **domain**: `ClubMemberModel` + reglas de jerarquía de roles (`canModerate(actor, target)`), función pura con tests.
- **infrastructure**: columna `left_at TIMESTAMPTZ NULL` en `club_members`; los repositorios filtran `left_at IS NULL`. El índice único `(club_id, user_id)` pasa a parcial (`WHERE left_at IS NULL`) para permitir volver a unirse.
- **service**: `leaveClub`, `kickMember`, `changeRole`, `transferOwnership`, `updateClub`. `ClubMembershipQueryService` ignora miembros que han salido.
- **eventos**: publicar `ClubEvent.MemberJoined`/`MemberKicked` y nuevo `MemberLeft` en `club.events`; `match` los consume para retirar al miembro de convocatorias abiertas (RN-10).
- **api**: nuevos endpoints de la tabla; DTO `ClubMemberDto` sin email (pendiente de la pregunta abierta).
- **user → club**: sustituir `ClubParticipantService.findInUserService` (SQL a `user_service`) por un puerto `UserDirectory` en `common` implementado por `user`.

## Riesgos
- Cambiar el índice único requiere migración (depende de Flyway, spec 006).
- Consumir eventos en `match` añade consistencia eventual: aceptable para RN-10.

## Estrategia de test
- Unitarios: jerarquía de roles.
- Integración: unirse/salir/volver a unirse, expulsión con permisos, owner no puede salir.
