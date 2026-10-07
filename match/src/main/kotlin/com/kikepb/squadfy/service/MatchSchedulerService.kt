package com.kikepb.squadfy.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class MatchSchedulerService(
    private val clubMatchScheduleService: ClubMatchScheduleService,
    private val matchPlanningService: MatchPlanningService,
    private val matchAnnouncementService: MatchAnnouncementService,
    private val matchTeamService: MatchTeamService,
    private val matchAnnouncementNotificationService: MatchAnnouncementNotificationService
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

    /**
     * Every 5 minutes: closes the announcements whose close time passed and then publishes the teams of
     * the closed announcements whose draw time arrived (spec 008 RN-D3), in any time zone.
     */
    @Scheduled(cron = "0 */5 * * * *", zone = "UTC")
    fun closeExpiredMatchAnnouncements() {
        val closedMatchIds = matchAnnouncementService.closeExpiredMatchAnnouncements()
        if (closedMatchIds.isNotEmpty()) log.info("[MatchScheduler] Closed {} expired match announcements", closedMatchIds.size)

        matchAnnouncementService.findDueDraws().forEach { matchId ->
            try {
                matchTeamService.publishTeamsAtDrawTime(matchId = matchId)
                    ?.let { log.info("[MatchScheduler] Published teams for match={}", matchId) }
            } catch (ex: Exception) {
                log.error("[MatchScheduler] Error publishing teams for match={}: {}", matchId, ex.message, ex)
            }
        }
    }

    /** Opening and closing-soon push notifications (spec 005), each sent once per announcement. */
    @Scheduled(cron = "30 */5 * * * *", zone = "UTC")
    fun sendAnnouncementNotifications() {
        try {
            val opened = matchAnnouncementNotificationService.notifyOpenedAnnouncements()
            val reminded = matchAnnouncementNotificationService.sendClosingReminders()
            if (opened + reminded > 0) log.info("[MatchScheduler] Notified {} opened and {} closing announcements", opened, reminded)
        } catch (ex: Exception) {
            log.error("[MatchScheduler] Error sending announcement notifications: {}", ex.message, ex)
        }
    }
}
