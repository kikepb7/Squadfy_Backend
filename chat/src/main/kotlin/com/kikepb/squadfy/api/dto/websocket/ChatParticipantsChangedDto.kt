package com.kikepb.squadfy.api.dto.websocket

import com.kikepb.squadfy.domain.type.ChatId

data class ChatParticipantsChangedDto(
    val chatId: ChatId
)
