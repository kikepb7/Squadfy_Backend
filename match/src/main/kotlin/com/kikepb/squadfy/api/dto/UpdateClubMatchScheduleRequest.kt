package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.MatchFormat
import java.time.DayOfWeek
import java.time.LocalTime

data class UpdateClubMatchScheduleRequest(
    val matchDayOfWeek: DayOfWeek? = null,
    val matchTime: LocalTime? = null,
    val timeZone: String? = null,
    val format: MatchFormat? = null,
    val isActive: Boolean? = null
)
