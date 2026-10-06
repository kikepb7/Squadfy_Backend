package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.ADMIN
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.CAPTAIN
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.OWNER
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.PLAYER
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClubRolePolicyTest {

    @Test
    fun `owner can remove anyone except an owner`() {
        listOf(ADMIN, CAPTAIN, PLAYER).forEach { assertTrue(ClubRolePolicy.canRemove(OWNER, it)) }
        assertFalse(ClubRolePolicy.canRemove(OWNER, OWNER))
    }

    @Test
    fun `admin can only remove captains and players`() {
        assertTrue(ClubRolePolicy.canRemove(ADMIN, PLAYER))
        assertTrue(ClubRolePolicy.canRemove(ADMIN, CAPTAIN))
        assertFalse(ClubRolePolicy.canRemove(ADMIN, ADMIN))
        assertFalse(ClubRolePolicy.canRemove(ADMIN, OWNER))
    }

    @Test
    fun `captains and players cannot remove anyone`() {
        listOf(CAPTAIN, PLAYER).forEach { actor ->
            listOf(OWNER, ADMIN, CAPTAIN, PLAYER).forEach { target -> assertFalse(ClubRolePolicy.canRemove(actor, target)) }
        }
    }

    @Test
    fun `owner grants and revokes admin but never assigns owner`() {
        assertTrue(ClubRolePolicy.canAssign(OWNER, PLAYER, ADMIN))
        assertTrue(ClubRolePolicy.canAssign(OWNER, ADMIN, PLAYER))
        assertFalse(ClubRolePolicy.canAssign(OWNER, PLAYER, OWNER))
    }

    @Test
    fun `admin only toggles between captain and player`() {
        assertTrue(ClubRolePolicy.canAssign(ADMIN, PLAYER, CAPTAIN))
        assertTrue(ClubRolePolicy.canAssign(ADMIN, CAPTAIN, PLAYER))
        assertFalse(ClubRolePolicy.canAssign(ADMIN, PLAYER, ADMIN))
        assertFalse(ClubRolePolicy.canAssign(ADMIN, ADMIN, PLAYER))
        assertFalse(ClubRolePolicy.canAssign(ADMIN, OWNER, ADMIN))
    }

    @Test
    fun `players and captains cannot change roles`() {
        assertFalse(ClubRolePolicy.canAssign(CAPTAIN, PLAYER, CAPTAIN))
        assertFalse(ClubRolePolicy.canAssign(PLAYER, PLAYER, CAPTAIN))
    }
}
