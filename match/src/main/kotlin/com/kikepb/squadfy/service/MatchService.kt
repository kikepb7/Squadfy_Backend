package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.InvalidMatchStateException
import com.kikepb.squadfy.domain.exception.MatchNotFoundException
import com.kikepb.squadfy.domain.model.MatchFormat
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.CANCELLED
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.COMPLETED
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.SCHEDULED
import com.kikepb.squadfy.domain.type.ClubId
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
    private val clubAccessGuard: ClubAccessGuard,
    private val clock: Clock
) {

    @Transactional
    fun createMatch(clubId: ClubId, userId: UserId, scheduledAt: Instant, format: MatchFormat?): MatchModel {
        clubAccessGuard.requireManager(clubId = clubId, userId = userId)
        val match = matchPlanningService.createMatchWithAnnouncement(
            clubId = clubId,
            scheduledAt = scheduledAt,
            maxPlayers = (format ?: matchPlanningService.formatFor(clubId = clubId)).maxPlayers
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

        match.status = CANCELLED
        matchRepository.saveAndFlush(match)
        matchAnnouncementService.cancelForMatch(matchId = matchId)
        return loadMatch(matchId = matchId)
    }

    /** Closes the match with the score given by its goal events and updates player ratings. */
    @Transactional
    fun completeMatch(matchId: MatchId, userId: UserId): MatchModel {
        val match = findMatchEntity(matchId = matchId)
        clubAccessGuard.requireManager(clubId = match.clubId, userId = userId)
        if (match.status != SCHEDULED) throw InvalidMatchStateException("Only scheduled matches can be completed")
        if (match.scheduledAt.isAfter(clock.instant())) throw InvalidMatchStateException("The match has not started yet")

        val model = loadMatch(matchId = matchId)
        if (model.teamA.isEmpty() || model.teamB.isEmpty()) {
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

        return matches.map { match ->
            match.toMatchModel(
                players = playersByMatch[match.id].orEmpty(),
                events = eventsByMatch[match.id].orEmpty().sortedBy { it.createdAt },
                enrolledPlayers = enrolledByMatch[match.id].orEmpty()
            )
        }
    }
}
