package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.UserId

data class ChatParticipantModel(
    val userId: UserId,
    val username: String,
    val email: String,
    val profilePictureUrl: String?
)
