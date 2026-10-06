package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.AddMatchEventRequest
import com.kikepb.squadfy.api.dto.GenerateTeamsRequest
import com.kikepb.squadfy.api.dto.MatchAnnouncementDto
import com.kikepb.squadfy.api.dto.MatchDto
import com.kikepb.squadfy.api.dto.TeamBalanceDto
import com.kikepb.squadfy.api.dto.TeamGenerationModeDto
import com.kikepb.squadfy.api.mappers.toMatchAnnouncementDto
import com.kikepb.squadfy.api.mappers.toMatchDto
import com.kikepb.squadfy.api.mappers.toTeamBalanceDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.MatchEventId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.service.MatchAnnouncementService
import com.kikepb.squadfy.service.MatchEventService
import com.kikepb.squadfy.service.MatchService
import com.kikepb.squadfy.service.MatchTeamService
import com.kikepb.squadfy.service.MatchTeamService.TeamGenerationMode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
@RequestMapping("/api/v1/matches/{matchId}")
@Tag(name = "Matches")
class MatchController(
    private val matchService: MatchService,
    private val matchTeamService: MatchTeamService,
    private val matchEventService: MatchEventService,
    private val matchAnnouncementService: MatchAnnouncementService
) {

    @GetMapping
    @Operation(summary = "Match with teams, score and events (members only)")
    fun getMatch(@PathVariable("matchId") matchId: MatchId): MatchDto =
        matchService
            .getMatchById(matchId = matchId, userId = requestUserId)
            .toMatchDto()

    @GetMapping("/announcement")
    @Operation(summary = "Announcement of the match (members only)")
    fun getAnnouncement(@PathVariable("matchId") matchId: MatchId): MatchAnnouncementDto =
        matchAnnouncementService
            .getMatchAnnouncementByMatch(matchId = matchId, userId = requestUserId)
            .toMatchAnnouncementDto()

    @PostMapping("/cancel")
    @Operation(summary = "Cancel the match and its announcement (managers only)")
    fun cancelMatch(@PathVariable("matchId") matchId: MatchId): MatchDto =
        matchService
            .cancelMatch(matchId = matchId, userId = requestUserId)
            .toMatchDto()

    @PostMapping("/complete")
    @Operation(summary = "Close the match with the score of its goal events and update ratings (managers only)")
    fun completeMatch(@PathVariable("matchId") matchId: MatchId): MatchDto =
        matchService
            .completeMatch(matchId = matchId, userId = requestUserId)
            .toMatchDto()

    @PostMapping("/reopen")
    @Operation(summary = "Reopen the club's latest completed match and revert its rating changes (managers only)")
    fun reopenMatch(@PathVariable("matchId") matchId: MatchId): MatchDto =
        matchService
            .reopenMatch(matchId = matchId, userId = requestUserId)
            .toMatchDto()

    @PostMapping("/teams")
    @Operation(summary = "Draw balanced teams (AUTO) or set them (MANUAL); replaces current teams (managers only)")
    fun generateTeams(
        @PathVariable("matchId") matchId: MatchId,
        @Valid @RequestBody body: GenerateTeamsRequest
    ): MatchDto =
        matchTeamService.generateTeams(
            matchId = matchId,
            userId = requestUserId,
            mode = body.mode.toDomain(),
            manualTeamA = body.manualTeamA,
            manualTeamB = body.manualTeamB
        ).toMatchDto()

    @GetMapping("/team-balance")
    @Operation(summary = "How even the teams are, with each player's rating (managers only)")
    fun getTeamBalance(@PathVariable("matchId") matchId: MatchId): TeamBalanceDto =
        matchTeamService
            .getTeamBalance(matchId = matchId, userId = requestUserId)
            .toTeamBalanceDto()

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a goal, assist or card of a player of the match (managers only)")
    fun addEvent(
        @PathVariable("matchId") matchId: MatchId,
        @Valid @RequestBody body: AddMatchEventRequest
    ): MatchDto =
        matchEventService.addEvent(
            matchId = matchId,
            userId = requestUserId,
            clubMemberId = body.clubMemberId,
            type = body.type,
            minute = body.minute
        ).toMatchDto()

    @DeleteMapping("/events/{eventId}")
    @Operation(summary = "Remove an event of the match (managers only)")
    fun removeEvent(
        @PathVariable("matchId") matchId: MatchId,
        @PathVariable("eventId") eventId: MatchEventId
    ): MatchDto =
        matchEventService
            .removeEvent(matchId = matchId, userId = requestUserId, eventId = eventId)
            .toMatchDto()

    private fun TeamGenerationModeDto.toDomain(): TeamGenerationMode = when (this) {
        TeamGenerationModeDto.AUTO   -> TeamGenerationMode.AUTO
        TeamGenerationModeDto.MANUAL -> TeamGenerationMode.MANUAL
    }
}
