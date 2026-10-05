package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.club.ClubMembershipProvider
import com.kikepb.squadfy.domain.club.ClubMembershipSnapshot
import com.kikepb.squadfy.domain.exception.ForbiddenException
import com.kikepb.squadfy.domain.exception.NotClubMemberException
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.UserId
import org.springframework.stereotype.Component

@Component
class ClubAccessGuard(
    private val clubMembershipProvider: ClubMembershipProvider
) {

    fun requireMember(clubId: ClubId, userId: UserId): ClubMembershipSnapshot =
        clubMembershipProvider.findMembership(clubId = clubId, userId = userId)
            ?: throw NotClubMemberException()

    fun requireManager(clubId: ClubId, userId: UserId): ClubMembershipSnapshot {
        val membership = requireMember(clubId = clubId, userId = userId)
        if (!membership.role.canManageClub) throw ForbiddenException()
        return membership
    }
}
