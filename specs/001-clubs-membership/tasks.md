# 001 — Tareas

## Hecho
- [x] T1 Crear club, unirse por código, listar clubes y miembros, regenerar código.
- [x] T2 Subida de logo con permisos comprobados antes de subir — `ClubService.updateClubLogo`.
- [x] T3 Posición tipada `PlayerPosition` en `JoinClubRequest` + parser tolerante para datos antiguos — `common/domain/club/PlayerPosition.kt`.
- [x] T4 `PATCH /api/club/{clubId}/members/me` para dorsal y posición.
- [x] T5 Puerto `ClubMembershipProvider` implementado por `ClubMembershipQueryService`.
- [x] T6 Eliminar `ClubStorageService` duplicado.

## Pendiente
- [ ] T7 Resolver preguntas abiertas de la spec (email visible, código visible, permisos de capitán).
- [ ] T8 Migración: `club_members.left_at` + índice único parcial — requiere spec 006 T3.
- [ ] T9 [P] Función pura de jerarquía de roles + tests — `club/domain/model`.
- [ ] T10 `leaveClub` + `DELETE /members/me` (RN-8, RN-10).
- [ ] T11 `kickMember`, `changeRole`, `transferOwnership` + endpoints (RN-9).
- [ ] T12 `PATCH /api/club/{clubId}` para editar datos del club.
- [ ] T13 Publicar `ClubEvent` (joined/left/kicked) y consumir en `match` para retirar inscripciones abiertas.
- [ ] T14 Sustituir SQL a `user_service` por puerto `UserDirectory`.
- [ ] T15 Tests de integración de los flujos de membresía (Testcontainers).
