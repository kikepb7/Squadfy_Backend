package com.kikepb.squadfy.infrastructure.message_queue

import com.kikepb.squadfy.domain.events.match.MatchEvent
import com.kikepb.squadfy.service.MatchPushNotificationService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class NotificationMatchEventListener(
    private val matchPushNotificationService: MatchPushNotificationService
) {

    @RabbitListener(queues = [MessageQueues.NOTIFICATION_MATCH_EVENTS])
    fun handleMatchEvent(event: MatchEvent) = matchPushNotificationService.send(event = event)
}
