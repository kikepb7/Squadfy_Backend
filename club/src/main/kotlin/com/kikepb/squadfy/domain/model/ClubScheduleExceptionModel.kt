package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubScheduleExceptionId
import java.time.Instant
import java.time.LocalDate

data class ClubScheduleExceptionModel(
    val id: ClubScheduleExceptionId,
    val clubId: ClubId,
    val date: LocalDate,
    val reason: String?,
    val createdAt: Instant
)
