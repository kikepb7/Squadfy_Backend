package com.kikepb.squadfy.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class MatchSchedulerService(
    private val clubMatchScheduleService: ClubMatchScheduleService,
    private val matchPlanningService: MatchPlanningService,
    private val matchAnnouncementService: MatchAnnouncementService
) {

    private val log = LoggerFactory.getLogger(MatchSchedulerService::class.java)

    /** Hourly and idempotent, so a missed run (deploy, restart) is caught up automatically. */
    @Scheduled(cron = "0 5 * * * *", zone = "UTC")
    fun planUpcomingMatches() {
        clubMatchScheduleService.findAllActive().forEach { schedule ->
            try {
                matchPlanningService.planNextMatch(schedule = schedule)
            } catch (ex: Exception) {
                log.error("[MatchScheduler] Error planning match for club={}: {}", schedule.clubId, ex.message, ex)
            }
        }
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    fun closeExpiredMatchAnnouncements() {
        val closed = matchAnnouncementService.closeExpiredMatchAnnouncements()
        if (closed > 0) log.info("[MatchScheduler] Closed {} expired match announcements", closed)
    }
}
