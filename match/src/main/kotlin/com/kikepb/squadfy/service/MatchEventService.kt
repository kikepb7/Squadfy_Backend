package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.InvalidMatchEventException
import com.kikepb.squadfy.domain.exception.InvalidMatchStateException
import com.kikepb.squadfy.domain.exception.MatchEventNotFoundException
import com.kikepb.squadfy.domain.model.MatchEventType
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.SCHEDULED
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchEventId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.MatchEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchEventEntity
import com.kikepb.squadfy.infrastructure.database.repositories.MatchEventRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchTeamPlayerRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MatchEventService(
    private val matchService: MatchService,
    private val liveUpdatePublisher: LiveUpdatePublisher,
    private val matchEventRepository: MatchEventRepository,
    private val matchTeamPlayerRepository: MatchTeamPlayerRepository,
    private val clubAccessGuard: ClubAccessGuard
) {

    @Transactional
    fun addEvent(
        matchId: MatchId,
        userId: UserId,
        clubMemberId: ClubMemberId,
        type: MatchEventType,
        minute: Int?
    ): MatchModel {
        val match = findEditableMatch(matchId = matchId, userId = userId)

        val isPlayingThisMatch = matchTeamPlayerRepository.findAllByMatchId(matchId = requireNotNull(match.id))
            .any { it.clubMemberId == clubMemberId }
        if (!isPlayingThisMatch) throw InvalidMatchEventException("The player is not assigned to any team in this match")

        matchEventRepository.saveAndFlush(
            MatchEventEntity(
                matchId = matchId,
                clubMemberId = clubMemberId,
                type = type,
                minute = minute
            )
        )
        liveUpdatePublisher.matchChanged(clubId = match.clubId, matchId = matchId)

        return matchService.loadMatch(matchId = matchId)
    }

    @Transactional
    fun removeEvent(matchId: MatchId, userId: UserId, eventId: MatchEventId): MatchModel {
        val match = findEditableMatch(matchId = matchId, userId = userId)

        val event = matchEventRepository.findByIdOrNull(eventId)
            ?.takeIf { it.matchId == matchId }
            ?: throw MatchEventNotFoundException()

        matchEventRepository.delete(event)
        matchEventRepository.flush()
        liveUpdatePublisher.matchChanged(clubId = match.clubId, matchId = matchId)

        return matchService.loadMatch(matchId = matchId)
    }

    private fun findEditableMatch(matchId: MatchId, userId: UserId): MatchEntity {
        val match = matchService.findMatchEntity(matchId = matchId)
        clubAccessGuard.requireManager(clubId = match.clubId, userId = userId)
        if (match.status != SCHEDULED) {
            throw InvalidMatchStateException("Events can only be modified on scheduled matches (reopen a completed match first)")
        }
        return match
    }
}
