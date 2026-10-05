package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchScheduleId
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

data class ClubMatchScheduleDto(
    val id: ClubMatchScheduleId,
    val clubId: ClubId,
    val matchDayOfWeek: DayOfWeek,
    val matchTime: LocalTime,
    val matchAnnouncementOpenDaysBeforeMatch: Int,
    val maxPlayers: Int,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
