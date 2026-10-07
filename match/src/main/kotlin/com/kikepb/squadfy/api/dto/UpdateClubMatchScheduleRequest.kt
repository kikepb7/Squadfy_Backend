package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.MatchFormat
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.DayOfWeek
import java.time.LocalTime

data class UpdateClubMatchScheduleRequest(
    val matchDayOfWeek: DayOfWeek? = null,
    val matchTime: LocalTime? = null,
    val timeZone: String? = null,
    val format: MatchFormat? = null,
    @field:Min(value = 10, message = "matchDurationMinutes must be at least 10")
    @field:Max(value = 180, message = "matchDurationMinutes cannot exceed 180")
    val matchDurationMinutes: Int? = null,
    val isActive: Boolean? = null
)
