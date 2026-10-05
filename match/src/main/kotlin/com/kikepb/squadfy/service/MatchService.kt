package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.MatchNotFoundException
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.CANCELLED
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.SCHEDULED
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.infrastructure.database.entities.MatchEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toMatchModel
import com.kikepb.squadfy.infrastructure.database.repositories.MatchEventRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchTeamPlayerRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class MatchService(
    private val matchRepository: MatchRepository,
    private val matchTeamPlayerRepository: MatchTeamPlayerRepository,
    private val matchEventRepository: MatchEventRepository,
    private val matchAnnouncementService: MatchAnnouncementService
) {

    @Transactional
    fun createMatch(clubId: ClubId, scheduledAt: Instant): MatchModel {
        val match = matchRepository.saveAndFlush(
            MatchEntity(
                clubId = clubId,
                scheduledAt = scheduledAt,
                status = SCHEDULED
            )
        )
        return match.toMatchModel()
    }

    fun getMatchById(matchId: MatchId): MatchModel =
        buildMatchModel(matchId)

    fun getMatchesByClub(clubId: ClubId): List<MatchModel> =
        matchRepository
            .findAllByClubIdOrderByScheduledAtDesc(clubId = clubId)
            .map { buildMatchModel(requireNotNull(it.id)) }

    fun getScheduledMatchesByClub(clubId: ClubId): List<MatchModel> =
        matchRepository
            .findAllByClubIdAndStatusOrderByScheduledAtDesc(clubId = clubId, status = SCHEDULED)
            .map { buildMatchModel(requireNotNull(it.id)) }

    @Transactional
    fun cancelMatch(matchId: MatchId): MatchModel {
        val match = matchRepository.findByIdOrNull(matchId)
            ?: throw MatchNotFoundException()
        match.status = CANCELLED
        matchRepository.saveAndFlush(match)
        return buildMatchModel(matchId)
    }

    @Transactional
    fun updateStatus(matchId: MatchId, status: MatchStatus): MatchModel {
        val match = matchRepository.findByIdOrNull(matchId)
            ?: throw MatchNotFoundException()
        match.status = status
        matchRepository.saveAndFlush(match)
        return buildMatchModel(matchId)
    }

    fun hasMatchInWeek(clubId: ClubId, weekStart: Instant, weekEnd: Instant): Boolean =
        matchRepository.existsByClubIdAndScheduledAtBetween(
            clubId = clubId,
            from = weekStart,
            to = weekEnd
        )

    private fun buildMatchModel(matchId: MatchId): MatchModel {
        val match = matchRepository.findByIdOrNull(matchId) ?: throw MatchNotFoundException()
        val players = matchTeamPlayerRepository.findAllByMatchId(matchId)
        val events = matchEventRepository.findAllByMatchId(matchId)
        val enrolledPlayers = matchAnnouncementService.getEnrolledPlayersByMatch(matchId)
        return match.toMatchModel(players = players, events = events, enrolledPlayers = enrolledPlayers)
    }
}
