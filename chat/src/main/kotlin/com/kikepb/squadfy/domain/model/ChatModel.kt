package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ChatId
import java.time.Instant

data class ChatModel(
    val id: ChatId,
    val participants: Set<ChatParticipantModel>,
    val lastMessage: ChatMessageModel?,
    val creator: ChatParticipantModel,
    val lastActivityAt: Instant,
    val createdAt: Instant
)
