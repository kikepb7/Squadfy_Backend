package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.UserId
import java.time.Instant

data class DeviceTokenModel(
    val id: Long,
    val userId: UserId,
    val token: String,
    val platform: Platform,
    val createdAt: Instant = Instant.now()
) {
    enum class Platform {
        ANDROID, IOS
    }
}
