package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.club.ClubMembershipProvider
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.COMPLETED
import com.kikepb.squadfy.domain.model.PlayerStatsCalculator
import com.kikepb.squadfy.domain.model.PlayerStatsCalculator.CompletedMatch
import com.kikepb.squadfy.domain.model.PlayerStatsModel
import com.kikepb.squadfy.domain.model.StatsLeaderboard
import com.kikepb.squadfy.domain.model.StatsPeriod
import com.kikepb.squadfy.domain.model.StatsSortBy
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.DEFAULT_CLUB_TIME_ZONE
import com.kikepb.squadfy.infrastructure.database.entities.MatchTeamPlayerEntity.TeamSideEntity
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMatchScheduleRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchEventRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchTeamPlayerRepository
import org.springframework.stereotype.Service
import java.time.ZoneId

/** Statistics derived from the club's completed matches (spec 004 RN-7, RN-8). */
@Service
class PlayerStatsService(
    private val matchRepository: MatchRepository,
    private val matchTeamPlayerRepository: MatchTeamPlayerRepository,
    private val matchEventRepository: MatchEventRepository,
    private val clubMembershipProvider: ClubMembershipProvider,
    private val clubMatchScheduleRepository: ClubMatchScheduleRepository,
    private val clubAccessGuard: ClubAccessGuard
) {

    /**
     * Every active member, ranked by [sortBy]; members who have not played appear with zeros.
     * Only matches played within [period] count (spec 012 RN-B1).
     */
    fun getClubStats(clubId: ClubId, userId: UserId, sortBy: StatsSortBy, period: StatsPeriod = StatsPeriod.ALL): List<StatsLeaderboard.RankedStats> {
        clubAccessGuard.requireMember(clubId = clubId, userId = userId)
        val stats = statsByMember(clubId = clubId, period = period)
        val members = clubMembershipProvider.findAllMembers(clubId = clubId).map { it.memberId }
        return StatsLeaderboard.rank(
            stats = members.map { stats[it] ?: PlayerStatsModel(clubMemberId = it) },
            sortBy = sortBy
        )
    }

    fun getMyStats(clubId: ClubId, userId: UserId, period: StatsPeriod = StatsPeriod.ALL): PlayerStatsModel {
        val memberId = clubAccessGuard.requireMember(clubId = clubId, userId = userId).memberId
        return statsByMember(clubId = clubId, period = period)[memberId] ?: PlayerStatsModel(clubMemberId = memberId)
    }

    private fun statsByMember(clubId: ClubId, period: StatsPeriod): Map<ClubMemberId, PlayerStatsModel> {
        val zone = ZoneId.of(clubMatchScheduleRepository.findByClubId(clubId = clubId)?.timeZone ?: DEFAULT_CLUB_TIME_ZONE)
        val matches = matchRepository.findAllByClubIdAndStatusOrderByScheduledAtDesc(clubId = clubId, status = COMPLETED)
            .filter { period.contains(instant = it.scheduledAt, zone = zone) }
        if (matches.isEmpty()) return emptyMap()

        val matchIds = matches.map { requireNotNull(it.id) }
        val playersByMatch = matchTeamPlayerRepository.findAllByMatchIdIn(matchIds = matchIds).groupBy { it.matchId }
        val eventsByMatch = matchEventRepository.findAllByMatchIdIn(matchIds = matchIds).groupBy { it.matchId }

        return PlayerStatsCalculator.aggregate(
            matches.map { match ->
                val players = playersByMatch[match.id].orEmpty()
                fun team(side: TeamSideEntity) = players
                    .filter { it.teamSide == side && it.clubMemberId != null }
                    .associate { requireNotNull(it.clubMemberId) to (it.minutesPlayed ?: match.durationMinutes) }
                val isManualScore = match.manualTeamAScore != null && match.manualTeamBScore != null

                CompletedMatch(
                    teamA = team(TeamSideEntity.TEAM_A),
                    teamB = team(TeamSideEntity.TEAM_B),
                    events = eventsByMatch[match.id].orEmpty().map { CompletedMatch.Event(clubMemberId = it.clubMemberId, type = it.type) },
                    officialScoreA = if (isManualScore) match.manualTeamAScore else null,
                    officialScoreB = if (isManualScore) match.manualTeamBScore else null
                )
            }
        )
    }
}
