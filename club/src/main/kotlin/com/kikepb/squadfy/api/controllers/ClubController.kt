package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.AddScheduleExceptionRequest
import com.kikepb.squadfy.api.dto.ClubDto
import com.kikepb.squadfy.api.dto.ClubMemberDto
import com.kikepb.squadfy.api.dto.ClubScheduleExceptionDto
import com.kikepb.squadfy.api.dto.CreateClubRequest
import com.kikepb.squadfy.api.dto.InvitationCodeDto
import com.kikepb.squadfy.api.dto.JoinClubRequest
import com.kikepb.squadfy.api.dto.UpdateClubMemberRequest
import com.kikepb.squadfy.api.dto.UpdateScheduleRequest
import com.kikepb.squadfy.api.mappers.toClubDto
import com.kikepb.squadfy.api.mappers.toClubMemberDto
import com.kikepb.squadfy.api.mappers.toClubScheduleExceptionDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.ClubScheduleExceptionId
import com.kikepb.squadfy.infrastructure.storage.SupabaseStorageService
import com.kikepb.squadfy.service.ClubService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/club")
class ClubController(
    private val clubService: ClubService,
    private val storageService: SupabaseStorageService
) {

    @GetMapping
    fun getClubsForUser(): List<ClubDto> {
        return clubService
            .getClubsForUser(userId = requestUserId)
            .map { it.toClubDto() }
    }

    @GetMapping("/{clubId}")
    fun getClubById(@PathVariable("clubId") clubId: ClubId): ClubDto {
        return clubService
            .getClubById(clubId = clubId, userId = requestUserId)
            .toClubDto()
    }

    @PostMapping("/create")
    fun createClub(@Valid @RequestBody body: CreateClubRequest): ClubDto {
        return clubService.createClub(
            userId = requestUserId,
            name = body.name,
            description = body.description,
            clubLogoUrl = body.clubLogoUrl,
            maxMembers = body.maxMembers
        ).toClubDto()
    }

    @PostMapping("/{clubId}/logo", consumes = ["multipart/form-data"])
    fun uploadClubLogo(
        @PathVariable("clubId") clubId: ClubId,
        @RequestPart("clubLogo") clubLogo: MultipartFile
    ): ClubDto {
        val logoUrl = storageService.uploadImage(
            bucket = "profile-pictures",
            folder = "clubs",
            bytes = clubLogo.bytes,
            mimeType = clubLogo.contentType ?: "image/jpeg"
        )
        return clubService.updateClubLogo(clubId = clubId, userId = requestUserId, logoUrl = logoUrl).toClubDto()
    }

    @PostMapping("/join")
    fun joinClub(@Valid @RequestBody body: JoinClubRequest): ClubDto {
        return clubService.joinClub(
            userId = requestUserId,
            invitationCode = body.invitationCode,
            shirtNumber = body.shirtNumber,
            position = body.position
        ).toClubDto()
    }

    @GetMapping("/{clubId}/members")
    fun getClubMembers(@PathVariable("clubId") clubId: ClubId): List<ClubMemberDto> {
        return clubService
            .getMembers(clubId = clubId, userId = requestUserId)
            .map { it.toClubMemberDto() }
    }

    @PatchMapping("/{clubId}/members/{memberId}")
    fun updateMember(
        @PathVariable("clubId") clubId: ClubId,
        @PathVariable("memberId") memberId: ClubMemberId,
        @Valid @RequestBody body: UpdateClubMemberRequest
    ): ClubMemberDto {
        return clubService.updateMember(
            clubId = clubId,
            userId = requestUserId,
            memberId = memberId,
            shirtNumber = body.shirtNumber,
            position = body.position,
            rating = body.rating
        ).toClubMemberDto()
    }

    @PostMapping("/{clubId}/regenerate-invitation-code")
    fun regenerateInvitationCode(@PathVariable("clubId") clubId: ClubId): InvitationCodeDto {
        val invitationCode = clubService.regenerateInvitationCode(clubId = clubId, userId = requestUserId)
        return InvitationCodeDto(invitationCode = invitationCode)
    }

    @PatchMapping("/{clubId}/schedule")
    fun updateSchedule(
        @PathVariable("clubId") clubId: ClubId,
        @Valid @RequestBody body: UpdateScheduleRequest
    ): ClubDto {
        return clubService.updateSchedule(
            clubId = clubId,
            userId = requestUserId,
            matchDayOfWeek = body.matchDayOfWeek,
            matchStartTime = body.matchStartTime,
            matchEndTime = body.matchEndTime,
            seasonStartMonth = body.seasonStartMonth,
            seasonStartDay = body.seasonStartDay,
            drawTime = body.drawTime
        ).toClubDto()
    }

    @GetMapping("/{clubId}/schedule/exceptions")
    fun listScheduleExceptions(@PathVariable("clubId") clubId: ClubId): List<ClubScheduleExceptionDto> {
        return clubService
            .listScheduleExceptions(clubId = clubId, userId = requestUserId)
            .map { it.toClubScheduleExceptionDto() }
    }

    @PostMapping("/{clubId}/schedule/exceptions")
    fun addScheduleException(
        @PathVariable("clubId") clubId: ClubId,
        @Valid @RequestBody body: AddScheduleExceptionRequest
    ): ClubScheduleExceptionDto {
        return clubService.addScheduleException(
            clubId = clubId,
            userId = requestUserId,
            date = body.date,
            reason = body.reason
        ).toClubScheduleExceptionDto()
    }

    @DeleteMapping("/{clubId}/schedule/exceptions/{exceptionId}")
    fun removeScheduleException(
        @PathVariable("clubId") clubId: ClubId,
        @PathVariable("exceptionId") exceptionId: ClubScheduleExceptionId
    ) {
        clubService.removeScheduleException(clubId = clubId, userId = requestUserId, exceptionId = exceptionId)
    }
}
