package com.kikepb.squadfy.domain.exception

import com.kikepb.squadfy.domain.type.ChatMessageId

class MessageNotFoundException(
    private val id: ChatMessageId
): RuntimeException(
    "Message with ID $id not found"
)