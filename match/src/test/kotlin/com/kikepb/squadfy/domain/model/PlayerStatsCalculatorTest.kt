package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.model.MatchEventType.ASSIST
import com.kikepb.squadfy.domain.model.MatchEventType.GOAL
import com.kikepb.squadfy.domain.model.MatchEventType.RED_CARD
import com.kikepb.squadfy.domain.model.MatchEventType.YELLOW_CARD
import com.kikepb.squadfy.domain.model.PlayerStatsCalculator.CompletedMatch
import com.kikepb.squadfy.domain.model.PlayerStatsCalculator.CompletedMatch.Event
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class PlayerStatsCalculatorTest {

    private val striker = UUID.randomUUID()
    private val keeper = UUID.randomUUID()
    private val rival = UUID.randomUUID()

    @Test
    fun `results, events and minutes are accumulated per player`() {
        val won = CompletedMatch(
            teamA = mapOf(striker to 60, keeper to 60),
            teamB = mapOf(rival to 45),
            events = listOf(Event(striker, GOAL), Event(striker, GOAL), Event(keeper, ASSIST), Event(rival, YELLOW_CARD))
        )
        val lost = CompletedMatch(
            teamA = mapOf(striker to 30),
            teamB = mapOf(rival to 60, keeper to 60),
            events = listOf(Event(rival, GOAL), Event(striker, RED_CARD))
        )
        val drawn = CompletedMatch(teamA = mapOf(rival to 60), teamB = mapOf(keeper to 60), events = emptyList())

        val stats = PlayerStatsCalculator.aggregate(listOf(won, lost, drawn))

        assertEquals(PlayerStatsModel(striker, matchesPlayed = 2, wins = 1, losses = 1, goals = 2, redCards = 1, minutesPlayed = 90), stats[striker])
        assertEquals(PlayerStatsModel(keeper, matchesPlayed = 3, wins = 2, draws = 1, assists = 1, minutesPlayed = 180), stats[keeper])
        assertEquals(PlayerStatsModel(rival, matchesPlayed = 3, wins = 1, draws = 1, losses = 1, goals = 1, yellowCards = 1, minutesPlayed = 165), stats[rival])
    }

    @Test
    fun `players without completed matches do not appear`() {
        assertEquals(emptyMap(), PlayerStatsCalculator.aggregate(emptyList()))
    }

    @Test
    fun `classification orders by the chosen metric and ties share the position`() {
        val top = PlayerStatsModel(UUID.randomUUID(), goals = 5, wins = 1)
        val tiedMoreWins = PlayerStatsModel(UUID.randomUUID(), goals = 3, wins = 4)
        val tiedFewerWins = PlayerStatsModel(UUID.randomUUID(), goals = 3, wins = 2)
        val newcomer = PlayerStatsModel(UUID.randomUUID())

        val byGoals = StatsLeaderboard.rank(listOf(newcomer, tiedFewerWins, top, tiedMoreWins), StatsSortBy.GOALS)
        val byWins = StatsLeaderboard.rank(listOf(newcomer, tiedFewerWins, top, tiedMoreWins), StatsSortBy.WINS)

        assertEquals(listOf(top, tiedMoreWins, tiedFewerWins, newcomer), byGoals.map { it.stats })
        assertEquals(listOf(1, 2, 2, 4), byGoals.map { it.rank })
        assertEquals(tiedMoreWins, byWins.first().stats)
    }
}
