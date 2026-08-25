package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.PlayerPositionModel
import com.kikepb.squadfy.domain.model.SignupStatusModel
import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchSignupId
import java.time.Instant

data class MatchSignupDto(
    val id: MatchSignupId,
    val matchId: ClubMatchId,
    val clubMemberId: ClubMemberId?,
    val guestName: String?,
    val position: PlayerPositionModel?,
    val rating: Int?,
    val status: SignupStatusModel,
    val signedUpAt: Instant
)
