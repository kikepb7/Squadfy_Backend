package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId

/**
 * How even the two teams of a match are, from the players' current ratings (spec 003 RN-10).
 * Averages are compared because team sizes may differ by one player.
 */
data class TeamBalanceModel(
    val matchId: MatchId,
    val teamA: TeamStrength,
    val teamB: TeamStrength
) {
    val averageRatingDifference: Double get() = teamA.averageRating - teamB.averageRating

    /** Expected result of team A between 0 and 1 (0.5 = even), same Elo formula that updates ratings. */
    val teamAExpectedScore: Double get() =
        PlayerRatingCalculator.expectedScore(ratingA = teamA.averageRating, ratingB = teamB.averageRating)

    data class TeamStrength(
        /** Highest rating first. */
        val playerRatings: List<PlayerRating>
    ) {
        val players: Int get() = playerRatings.size
        val averageRating: Double get() = playerRatings.map { it.rating }.average()
        val totalRating: Double get() = playerRatings.sumOf { it.rating }
    }

    data class PlayerRating(
        val clubMemberId: ClubMemberId,
        val rating: Double
    )

    companion object {
        fun of(matchId: MatchId, teamA: List<PlayerRating>, teamB: List<PlayerRating>): TeamBalanceModel {
            require(teamA.isNotEmpty() && teamB.isNotEmpty()) { "Both teams need players" }
            return TeamBalanceModel(
                matchId = matchId,
                teamA = TeamStrength(playerRatings = teamA.sortedByDescending { it.rating }),
                teamB = TeamStrength(playerRatings = teamB.sortedByDescending { it.rating })
            )
        }
    }
}
