package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubMemberId
import kotlin.math.roundToInt

/** Club classification by rating (spec 003 RN-11). */
object RatingLeaderboard {

    data class Entry(
        val clubMemberId: ClubMemberId,
        val rating: Double,
        val matchesRated: Int
    )

    data class RankedEntry(
        val rank: Int,
        val clubMemberId: ClubMemberId,
        val rating: Double,
        val matchesRated: Int
    ) {
        val isProvisional: Boolean get() = matchesRated < PlayerRatingCalculator.PROVISIONAL_MATCHES
    }

    /**
     * Highest rating first. Ties share the position (1, 2, 2, 4), comparing the rounded rating that
     * users see; ties are listed by matches played and then id so the order is stable.
     */
    fun rank(entries: List<Entry>): List<RankedEntry> {
        val sorted = entries.sortedWith(
            compareByDescending<Entry> { it.rating }
                .thenByDescending { it.matchesRated }
                .thenBy { it.clubMemberId.toString() }
        )

        val ranked = mutableListOf<RankedEntry>()
        sorted.forEachIndexed { index, entry ->
            val previous = ranked.lastOrNull()
            val rank = if (previous != null && previous.rating.roundToInt() == entry.rating.roundToInt()) {
                previous.rank
            } else {
                index + 1
            }
            ranked += RankedEntry(
                rank = rank,
                clubMemberId = entry.clubMemberId,
                rating = entry.rating,
                matchesRated = entry.matchesRated
            )
        }
        return ranked
    }
}
