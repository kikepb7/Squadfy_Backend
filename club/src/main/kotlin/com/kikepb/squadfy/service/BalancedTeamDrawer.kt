package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.model.PlayerPositionModel
import com.kikepb.squadfy.domain.model.PlayerPositionModel.DEFENDER
import com.kikepb.squadfy.domain.model.PlayerPositionModel.FORWARD
import com.kikepb.squadfy.domain.model.PlayerPositionModel.GOALKEEPER
import com.kikepb.squadfy.domain.model.PlayerPositionModel.MIDFIELDER
import com.kikepb.squadfy.domain.type.MatchSignupId
import kotlin.random.Random

data class DraftPlayer(
    val signupId: MatchSignupId,
    val position: PlayerPositionModel?,
    val rating: Int
)

/**
 * Splits signed-up players into two balanced sides for an 11-a-side match.
 *
 * Players are processed one position group at a time (goalkeepers first, so each side gets one
 * whenever at least two are available, then defenders/midfielders/forwards), highest rating
 * first within a group. Each player is greedily assigned to whichever side currently has the
 * lower total rating, which keeps both overall strength and position mix close between the two
 * teams regardless of squad size or how lopsided the sign-up list is.
 */
object BalancedTeamDrawer {

    private val POSITION_ORDER = listOf(GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD)

    fun draw(players: List<DraftPlayer>, random: Random = Random.Default): Pair<List<MatchSignupId>, List<MatchSignupId>> {
        if (players.isEmpty()) return emptyList<MatchSignupId>() to emptyList()

        val byPosition = players.groupBy { it.position ?: MIDFIELDER }

        var teamARating = 0
        var teamBRating = 0
        val teamA = mutableListOf<MatchSignupId>()
        val teamB = mutableListOf<MatchSignupId>()

        POSITION_ORDER.forEach { position ->
            val group = byPosition[position].orEmpty()
            group
                .groupBy { it.rating }
                .toSortedMap(compareByDescending { it })
                .values
                .flatMap { sameRating -> sameRating.shuffled(random) }
                .forEach { player ->
                    val assignToTeamA = when {
                        teamARating < teamBRating -> true
                        teamBRating < teamARating -> false
                        teamA.size <= teamB.size -> true
                        else -> false
                    }

                    if (assignToTeamA) {
                        teamA += player.signupId
                        teamARating += player.rating
                    } else {
                        teamB += player.signupId
                        teamBRating += player.rating
                    }
                }
        }

        return teamA to teamB
    }
}
