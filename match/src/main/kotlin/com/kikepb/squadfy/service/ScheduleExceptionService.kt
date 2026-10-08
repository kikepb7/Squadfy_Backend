package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.ClubMatchScheduleNotFoundException
import com.kikepb.squadfy.domain.exception.InvalidMatchStateException
import com.kikepb.squadfy.domain.exception.InvalidScheduleExceptionException
import com.kikepb.squadfy.domain.exception.ScheduleExceptionAlreadyExistsException
import com.kikepb.squadfy.domain.exception.ScheduleExceptionNotFoundException
import com.kikepb.squadfy.domain.model.ClubMatchScheduleModel
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.CANCELLED
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.COMPLETED
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.SCHEDULED
import com.kikepb.squadfy.domain.model.ScheduleExceptionModel
import com.kikepb.squadfy.domain.model.ScheduleExceptionModel.ExceptionType
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ScheduleExceptionEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toClubMatchScheduleModel
import com.kikepb.squadfy.infrastructure.database.mappers.toScheduleExceptionModel
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMatchScheduleRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ScheduleExceptionRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Cancelled or moved weeks of the weekly schedule (spec 008 RN-B). */
@Service
class ScheduleExceptionService(
    private val scheduleExceptionRepository: ScheduleExceptionRepository,
    private val clubMatchScheduleRepository: ClubMatchScheduleRepository,
    private val matchRepository: MatchRepository,
    private val matchService: MatchService,
    private val matchPlanningService: MatchPlanningService,
    private val liveUpdatePublisher: LiveUpdatePublisher,
    private val clubAccessGuard: ClubAccessGuard,
    private val clock: Clock
) {

    fun getExceptions(clubId: ClubId, userId: UserId): List<ScheduleExceptionModel> {
        clubAccessGuard.requireMember(clubId = clubId, userId = userId)
        return scheduleExceptionRepository.findAllByClubIdOrderByScheduleDateAsc(clubId = clubId)
            .map { it.toScheduleExceptionModel() }
    }

    @Transactional
    fun createException(
        clubId: ClubId,
        userId: UserId,
        date: LocalDate,
        type: ExceptionType,
        newScheduledAt: Instant?,
        reason: String?
    ): ScheduleExceptionModel {
        clubAccessGuard.requireManager(clubId = clubId, userId = userId)
        val schedule = scheduleOf(clubId = clubId)
        val now = clock.instant()

        if (date.dayOfWeek != schedule.matchDayOfWeek) {
            throw InvalidScheduleExceptionException("date must be a ${schedule.matchDayOfWeek}, the club's match day")
        }
        val match = matchRepository.findFirstByClubIdAndScheduleDateOrderByCreatedAtDesc(clubId = clubId, scheduleDate = date)
        val currentMatchAt = match?.scheduledAt ?: matchPlanningService.regularMatchTime(schedule = schedule, date = date)
        if (!currentMatchAt.isAfter(now)) throw InvalidScheduleExceptionException("Only future weeks can have an exception")
        when (type) {
            ExceptionType.CANCELLED -> if (newScheduledAt != null) {
                throw InvalidScheduleExceptionException("newScheduledAt is only allowed for RESCHEDULED")
            }
            ExceptionType.RESCHEDULED -> if (newScheduledAt == null || !newScheduledAt.isAfter(now)) {
                throw InvalidScheduleExceptionException("RESCHEDULED requires a future newScheduledAt")
            }
        }
        if (scheduleExceptionRepository.existsByClubIdAndScheduleDate(clubId = clubId, scheduleDate = date)) {
            throw ScheduleExceptionAlreadyExistsException()
        }
        if (match?.status == COMPLETED) throw InvalidMatchStateException("The match of that week is already completed")
        if (type == ExceptionType.RESCHEDULED && match?.status == CANCELLED) {
            throw InvalidMatchStateException("The match of that week is cancelled")
        }

        val plannedMatch = match?.takeIf { it.status == SCHEDULED }
        val exception = scheduleExceptionRepository.saveAndFlush(
            ScheduleExceptionEntity(
                clubId = clubId,
                scheduleDate = date,
                type = type,
                newScheduledAt = newScheduledAt,
                reason = reason,
                affectedMatchId = plannedMatch?.id
            )
        )

        if (plannedMatch != null) {
            when (type) {
                ExceptionType.CANCELLED -> matchService.cancel(match = plannedMatch)
                ExceptionType.RESCHEDULED -> matchPlanningService.moveMatch(match = plannedMatch, newScheduledAt = requireNotNull(newScheduledAt))
            }
        }
        // A cancelled week lets the following one be planned right away.
        matchPlanningService.planNextMatch(schedule = schedule)
        liveUpdatePublisher.scheduleChanged(clubId = clubId)
        return exception.toScheduleExceptionModel()
    }

    /** Undoes an exception while the week is in the future and its match is not completed (RN-B4). */
    @Transactional
    fun deleteException(clubId: ClubId, userId: UserId, exceptionId: UUID) {
        clubAccessGuard.requireManager(clubId = clubId, userId = userId)
        val exception = scheduleExceptionRepository.findByIdOrNull(exceptionId)?.takeIf { it.clubId == clubId }
            ?: throw ScheduleExceptionNotFoundException()
        val schedule = scheduleOf(clubId = clubId)
        val now = clock.instant()

        val regularAt = matchPlanningService.regularMatchTime(schedule = schedule, date = exception.scheduleDate)
        val match = matchRepository.findFirstByClubIdAndScheduleDateOrderByCreatedAtDesc(clubId = clubId, scheduleDate = exception.scheduleDate)
        if (!regularAt.isAfter(now) || match?.scheduledAt?.isAfter(now) == false || match?.status == COMPLETED) {
            throw InvalidMatchStateException("The exception can no longer be undone: its week has already been played")
        }

        scheduleExceptionRepository.delete(exception)
        scheduleExceptionRepository.flush()
        liveUpdatePublisher.scheduleChanged(clubId = clubId)

        when (exception.type) {
            ExceptionType.CANCELLED -> when {
                match == null -> restoreWeek(schedule = schedule, date = exception.scheduleDate)
                match.status == CANCELLED && match.id == exception.affectedMatchId -> matchPlanningService.reactivateMatch(match = match)
            }
            ExceptionType.RESCHEDULED -> if (match?.status == SCHEDULED) {
                matchPlanningService.moveMatch(match = match, newScheduledAt = regularAt)
            }
        }
    }

    /**
     * Plans a week whose cancellation was undone before its match existed: right away when it comes
     * before every upcoming match (a later week may already be planned), otherwise planning reaches it.
     */
    private fun restoreWeek(schedule: ClubMatchScheduleModel, date: LocalDate) {
        if (!schedule.isActive) return
        val regularAt = matchPlanningService.regularMatchTime(schedule = schedule, date = date)
        val nextMatch = matchRepository.findFirstByClubIdAndStatusAndScheduledAtAfterOrderByScheduledAtAsc(
            clubId = schedule.clubId,
            status = SCHEDULED,
            after = clock.instant()
        )
        if (nextMatch == null) {
            matchPlanningService.planNextMatch(schedule = schedule)
        } else if (regularAt.isBefore(nextMatch.scheduledAt)) {
            matchPlanningService.planWeek(schedule = schedule, date = date)
        }
    }

    private fun scheduleOf(clubId: ClubId): ClubMatchScheduleModel =
        clubMatchScheduleRepository.findByClubId(clubId = clubId)?.toClubMatchScheduleModel()
            ?: throw ClubMatchScheduleNotFoundException()
}
