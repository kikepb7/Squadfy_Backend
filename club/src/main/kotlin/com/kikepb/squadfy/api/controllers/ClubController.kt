package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.ClubDto
import com.kikepb.squadfy.api.dto.ClubMatchDto
import com.kikepb.squadfy.api.dto.ClubMemberDto
import com.kikepb.squadfy.api.dto.CreateClubRequest
import com.kikepb.squadfy.api.dto.CreateMatchRequest
import com.kikepb.squadfy.api.dto.GenerateTeamsRequest
import com.kikepb.squadfy.api.dto.InvitationCodeDto
import com.kikepb.squadfy.api.dto.JoinClubRequest
import com.kikepb.squadfy.api.mappers.toClubDto
import com.kikepb.squadfy.api.mappers.toClubMatchDto
import com.kikepb.squadfy.api.mappers.toClubMemberDto
import com.kikepb.squadfy.api.mappers.toDomain
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.infrastructure.storage.SupabaseStorageService
import com.kikepb.squadfy.service.ClubService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
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
        val logoUrl = storageService.uploadClubLogo(
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

    @PostMapping("/{clubId}/regenerate-invitation-code")
    fun regenerateInvitationCode(@PathVariable("clubId") clubId: ClubId): InvitationCodeDto {
        val invitationCode = clubService.regenerateInvitationCode(clubId = clubId, userId = requestUserId)
        return InvitationCodeDto(invitationCode = invitationCode)
    }

    @PostMapping("/{clubId}/matches")
    fun createMatch(
        @PathVariable("clubId") clubId: ClubId,
        @Valid @RequestBody body: CreateMatchRequest
    ): ClubMatchDto {
        return clubService
            .createMatch(clubId = clubId, userId = requestUserId, scheduledAt = body.scheduledAt)
            .toClubMatchDto()
    }

    @PostMapping("/matches/{matchId}/generate-teams")
    fun generateTeams(
        @PathVariable("matchId") matchId: ClubMatchId,
        @Valid @RequestBody body: GenerateTeamsRequest
    ): ClubMatchDto {
        return clubService.generateTeams(
            matchId = matchId,
            userId = requestUserId,
            mode = body.mode.toDomain(),
            manualTeamA = body.manualTeamA,
            manualTeamB = body.manualTeamB
        ).toClubMatchDto()
    }

    @GetMapping("/matches/{matchId}")
    fun getMatch(@PathVariable("matchId") matchId: ClubMatchId): ClubMatchDto {
        return clubService
            .getMatch(matchId = matchId, userId = requestUserId)
            .toClubMatchDto()
    }
}
