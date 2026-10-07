package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.ScheduleExceptionModel.ExceptionType
import com.kikepb.squadfy.domain.type.ClubId
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ScheduleExceptionDto(
    val id: UUID,
    val clubId: ClubId,
    /** Date of the weekly schedule occurrence affected (club local date). */
    val date: LocalDate,
    val type: ExceptionType,
    val newScheduledAt: Instant?,
    val reason: String?,
    val createdAt: Instant
)

data class CreateScheduleExceptionRequest(
    @field:NotNull(message = "date is required")
    val date: LocalDate,
    @field:NotNull(message = "type is required")
    val type: ExceptionType,
    /** Required when type is RESCHEDULED. */
    val newScheduledAt: Instant? = null,
    @field:Size(max = 200, message = "reason can have at most 200 characters")
    val reason: String? = null
)
