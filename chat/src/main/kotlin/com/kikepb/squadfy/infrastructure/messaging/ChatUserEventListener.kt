package com.kikepb.squadfy.infrastructure.messaging

import com.kikepb.squadfy.domain.events.user.UserEvent
import com.kikepb.squadfy.domain.events.user.UserEvent.Verified
import com.kikepb.squadfy.domain.model.ChatParticipantModel
import com.kikepb.squadfy.infrastructure.message_queue.MessageQueues
import com.kikepb.squadfy.service.ChatParticipantService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ChatUserEventListener(
    private val chatParticipantService: ChatParticipantService
) {

    @RabbitListener(queues = [MessageQueues.CHAT_USER_EVENTS])
    @Transactional
    fun handleUserEvent(event: UserEvent) {
        when (event) {
            is Verified -> {
                chatParticipantService.createChatParticipant(
                    chatParticipant = ChatParticipantModel(
                        userId = event.userId,
                        username = event.username,
                        email = event.email,
                        profilePictureUrl = null
                    )
                )
            } else -> Unit
        }
    }
}