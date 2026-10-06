package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.model.RatingLeaderboard.Entry
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RatingLeaderboardTest {

    private fun entry(rating: Double, matches: Int = 10) = Entry(UUID.randomUUID(), rating, matches)

    @Test
    fun `highest rating first and ties share the position`() {
        val best = entry(1100.0)
        val tiedA = entry(1000.0)
        val tiedB = entry(1000.0)
        val worst = entry(950.0)

        val ranked = RatingLeaderboard.rank(listOf(worst, tiedA, best, tiedB))

        assertEquals(listOf(1, 2, 2, 4), ranked.map { it.rank })
        assertEquals(best.clubMemberId, ranked.first().clubMemberId)
        assertEquals(worst.clubMemberId, ranked.last().clubMemberId)
    }

    @Test
    fun `ratings that round to the same value tie`() {
        val ranked = RatingLeaderboard.rank(listOf(entry(1000.2), entry(999.8), entry(998.0)))

        assertEquals(listOf(1, 1, 3), ranked.map { it.rank })
    }

    @Test
    fun `players without enough matches are provisional and listed after tied veterans`() {
        val rookie = entry(1000.0, matches = 0)
        val veteran = entry(1000.0, matches = 25)

        val ranked = RatingLeaderboard.rank(listOf(rookie, veteran))

        assertEquals(veteran.clubMemberId, ranked.first().clubMemberId)
        assertTrue(ranked.last().isProvisional)
        assertEquals(listOf(1, 1), ranked.map { it.rank })
    }

    @Test
    fun `empty club gives an empty classification`() {
        assertTrue(RatingLeaderboard.rank(emptyList()).isEmpty())
    }
}
