package com.kikepb.squadfy.api.dto.websocket

import com.kikepb.squadfy.domain.type.ChatId
import com.kikepb.squadfy.domain.type.ChatMessageId

data class DeleteMessageDto(
    val chatId: ChatId,
    val messageId: ChatMessageId
)
