package com.kikepb.squadfy.domain.club

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId

/**
 * Read-only port that lets other modules (e.g. match) query club memberships
 * without reaching into the club module's tables. Implemented by the club module.
 */
interface ClubMembershipProvider {

    fun findMembership(clubId: ClubId, userId: UserId): ClubMembershipSnapshot?

    fun findMembers(clubId: ClubId, memberIds: Collection<ClubMemberId>): List<ClubMembershipSnapshot>

    fun findAllMembers(clubId: ClubId): List<ClubMembershipSnapshot>

    fun findClubName(clubId: ClubId): String?

    /** Every membership of the user in any club, active or not (spec 010: data erasure). */
    fun findMemberIdsOfUser(userId: UserId): List<ClubMemberId>
}
