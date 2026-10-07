package com.kikepb.squadfy.api.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class MatchScoreRequest(
    @field:Min(value = 0, message = "teamAScore cannot be negative")
    @field:Max(value = 99, message = "teamAScore cannot exceed 99")
    val teamAScore: Int,
    @field:Min(value = 0, message = "teamBScore cannot be negative")
    @field:Max(value = 99, message = "teamBScore cannot exceed 99")
    val teamBScore: Int
)
