package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.CreateMemberAbsenceRequest
import com.kikepb.squadfy.api.dto.MemberAbsenceDto
import com.kikepb.squadfy.api.mappers.toMemberAbsenceDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.service.MemberAbsenceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/clubs/{clubId}")
@Tag(name = "Absences", description = "Periods in which a member will not play")
class MemberAbsenceController(
    private val memberAbsenceService: MemberAbsenceService
) {

    @GetMapping("/absences")
    @Operation(summary = "Absences of the club overlapping from–to, both optional (members only)")
    fun getAbsences(
        @PathVariable("clubId") clubId: ClubId,
        @RequestParam("from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam("to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): List<MemberAbsenceDto> =
        memberAbsenceService
            .getAbsences(clubId = clubId, userId = requestUserId, from = from, to = to)
            .map { it.toMemberAbsenceDto() }

    @PostMapping("/members/me/absences")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Register an absence of the current member",
        description = "Withdraws the member from open announcements of matches in the period. They can still enroll again if they finally come."
    )
    fun createAbsence(
        @PathVariable("clubId") clubId: ClubId,
        @Valid @RequestBody body: CreateMemberAbsenceRequest
    ): MemberAbsenceDto =
        memberAbsenceService.createAbsence(
            clubId = clubId,
            userId = requestUserId,
            fromDate = body.fromDate,
            toDate = body.toDate,
            reason = body.reason?.trim()?.ifBlank { null }
        ).toMemberAbsenceDto()

    @DeleteMapping("/members/me/absences/{absenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete one of my absences")
    fun deleteAbsence(
        @PathVariable("clubId") clubId: ClubId,
        @PathVariable("absenceId") absenceId: UUID
    ) {
        memberAbsenceService.deleteAbsence(clubId = clubId, userId = requestUserId, absenceId = absenceId)
    }
}
