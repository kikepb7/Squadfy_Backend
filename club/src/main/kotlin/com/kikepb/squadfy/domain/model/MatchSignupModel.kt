package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchSignupId
import java.time.Instant

data class MatchSignupModel(
    val id: MatchSignupId,
    val matchId: ClubMatchId,
    val clubMemberId: ClubMemberId?,
    val guestName: String?,
    val position: PlayerPositionModel?,
    val rating: Int?,
    val status: SignupStatusModel,
    val signedUpAt: Instant
) {
    val isGuest: Boolean get() = clubMemberId == null
}
