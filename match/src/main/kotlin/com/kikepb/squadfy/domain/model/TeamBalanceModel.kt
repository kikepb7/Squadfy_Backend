package com.kikepb.squadfy.domain.model

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
        val players: Int,
        val averageRating: Double,
        val totalRating: Double
    )

    companion object {
        fun of(matchId: MatchId, teamARatings: List<Double>, teamBRatings: List<Double>): TeamBalanceModel {
            require(teamARatings.isNotEmpty() && teamBRatings.isNotEmpty()) { "Both teams need players" }
            return TeamBalanceModel(matchId = matchId, teamA = strengthOf(teamARatings), teamB = strengthOf(teamBRatings))
        }

        private fun strengthOf(ratings: List<Double>) = TeamStrength(
            players = ratings.size,
            averageRating = ratings.average(),
            totalRating = ratings.sum()
        )
    }
}
