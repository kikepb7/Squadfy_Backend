package com.kikepb.squadfy.api.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.DayOfWeek
import java.time.LocalTime

data class UpdateClubMatchScheduleRequest(
    val matchDayOfWeek: DayOfWeek? = null,
    val matchTime: LocalTime? = null,
    @field:Min(value = 1, message = "matchAnnouncementOpenDaysBeforeMatch must be at least 1")
    @field:Max(value = 6, message = "matchAnnouncementOpenDaysBeforeMatch cannot exceed 6")
    val matchAnnouncementOpenDaysBeforeMatch: Int? = null,
    @field:Min(value = 2, message = "maxPlayers must be at least 2")
    @field:Max(value = 50, message = "maxPlayers cannot exceed 50")
    val maxPlayers: Int? = null,
    val isActive: Boolean? = null
)
