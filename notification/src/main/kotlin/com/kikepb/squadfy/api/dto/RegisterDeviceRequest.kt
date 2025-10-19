package com.kikepb.squadfy.api.dto

import jakarta.validation.constraints.NotBlank

data class RegisterDeviceRequest(
    @field:NotBlank
    val token: String,
    val platform: PlatformDto
)

enum class PlatformDto {
    ANDROID, IOS
}