package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.MatchFormat
import com.kikepb.squadfy.infrastructure.database.entities.DEFAULT_CLUB_TIME_ZONE
import com.kikepb.squadfy.infrastructure.database.entities.DEFAULT_MATCH_DURATION_MINUTES
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.DayOfWeek
import java.time.LocalTime

data class CreateClubMatchScheduleRequest(
    @field:NotNull(message = "matchDayOfWeek is required")
    val matchDayOfWeek: DayOfWeek,
    @field:NotNull(message = "matchTime is required")
    val matchTime: LocalTime,
    @field:NotBlank(message = "timeZone cannot be blank")
    val timeZone: String = DEFAULT_CLUB_TIME_ZONE,
    val format: MatchFormat = MatchFormat.ELEVEN_A_SIDE,
    @field:Min(value = 10, message = "matchDurationMinutes must be at least 10")
    @field:Max(value = 180, message = "matchDurationMinutes cannot exceed 180")
    val matchDurationMinutes: Int = DEFAULT_MATCH_DURATION_MINUTES
)
