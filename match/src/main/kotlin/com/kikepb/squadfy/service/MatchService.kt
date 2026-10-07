package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.InvalidMatchScoreException
import com.kikepb.squadfy.domain.exception.InvalidMatchStateException
import com.kikepb.squadfy.domain.exception.InvalidPlayerMinutesException
import com.kikepb.squadfy.domain.exception.MatchNotFoundException
import com.kikepb.squadfy.domain.model.MatchFormat
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.CANCELLED
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.COMPLETED
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.SCHEDULED
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.MatchEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toMatchModel
import com.kikepb.squadfy.infrastructure.database.repositories.MatchEventRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchTeamPlayerRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class MatchService(
    private val matchRepository: MatchRepository,
    private val matchTeamPlayerRepository: MatchTeamPlayerRepository,
    private val matchEventRepository: MatchEventRepository,
    private val matchAnnouncementService: MatchAnnouncementService,
    private val matchPlanningService: MatchPlanningService,
    private val playerRatingService: PlayerRatingService,
    private val matchNotificationPublisher: MatchNotificationPublisher,
    private val clubAccessGuard: ClubAccessGuard,
    private val clock: Clock
) {

    @Transactional
    fun createMatch(clubId: ClubId, userId: UserId, scheduledAt: Instant, format: MatchFormat?, durationMinutes: Int? = null): MatchModel {
        clubAccessGuard.requireManager(clubId = clubId, userId = userId)
        val match = matchPlanningService.createMatchWithAnnouncement(
            clubId = clubId,
            scheduledAt = scheduledAt,
            maxPlayers = (format ?: matchPlanningService.formatFor(clubId = clubId)).maxPlayers,
            durationMinutes = durationMinutes ?: matchPlanningService.durationFor(clubId = clubId)
        )
        return loadMatch(matchId = match.id)
    }

    fun getMatchById(matchId: MatchId, userId: UserId): MatchModel {
        val match = findMatchEntity(matchId = matchId)
        clubAccessGuard.requireMember(clubId = match.clubId, userId = userId)
        return toMatchModels(listOf(match)).single()
    }

    /** Matches of a club, newest first, optionally filtered by status. */
    fun getMatchesByClub(clubId: ClubId, userId: UserId, status: MatchStatus? = null): List<MatchModel> {
        clubAccessGuard.requireMember(clubId = clubId, userId = userId)
        val matches = if (status == null) {
            matchRepository.findAllByClubIdOrderByScheduledAtDesc(clubId = clubId)
        } else {
            matchRepository.findAllByClubIdAndStatusOrderByScheduledAtDesc(clubId = clubId, status = status)
        }
        return toMatchModels(matches)
    }

    @Transactional
    fun cancelMatch(matchId: MatchId, userId: UserId): MatchModel {
        val match = findMatchEntity(matchId = matchId)
        clubAccessGuard.requireManager(clubId = match.clubId, userId = userId)
        if (match.status == COMPLETED) throw InvalidMatchStateException("A completed match cannot be cancelled")
        cancel(match = match)
        return loadMatch(matchId = matchId)
    }

    /** Cancels the match and its announcement and notifies the enrolled members (no permission check). */
    @Transactional
    fun cancel(match: MatchEntity) {
        val matchId = requireNotNull(match.id)
        val enrolled = matchAnnouncementService.getAllEntriesByMatch(matchId = matchId)
        match.status = CANCELLED
        matchRepository.saveAndFlush(match)
        matchAnnouncementService.cancelForMatch(matchId = matchId)
        matchNotificationPublisher.matchCancelled(
            clubId = match.clubId,
            matchId = matchId,
            matchScheduledAt = match.scheduledAt,
            enrolledMemberIds = enrolled
        )
    }

    /** Official final score set by a manager (spec 008 RN-E1/E2); only while the match is scheduled. */
    @Transactional
    fun setScore(matchId: MatchId, userId: UserId, teamAScore: Int, teamBScore: Int): MatchModel {
        val match = findMatchEntity(matchId = matchId)
        clubAccessGuard.requireManager(clubId = match.clubId, userId = userId)
        ensureScoreEditable(match)
        if (teamAScore !in 0..MAX_SCORE || teamBScore !in 0..MAX_SCORE) {
            throw InvalidMatchScoreException("Scores must be between 0 and $MAX_SCORE")
        }
        match.manualTeamAScore = teamAScore
        match.manualTeamBScore = teamBScore
        matchRepository.saveAndFlush(match)
        return loadMatch(matchId = matchId)
    }

    /** Goes back to the score given by the goal events (spec 008 RN-E4). */
    @Transactional
    fun clearScore(matchId: MatchId, userId: UserId): MatchModel {
        val match = findMatchEntity(matchId = matchId)
        clubAccessGuard.requireManager(clubId = match.clubId, userId = userId)
        ensureScoreEditable(match)
        match.manualTeamAScore = null
        match.manualTeamBScore = null
        matchRepository.saveAndFlush(match)
        return loadMatch(matchId = matchId)
    }

    private fun ensureScoreEditable(match: MatchEntity) {
        if (match.status != SCHEDULED) {
            throw InvalidMatchStateException("The score can only be changed on scheduled matches (reopen a completed match first)")
        }
    }

    /** Closes the match with the score given by its goal events and updates player ratings. */
    @Transactional
    fun completeMatch(matchId: MatchId, userId: UserId): MatchModel {
        val match = findMatchEntity(matchId = matchId)
        clubAccessGuard.requireManager(clubId = match.clubId, userId = userId)
        if (match.status != SCHEDULED) throw InvalidMatchStateException("Only scheduled matches can be completed")
        if (match.scheduledAt.isAfter(clock.instant())) throw InvalidMatchStateException("The match has not started yet")

        val model = loadMatch(matchId = matchId)
        if (model.teamA.size + model.teamAGuests.size == 0 || model.teamB.size + model.teamBGuests.size == 0) {
            throw InvalidMatchStateException("Teams must be generated before completing the match")
        }

        match.status = COMPLETED
        matchRepository.saveAndFlush(match)
        playerRatingService.applyCompletedMatch(match = model)
        return loadMatch(matchId = matchId)
    }

    /** Reopens the club's latest completed match to fix events; its rating changes are reverted. */
    @Transactional
    fun reopenMatch(matchId: MatchId, userId: UserId): MatchModel {
        val match = findMatchEntity(matchId = matchId)
        clubAccessGuard.requireManager(clubId = match.clubId, userId = userId)

        val latestCompleted = matchRepository.findFirstByClubIdAndStatusOrderByScheduledAtDesc(clubId = match.clubId, status = COMPLETED)
        if (latestCompleted?.id != matchId) {
            throw InvalidMatchStateException("Only the latest completed match can be reopened")
        }

        playerRatingService.revertMatch(clubId = match.clubId, matchId = matchId)
        match.status = SCHEDULED
        matchRepository.saveAndFlush(match)
        return loadMatch(matchId = matchId)
    }

    /** Minutes a player actually played (spec 004 RN-6); only before the match is completed. */
    @Transactional
    fun setPlayerMinutes(matchId: MatchId, userId: UserId, clubMemberId: ClubMemberId, minutes: Int): MatchModel {
        val match = findMatchEntity(matchId = matchId)
        clubAccessGuard.requireManager(clubId = match.clubId, userId = userId)
        if (match.status != SCHEDULED) {
            throw InvalidMatchStateException("Minutes can only be changed on scheduled matches (reopen a completed match first)")
        }
        if (minutes !in 0..match.durationMinutes) {
            throw InvalidPlayerMinutesException("Minutes must be between 0 and ${match.durationMinutes}")
        }
        val player = matchTeamPlayerRepository.findAllByMatchId(matchId = matchId).firstOrNull { it.clubMemberId == clubMemberId }
            ?: throw InvalidPlayerMinutesException("The player is not assigned to any team in this match")

        player.minutesPlayed = minutes
        matchTeamPlayerRepository.saveAndFlush(player)
        return loadMatch(matchId = matchId)
    }

    fun findMatchEntity(matchId: MatchId): MatchEntity =
        matchRepository.findByIdOrNull(matchId) ?: throw MatchNotFoundException()

    fun loadMatch(matchId: MatchId): MatchModel =
        toMatchModels(listOf(findMatchEntity(matchId = matchId))).single()

    /** Loads teams, events and enrollments for all matches with a constant number of queries. */
    private fun toMatchModels(matches: List<MatchEntity>): List<MatchModel> {
        if (matches.isEmpty()) return emptyList()
        val matchIds = matches.map { requireNotNull(it.id) }
        val playersByMatch = matchTeamPlayerRepository.findAllByMatchIdIn(matchIds = matchIds).groupBy { it.matchId }
        val eventsByMatch = matchEventRepository.findAllByMatchIdIn(matchIds = matchIds).groupBy { it.matchId }
        val enrolledByMatch = matchAnnouncementService.getEnrolledPlayersByMatches(matchIds = matchIds)
        val guestsByMatch = matchAnnouncementService.getConfirmedGuestsByMatches(matchIds = matchIds)
        val teamGuestIds = playersByMatch.values.flatten().mapNotNull { it.guestEntryId }
        val guestEntries = matchAnnouncementService.findGuestEntries(guestIds = teamGuestIds)
        val ratingChangesByMatch = playerRatingService.changesByMatches(
            matchIds = matches.filter { it.status == COMPLETED }.map { requireNotNull(it.id) }
        )

        return matches.map { match ->
            match.toMatchModel(
                players = playersByMatch[match.id].orEmpty(),
                events = eventsByMatch[match.id].orEmpty().sortedBy { it.createdAt },
                enrolledPlayers = enrolledByMatch[match.id].orEmpty(),
                enrolledGuests = guestsByMatch[match.id].orEmpty(),
                guestEntries = guestEntries,
                ratingChanges = ratingChangesByMatch[match.id].orEmpty()
            )
        }
    }

    private companion object {
        const val MAX_SCORE = 99
    }
}
