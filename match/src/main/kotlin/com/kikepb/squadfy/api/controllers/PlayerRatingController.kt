package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.PlayerRatingDto
import com.kikepb.squadfy.api.mappers.toPlayerRatingDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.service.PlayerRatingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/player-ratings")
class PlayerRatingController(
    private val playerRatingService: PlayerRatingService
) {

    @GetMapping("/club/{clubId}/me")
    fun getMyRating(@PathVariable("clubId") clubId: ClubId): PlayerRatingDto =
        playerRatingService
            .getMyRating(clubId = clubId, userId = requestUserId)
            .toPlayerRatingDto()
}
