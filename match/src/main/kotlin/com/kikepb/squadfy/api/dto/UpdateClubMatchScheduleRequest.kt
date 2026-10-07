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
    val isActive: Boolean? = null,
    /** Announcement closes this many days before the match at [closeTime] (default 1 day before, 22:00). */
    @field:Min(value = 0, message = "closeDaysBefore cannot be negative")
    @field:Max(value = 6, message = "closeDaysBefore cannot exceed 6")
    val closeDaysBefore: Int? = null,
    val closeTime: LocalTime? = null,
    /** Teams are drawn this many days before the match at [drawTime] (default: same as the close). */
    @field:Min(value = 0, message = "drawDaysBefore cannot be negative")
    @field:Max(value = 6, message = "drawDaysBefore cannot exceed 6")
    val drawDaysBefore: Int? = null,
    val drawTime: LocalTime? = null
)
