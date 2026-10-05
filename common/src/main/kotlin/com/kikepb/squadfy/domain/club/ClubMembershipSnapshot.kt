package com.kikepb.squadfy.domain.club

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId

data class ClubMembershipSnapshot(
    val memberId: ClubMemberId,
    val clubId: ClubId,
    val userId: UserId,
    val role: ClubRole,
    val position: PlayerPosition?
)

enum class ClubRole {
    OWNER,
    ADMIN,
    CAPTAIN,
    PLAYER;

    val canManageClub: Boolean get() = this == OWNER || this == ADMIN
}
