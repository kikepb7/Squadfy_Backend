package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.MatchAnnouncementDto
import com.kikepb.squadfy.api.mappers.toMatchAnnouncementDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.service.MatchAnnouncementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/announcements/{announcementId}")
@Tag(name = "Announcements", description = "Match call-ups: enrollment and waitlist")
class MatchAnnouncementController(
    private val matchAnnouncementService: MatchAnnouncementService
) {

    @GetMapping
    @Operation(summary = "Announcement with confirmed players and waitlist (members only)")
    fun getAnnouncement(@PathVariable("announcementId") announcementId: MatchAnnouncementId): MatchAnnouncementDto =
        matchAnnouncementService
            .getMatchAnnouncementById(matchAnnouncementId = announcementId, userId = requestUserId)
            .toMatchAnnouncementDto()

    @PostMapping("/enrollment")
    @Operation(
        summary = "Enroll in the announcement",
        description = "CONFIRMED while there are free places, WAITLISTED otherwise. Only between opensAt and closesAt (22:00 the day before)."
    )
    fun enroll(@PathVariable("announcementId") announcementId: MatchAnnouncementId): MatchAnnouncementDto =
        matchAnnouncementService
            .enroll(matchAnnouncementId = announcementId, userId = requestUserId)
            .toMatchAnnouncementDto()

    @DeleteMapping("/enrollment")
    @Operation(
        summary = "Withdraw from the announcement",
        description = "If a confirmed player withdraws, the first waitlisted player takes the place. Not allowed after closesAt."
    )
    fun withdraw(@PathVariable("announcementId") announcementId: MatchAnnouncementId): MatchAnnouncementDto =
        matchAnnouncementService
            .withdraw(matchAnnouncementId = announcementId, userId = requestUserId)
            .toMatchAnnouncementDto()
}
