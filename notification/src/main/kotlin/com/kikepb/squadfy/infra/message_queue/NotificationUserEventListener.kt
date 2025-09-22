package com.kikepb.squadfy.infra.message_queue

import com.kikepb.squadfy.domain.events.user.UserEvent
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class NotificationUserEventListener {

    @RabbitListener(queues = [MessageQueues.NOTIFICATION_USER_EVENTS])
    @Transactional
    fun handleUserEvent(event: UserEvent) {
        when (event) {
            is UserEvent.Created -> println("User created!")
            is UserEvent.RequestResendVerification -> println("Request resend verification!")
            is UserEvent.RequestResetPassword -> println("Request resend password!")
            is UserEvent.Verified -> println("User verified!")
            else -> Unit
        }
    }
}