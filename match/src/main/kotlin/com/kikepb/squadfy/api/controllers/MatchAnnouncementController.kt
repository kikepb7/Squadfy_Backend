package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.MatchAnnouncementDto
import com.kikepb.squadfy.api.mappers.toMatchAnnouncementDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.service.MatchAnnouncementService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/matchAnnouncements")
class MatchAnnouncementController(
    private val matchAnnouncementService: MatchAnnouncementService
) {

    @GetMapping("/{matchAnnouncementId}")
    fun getMatchAnnouncementById(@PathVariable("matchAnnouncementId") matchAnnouncementId: MatchAnnouncementId): MatchAnnouncementDto =
        matchAnnouncementService
            .getMatchAnnouncementById(matchAnnouncementId = matchAnnouncementId)
            .toMatchAnnouncementDto()

    @GetMapping("/match/{matchId}")
    fun getMatchAnnouncementByMatch(@PathVariable("matchId") matchId: MatchId): MatchAnnouncementDto =
        matchAnnouncementService
            .getMatchAnnouncementByMatch(matchId = matchId)
            .toMatchAnnouncementDto()

    @GetMapping("/club/{clubId}")
    fun getMatchAnnouncementsByClub(@PathVariable("clubId") clubId: ClubId): List<MatchAnnouncementDto> =
        matchAnnouncementService
            .getMatchAnnouncementsByClub(clubId = clubId)
            .map { it.toMatchAnnouncementDto() }

    @PostMapping("/{matchAnnouncementId}/enroll")
    fun enroll(@PathVariable("matchAnnouncementId") matchAnnouncementId: MatchAnnouncementId): MatchAnnouncementDto =
        matchAnnouncementService
            .enroll(matchAnnouncementId = matchAnnouncementId, userId = requestUserId)
            .toMatchAnnouncementDto()

    @DeleteMapping("/{matchAnnouncementId}/withdraw")
    fun withdraw(@PathVariable("matchAnnouncementId") matchAnnouncementId: MatchAnnouncementId): MatchAnnouncementDto =
        matchAnnouncementService
            .withdraw(matchAnnouncementId = matchAnnouncementId, userId = requestUserId)
            .toMatchAnnouncementDto()
}
