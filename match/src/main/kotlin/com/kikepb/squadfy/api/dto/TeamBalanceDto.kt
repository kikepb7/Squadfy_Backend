package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId

data class TeamBalanceDto(
    val matchId: MatchId,
    val teamA: TeamStrengthDto,
    val teamB: TeamStrengthDto,
    /** Team A average rating minus team B average rating. */
    val averageRatingDifference: Int,
    /** Expected result of team A between 0 and 1 (0.5 = even). */
    val teamAExpectedScore: Double
)

data class TeamStrengthDto(
    val players: Int,
    val averageRating: Int,
    val totalRating: Int,
    /** Highest rating first. */
    val playerRatings: List<TeamPlayerRatingDto>
)

data class TeamPlayerRatingDto(
    /** Member id, or the guestId when [isGuest] (guests always have the neutral rating). */
    val clubMemberId: ClubMemberId,
    val rating: Int,
    val isGuest: Boolean
)
