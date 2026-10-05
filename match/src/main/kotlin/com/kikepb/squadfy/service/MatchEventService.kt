package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.MatchEventNotFoundException
import com.kikepb.squadfy.domain.exception.MatchNotFoundException
import com.kikepb.squadfy.domain.model.MatchEventType
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchEventId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.infrastructure.database.entities.MatchEventEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toMatchModel
import com.kikepb.squadfy.infrastructure.database.repositories.MatchEventRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchTeamPlayerRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MatchEventService(
    private val matchRepository: MatchRepository,
    private val matchEventRepository: MatchEventRepository,
    private val matchTeamPlayerRepository: MatchTeamPlayerRepository,
    private val matchAnnouncementService: MatchAnnouncementService
) {

    @Transactional
    fun addEvent(
        matchId: MatchId,
        clubMemberId: ClubMemberId,
        type: MatchEventType,
        minute: Int?
    ): MatchModel {
        matchRepository.findByIdOrNull(matchId)
            ?: throw MatchNotFoundException()

        matchEventRepository.save(
            MatchEventEntity(
                matchId = matchId,
                clubMemberId = clubMemberId,
                type = type,
                minute = minute
            )
        )

        return buildMatchModel(matchId)
    }

    @Transactional
    fun removeEvent(matchId: MatchId, eventId: MatchEventId): MatchModel {
        matchRepository.findByIdOrNull(matchId)
            ?: throw MatchNotFoundException()

        val event = matchEventRepository.findByIdOrNull(eventId)
            ?: throw MatchEventNotFoundException()

        matchEventRepository.delete(event)

        return buildMatchModel(matchId)
    }

    private fun buildMatchModel(matchId: MatchId): MatchModel {
        val match = matchRepository.findByIdOrNull(matchId) ?: throw MatchNotFoundException()
        val players = matchTeamPlayerRepository.findAllByMatchId(matchId)
        val events = matchEventRepository.findAllByMatchId(matchId)
        val enrolledPlayers = matchAnnouncementService.getEnrolledPlayersByMatch(matchId)
        return match.toMatchModel(players = players, events = events, enrolledPlayers = enrolledPlayers)
    }
}
