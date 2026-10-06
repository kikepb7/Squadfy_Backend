# 001 — Plan técnico

## Enfoque
Gestión de miembros en `ClubService`, con permisos en servicio y reglas de jerarquía en una función pura. Borrado lógico de la membresía para conservar el histórico que referencian `match_service` (eventos, equipos, ratings por `club_member_id`).

## Membresía con borrado lógico
- Migración Flyway `V3`: `club_service.club_members.left_at timestamptz null`.
- Una membresía activa tiene `left_at IS NULL`. Todas las consultas de miembros (permisos, recuento, listados, puerto `ClubMembershipProvider`) filtran por activas.
- Volver a unirse **reactiva la misma fila** (RN-12): el índice único `(club_id, user_id)` se mantiene y el `clubMemberId` no cambia, así que el rating y el historial se conservan.

## Dominio
- `ClubRolePolicy` (`club/domain/model`), pura y testeada: `canRemove(actor, target)`, `canAssign(actor, target, newRole)`, con los casos de RN-9.

## Casos de uso (`ClubService`)
- `leaveClub`, `removeMember`, `changeMemberRole`, `transferOwnership`, `updateClub`; `joinClub` reactiva membresías antiguas.
- `ClubEntity.ownerId` pasa a actualizable (transferencia).

## Eventos (RN-10)
- `ClubEvent.MemberLeft` (nuevo) y `ClubEvent.MemberKicked` llevan `clubMemberId`; `MemberJoined` se publica al unirse (consumidor en spec 005).
- `EventPublisher.publishAfterCommit`: publica al confirmar la transacción para no anunciar cambios revertidos (también lo usará la spec 005).
- `RabbitMqConfig`: exchange `club.events` y cola `match.club.events` enlazada a `club.member.left` y `club.member.kicked`.
- `match`: `MatchClubEventListener` → `MatchAnnouncementService.withdrawFromOpenAnnouncements(clubId, memberId)`: borra sus entradas de convocatorias abiertas y promociona la lista de espera, con el mismo bloqueo que `withdraw`.

## Fronteras entre módulos (T14)
- Puerto `UserDirectory` en `common/domain/user`, implementado por `user` (`UserProfileService`), sustituye el SQL de `club` sobre `user_service.users`.

## Estrategia de test
- Unitarios: `ClubRolePolicyTest` (CA-8, CA-9).
- Integración (`club`): salir, expulsar, roles, transferencia, volver a unirse, edición (CA-6..CA-13 salvo CA-10).
- Integración (`app`): CA-10 extremo a extremo a través de RabbitMQ (salir del club → la convocatoria promociona la espera).
- Migraciones: `V3` en los tests de Flyway.
