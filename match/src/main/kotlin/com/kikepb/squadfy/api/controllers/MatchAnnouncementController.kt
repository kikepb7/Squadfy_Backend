package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.AddGuestRequest
import com.kikepb.squadfy.api.dto.MatchAnnouncementDto
import com.kikepb.squadfy.api.mappers.toMatchAnnouncementDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.service.MatchAnnouncementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

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

    @PostMapping("/guests")
    @Operation(
        summary = "Add a guest to the announcement (members only, at most 2 per member)",
        description = "Members have priority: a guest is CONFIRMED only while there are free places and goes back to the waitlist if a member takes the last one."
    )
    fun addGuest(
        @PathVariable("announcementId") announcementId: MatchAnnouncementId,
        @Valid @RequestBody body: AddGuestRequest
    ): MatchAnnouncementDto =
        matchAnnouncementService
            .addGuest(matchAnnouncementId = announcementId, userId = requestUserId, name = body.name.trim(), position = body.position)
            .toMatchAnnouncementDto()

    @DeleteMapping("/guests/{guestId}")
    @Operation(summary = "Remove a guest (the member who invited them or a manager)")
    fun removeGuest(
        @PathVariable("announcementId") announcementId: MatchAnnouncementId,
        @PathVariable("guestId") guestId: UUID
    ): MatchAnnouncementDto =
        matchAnnouncementService
            .removeGuest(matchAnnouncementId = announcementId, userId = requestUserId, guestId = guestId)
            .toMatchAnnouncementDto()
}
