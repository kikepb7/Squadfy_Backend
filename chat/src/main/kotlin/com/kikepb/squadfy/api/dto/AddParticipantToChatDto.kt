package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.UserId

data class AddParticipantToChatDto(
    val userIds: List<UserId>
)