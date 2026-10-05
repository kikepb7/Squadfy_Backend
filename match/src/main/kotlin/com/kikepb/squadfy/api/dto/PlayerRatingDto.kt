package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId

data class PlayerRatingDto(
    val clubId: ClubId,
    val clubMemberId: ClubMemberId,
    val rating: Int,
    val matchesRated: Int,
    val isProvisional: Boolean
)
