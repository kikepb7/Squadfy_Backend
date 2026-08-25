package com.kikepb.squadfy.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class AddScheduleExceptionRequest(
    @field:NotNull(message = "Date is required")
    @JsonProperty("date")
    val date: LocalDate,
    @field:Size(max = 500, message = "Reason can have at most 500 characters")
    @JsonProperty("reason")
    val reason: String? = null
)
