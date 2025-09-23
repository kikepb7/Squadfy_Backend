package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ChatId
import com.kikepb.squadfy.domain.type.ChatMessageId
import java.time.Instant

data class ChatMessage(
    val id: ChatMessageId,
    val chatId: ChatId,
    val sender: ChatParticipant,
    val content: String,
    val createdAt: Instant
)
