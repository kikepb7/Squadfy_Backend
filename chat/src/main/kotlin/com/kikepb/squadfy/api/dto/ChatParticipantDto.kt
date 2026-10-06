package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.UserId

data class ChatParticipantDto(
    val userId: UserId,
    val username: String,
    val profilePictureUrl: String?
)
