package com.kikepb.squadfy.domain.event

import com.kikepb.squadfy.domain.type.ChatId
import com.kikepb.squadfy.domain.type.UserId

data class ChatParticipantsJoinedEvent(
    val chatId: ChatId,
    val userIds: Set<UserId>
)
