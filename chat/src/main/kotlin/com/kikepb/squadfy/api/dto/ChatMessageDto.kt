package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.ChatId
import com.kikepb.squadfy.domain.type.ChatMessageId
import com.kikepb.squadfy.domain.type.UserId
import java.time.Instant

data class ChatMessageDto(
    val id: ChatMessageId,
    val chatId: ChatId,
    val content: String,
    val createdAt: Instant,
    val senderId: UserId
)
