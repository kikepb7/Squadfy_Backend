package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.CurrentMatchAnnouncementDto
import com.kikepb.squadfy.api.dto.MatchAnnouncementDto
import com.kikepb.squadfy.api.mappers.toCurrentMatchAnnouncementDto
import com.kikepb.squadfy.api.mappers.toMatchAnnouncementDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.service.MatchAnnouncementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/clubs/{clubId}/announcements")
@Tag(name = "Announcements", description = "Match call-ups: enrollment and waitlist")
class ClubAnnouncementController(
    private val matchAnnouncementService: MatchAnnouncementService
) {

    @GetMapping
    @Operation(summary = "Announcements of the club, newest first (members only)")
    fun getAnnouncements(@PathVariable("clubId") clubId: ClubId): List<MatchAnnouncementDto> =
        matchAnnouncementService
            .getMatchAnnouncementsByClub(clubId = clubId, userId = requestUserId)
            .map { it.toMatchAnnouncementDto() }

    @GetMapping("/current")
    @Operation(
        summary = "Announcement of the next scheduled match with my enrollment status",
        description = "404 when the club has no upcoming scheduled match."
    )
    fun getCurrentAnnouncement(@PathVariable("clubId") clubId: ClubId): CurrentMatchAnnouncementDto =
        matchAnnouncementService
            .getCurrentForClub(clubId = clubId, userId = requestUserId)
            .toCurrentMatchAnnouncementDto()
}
