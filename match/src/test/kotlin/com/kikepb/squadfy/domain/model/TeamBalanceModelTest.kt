package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.model.PlayerRatingCalculator.INITIAL_RATING
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TeamBalanceModelTest {

    private val matchId = UUID.randomUUID()

    private fun team(vararg ratings: Double) = ratings.map { TeamBalanceModel.PlayerRating(UUID.randomUUID(), it) }
    private fun team(size: Int, rating: Double) = List(size) { TeamBalanceModel.PlayerRating(UUID.randomUUID(), rating) }

    @Test
    fun `teams of new players are perfectly even`() {
        val balance = TeamBalanceModel.of(matchId, team(5, INITIAL_RATING), team(5, INITIAL_RATING))

        assertEquals(5, balance.teamA.players)
        assertEquals(INITIAL_RATING, balance.teamA.averageRating)
        assertEquals(5 * INITIAL_RATING, balance.teamB.totalRating)
        assertEquals(0.0, balance.averageRatingDifference)
        assertEquals(0.5, balance.teamAExpectedScore)
    }

    @Test
    fun `the stronger team is expected to win`() {
        val balance = TeamBalanceModel.of(matchId, team(1050.0, 1100.0), team(950.0, 1000.0))

        assertEquals(100.0, balance.averageRatingDifference)
        assertTrue(balance.teamAExpectedScore > 0.5)
        assertEquals(1.0, balance.teamAExpectedScore + PlayerRatingCalculator.expectedScore(975.0, 1075.0), 1e-9)
        assertEquals(listOf(1100.0, 1050.0), balance.teamA.playerRatings.map { it.rating })
    }

    @Test
    fun `averages are compared when team sizes differ`() {
        val balance = TeamBalanceModel.of(matchId, team(6, 1000.0), team(5, 1000.0))

        assertEquals(6000.0, balance.teamA.totalRating)
        assertEquals(0.5, balance.teamAExpectedScore)
    }

    @Test
    fun `both teams need players`() {
        assertThrows<IllegalArgumentException> { TeamBalanceModel.of(matchId, emptyList(), team(1000.0)) }
    }
}
