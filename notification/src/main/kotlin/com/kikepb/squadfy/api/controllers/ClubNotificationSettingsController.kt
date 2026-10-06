package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.ClubNotificationSettingsDto
import com.kikepb.squadfy.api.dto.UpdateClubNotificationSettingsRequest
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.service.ClubNotificationSettingsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/clubs/{clubId}/notification-settings")
@Tag(name = "Notifications", description = "Push notification preferences per club")
class ClubNotificationSettingsController(
    private val clubNotificationSettingsService: ClubNotificationSettingsService
) {

    @GetMapping
    @Operation(summary = "My notification settings for this club (members only)")
    fun getSettings(@PathVariable("clubId") clubId: ClubId): ClubNotificationSettingsDto =
        ClubNotificationSettingsDto(
            clubId = clubId,
            muted = clubNotificationSettingsService.isMuted(clubId = clubId, userId = requestUserId)
        )

    @PutMapping
    @Operation(summary = "Mute or unmute this club's push notifications (waitlist promotions are always sent)")
    fun updateSettings(
        @PathVariable("clubId") clubId: ClubId,
        @RequestBody body: UpdateClubNotificationSettingsRequest
    ): ClubNotificationSettingsDto =
        ClubNotificationSettingsDto(
            clubId = clubId,
            muted = clubNotificationSettingsService.setMuted(clubId = clubId, userId = requestUserId, muted = body.muted)
        )
}
