package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.club.PlayerPosition
import com.kikepb.squadfy.domain.club.PlayerPosition.DEFENDER
import com.kikepb.squadfy.domain.club.PlayerPosition.FORWARD
import com.kikepb.squadfy.domain.club.PlayerPosition.GOALKEEPER
import com.kikepb.squadfy.domain.club.PlayerPosition.MIDFIELDER
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TeamBalancerTest {

    private fun player(position: PlayerPosition?, rating: Double = PlayerRatingCalculator.INITIAL_RATING) =
        PlayerProfile(memberId = UUID.randomUUID(), position = position, rating = rating)

    @ParameterizedTest
    @ValueSource(ints = [2, 3, 7, 10, 11, 14, 22])
    fun `team sizes differ by at most one and every player is assigned once`(size: Int) {
        val positions = listOf(GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD, null)
        val players = List(size) { player(positions[it % positions.size], rating = (it % 7).toDouble()) }

        val result = TeamBalancer(Random(size)).balance(players)

        assertTrue(abs(result.teamA.size - result.teamB.size) <= 1)
        assertEquals(players.map { it.memberId }.toSet(), (result.teamA + result.teamB).map { it.memberId }.toSet())
        assertEquals(size, result.teamA.size + result.teamB.size)
    }

    @Test
    fun `goalkeepers are split between both teams`() {
        val players = listOf(player(GOALKEEPER, 9.0), player(GOALKEEPER, 3.0)) + List(8) { player(MIDFIELDER) }

        val result = TeamBalancer(Random(1)).balance(players)

        assertEquals(1, result.teamA.count { it.position == GOALKEEPER })
        assertEquals(1, result.teamB.count { it.position == GOALKEEPER })
    }

    @Test
    fun `every position is spread evenly`() {
        val players = List(4) { player(DEFENDER) } + List(4) { player(MIDFIELDER) } + List(2) { player(FORWARD) }

        val result = TeamBalancer(Random(7)).balance(players)

        listOf(DEFENDER, MIDFIELDER, FORWARD).forEach { position ->
            assertEquals(result.teamA.count { it.position == position }, result.teamB.count { it.position == position })
        }
    }

    @Test
    fun `ratings end up balanced`() {
        val ratings = listOf(9.5, 9.0, 8.0, 7.5, 6.0, 5.5, 5.0, 4.0, 3.0, 2.0)
        val players = ratings.map { player(MIDFIELDER, it) }

        val result = TeamBalancer(Random(3)).balance(players)

        assertTrue(result.ratingDifference <= 0.5, "Rating difference too high: ${result.ratingDifference}")
    }

    @Test
    fun `the strongest players do not all land on the same team`() {
        val stars = List(4) { player(FORWARD, 10.0) }
        val others = List(4) { player(FORWARD, 2.0) }

        val result = TeamBalancer(Random(11)).balance(stars + others)

        assertEquals(2, result.teamA.count { it.rating == 10.0 })
        assertEquals(2, result.teamB.count { it.rating == 10.0 })
    }

    @Test
    fun `same seed gives the same draw and different seeds can change it`() {
        val players = List(12) { player(listOf(DEFENDER, MIDFIELDER, FORWARD)[it % 3]) }

        val first = TeamBalancer(Random(42)).balance(players)
        val second = TeamBalancer(Random(42)).balance(players)
        val draws = (1..20).map { seed -> TeamBalancer(Random(seed)).balance(players).teamA.map { it.memberId }.toSet() }.toSet()

        assertEquals(first, second)
        assertTrue(draws.size > 1)
    }
}
