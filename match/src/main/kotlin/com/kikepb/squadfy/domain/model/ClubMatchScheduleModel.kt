package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchScheduleId
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class ClubMatchScheduleModel(
    val id: ClubMatchScheduleId,
    val clubId: ClubId,
    val matchDayOfWeek: DayOfWeek,
    val matchTime: LocalTime,
    val timeZone: ZoneId,
    val format: MatchFormat,
    val maxPlayers: Int,
    val matchDurationMinutes: Int,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
