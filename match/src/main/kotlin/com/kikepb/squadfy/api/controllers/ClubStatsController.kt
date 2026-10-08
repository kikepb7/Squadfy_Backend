package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.ClubStatsEntryDto
import com.kikepb.squadfy.api.dto.PlayerStatsDto
import com.kikepb.squadfy.api.mappers.toClubStatsEntryDto
import com.kikepb.squadfy.api.mappers.toPlayerStatsDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.model.StatsSortBy
import com.kikepb.squadfy.domain.exception.InvalidStatsPeriodException
import com.kikepb.squadfy.domain.model.StatsPeriod
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.service.PlayerStatsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/clubs/{clubId}/stats")
@Tag(name = "Stats", description = "Player statistics from completed matches")
class ClubStatsController(
    private val playerStatsService: PlayerStatsService
) {

    @GetMapping
    @Operation(
        summary = "Statistics classification of every member, highest first (members only)",
        description = "from/to (YYYY-MM-DD, inclusive, club time zone) limit the completed matches counted, e.g. a season."
    )
    fun getClubStats(
        @PathVariable("clubId") clubId: ClubId,
        @RequestParam("sortBy", required = false, defaultValue = "GOALS") sortBy: StatsSortBy,
        @RequestParam("from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam("to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): List<ClubStatsEntryDto> =
        playerStatsService
            .getClubStats(clubId = clubId, userId = requestUserId, sortBy = sortBy, period = period(from, to))
            .map { it.toClubStatsEntryDto() }

    @GetMapping("/me")
    @Operation(summary = "My statistics in this club")
    fun getMyStats(
        @PathVariable("clubId") clubId: ClubId,
        @RequestParam("from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam("to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): PlayerStatsDto =
        playerStatsService
            .getMyStats(clubId = clubId, userId = requestUserId, period = period(from, to))
            .toPlayerStatsDto()

    private fun period(from: LocalDate?, to: LocalDate?): StatsPeriod =
        try {
            StatsPeriod(from = from, to = to)
        } catch (e: IllegalArgumentException) {
            throw InvalidStatsPeriodException(e.message.orEmpty())
        }
}
