package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.AddGuestRequest
import com.kikepb.squadfy.api.dto.ClubMatchDto
import com.kikepb.squadfy.api.dto.CreateMatchRequest
import com.kikepb.squadfy.api.dto.GenerateTeamsRequest
import com.kikepb.squadfy.api.dto.MatchSignupDto
import com.kikepb.squadfy.api.dto.RecordMatchResultRequest
import com.kikepb.squadfy.api.mappers.toClubMatchDto
import com.kikepb.squadfy.api.mappers.toDomain
import com.kikepb.squadfy.api.mappers.toMatchSignupDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.domain.type.MatchSignupId
import com.kikepb.squadfy.service.ClubService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/club")
class ClubMatchController(
    private val clubService: ClubService
) {

    @GetMapping("/{clubId}/matches")
    fun getMatchesForClub(@PathVariable("clubId") clubId: ClubId): List<ClubMatchDto> {
        return clubService
            .getMatchesForClub(clubId = clubId, userId = requestUserId)
            .map { it.toClubMatchDto() }
    }

    @PostMapping("/{clubId}/matches")
    fun createMatch(
        @PathVariable("clubId") clubId: ClubId,
        @Valid @RequestBody body: CreateMatchRequest
    ): ClubMatchDto {
        return clubService.createMatch(
            clubId = clubId,
            userId = requestUserId,
            scheduledAt = body.scheduledAt,
            signupOpensAt = body.signupOpensAt,
            signupClosesAt = body.signupClosesAt
        ).toClubMatchDto()
    }

    @GetMapping("/matches/{matchId}")
    fun getMatch(@PathVariable("matchId") matchId: ClubMatchId): ClubMatchDto {
        return clubService
            .getMatch(matchId = matchId, userId = requestUserId)
            .toClubMatchDto()
    }

    @PostMapping("/matches/{matchId}/cancel")
    fun cancelMatch(@PathVariable("matchId") matchId: ClubMatchId): ClubMatchDto {
        return clubService
            .cancelMatch(matchId = matchId, userId = requestUserId)
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

    @PostMapping("/matches/{matchId}/result")
    fun recordMatchResult(
        @PathVariable("matchId") matchId: ClubMatchId,
        @Valid @RequestBody body: RecordMatchResultRequest
    ): ClubMatchDto {
        return clubService.recordMatchResult(
            matchId = matchId,
            userId = requestUserId,
            teamAScore = body.teamAScore,
            teamBScore = body.teamBScore,
            playerStats = body.playerStats.map {
                ClubService.PlayerMatchStatInput(
                    clubMemberId = it.clubMemberId,
                    goals = it.goals,
                    assists = it.assists,
                    yellowCards = it.yellowCards,
                    redCards = it.redCards,
                    minutesPlayed = it.minutesPlayed
                )
            }
        ).toClubMatchDto()
    }

    @GetMapping("/matches/{matchId}/signups")
    fun listSignups(@PathVariable("matchId") matchId: ClubMatchId): List<MatchSignupDto> {
        return clubService
            .listSignups(matchId = matchId, userId = requestUserId)
            .map { it.toMatchSignupDto() }
    }

    @PostMapping("/matches/{matchId}/signups")
    fun signUpForMatch(@PathVariable("matchId") matchId: ClubMatchId): MatchSignupDto {
        return clubService
            .signUpForMatch(matchId = matchId, userId = requestUserId)
            .toMatchSignupDto()
    }

    @DeleteMapping("/matches/{matchId}/signups/me")
    fun cancelSignup(@PathVariable("matchId") matchId: ClubMatchId) {
        clubService.cancelSignup(matchId = matchId, userId = requestUserId)
    }

    @PostMapping("/matches/{matchId}/guests")
    fun addGuest(
        @PathVariable("matchId") matchId: ClubMatchId,
        @Valid @RequestBody body: AddGuestRequest
    ): MatchSignupDto {
        return clubService.addGuest(
            matchId = matchId,
            userId = requestUserId,
            guestName = body.guestName,
            position = body.position,
            rating = body.rating
        ).toMatchSignupDto()
    }

    @DeleteMapping("/matches/{matchId}/signups/{signupId}")
    fun removeSignup(
        @PathVariable("matchId") matchId: ClubMatchId,
        @PathVariable("signupId") signupId: MatchSignupId
    ) {
        clubService.removeSignup(matchId = matchId, userId = requestUserId, signupId = signupId)
    }
}
