package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.MatchEventType
import com.kikepb.squadfy.domain.type.ClubMemberId
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class AddMatchEventRequest(
    @field:NotNull(message = "clubMemberId is required")
    val clubMemberId: ClubMemberId,
    @field:NotNull(message = "type is required")
    val type: MatchEventType,
    @field:Min(value = 1, message = "minute must be at least 1")
    @field:Max(value = 120, message = "minute cannot exceed 120")
    val minute: Int? = null
)
