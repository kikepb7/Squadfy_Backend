package com.kikepb.squadfy.domain.exception

import com.kikepb.squadfy.domain.type.UserId

class ChatParticipantNotFoundException(
    private val id: UserId
): RuntimeException(
    "The chat participant with the ID $id was not found."
)