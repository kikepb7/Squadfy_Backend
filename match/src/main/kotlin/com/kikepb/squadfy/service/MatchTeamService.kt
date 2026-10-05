package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.club.ClubMembershipProvider
import com.kikepb.squadfy.domain.exception.InvalidTeamGenerationRequestException
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.SCHEDULED
import com.kikepb.squadfy.domain.model.PlayerProfile
import com.kikepb.squadfy.domain.model.TeamBalancer
import com.kikepb.squadfy.domain.model.TeamSideModel.TEAM_A
import com.kikepb.squadfy.domain.model.TeamSideModel.TEAM_B
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.MatchTeamPlayerEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toEntitySide
import com.kikepb.squadfy.infrastructure.database.repositories.MatchTeamPlayerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.abs

@Service
class MatchTeamService(
    private val matchService: MatchService,
    private val matchTeamPlayerRepository: MatchTeamPlayerRepository,
    private val matchAnnouncementService: MatchAnnouncementService,
    private val clubMembershipProvider: ClubMembershipProvider,
    private val playerRatingService: PlayerRatingService,
    private val clubAccessGuard: ClubAccessGuard
) {

    private val teamBalancer = TeamBalancer()

    @Transactional
    fun generateTeams(
        matchId: MatchId,
        userId: UserId,
        mode: TeamGenerationMode,
        manualTeamA: List<ClubMemberId>?,
        manualTeamB: List<ClubMemberId>?
    ): MatchModel {
        val match = matchService.findMatchEntity(matchId = matchId)
        clubAccessGuard.requireManager(clubId = match.clubId, userId = userId)
        if (match.status != SCHEDULED) {
            throw InvalidTeamGenerationRequestException("Teams can only be generated for scheduled matches")
        }

        val enrolled = matchAnnouncementService.getEnrolledPlayersByMatch(matchId = matchId)
        val (teamA, teamB) = when (mode) {
            TeamGenerationMode.AUTO -> autoAssignTeams(clubId = match.clubId, enrolled = enrolled)
            TeamGenerationMode.MANUAL -> manualAssignTeams(teamA = manualTeamA, teamB = manualTeamB, enrolled = enrolled)
        }

        replaceTeams(matchId = matchId, teamA = teamA, teamB = teamB)
        return matchService.loadMatch(matchId = matchId)
    }

    /**
     * Publishes the teams automatically when the announcement closes (spec 003 RN-9). Teams that a
     * manager already drew with exactly the confirmed players are kept; otherwise a balanced draw is
     * made. Managers can still rectify afterwards with [generateTeams].
     *
     * @return the match with its teams, or null when nothing was published.
     */
    @Transactional
    fun publishTeamsOnAnnouncementClosed(matchId: MatchId): MatchModel? {
        val match = matchService.findMatchEntity(matchId = matchId)
        if (match.status != SCHEDULED) return null

        val confirmed = matchAnnouncementService.getEnrolledPlayersByMatch(matchId = matchId)
        if (confirmed.size < 2) return null

        val currentTeams = matchTeamPlayerRepository.findAllByMatchId(matchId = matchId).map { it.clubMemberId }
        if (currentTeams.toSet() == confirmed.toSet()) return null

        val (teamA, teamB) = autoAssignTeams(clubId = match.clubId, enrolled = confirmed)
        replaceTeams(matchId = matchId, teamA = teamA, teamB = teamB)
        return matchService.loadMatch(matchId = matchId)
    }

    private fun replaceTeams(matchId: MatchId, teamA: List<ClubMemberId>, teamB: List<ClubMemberId>) {
        matchTeamPlayerRepository.deleteAllByMatchIdInBulk(matchId = matchId)
        matchTeamPlayerRepository.saveAllAndFlush(
            teamA.map { MatchTeamPlayerEntity(matchId = matchId, clubMemberId = it, teamSide = TEAM_A.toEntitySide()) } +
                teamB.map { MatchTeamPlayerEntity(matchId = matchId, clubMemberId = it, teamSide = TEAM_B.toEntitySide()) }
        )
    }

    private fun autoAssignTeams(clubId: ClubId, enrolled: List<ClubMemberId>): Pair<List<ClubMemberId>, List<ClubMemberId>> {
        val members = clubMembershipProvider.findMembers(clubId = clubId, memberIds = enrolled)
        val ratings = playerRatingService.ratingsFor(clubId = clubId, memberIds = members.map { it.memberId })
        val players = members.map {
            PlayerProfile(memberId = it.memberId, position = it.position, rating = ratings.getValue(it.memberId))
        }

        if (players.size < 2) {
            throw InvalidTeamGenerationRequestException("At least 2 enrolled players are required for AUTO mode")
        }

        val assignment = teamBalancer.balance(players = players)
        return assignment.teamA.map { it.memberId } to assignment.teamB.map { it.memberId }
    }

    private fun manualAssignTeams(
        teamA: List<ClubMemberId>?,
        teamB: List<ClubMemberId>?,
        enrolled: List<ClubMemberId>
    ): Pair<List<ClubMemberId>, List<ClubMemberId>> {
        val safeTeamA = teamA?.distinct().orEmpty()
        val safeTeamB = teamB?.distinct().orEmpty()

        if (safeTeamA.isEmpty() || safeTeamB.isEmpty()) {
            throw InvalidTeamGenerationRequestException("Manual mode requires non-empty teamA and teamB")
        }
        if (safeTeamA.any { it in safeTeamB }) {
            throw InvalidTeamGenerationRequestException("A player cannot be in both teams")
        }
        if (abs(safeTeamA.size - safeTeamB.size) > 1) {
            throw InvalidTeamGenerationRequestException("Team sizes must be balanced (difference ≤ 1)")
        }
        if (!enrolled.containsAll(safeTeamA + safeTeamB)) {
            throw InvalidTeamGenerationRequestException("Only enrolled players can be assigned to a team")
        }

        return safeTeamA to safeTeamB
    }

    enum class TeamGenerationMode {
        AUTO,
        MANUAL
    }
}
