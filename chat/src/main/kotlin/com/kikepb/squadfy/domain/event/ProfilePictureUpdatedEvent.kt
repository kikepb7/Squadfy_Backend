package com.kikepb.squadfy.domain.event

import com.kikepb.squadfy.domain.type.UserId

data class ProfilePictureUpdatedEvent(
    val userId: UserId,
    val newUrl: String?
)
