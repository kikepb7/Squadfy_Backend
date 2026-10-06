package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.ADMIN
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.CAPTAIN
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.OWNER
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.PLAYER

/**
 * Who can act on whom inside a club (spec 001 RN-9):
 * - OWNER acts on anyone else and can assign ADMIN, CAPTAIN or PLAYER.
 * - ADMIN only acts on CAPTAIN/PLAYER and only toggles between those two roles.
 * - OWNER is never assigned through a role change (ownership is transferred).
 * Acting on oneself is handled by the callers (leave / no self role change).
 */
object ClubRolePolicy {

    private val ROLES_MANAGED_BY_ADMIN = setOf(CAPTAIN, PLAYER)

    fun canRemove(actor: ClubMemberRole, target: ClubMemberRole): Boolean =
        when (actor) {
            OWNER -> target != OWNER
            ADMIN -> target in ROLES_MANAGED_BY_ADMIN
            CAPTAIN, PLAYER -> false
        }

    fun canAssign(actor: ClubMemberRole, target: ClubMemberRole, newRole: ClubMemberRole): Boolean =
        newRole != OWNER && when (actor) {
            OWNER -> target != OWNER
            ADMIN -> target in ROLES_MANAGED_BY_ADMIN && newRole in ROLES_MANAGED_BY_ADMIN
            CAPTAIN, PLAYER -> false
        }
}
