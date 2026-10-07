package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.model.ClubMatchScheduleModel
import com.kikepb.squadfy.domain.model.MatchCalendar
import com.kikepb.squadfy.domain.model.MatchFormat
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.CANCELLED
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.SCHEDULED
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.infrastructure.database.entities.DEFAULT_CLUB_TIME_ZONE
import com.kikepb.squadfy.infrastructure.database.entities.DEFAULT_MATCH_DURATION_MINUTES
import com.kikepb.squadfy.infrastructure.database.entities.MatchEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toMatchModel
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMatchScheduleRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
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
    private val matchAnnouncementService: MatchAnnouncementService,
    private val clock: Clock
) {

    private val log = LoggerFactory.getLogger(MatchPlanningService::class.java)

    /**
     * Plans the next weekly match when the club has no upcoming scheduled match and no match
     * (even a cancelled one) exists yet on the next match day.
     */
    @Transactional
    fun planNextMatch(schedule: ClubMatchScheduleModel): MatchModel? {
        if (!schedule.isActive) return null

        val now = clock.instant()
        val zone = schedule.timeZone

        if (matchRepository.existsByClubIdAndStatusAndScheduledAtAfter(clubId = schedule.clubId, status = SCHEDULED, after = now)) {
            return null
        }

        val nextMatchDate = MatchCalendar.nextMatchDate(today = now.atZone(zone).toLocalDate(), matchDay = schedule.matchDayOfWeek)
        val dayStart = nextMatchDate.atStartOfDay(zone).toInstant()
        val nextDayStart = nextMatchDate.plusDays(1).atStartOfDay(zone).toInstant()

        if (matchRepository.existsByClubIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThan(
                clubId = schedule.clubId,
                from = dayStart,
                to = nextDayStart
            )
        ) return null

        val match = createMatchWithAnnouncement(
            clubId = schedule.clubId,
            scheduledAt = nextMatchDate.atTime(schedule.matchTime).atZone(zone).toInstant(),
            maxPlayers = schedule.maxPlayers,
            durationMinutes = schedule.matchDurationMinutes,
            zone = zone
        )
        log.info("[MatchPlanning] Planned match={} for club={} on {}", match.id, schedule.clubId, nextMatchDate)
        return match
    }

    @Transactional
    fun createMatchWithAnnouncement(
        clubId: ClubId,
        scheduledAt: Instant,
        maxPlayers: Int,
        durationMinutes: Int = durationFor(clubId),
        zone: ZoneId = zoneFor(clubId)
    ): MatchModel {
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
                durationMinutes = durationMinutes
            )
        )

        val window = MatchCalendar.announcementWindow(
            matchAt = scheduledAt,
            zone = zone,
            previousMatchAt = previousMatch?.scheduledAt,
            now = clock.instant()
        )

        matchAnnouncementService.createMatchAnnouncement(
            matchId = requireNotNull(match.id),
            clubId = clubId,
            maxPlayers = maxPlayers,
            opensAt = window.opensAt,
            closesAt = window.closesAt
        )

        return match.toMatchModel()
    }

    fun durationFor(clubId: ClubId): Int =
        clubMatchScheduleRepository.findByClubId(clubId = clubId)?.matchDurationMinutes ?: DEFAULT_MATCH_DURATION_MINUTES

    fun formatFor(clubId: ClubId): MatchFormat =
        clubMatchScheduleRepository.findByClubId(clubId = clubId)?.format ?: MatchFormat.ELEVEN_A_SIDE

    private fun zoneFor(clubId: ClubId): ZoneId =
        ZoneId.of(clubMatchScheduleRepository.findByClubId(clubId = clubId)?.timeZone ?: DEFAULT_CLUB_TIME_ZONE)
}
