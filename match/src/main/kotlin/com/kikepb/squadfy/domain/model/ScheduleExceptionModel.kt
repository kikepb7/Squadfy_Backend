package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubId
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ScheduleExceptionModel(
    val id: UUID,
    val clubId: ClubId,
    val scheduleDate: LocalDate,
    val type: ExceptionType,
    val newScheduledAt: Instant?,
    val reason: String?,
    val createdAt: Instant
) {
    enum class ExceptionType {
        /** No match that week. */
        CANCELLED,
        /** The match of that week is played at [newScheduledAt]. */
        RESCHEDULED
    }
}
