package com.kikepb.squadfy.api.dto.websocket

import com.kikepb.squadfy.domain.type.UserId

data class ProfilePictureUpdateDto(
    val userId: UserId,
    val newUrl: String?
)
