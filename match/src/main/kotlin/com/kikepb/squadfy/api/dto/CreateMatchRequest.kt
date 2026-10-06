package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.MatchFormat
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class CreateMatchRequest(
    @field:NotNull(message = "scheduledAt is required")
    @field:Future(message = "scheduledAt must be a future date")
    val scheduledAt: Instant,
    /** Defaults to the club schedule's format. */
    val format: MatchFormat? = null
)
