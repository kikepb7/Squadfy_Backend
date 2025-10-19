package com.kikepb.squadfy.infrastructure.message_queue

import com.kikepb.squadfy.domain.events.chat.ChatEvent
import com.kikepb.squadfy.domain.events.chat.ChatEvent.NewMessage
import com.kikepb.squadfy.service.PushNotificationService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class NotificationChatEventListener(private val pushNotificationService: PushNotificationService) {

    @RabbitListener(queues = [MessageQueues.NOTIFICATION_CHAT_EVENTS])
    @Transactional
    fun handleUserEvent(event: ChatEvent) {
        when (event) {
            is NewMessage -> {
                pushNotificationService.sendNewMessageNotifications(
                    recipientUserIds = event.recipientIds.toList(),
                    senderUserId = event.senderId,
                    senderUsername = event.senderUsername,
                    message = event.message,
                    chatId = event.chatId
                )
            }
        }
    }
}