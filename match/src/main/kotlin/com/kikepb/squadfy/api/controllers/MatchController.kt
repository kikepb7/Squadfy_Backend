package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.AddMatchEventRequest
import com.kikepb.squadfy.api.dto.CreateMatchRequest
import com.kikepb.squadfy.api.dto.GenerateTeamsRequest
import com.kikepb.squadfy.api.dto.MatchDto
import com.kikepb.squadfy.api.dto.TeamGenerationModeDto
import com.kikepb.squadfy.api.mappers.toMatchDto
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.MatchEventId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.service.MatchEventService
import com.kikepb.squadfy.service.MatchService
import com.kikepb.squadfy.service.MatchTeamService
import com.kikepb.squadfy.service.MatchTeamService.TeamGenerationMode
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/matches")
class MatchController(
    private val matchService: MatchService,
    private val matchTeamService: MatchTeamService,
    private val matchEventService: MatchEventService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createMatch(@Valid @RequestBody body: CreateMatchRequest): MatchDto =
        matchService
            .createMatch(clubId = body.clubId, scheduledAt = body.scheduledAt)
            .toMatchDto()

    @GetMapping("/{matchId}")
    fun getMatchById(@PathVariable("matchId") matchId: MatchId): MatchDto =
        matchService
            .getMatchById(matchId = matchId)
            .toMatchDto()

    @GetMapping("/club/{clubId}")
    fun getMatchesByClub(@PathVariable("clubId") clubId: ClubId): List<MatchDto> =
        matchService
            .getMatchesByClub(clubId = clubId)
            .map { it.toMatchDto() }

    @GetMapping("/club/{clubId}/scheduled")
    fun getScheduledMatchesByClub(@PathVariable("clubId") clubId: ClubId): List<MatchDto> =
        matchService
            .getScheduledMatchesByClub(clubId = clubId)
            .map { it.toMatchDto() }

    @DeleteMapping("/{matchId}/cancel")
    fun cancelMatch(@PathVariable("matchId") matchId: MatchId): MatchDto =
        matchService
            .cancelMatch(matchId = matchId)
            .toMatchDto()

    @PostMapping("/{matchId}/generate-teams")
    fun generateTeams(
        @PathVariable("matchId") matchId: MatchId,
        @Valid @RequestBody body: GenerateTeamsRequest
    ): MatchDto =
        matchTeamService.generateTeams(
            matchId = matchId,
            mode = body.mode.toDomain(),
            manualTeamA = body.manualTeamA,
            manualTeamB = body.manualTeamB
        ).toMatchDto()

    @PostMapping("/{matchId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    fun addEvent(
        @PathVariable("matchId") matchId: MatchId,
        @Valid @RequestBody body: AddMatchEventRequest
    ): MatchDto =
        matchEventService.addEvent(
            matchId = matchId,
            clubMemberId = body.clubMemberId,
            type = body.type,
            minute = body.minute
        ).toMatchDto()

    @DeleteMapping("/{matchId}/events/{eventId}")
    fun removeEvent(
        @PathVariable("matchId") matchId: MatchId,
        @PathVariable("eventId") eventId: MatchEventId
    ): MatchDto =
        matchEventService
            .removeEvent(matchId = matchId, eventId = eventId)
            .toMatchDto()

    private fun TeamGenerationModeDto.toDomain(): TeamGenerationMode = when (this) {
        TeamGenerationModeDto.AUTO   -> TeamGenerationMode.AUTO
        TeamGenerationModeDto.MANUAL -> TeamGenerationMode.MANUAL
    }
}
