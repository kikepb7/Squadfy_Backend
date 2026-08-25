package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchSignupId

data class MatchParticipantModel(
    val signupId: MatchSignupId,
    val clubMemberId: ClubMemberId?,
    val displayName: String,
    val position: PlayerPositionModel?,
    val rating: Int
)
