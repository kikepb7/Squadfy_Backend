package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.ADMIN
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.CAPTAIN
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.OWNER
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.PLAYER
import com.kikepb.squadfy.domain.model.OwnerSuccession.Candidate
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OwnerSuccessionTest {

    private val start = Instant.parse("2026-01-01T00:00:00Z")
    private val owner = Candidate(UUID.randomUUID(), OWNER, start)

    private fun member(role: ClubMemberModel.ClubMemberRole, days: Long) = Candidate(UUID.randomUUID(), role, start.plusSeconds(days * 86_400))

    @Test
    fun `the oldest admin inherits even if a player joined earlier`() {
        val oldPlayer = member(PLAYER, 1)
        val newAdmin = member(ADMIN, 5)
        val oldAdmin = member(ADMIN, 3)

        assertEquals(oldAdmin.memberId, OwnerSuccession.successor(owner.memberId, listOf(owner, oldPlayer, newAdmin, oldAdmin)))
    }

    @Test
    fun `without admins the oldest member inherits`() {
        val captain = member(CAPTAIN, 2)
        val player = member(PLAYER, 1)

        assertEquals(player.memberId, OwnerSuccession.successor(owner.memberId, listOf(captain, owner, player)))
    }

    @Test
    fun `nobody inherits a club where the owner is alone`() {
        assertNull(OwnerSuccession.successor(owner.memberId, listOf(owner)))
        assertNull(OwnerSuccession.successor(owner.memberId, emptyList()))
    }
}
