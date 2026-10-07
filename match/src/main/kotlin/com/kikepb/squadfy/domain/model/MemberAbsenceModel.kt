package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class MemberAbsenceModel(
    val id: UUID,
    val clubId: ClubId,
    val clubMemberId: ClubMemberId,
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val reason: String?,
    val createdAt: Instant
) {
    fun covers(date: LocalDate): Boolean = !date.isBefore(fromDate) && !date.isAfter(toDate)
}
