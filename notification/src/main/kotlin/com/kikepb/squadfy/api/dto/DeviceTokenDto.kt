package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.UserId
import java.time.Instant

data class DeviceTokenDto(
    val userId: UserId,
    val token: String,
    val createdAt: Instant
)
