package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.club.ClubMembershipProvider
import com.kikepb.squadfy.domain.exception.InvalidMatchStateException
import com.kikepb.squadfy.domain.exception.InvalidTeamGenerationRequestException
import com.kikepb.squadfy.domain.model.MatchGuestModel
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.SCHEDULED
import com.kikepb.squadfy.domain.model.PlayerProfile
import com.kikepb.squadfy.domain.model.PlayerRatingCalculator
import com.kikepb.squadfy.domain.model.TeamBalanceModel
import com.kikepb.squadfy.domain.model.TeamBalancer
import com.kikepb.squadfy.domain.model.TeamSideModel
import com.kikepb.squadfy.domain.model.TeamSideModel.TEAM_A
import com.kikepb.squadfy.domain.model.TeamSideModel.TEAM_B
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.MatchEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchTeamPlayerEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toEntitySide
import com.kikepb.squadfy.infrastructure.database.repositories.MatchTeamPlayerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.math.abs

@Service
class MatchTeamService(
    private val matchService: MatchService,
    private val matchTeamPlayerRepository: MatchTeamPlayerRepository,
    private val matchAnnouncementService: MatchAnnouncementService,
    private val clubMembershipProvider: ClubMembershipProvider,
    private val playerRatingService: PlayerRatingService,
    private val matchNotificationPublisher: MatchNotificationPublisher,
    private val clubAccessGuard: ClubAccessGuard
) {

    private val teamBalancer = TeamBalancer()

    /**
     * Draws or sets the teams of a match. Participants are the confirmed members and guests
     * (spec 008 RN-A6); manual teams accept member ids and guest ids.
     */
    @Transactional
    fun generateTeams(
        matchId: MatchId,
        userId: UserId,
        mode: TeamGenerationMode,
        manualTeamA: List<UUID>?,
        manualTeamB: List<UUID>?
    ): MatchModel {
        val match = matchService.findMatchEntity(matchId = matchId)
        clubAccessGuard.requireManager(clubId = match.clubId, userId = userId)
        if (match.status != SCHEDULED) {
            throw InvalidTeamGenerationRequestException("Teams can only be generated for scheduled matches")
        }

        val participants = participantsOf(matchId = matchId)
        val (teamA, teamB) = when (mode) {
            TeamGenerationMode.AUTO -> autoAssignTeams(clubId = match.clubId, participants = participants)
            TeamGenerationMode.MANUAL -> manualAssignTeams(teamA = manualTeamA, teamB = manualTeamB, participants = participants)
        }

        publish(match = match, participants = participants, teamA = teamA, teamB = teamB)
        return matchService.loadMatch(matchId = matchId)
    }

    /**
     * Publishes the teams automatically at the draw time (spec 003 RN-9, spec 008 RN-D3). Teams that a
     * manager already drew with exactly the confirmed participants are kept; otherwise a balanced draw
     * is made. The announcement is marked as published either way, so it is processed only once.
     * Managers can still rectify afterwards with [generateTeams].
     *
     * @return the match with its teams, or null when nothing was published.
     */
    @Transactional
    fun publishTeamsAtDrawTime(matchId: MatchId): MatchModel? {
        matchAnnouncementService.markTeamsPublished(matchId = matchId)
        val match = matchService.findMatchEntity(matchId = matchId)
        if (match.status != SCHEDULED) return null

        val participants = participantsOf(matchId = matchId)
        if (participants.ids.size < 2) return null

        val currentTeams = matchTeamPlayerRepository.findAllByMatchId(matchId = matchId).map { it.participantId() }
        if (currentTeams.toSet() == participants.ids.toSet()) return null

        val (teamA, teamB) = autoAssignTeams(clubId = match.clubId, participants = participants)
        publish(match = match, participants = participants, teamA = teamA, teamB = teamB)
        return matchService.loadMatch(matchId = matchId)
    }

    /** Strength of each team from the players' current ratings, for managers only (spec 003 RN-10). */
    fun getTeamBalance(matchId: MatchId, userId: UserId): TeamBalanceModel {
        val match = matchService.loadMatch(matchId = matchId)
        clubAccessGuard.requireManager(clubId = match.clubId, userId = userId)
        if (match.teamA.size + match.teamAGuests.size == 0 || match.teamB.size + match.teamBGuests.size == 0) {
            throw InvalidMatchStateException("Teams have not been generated for this match yet")
        }

        val ratings = playerRatingService.ratingsFor(clubId = match.clubId, memberIds = match.teamA + match.teamB)
        fun strength(members: List<ClubMemberId>, guests: List<MatchGuestModel>) =
            members.map { TeamBalanceModel.PlayerRating(clubMemberId = it, rating = ratings.getValue(it)) } +
                guests.map { TeamBalanceModel.PlayerRating(clubMemberId = it.guestId, rating = PlayerRatingCalculator.INITIAL_RATING, isGuest = true) }

        return TeamBalanceModel.of(
            matchId = matchId,
            teamA = strength(match.teamA, match.teamAGuests),
            teamB = strength(match.teamB, match.teamBGuests)
        )
    }

    private fun publish(match: MatchEntity, participants: Participants, teamA: List<UUID>, teamB: List<UUID>) {
        val matchId = requireNotNull(match.id)
        replaceTeams(matchId = matchId, teamA = teamA, teamB = teamB, guestIds = participants.guestIds)
        matchNotificationPublisher.teamsPublished(
            clubId = match.clubId,
            matchId = matchId,
            matchScheduledAt = match.scheduledAt,
            teamA = teamA.filterNot { it in participants.guestIds },
            teamB = teamB.filterNot { it in participants.guestIds }
        )
    }

    private fun replaceTeams(matchId: MatchId, teamA: List<UUID>, teamB: List<UUID>, guestIds: Set<UUID>) {
        fun entity(participantId: UUID, side: TeamSideModel) = MatchTeamPlayerEntity(
            matchId = matchId,
            clubMemberId = participantId.takeUnless { it in guestIds },
            guestEntryId = participantId.takeIf { it in guestIds },
            teamSide = side.toEntitySide()
        )
        matchTeamPlayerRepository.deleteAllByMatchIdInBulk(matchId = matchId)
        matchTeamPlayerRepository.saveAllAndFlush(teamA.map { entity(it, TEAM_A) } + teamB.map { entity(it, TEAM_B) })
    }

    private fun autoAssignTeams(clubId: ClubId, participants: Participants): Pair<List<UUID>, List<UUID>> {
        val members = clubMembershipProvider.findMembers(clubId = clubId, memberIds = participants.memberIds)
        val ratings = playerRatingService.ratingsFor(clubId = clubId, memberIds = members.map { it.memberId })
        val players = members.map {
            PlayerProfile(memberId = it.memberId, position = it.position, rating = ratings.getValue(it.memberId))
        } + participants.guests.map {
            PlayerProfile(memberId = it.guestId, position = it.position, rating = PlayerRatingCalculator.INITIAL_RATING)
        }

        if (players.size < 2) {
            throw InvalidTeamGenerationRequestException("At least 2 enrolled players are required for AUTO mode")
        }

        val assignment = teamBalancer.balance(players = players)
        return assignment.teamA.map { it.memberId } to assignment.teamB.map { it.memberId }
    }

    private fun manualAssignTeams(teamA: List<UUID>?, teamB: List<UUID>?, participants: Participants): Pair<List<UUID>, List<UUID>> {
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
        if (!participants.ids.containsAll(safeTeamA + safeTeamB)) {
            throw InvalidTeamGenerationRequestException("Only confirmed players and guests can be assigned to a team")
        }

        return safeTeamA to safeTeamB
    }

    private fun participantsOf(matchId: MatchId): Participants = Participants(
        memberIds = matchAnnouncementService.getEnrolledPlayersByMatch(matchId = matchId),
        guests = matchAnnouncementService.getConfirmedGuestsByMatch(matchId = matchId)
    )

    private fun MatchTeamPlayerEntity.participantId(): UUID = clubMemberId ?: requireNotNull(guestEntryId)

    /** Confirmed members and guests of a match; guests are identified by their guestId. */
    private data class Participants(val memberIds: List<ClubMemberId>, val guests: List<MatchGuestModel>) {
        val guestIds: Set<UUID> = guests.map { it.guestId }.toSet()
        val ids: List<UUID> = memberIds + guests.map { it.guestId }
    }

    enum class TeamGenerationMode {
        AUTO,
        MANUAL
    }
}
