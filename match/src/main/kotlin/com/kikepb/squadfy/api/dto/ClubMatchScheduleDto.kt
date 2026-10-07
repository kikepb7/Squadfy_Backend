package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.MatchFormat
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
    val timeZone: String,
    val format: MatchFormat,
    val maxPlayers: Int,
    val matchDurationMinutes: Int,
    val closeDaysBefore: Int,
    val closeTime: LocalTime,
    val drawDaysBefore: Int,
    val drawTime: LocalTime,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
