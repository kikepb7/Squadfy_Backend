package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole
import com.kikepb.squadfy.domain.type.ClubMemberId
import java.time.Instant

/**
 * Who inherits a club when its owner deletes their account (spec 010 RN-A6): the oldest active
 * ADMIN, otherwise the oldest active member; null when nobody else is left (the club is deleted).
 */
object OwnerSuccession {

    data class Candidate(
        val memberId: ClubMemberId,
        val role: ClubMemberRole,
        val joinedAt: Instant
    )

    fun successor(owner: ClubMemberId, activeMembers: List<Candidate>): ClubMemberId? {
        val others = activeMembers.filter { it.memberId != owner }.sortedWith(compareBy({ it.joinedAt }, { it.memberId }))
        return (others.firstOrNull { it.role == ClubMemberRole.ADMIN } ?: others.firstOrNull())?.memberId
    }
}
