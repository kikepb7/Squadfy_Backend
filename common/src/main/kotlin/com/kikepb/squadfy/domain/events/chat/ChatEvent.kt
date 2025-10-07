package com.kikepb.squadfy.domain.events.chat

import com.kikepb.squadfy.domain.events.SquadfyEvent
import com.kikepb.squadfy.domain.type.ChatId
import com.kikepb.squadfy.domain.type.UserId
import java.time.Instant
import java.util.UUID

sealed class ChatEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val exchange: String = ChatEventConstant.CHAT_EXCHANGE,
    override val occurredAt: Instant = Instant.now()
): SquadfyEvent {

    data class NewMessage(
        val senderId: UserId,
        val senderUsername: String,
        val recipientIds: Set<UserId>,
        val chatId: ChatId,
        val message: String,
        override val eventKey: String = ChatEventConstant.CHAT_NEW_MESSAGE
    ): ChatEvent(), SquadfyEvent
}