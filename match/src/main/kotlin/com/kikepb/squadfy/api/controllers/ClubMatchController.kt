package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.CreateMatchRequest
import com.kikepb.squadfy.api.dto.MatchDto
import com.kikepb.squadfy.api.mappers.toMatchDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.service.MatchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/clubs/{clubId}/matches")
@Tag(name = "Matches")
class ClubMatchController(
    private val matchService: MatchService
) {

    @GetMapping
    @Operation(summary = "Matches of the club, newest first, optionally filtered by status (members only)")
    fun getMatches(
        @PathVariable("clubId") clubId: ClubId,
        @RequestParam("status", required = false) status: MatchStatus?
    ): List<MatchDto> =
        matchService
            .getMatchesByClub(clubId = clubId, userId = requestUserId, status = status)
            .map { it.toMatchDto() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an extra match with its announcement (managers only)")
    fun createMatch(
        @PathVariable("clubId") clubId: ClubId,
        @Valid @RequestBody body: CreateMatchRequest
    ): MatchDto =
        matchService.createMatch(
            clubId = clubId,
            userId = requestUserId,
            scheduledAt = body.scheduledAt,
            format = body.format
        ).toMatchDto()
}
