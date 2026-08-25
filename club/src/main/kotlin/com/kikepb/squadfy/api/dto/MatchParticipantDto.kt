package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.PlayerPositionModel
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchSignupId

data class MatchParticipantDto(
    val signupId: MatchSignupId,
    val clubMemberId: ClubMemberId?,
    val displayName: String,
    val position: PlayerPositionModel?,
    val rating: Int
)
