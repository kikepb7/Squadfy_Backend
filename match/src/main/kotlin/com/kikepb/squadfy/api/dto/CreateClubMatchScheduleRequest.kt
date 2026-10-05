package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.ClubId
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.DayOfWeek
import java.time.LocalTime

data class CreateClubMatchScheduleRequest(
    @field:NotNull(message = "clubId is required")
    val clubId: ClubId,
    @field:NotNull(message = "matchDayOfWeek is required")
    val matchDayOfWeek: DayOfWeek,
    @field:NotNull(message = "matchTime is required")
    val matchTime: LocalTime,
    @field:Min(value = 1, message = "matchAnnouncementOpenDaysBeforeMatch must be at least 1")
    @field:Max(value = 6, message = "matchAnnouncementOpenDaysBeforeMatch cannot exceed 6")
    val matchAnnouncementOpenDaysBeforeMatch: Int = 6,
    @field:Min(value = 2, message = "maxPlayers must be at least 2")
    @field:Max(value = 50, message = "maxPlayers cannot exceed 50")
    val maxPlayers: Int = 22
)
