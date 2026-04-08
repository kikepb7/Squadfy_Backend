package com.kikepb.squadfy.infrastructure.messaging

import com.kikepb.squadfy.domain.events.user.UserEvent
import com.kikepb.squadfy.domain.events.user.UserEvent.Created
import com.kikepb.squadfy.domain.events.user.UserEvent.Verified
import com.kikepb.squadfy.domain.model.ClubParticipantModel
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.message_queue.MessageQueues
import com.kikepb.squadfy.service.ClubParticipantService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ClubUserEventListener(
    private val clubParticipantService: ClubParticipantService
) {

    @RabbitListener(queues = [MessageQueues.CLUB_USER_EVENTS])
    @Transactional
    fun handleUserEvent(event: UserEvent) {
        when (event) {
            is Created -> upsertUserParticipant(
                userId = event.userId,
                username = event.username,
                email = event.email
            )
            is Verified -> upsertUserParticipant(
                userId = event.userId,
                username = event.username,
                email = event.email
            )
            else -> Unit
        }
    }

    private fun upsertUserParticipant(
        userId: UserId,
        username: String,
        email: String
    ) {
        clubParticipantService.createClubParticipant(
            clubParticipantModel = ClubParticipantModel(
                userId = userId,
                username = username,
                email = email
            )
        )
    }
}
