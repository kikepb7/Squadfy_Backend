package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.club.PlayerPosition
import com.kikepb.squadfy.domain.type.ClubMemberId
import kotlin.math.abs
import kotlin.random.Random

data class PlayerProfile(
    val memberId: ClubMemberId,
    val position: PlayerPosition?,
    val rating: Double
)

data class TeamAssignment(
    val teamA: List<PlayerProfile>,
    val teamB: List<PlayerProfile>
) {
    val ratingA: Double get() = teamA.sumOf { it.rating }
    val ratingB: Double get() = teamB.sumOf { it.rating }
    val ratingDifference: Double get() = abs(ratingA - ratingB)
}

/**
 * Splits players into two teams that are as even as possible:
 * 1. Team sizes differ by at most one.
 * 2. Each position (goalkeepers first) is spread evenly between both teams.
 * 3. Total rating is balanced with a greedy draft followed by same-position swaps.
 *
 * Players with equal rating are shuffled with [random], so repeated draws give different line-ups.
 */
class TeamBalancer(private val random: Random = Random.Default) {

    fun balance(players: List<PlayerProfile>): TeamAssignment {
        require(players.map { it.memberId }.toSet().size == players.size) { "Players must be unique" }

        val teamA = mutableListOf<PlayerProfile>()
        val teamB = mutableListOf<PlayerProfile>()

        players
            .shuffled(random)
            .groupBy { it.position }
            .toList()
            .sortedBy { (position, _) -> POSITION_DRAFT_ORDER.indexOf(position) }
            .forEach { (position, group) ->
                group.sortedByDescending { it.rating }.forEach { player ->
                    pickTeam(teamA, teamB, position).add(player)
                }
            }

        improveBySwaps(teamA, teamB)
        return TeamAssignment(teamA = teamA.toList(), teamB = teamB.toList())
    }

    private fun pickTeam(
        teamA: MutableList<PlayerProfile>,
        teamB: MutableList<PlayerProfile>,
        position: PlayerPosition?
    ): MutableList<PlayerProfile> {
        val positionCountA = teamA.count { it.position == position }
        val positionCountB = teamB.count { it.position == position }
        return when {
            positionCountA != positionCountB -> if (positionCountA < positionCountB) teamA else teamB
            teamA.size != teamB.size -> if (teamA.size < teamB.size) teamA else teamB
            else -> if (teamA.sumOf { it.rating } <= teamB.sumOf { it.rating }) teamA else teamB
        }
    }

    /** Swaps same-position players while it reduces the rating gap; sizes and position spread are kept. */
    private fun improveBySwaps(teamA: MutableList<PlayerProfile>, teamB: MutableList<PlayerProfile>) {
        repeat(MAX_SWAP_ITERATIONS) {
            val currentDiff = teamA.sumOf { it.rating } - teamB.sumOf { it.rating }
            var bestSwap: Pair<Int, Int>? = null
            var bestDiff = abs(currentDiff)

            for (i in teamA.indices) {
                for (j in teamB.indices) {
                    if (teamA[i].position != teamB[j].position) continue
                    val newDiff = abs(currentDiff - 2 * (teamA[i].rating - teamB[j].rating))
                    if (newDiff < bestDiff - EPSILON) {
                        bestDiff = newDiff
                        bestSwap = i to j
                    }
                }
            }

            val (i, j) = bestSwap ?: return
            val playerA = teamA[i]
            teamA[i] = teamB[j]
            teamB[j] = playerA
        }
    }

    private companion object {
        const val MAX_SWAP_ITERATIONS = 100
        const val EPSILON = 1e-9
        val POSITION_DRAFT_ORDER = listOf(
            PlayerPosition.GOALKEEPER,
            PlayerPosition.DEFENDER,
            PlayerPosition.MIDFIELDER,
            PlayerPosition.FORWARD,
            null
        )
    }
}
