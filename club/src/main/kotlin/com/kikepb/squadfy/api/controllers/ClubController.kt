package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.ClubDto
import com.kikepb.squadfy.api.dto.ClubMemberDto
import com.kikepb.squadfy.api.dto.CreateClubRequest
import com.kikepb.squadfy.api.dto.InvitationCodeDto
import com.kikepb.squadfy.api.dto.JoinClubRequest
import com.kikepb.squadfy.api.dto.UpdateMyMembershipRequest
import com.kikepb.squadfy.api.mappers.toClubDto
import com.kikepb.squadfy.api.mappers.toClubMemberDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.service.ClubService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/clubs")
@Tag(name = "Clubs", description = "Clubs, invitations and memberships")
class ClubController(
    private val clubService: ClubService
) {

    @GetMapping
    @Operation(summary = "Clubs the authenticated user belongs to")
    fun getClubsForUser(): List<ClubDto> =
        clubService
            .getClubsForUser(userId = requestUserId)
            .map { it.toClubDto() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a club; the creator becomes its OWNER")
    fun createClub(@Valid @RequestBody body: CreateClubRequest): ClubDto =
        clubService.createClub(
            userId = requestUserId,
            name = body.name,
            description = body.description,
            clubLogoUrl = body.clubLogoUrl,
            maxMembers = body.maxMembers
        ).toClubDto()

    @PostMapping("/join")
    @Operation(summary = "Join a club with its invitation code")
    fun joinClub(@Valid @RequestBody body: JoinClubRequest): ClubDto =
        clubService.joinClub(
            userId = requestUserId,
            invitationCode = body.invitationCode,
            shirtNumber = body.shirtNumber,
            position = body.position
        ).toClubDto()

    @GetMapping("/{clubId}")
    @Operation(summary = "Club details (members only)")
    fun getClubById(@PathVariable("clubId") clubId: ClubId): ClubDto =
        clubService
            .getClubById(clubId = clubId, userId = requestUserId)
            .toClubDto()

    @PutMapping("/{clubId}/logo", consumes = ["multipart/form-data"])
    @Operation(summary = "Upload the club logo (managers only); part name `clubLogo`")
    fun uploadClubLogo(
        @PathVariable("clubId") clubId: ClubId,
        @RequestPart("clubLogo") clubLogo: MultipartFile
    ): ClubDto =
        clubService.updateClubLogo(
            clubId = clubId,
            userId = requestUserId,
            bytes = clubLogo.bytes,
            mimeType = clubLogo.contentType ?: "image/jpeg"
        ).toClubDto()

    @PostMapping("/{clubId}/invitation-code")
    @Operation(summary = "Generate a new invitation code (managers only); the previous one stops working")
    fun regenerateInvitationCode(@PathVariable("clubId") clubId: ClubId): InvitationCodeDto =
        InvitationCodeDto(invitationCode = clubService.regenerateInvitationCode(clubId = clubId, userId = requestUserId))

    @GetMapping("/{clubId}/members")
    @Operation(summary = "Members of the club (members only); emails are not exposed")
    fun getClubMembers(@PathVariable("clubId") clubId: ClubId): List<ClubMemberDto> =
        clubService
            .getMembers(clubId = clubId, userId = requestUserId)
            .map { it.toClubMemberDto() }

    @PatchMapping("/{clubId}/members/me")
    @Operation(summary = "Update my shirt number and position in this club")
    fun updateMyMembership(
        @PathVariable("clubId") clubId: ClubId,
        @Valid @RequestBody body: UpdateMyMembershipRequest
    ): ClubMemberDto =
        clubService.updateMyMembership(
            clubId = clubId,
            userId = requestUserId,
            shirtNumber = body.shirtNumber,
            position = body.position
        ).toClubMemberDto()
}
