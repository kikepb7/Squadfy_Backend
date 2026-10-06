package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.PlayerRatingDto
import com.kikepb.squadfy.api.dto.RatingLeaderboardEntryDto
import com.kikepb.squadfy.api.mappers.toPlayerRatingDto
import com.kikepb.squadfy.api.mappers.toRatingLeaderboardEntryDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.service.PlayerRatingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/clubs/{clubId}/ratings")
@Tag(name = "Ratings", description = "Elo-style player ratings, public inside the club")
class PlayerRatingController(
    private val playerRatingService: PlayerRatingService
) {

    @GetMapping
    @Operation(summary = "Club classification by rating, highest first (members only)")
    fun getLeaderboard(@PathVariable("clubId") clubId: ClubId): List<RatingLeaderboardEntryDto> =
        playerRatingService
            .getLeaderboard(clubId = clubId, userId = requestUserId)
            .map { it.toRatingLeaderboardEntryDto() }

    @GetMapping("/me")
    @Operation(summary = "My rating and position in the club classification")
    fun getMyRating(@PathVariable("clubId") clubId: ClubId): PlayerRatingDto =
        playerRatingService
            .getMyRating(clubId = clubId, userId = requestUserId)
            .toPlayerRatingDto()
}
