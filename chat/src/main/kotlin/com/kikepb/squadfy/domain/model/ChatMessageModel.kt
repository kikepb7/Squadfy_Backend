package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ChatId
import com.kikepb.squadfy.domain.type.ChatMessageId
import java.time.Instant

data class ChatMessageModel(
    val id: ChatMessageId,
    val chatId: ChatId,
    val sender: ChatParticipantModel,
    val content: String,
    val createdAt: Instant
)
