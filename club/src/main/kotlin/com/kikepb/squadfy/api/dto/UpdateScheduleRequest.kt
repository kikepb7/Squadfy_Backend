package com.kikepb.squadfy.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.DayOfWeek
import java.time.LocalTime

data class UpdateScheduleRequest(
    @JsonProperty("matchDayOfWeek")
    val matchDayOfWeek: DayOfWeek? = null,
    @JsonProperty("matchStartTime")
    val matchStartTime: LocalTime? = null,
    @JsonProperty("matchEndTime")
    val matchEndTime: LocalTime? = null,
    @field:Min(value = 1, message = "Season start month must be between 1 and 12")
    @field:Max(value = 12, message = "Season start month must be between 1 and 12")
    @JsonProperty("seasonStartMonth")
    val seasonStartMonth: Int? = null,
    @field:Min(value = 1, message = "Season start day must be between 1 and 31")
    @field:Max(value = 31, message = "Season start day must be between 1 and 31")
    @JsonProperty("seasonStartDay")
    val seasonStartDay: Int? = null,
    @JsonProperty("drawTime")
    val drawTime: LocalTime? = null
)
