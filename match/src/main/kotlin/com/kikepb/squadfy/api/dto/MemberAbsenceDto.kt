package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class MemberAbsenceDto(
    val id: UUID,
    val clubId: ClubId,
    val clubMemberId: ClubMemberId,
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val reason: String?,
    val createdAt: Instant
)

data class CreateMemberAbsenceRequest(
    @field:NotNull(message = "fromDate is required")
    val fromDate: LocalDate,
    @field:NotNull(message = "toDate is required")
    val toDate: LocalDate,
    @field:Size(max = 200, message = "reason can have at most 200 characters")
    val reason: String? = null
)
