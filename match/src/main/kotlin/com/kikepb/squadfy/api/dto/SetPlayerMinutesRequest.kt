package com.kikepb.squadfy.api.dto

import jakarta.validation.constraints.Min

data class SetPlayerMinutesRequest(
    @field:Min(value = 0, message = "minutes cannot be negative")
    val minutes: Int
)
