package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.model.PlayerRatingCalculator.INITIAL_RATING
import com.kikepb.squadfy.domain.model.PlayerRatingCalculator.RatedPlayer
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerRatingCalculatorTest {

    private fun team(size: Int, rating: Double = INITIAL_RATING, matches: Int = 20) =
        List(size) { RatedPlayer(memberId = UUID.randomUUID(), rating = rating, matchesRated = matches) }

    @Test
    fun `winners gain and losers lose the same amount when nobody stands out`() {
        val teamA = team(5)
        val teamB = team(5)

        val deltas = PlayerRatingCalculator.calculateDeltas(teamA, teamB, goalsA = 3, goalsB = 1)

        teamA.forEach { assertTrue(deltas.getValue(it.memberId) > 0) }
        teamB.forEach { assertTrue(deltas.getValue(it.memberId) < 0) }
        assertEquals(0.0, deltas.values.sum(), 1e-9)
    }

    @Test
    fun `a draw between equal teams does not change ratings`() {
        val deltas = PlayerRatingCalculator.calculateDeltas(team(7), team(7), goalsA = 2, goalsB = 2)

        deltas.values.forEach { assertEquals(0.0, it, 1e-9) }
    }

    @Test
    fun `an upset moves ratings more than an expected win`() {
        val weak = team(5, rating = 900.0)
        val strong = team(5, rating = 1100.0)

        val upset = PlayerRatingCalculator.calculateDeltas(weak, strong, goalsA = 2, goalsB = 1)
        val expected = PlayerRatingCalculator.calculateDeltas(strong, weak, goalsA = 2, goalsB = 1)

        assertTrue(upset.getValue(weak.first().memberId) > expected.getValue(strong.first().memberId))
    }

    @Test
    fun `a bigger goal margin moves ratings more`() {
        val teamA = team(5)
        val teamB = team(5)

        val narrow = PlayerRatingCalculator.calculateDeltas(teamA, teamB, goalsA = 1, goalsB = 0)
        val thrashing = PlayerRatingCalculator.calculateDeltas(teamA, teamB, goalsA = 6, goalsB = 0)

        assertTrue(thrashing.getValue(teamA.first().memberId) > narrow.getValue(teamA.first().memberId))
    }

    @Test
    fun `individual performance is rewarded without inflating the total`() {
        val scorer = RatedPlayer(memberId = UUID.randomUUID(), rating = INITIAL_RATING, matchesRated = 20, goals = 3, assists = 1)
        val teammates = team(4)
        val rivals = team(5)

        val deltas = PlayerRatingCalculator.calculateDeltas(listOf(scorer) + teammates, rivals, goalsA = 1, goalsB = 1)

        assertTrue(deltas.getValue(scorer.memberId) > deltas.getValue(teammates.first().memberId))
        assertTrue(abs(deltas.values.sum()) < 1e-9)
    }

    @Test
    fun `new players move faster than established ones`() {
        val rookie = RatedPlayer(memberId = UUID.randomUUID(), rating = INITIAL_RATING, matchesRated = 0)
        val veteran = RatedPlayer(memberId = UUID.randomUUID(), rating = INITIAL_RATING, matchesRated = 30)

        val deltas = PlayerRatingCalculator.calculateDeltas(listOf(rookie, veteran), team(2), goalsA = 2, goalsB = 0)

        assertTrue(deltas.getValue(rookie.memberId) > deltas.getValue(veteran.memberId))
    }

    @Test
    fun `guests count in the team average with the initial rating but get no delta`() {
        val strong = PlayerRatingCalculator.RatedPlayer(UUID.randomUUID(), rating = 1300.0, matchesRated = 20)
        val rival = PlayerRatingCalculator.RatedPlayer(UUID.randomUUID(), rating = 1300.0, matchesRated = 20)

        val withoutGuests = PlayerRatingCalculator.calculateDeltas(listOf(strong), listOf(rival), goalsA = 1, goalsB = 0)
        val withGuest = PlayerRatingCalculator.calculateDeltas(listOf(strong), listOf(rival), goalsA = 1, goalsB = 0, guestsB = 1)

        assertEquals(setOf(strong.memberId, rival.memberId), withGuest.keys)
        // Team B is weaker with a 1000 guest, so beating it is worth less.
        assertTrue(withGuest.getValue(strong.memberId) < withoutGuests.getValue(strong.memberId))

        val onlyGuestsInA = PlayerRatingCalculator.calculateDeltas(emptyList(), listOf(rival), goalsA = 2, goalsB = 0, guestsA = 2)
        assertEquals(setOf(rival.memberId), onlyGuestsInA.keys)
        assertTrue(onlyGuestsInA.getValue(rival.memberId) < 0)
    }
}
