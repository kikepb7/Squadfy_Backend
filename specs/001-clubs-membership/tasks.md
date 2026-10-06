# 001 — Tareas

## Hecho
- [x] T1 Crear club, unirse por código, listar clubes y miembros, regenerar código.
- [x] T2 Subida de logo con permisos comprobados antes de subir.
- [x] T3 Posición tipada `PlayerPosition` + parser tolerante para datos antiguos.
- [x] T4 Editar mi dorsal y posición.
- [x] T5 Puerto `ClubMembershipProvider` implementado por `ClubMembershipQueryService`.
- [x] T6 Eliminar `ClubStorageService` duplicado.
- [x] T7 Preguntas abiertas resueltas (código visible para miembros, email oculto, capitán sin permisos de gestión).
- [x] T16 `ClubMemberDto` sin `email` (hecho en la spec 007).

## Hecho en la rama `club-management-feature`
- [x] T8 Migración `V3__club_members_left_at.sql` + filtro de membresías activas en todas las consultas.
- [x] T9 [P] `ClubRolePolicy` + `ClubRolePolicyTest` (CA-8, CA-9).
- [x] T10 Salir del club (RN-8, RN-10) — `DELETE /members/me`.
- [x] T11 Expulsar, cambiar rol y transferir propiedad (RN-9, RN-11) + endpoints.
- [x] T12 Editar club (RN-13) — `PATCH /clubs/{clubId}`.
- [x] T17 Volver a unirse reactivando la membresía (RN-12).
- [x] T13 Eventos `MemberJoined/Left/Kicked` tras commit + consumo en `match` (retirar de convocatorias abiertas y promocionar espera).
- [x] T14 Puerto `UserDirectory` en lugar de SQL a `user_service`.
- [x] T15 Tests de integración (club y app) + migraciones; guía de migración de la API actualizada.

## Pendiente
- [ ] T18 Pendiente de producto: ¿un jugador expulsado puede volver con el código o queda vetado? (hoy puede volver).
