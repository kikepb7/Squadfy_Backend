package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.model.PlayerPositionModel.DEFENDER
import com.kikepb.squadfy.domain.model.PlayerPositionModel.FORWARD
import com.kikepb.squadfy.domain.model.PlayerPositionModel.GOALKEEPER
import com.kikepb.squadfy.domain.model.PlayerPositionModel.MIDFIELDER
import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BalancedTeamDrawerTest {

    private fun player(position: com.kikepb.squadfy.domain.model.PlayerPositionModel?, rating: Int) =
        DraftPlayer(signupId = UUID.randomUUID(), position = position, rating = rating)

    @Test
    fun `empty list produces two empty teams`() {
        val (teamA, teamB) = BalancedTeamDrawer.draw(emptyList())
        assertTrue(teamA.isEmpty())
        assertTrue(teamB.isEmpty())
    }

    @Test
    fun `every signup is assigned to exactly one team`() {
        val players = (1..21).map { player(MIDFIELDER, rating = it) }
        val (teamA, teamB) = BalancedTeamDrawer.draw(players, random = Random(42))

        assertEquals(players.size, teamA.size + teamB.size)
        assertEquals(players.map { it.signupId }.toSet(), (teamA + teamB).toSet())
    }

    @Test
    fun `team sizes never differ by more than one player`() {
        (1..23).forEach { count ->
            val players = (1..count).map { player(MIDFIELDER, rating = 50) }
            val (teamA, teamB) = BalancedTeamDrawer.draw(players, random = Random(count))
            assertTrue(kotlin.math.abs(teamA.size - teamB.size) <= 1, "count=$count produced ${teamA.size} vs ${teamB.size}")
        }
    }

    @Test
    fun `total rating stays close between both teams`() {
        val players = listOf(90, 85, 80, 75, 70, 65, 60, 55, 50, 45, 40, 35, 30, 25).map { player(MIDFIELDER, it) }
        val (teamA, teamB) = BalancedTeamDrawer.draw(players, random = Random(1))

        val ratingA = players.filter { it.signupId in teamA }.sumOf { it.rating }
        val ratingB = players.filter { it.signupId in teamB }.sumOf { it.rating }

        assertTrue(kotlin.math.abs(ratingA - ratingB) <= 5, "ratingA=$ratingA ratingB=$ratingB")
    }

    @Test
    fun `two goalkeepers are split one per team`() {
        val players = listOf(
            player(GOALKEEPER, 70),
            player(GOALKEEPER, 60),
            player(DEFENDER, 55),
            player(DEFENDER, 50),
            player(FORWARD, 65),
            player(FORWARD, 45)
        )
        val (teamA, teamB) = BalancedTeamDrawer.draw(players, random = Random(7))

        val goalkeeperIds = players.filter { it.position == GOALKEEPER }.map { it.signupId }
        val goalkeepersInA = goalkeeperIds.count { it in teamA }
        val goalkeepersInB = goalkeeperIds.count { it in teamB }

        assertEquals(1, goalkeepersInA)
        assertEquals(1, goalkeepersInB)
    }

    @Test
    fun `players with unknown position are still assigned`() {
        val players = listOf(player(null, 60), player(null, 40), player(DEFENDER, 50))
        val (teamA, teamB) = BalancedTeamDrawer.draw(players, random = Random(3))
        assertEquals(3, teamA.size + teamB.size)
    }
}
