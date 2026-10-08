package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.ClubMatchScheduleAlreadyExistsException
import com.kikepb.squadfy.domain.exception.ClubMatchScheduleNotFoundException
import com.kikepb.squadfy.domain.exception.InvalidClubMatchScheduleException
import com.kikepb.squadfy.domain.model.ClubMatchScheduleModel
import com.kikepb.squadfy.domain.model.DeadlineRule
import com.kikepb.squadfy.domain.model.MatchFormat
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ClubMatchScheduleEntity
import com.kikepb.squadfy.infrastructure.database.entities.DEFAULT_MATCH_DURATION_MINUTES
import com.kikepb.squadfy.infrastructure.database.mappers.toClubMatchScheduleModel
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMatchScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId

@Service
class ClubMatchScheduleService(
    private val clubMatchScheduleRepository: ClubMatchScheduleRepository,
    private val matchPlanningService: MatchPlanningService,
    private val liveUpdatePublisher: LiveUpdatePublisher,
    private val clubAccessGuard: ClubAccessGuard
) {

    @Transactional
    fun createSchedule(
        clubId: ClubId,
        userId: UserId,
        matchDayOfWeek: DayOfWeek,
        matchTime: LocalTime,
        timeZone: String,
        format: MatchFormat,
        matchDurationMinutes: Int = DEFAULT_MATCH_DURATION_MINUTES,
        close: DeadlineRule = DeadlineRule.DEFAULT,
        draw: DeadlineRule? = null
    ): ClubMatchScheduleModel {
        clubAccessGuard.requireManager(clubId = clubId, userId = userId)
        val drawRule = draw ?: close
        validateDeadlines(matchTime = matchTime, close = close, draw = drawRule)

        if (clubMatchScheduleRepository.existsByClubId(clubId = clubId)) {
            throw ClubMatchScheduleAlreadyExistsException()
        }

        val schedule = clubMatchScheduleRepository.saveAndFlush(
            ClubMatchScheduleEntity(
                clubId = clubId,
                matchDayOfWeek = matchDayOfWeek,
                matchTime = matchTime,
                timeZone = parseZone(timeZone).id,
                format = format,
                maxPlayers = format.maxPlayers,
                matchDurationMinutes = matchDurationMinutes,
                closeDaysBefore = close.daysBefore,
                closeTime = close.time,
                drawDaysBefore = drawRule.daysBefore,
                drawTime = drawRule.time
            )
        ).toClubMatchScheduleModel()

        matchPlanningService.planNextMatch(schedule = schedule)
        liveUpdatePublisher.scheduleChanged(clubId = clubId)
        return schedule
    }

    fun getScheduleByClub(clubId: ClubId, userId: UserId): ClubMatchScheduleModel {
        clubAccessGuard.requireMember(clubId = clubId, userId = userId)
        return clubMatchScheduleRepository.findByClubId(clubId = clubId)?.toClubMatchScheduleModel()
            ?: throw ClubMatchScheduleNotFoundException()
    }

    @Transactional
    fun updateSchedule(
        clubId: ClubId,
        userId: UserId,
        matchDayOfWeek: DayOfWeek?,
        matchTime: LocalTime?,
        timeZone: String?,
        format: MatchFormat?,
        isActive: Boolean?,
        matchDurationMinutes: Int? = null,
        closeDaysBefore: Int? = null,
        closeTime: LocalTime? = null,
        drawDaysBefore: Int? = null,
        drawTime: LocalTime? = null
    ): ClubMatchScheduleModel {
        clubAccessGuard.requireManager(clubId = clubId, userId = userId)
        val entity = clubMatchScheduleRepository.findByClubId(clubId = clubId)
            ?: throw ClubMatchScheduleNotFoundException()

        matchDayOfWeek?.let { entity.matchDayOfWeek = it }
        matchTime?.let { entity.matchTime = it }
        timeZone?.let { entity.timeZone = parseZone(it).id }
        format?.let {
            entity.format = it
            entity.maxPlayers = it.maxPlayers
        }
        isActive?.let { entity.isActive = it }
        matchDurationMinutes?.let { entity.matchDurationMinutes = it }
        closeDaysBefore?.let { entity.closeDaysBefore = it }
        closeTime?.let { entity.closeTime = it }
        drawDaysBefore?.let { entity.drawDaysBefore = it }
        drawTime?.let { entity.drawTime = it }
        validateDeadlines(
            matchTime = entity.matchTime,
            close = DeadlineRule(daysBefore = entity.closeDaysBefore, time = entity.closeTime),
            draw = DeadlineRule(daysBefore = entity.drawDaysBefore, time = entity.drawTime)
        )

        val schedule = clubMatchScheduleRepository.saveAndFlush(entity).toClubMatchScheduleModel()
        if (schedule.isActive) matchPlanningService.planNextMatch(schedule = schedule)
        liveUpdatePublisher.scheduleChanged(clubId = clubId)
        return schedule
    }

    fun findAllActive(): List<ClubMatchScheduleModel> =
        clubMatchScheduleRepository.findAllByIsActive(isActive = true)
            .map { it.toClubMatchScheduleModel() }

    /** Close before kickoff and draw between the close and kickoff (spec 008 RN-D2). */
    private fun validateDeadlines(matchTime: LocalTime, close: DeadlineRule, draw: DeadlineRule) {
        DeadlineRule.validate(matchTime = matchTime, close = close, draw = draw)
            ?.let { throw InvalidClubMatchScheduleException(it) }
    }

    private fun parseZone(timeZone: String): ZoneId =
        try {
            ZoneId.of(timeZone.trim())
        } catch (e: DateTimeException) {
            throw InvalidClubMatchScheduleException("Unknown time zone: $timeZone")
        }
}
