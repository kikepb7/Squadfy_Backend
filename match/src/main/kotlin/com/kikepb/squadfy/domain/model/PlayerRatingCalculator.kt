package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubMemberId
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

/**
 * Elo-style skill rating learned online from every completed match (see specs/003-team-draw/plan.md).
 *
 * - Team component: each player moves by K · M · (S − E), where E is the expected result from the
 *   average rating of both teams, S the actual result (1 / 0.5 / 0) and M grows with the goal margin.
 * - Individual component: zero-sum within the match, rewards goals and assists and penalises cards
 *   relative to the match average, so ratings do not inflate over time.
 * - New players move faster (provisional K) until their rating stabilises.
 * - Guests (spec 008 RN-A6) count in the team average with [INITIAL_RATING] but get no delta and
 *   do not take part in the individual component.
 */
object PlayerRatingCalculator {

    const val INITIAL_RATING = 1000.0

    private const val ELO_SCALE = 400.0
    private const val BASE_K = 20.0
    private const val PROVISIONAL_EXTRA_K = 20.0
    const val PROVISIONAL_MATCHES = 10
    private const val INDIVIDUAL_WEIGHT = 6.0
    private const val MAX_INDIVIDUAL_DELTA = 15.0

    data class RatedPlayer(
        val memberId: ClubMemberId,
        val rating: Double,
        val matchesRated: Int,
        val goals: Int = 0,
        val assists: Int = 0,
        val yellowCards: Int = 0,
        val redCards: Int = 0
    )

    fun calculateDeltas(
        teamA: List<RatedPlayer>,
        teamB: List<RatedPlayer>,
        goalsA: Int,
        goalsB: Int,
        guestsA: Int = 0,
        guestsB: Int = 0
    ): Map<ClubMemberId, Double> {
        require(teamA.size + guestsA > 0 && teamB.size + guestsB > 0) { "Both teams need players" }
        if (teamA.isEmpty() && teamB.isEmpty()) return emptyMap()

        val expectedA = expectedScore(teamAverage(teamA, guestsA), teamAverage(teamB, guestsB))
        val actualA = when {
            goalsA > goalsB -> 1.0
            goalsA < goalsB -> 0.0
            else -> 0.5
        }
        val marginMultiplier = 1.0 + ln(1.0 + abs(goalsA - goalsB)) / 2.0
        val teamSwingA = marginMultiplier * (actualA - expectedA)

        val individual = individualDeltas(players = teamA + teamB)

        return teamA.associate { it.memberId to kFactor(it) * teamSwingA + individual.getValue(it.memberId) } +
            teamB.associate { it.memberId to -kFactor(it) * teamSwingA + individual.getValue(it.memberId) }
    }

    fun expectedScore(ratingA: Double, ratingB: Double): Double =
        1.0 / (1.0 + 10.0.pow((ratingB - ratingA) / ELO_SCALE))

    private fun teamAverage(members: List<RatedPlayer>, guests: Int): Double =
        (members.sumOf { it.rating } + guests * INITIAL_RATING) / (members.size + guests)

    private fun kFactor(player: RatedPlayer): Double =
        BASE_K + PROVISIONAL_EXTRA_K * max(0.0, 1.0 - player.matchesRated.toDouble() / PROVISIONAL_MATCHES)

    private fun performance(player: RatedPlayer): Double =
        1.0 * player.goals + 0.75 * player.assists - 0.5 * player.yellowCards - 2.0 * player.redCards

    /** Capped and then re-centred, so the individual component always sums to zero. */
    private fun individualDeltas(players: List<RatedPlayer>): Map<ClubMemberId, Double> {
        val averagePerformance = players.map { performance(it) }.average()
        val capped = players.associate {
            it.memberId to (INDIVIDUAL_WEIGHT * (performance(it) - averagePerformance))
                .coerceIn(-MAX_INDIVIDUAL_DELTA, MAX_INDIVIDUAL_DELTA)
        }
        val drift = capped.values.average()
        return capped.mapValues { (_, delta) -> delta - drift }
    }
}
