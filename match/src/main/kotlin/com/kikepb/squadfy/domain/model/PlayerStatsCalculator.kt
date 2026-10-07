package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubMemberId

/**
 * Derives statistics from completed matches instead of storing counters, so reopening and
 * completing a match again always leaves consistent figures (spec 004 RN-7).
 */
object PlayerStatsCalculator {

    data class CompletedMatch(
        /** Team A players and the minutes each one played. */
        val teamA: Map<ClubMemberId, Int>,
        val teamB: Map<ClubMemberId, Int>,
        val events: List<Event>,
        /** Manual official score (spec 008 RN-E2); when null the score is the goals of [events]. */
        val officialScoreA: Int? = null,
        val officialScoreB: Int? = null
    ) {
        data class Event(val clubMemberId: ClubMemberId, val type: MatchEventType)
    }

    fun aggregate(matches: List<CompletedMatch>): Map<ClubMemberId, PlayerStatsModel> {
        val stats = mutableMapOf<ClubMemberId, PlayerStatsModel>()

        matches.forEach { match ->
            val goalsA = match.officialScoreA ?: match.events.count { it.type == MatchEventType.GOAL && it.clubMemberId in match.teamA }
            val goalsB = match.officialScoreB ?: match.events.count { it.type == MatchEventType.GOAL && it.clubMemberId in match.teamB }

            fun accumulate(team: Map<ClubMemberId, Int>, scored: Int, conceded: Int) {
                team.forEach { (memberId, minutes) ->
                    val own = match.events.filter { it.clubMemberId == memberId }
                    val current = stats[memberId] ?: PlayerStatsModel(clubMemberId = memberId)
                    stats[memberId] = current.copy(
                        matchesPlayed = current.matchesPlayed + 1,
                        wins = current.wins + if (scored > conceded) 1 else 0,
                        draws = current.draws + if (scored == conceded) 1 else 0,
                        losses = current.losses + if (scored < conceded) 1 else 0,
                        goals = current.goals + own.count { it.type == MatchEventType.GOAL },
                        assists = current.assists + own.count { it.type == MatchEventType.ASSIST },
                        yellowCards = current.yellowCards + own.count { it.type == MatchEventType.YELLOW_CARD },
                        redCards = current.redCards + own.count { it.type == MatchEventType.RED_CARD },
                        minutesPlayed = current.minutesPlayed + minutes
                    )
                }
            }

            accumulate(team = match.teamA, scored = goalsA, conceded = goalsB)
            accumulate(team = match.teamB, scored = goalsB, conceded = goalsA)
        }
        return stats
    }
}

enum class StatsSortBy(val metric: (PlayerStatsModel) -> Int) {
    GOALS({ it.goals }),
    ASSISTS({ it.assists }),
    MATCHES({ it.matchesPlayed }),
    MINUTES({ it.minutesPlayed }),
    WINS({ it.wins })
}

/** Club classification of statistics (spec 004 RN-8). */
object StatsLeaderboard {

    data class RankedStats(val rank: Int, val stats: PlayerStatsModel)

    /** Highest value first; ties share the position (1, 2, 2, 4) and are listed by wins, goals and id. */
    fun rank(stats: List<PlayerStatsModel>, sortBy: StatsSortBy): List<RankedStats> {
        val sorted = stats.sortedWith(
            compareByDescending<PlayerStatsModel> { sortBy.metric(it) }
                .thenByDescending { it.wins }
                .thenByDescending { it.goals }
                .thenBy { it.clubMemberId.toString() }
        )

        val ranked = mutableListOf<RankedStats>()
        sorted.forEachIndexed { index, playerStats ->
            val previous = ranked.lastOrNull()
            val rank = if (previous != null && sortBy.metric(previous.stats) == sortBy.metric(playerStats)) previous.rank else index + 1
            ranked += RankedStats(rank = rank, stats = playerStats)
        }
        return ranked
    }
}
