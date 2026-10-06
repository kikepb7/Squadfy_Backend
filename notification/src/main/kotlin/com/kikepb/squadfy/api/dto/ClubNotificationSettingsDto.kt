package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.ClubId

data class ClubNotificationSettingsDto(
    val clubId: ClubId,
    val muted: Boolean
)

data class UpdateClubNotificationSettingsRequest(
    val muted: Boolean
)
