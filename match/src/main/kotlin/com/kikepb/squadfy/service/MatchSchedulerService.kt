package com.kikepb.squadfy.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@Service
class MatchSchedulerService(
    private val clubMatchScheduleService: ClubMatchScheduleService,
    private val matchService: MatchService,
    private val matchAnnouncementService: MatchAnnouncementService
) {

    private val log = LoggerFactory.getLogger(MatchSchedulerService::class.java)

    @Scheduled(cron = "0 0 8 * * *", zone = "UTC")
    fun generateWeeklyMatchAnnouncements() {
        val today = LocalDate.now(ZoneOffset.UTC)
        log.info("[MatchScheduler] generateWeeklyMatchAnnouncements — today={}", today)

        val activeSchedules = clubMatchScheduleService.findAllActive()

        for (schedule in activeSchedules) {
            try {
                val nextMatchDate = nextOccurrence(from = today, target = schedule.matchDayOfWeek)
                val matchAnnouncementOpenDate = nextMatchDate.minusDays(schedule.matchAnnouncementOpenDaysBeforeMatch.toLong())

                if (today != matchAnnouncementOpenDate) continue

                val matchDayStart = nextMatchDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                val matchDayEnd   = nextMatchDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC)

                if (matchService.hasMatchInWeek(
                        clubId    = schedule.clubId,
                        weekStart = matchDayStart,
                        weekEnd   = matchDayEnd
                    )
                ) {
                    log.info(
                        "[MatchScheduler] Match already exists for club={} on {}. Skipping.",
                        schedule.clubId, nextMatchDate
                    )
                    continue
                }

                val matchScheduledAt = nextMatchDate
                    .atTime(schedule.matchTime)
                    .toInstant(ZoneOffset.UTC)

                val match = matchService.createMatch(
                    clubId      = schedule.clubId,
                    scheduledAt = matchScheduledAt
                )

                val matchAnnouncementOpensAt  = matchAnnouncementOpenDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                val matchAnnouncementClosesAt = nextMatchDate
                    .minusDays(1)
                    .atTime(LocalTime.of(23, 59, 59))
                    .toInstant(ZoneOffset.UTC)

                matchAnnouncementService.createMatchAnnouncement(
                    matchId    = match.id,
                    clubId     = schedule.clubId,
                    maxPlayers = schedule.maxPlayers,
                    opensAt    = matchAnnouncementOpensAt,
                    closesAt   = matchAnnouncementClosesAt
                )

                log.info(
                    "[MatchScheduler] Created match={} + matchAnnouncement for club={}, matchDate={}, closesAt={}",
                    match.id, schedule.clubId, nextMatchDate, matchAnnouncementClosesAt
                )
            } catch (ex: Exception) {
                log.error(
                    "[MatchScheduler] Error processing schedule for club={}: {}",
                    schedule.clubId, ex.message, ex
                )
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    fun closeExpiredMatchAnnouncements() {
        val now = Instant.now()
        log.info("[MatchScheduler] closeExpiredMatchAnnouncements — now={}", now)
        matchAnnouncementService.closeExpiredMatchAnnouncement(before = now)
    }

    private fun nextOccurrence(from: LocalDate, target: DayOfWeek): LocalDate {
        val daysUntil = (target.value - from.dayOfWeek.value + 7) % 7
        return if (daysUntil == 0) from.plusWeeks(1) else from.plusDays(daysUntil.toLong())
    }
}
