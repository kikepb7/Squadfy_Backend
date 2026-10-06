package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId

data class PlayerRatingDto(
    val clubId: ClubId,
    val clubMemberId: ClubMemberId,
    val rating: Int,
    val matchesRated: Int,
    val isProvisional: Boolean,
    /** Position in the club classification (ties share the position). */
    val rank: Int,
    val totalPlayers: Int
)

/** Row of the club classification by rating. Name and photo come from GET /api/v1/clubs/{clubId}/members. */
data class RatingLeaderboardEntryDto(
    val rank: Int,
    val clubMemberId: ClubMemberId,
    val rating: Int,
    val matchesRated: Int,
    val isProvisional: Boolean
)
