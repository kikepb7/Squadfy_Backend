package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.events.match.MatchEvent
import com.kikepb.squadfy.domain.model.MatchNotificationMessages
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/** Turns match cycle events into push notifications, skipping users who muted the club. */
@Service
class MatchPushNotificationService(
    private val pushNotificationService: PushNotificationService,
    private val clubNotificationSettingsService: ClubNotificationSettingsService
) {

    private val logger = LoggerFactory.getLogger(MatchPushNotificationService::class.java)

    fun send(event: MatchEvent) {
        val pushes = MatchNotificationMessages.of(event)
        val muted = if (MatchNotificationMessages.ignoresMute(event)) {
            emptySet()
        } else {
            clubNotificationSettingsService.mutedUsers(clubId = event.clubId, userIds = pushes.flatMap { it.userIds })
        }

        pushes.forEach { push ->
            val recipients = push.userIds.filterNot { it in muted }
            pushNotificationService.sendToUsers(
                userIds = recipients,
                title = push.title,
                message = push.body,
                data = push.data,
                collapseKey = push.collapseKey
            )
        }
        logger.info("Processed {} for match {} ({} muted users skipped)", event.eventKey, event.matchId, muted.size)
    }
}
