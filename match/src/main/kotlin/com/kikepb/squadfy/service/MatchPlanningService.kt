package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.model.ClubMatchScheduleModel
import com.kikepb.squadfy.domain.model.DeadlineRule
import com.kikepb.squadfy.domain.model.MatchCalendar
import com.kikepb.squadfy.domain.model.MatchFormat
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.CANCELLED
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.SCHEDULED
import com.kikepb.squadfy.domain.model.ScheduleExceptionModel.ExceptionType
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.infrastructure.database.entities.DEFAULT_CLUB_TIME_ZONE
import com.kikepb.squadfy.infrastructure.database.entities.DEFAULT_MATCH_DURATION_MINUTES
import com.kikepb.squadfy.infrastructure.database.entities.MatchEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toClubMatchScheduleModel
import com.kikepb.squadfy.infrastructure.database.mappers.toMatchModel
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMatchScheduleRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ScheduleExceptionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Creates matches together with their announcement, either from the weekly schedule or manually.
 * Planning is idempotent: it is safe to run it every hour and from several instances
 * (the unique (club_id, scheduled_at) index rejects duplicates).
 */
@Service
class MatchPlanningService(
    private val matchRepository: MatchRepository,
    private val clubMatchScheduleRepository: ClubMatchScheduleRepository,
    private val scheduleExceptionRepository: ScheduleExceptionRepository,
    private val matchAnnouncementService: MatchAnnouncementService,
    private val matchNotificationPublisher: MatchNotificationPublisher,
    private val clock: Clock
) {

    private val log = LoggerFactory.getLogger(MatchPlanningService::class.java)

    /**
     * Plans the next weekly match when the club has no upcoming scheduled match and no match
     * (even a cancelled one) exists yet for the next match week. Weeks cancelled by a schedule
     * exception are skipped and moved weeks are planned at their new time (spec 008 RN-B2).
     */
    @Transactional
    fun planNextMatch(schedule: ClubMatchScheduleModel): MatchModel? {
        if (!schedule.isActive) return null

        val now = clock.instant()
        val zone = schedule.timeZone

        if (matchRepository.existsByClubIdAndStatusAndScheduledAtAfter(clubId = schedule.clubId, status = SCHEDULED, after = now)) {
            return null
        }

        val firstDate = MatchCalendar.nextMatchDate(today = now.atZone(zone).toLocalDate(), matchDay = schedule.matchDayOfWeek)
        for (week in 0L until MAX_WEEKS_AHEAD) {
            val date = firstDate.plusWeeks(week)
            val exception = scheduleExceptionRepository.findByClubIdAndScheduleDate(clubId = schedule.clubId, scheduleDate = date)
            val scheduledAt = when (exception?.type) {
                ExceptionType.CANCELLED -> continue
                ExceptionType.RESCHEDULED -> exception.newScheduledAt?.takeIf { it.isAfter(now) } ?: continue
                null -> regularMatchTime(schedule = schedule, date = date)
            }
            if (isWeekPlanned(schedule = schedule, date = date)) return null
            return planWeek(schedule = schedule, date = date, scheduledAt = scheduledAt)
        }
        return null
    }

    /** Creates the match of a schedule week (its regular time unless [scheduledAt] says otherwise). */
    @Transactional
    fun planWeek(
        schedule: ClubMatchScheduleModel,
        date: LocalDate,
        scheduledAt: Instant? = null
    ): MatchModel {
        val match = createMatchWithAnnouncement(
            clubId = schedule.clubId,
            scheduledAt = scheduledAt ?: regularMatchTime(schedule = schedule, date = date),
            maxPlayers = schedule.maxPlayers,
            durationMinutes = schedule.matchDurationMinutes,
            zone = schedule.timeZone,
            scheduleDate = date,
            close = schedule.close,
            draw = schedule.draw
        )
        log.info("[MatchPlanning] Planned match={} for club={} (week of {})", match.id, schedule.clubId, date)
        return match
    }

    /**
     * Optional values are resolved inside the method, never as default-argument expressions: Kotlin evaluates those
     * in a static `$default` bridge that receives the Spring proxy, whose injected fields are null (extra match 500).
     */
    @Transactional
    fun createMatchWithAnnouncement(
        clubId: ClubId,
        scheduledAt: Instant,
        maxPlayers: Int,
        durationMinutes: Int? = null,
        zone: ZoneId? = null,
        scheduleDate: LocalDate? = null,
        close: DeadlineRule? = null,
        draw: DeadlineRule? = null
    ): MatchModel {
        val schedule by lazy { scheduleOf(clubId) }
        val matchDuration = durationMinutes ?: durationFor(clubId)
        val matchZone = zone ?: zoneFor(clubId)
        val closeRule = close ?: schedule?.close ?: DeadlineRule.DEFAULT
        val drawRule = draw ?: schedule?.draw ?: DeadlineRule.DEFAULT

        val previousMatch = matchRepository.findFirstByClubIdAndStatusNotAndScheduledAtBeforeOrderByScheduledAtDesc(
            clubId = clubId,
            status = CANCELLED,
            before = scheduledAt
        )

        val match = matchRepository.saveAndFlush(
            MatchEntity(
                clubId = clubId,
                scheduledAt = scheduledAt,
                status = SCHEDULED,
                scheduleDate = scheduleDate,
                durationMinutes = matchDuration
            )
        )

        val window = MatchCalendar.announcementWindow(
            matchAt = scheduledAt,
            zone = matchZone,
            previousMatchAt = previousMatch?.scheduledAt,
            now = clock.instant(),
            close = closeRule,
            draw = drawRule
        )

        matchAnnouncementService.createMatchAnnouncement(
            matchId = requireNotNull(match.id),
            clubId = clubId,
            maxPlayers = maxPlayers,
            opensAt = window.opensAt,
            closesAt = window.closesAt,
            drawAt = window.drawAt
        )

        return match.toMatchModel()
    }

    /**
     * Moves a scheduled match keeping its enrollments: close and draw are recalculated for the new
     * date and every member is notified (spec 008 RN-B3).
     */
    @Transactional
    fun moveMatch(match: MatchEntity, newScheduledAt: Instant) {
        val previousScheduledAt = match.scheduledAt
        if (previousScheduledAt == newScheduledAt) return

        val schedule = scheduleOf(match.clubId)
        match.scheduledAt = newScheduledAt
        matchRepository.saveAndFlush(match)

        val window = MatchCalendar.announcementWindow(
            matchAt = newScheduledAt,
            zone = schedule?.timeZone ?: ZoneId.of(DEFAULT_CLUB_TIME_ZONE),
            previousMatchAt = null,
            now = clock.instant(),
            close = schedule?.close ?: DeadlineRule.DEFAULT,
            draw = schedule?.draw ?: DeadlineRule.DEFAULT
        )
        matchAnnouncementService.updateDeadlines(matchId = requireNotNull(match.id), closesAt = window.closesAt, drawAt = window.drawAt)
        matchNotificationPublisher.matchRescheduled(
            clubId = match.clubId,
            matchId = requireNotNull(match.id),
            previousScheduledAt = previousScheduledAt,
            newScheduledAt = newScheduledAt
        )
        log.info("[MatchPlanning] Moved match={} from {} to {}", match.id, previousScheduledAt, newScheduledAt)
    }

    /** Brings back a match cancelled by a schedule exception, with its enrollments (spec 008 RN-B4). */
    @Transactional
    fun reactivateMatch(match: MatchEntity) {
        match.status = SCHEDULED
        matchRepository.saveAndFlush(match)
        matchAnnouncementService.reactivateForMatch(matchId = requireNotNull(match.id))
    }

    fun regularMatchTime(schedule: ClubMatchScheduleModel, date: LocalDate): Instant =
        date.atTime(schedule.matchTime).atZone(schedule.timeZone).toInstant()

    fun durationFor(clubId: ClubId): Int =
        clubMatchScheduleRepository.findByClubId(clubId = clubId)?.matchDurationMinutes ?: DEFAULT_MATCH_DURATION_MINUTES

    fun formatFor(clubId: ClubId): MatchFormat =
        clubMatchScheduleRepository.findByClubId(clubId = clubId)?.format ?: MatchFormat.ELEVEN_A_SIDE

    /** A week is planned when a match of that schedule date exists or (older data) a match on that day. */
    private fun isWeekPlanned(schedule: ClubMatchScheduleModel, date: LocalDate): Boolean {
        if (matchRepository.existsByClubIdAndScheduleDate(clubId = schedule.clubId, scheduleDate = date)) return true
        return matchRepository.existsByClubIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThan(
            clubId = schedule.clubId,
            from = date.atStartOfDay(schedule.timeZone).toInstant(),
            to = date.plusDays(1).atStartOfDay(schedule.timeZone).toInstant()
        )
    }

    private fun scheduleOf(clubId: ClubId): ClubMatchScheduleModel? =
        clubMatchScheduleRepository.findByClubId(clubId = clubId)?.toClubMatchScheduleModel()

    private fun zoneFor(clubId: ClubId): ZoneId =
        ZoneId.of(clubMatchScheduleRepository.findByClubId(clubId = clubId)?.timeZone ?: DEFAULT_CLUB_TIME_ZONE)

    private companion object {
        /** Consecutive cancelled weeks that planning looks through. */
        const val MAX_WEEKS_AHEAD = 26L
    }
}
