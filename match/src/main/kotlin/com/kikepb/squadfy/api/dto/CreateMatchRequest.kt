package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.MatchFormat
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class CreateMatchRequest(
    @field:NotNull(message = "scheduledAt is required")
    @field:Future(message = "scheduledAt must be a future date")
    val scheduledAt: Instant,
    /** Defaults to the club schedule's format. */
    val format: MatchFormat? = null,
    /** Defaults to the club schedule's duration (60 minutes without schedule). */
    @field:Min(value = 10, message = "durationMinutes must be at least 10")
    @field:Max(value = 180, message = "durationMinutes cannot exceed 180")
    val durationMinutes: Int? = null
)
