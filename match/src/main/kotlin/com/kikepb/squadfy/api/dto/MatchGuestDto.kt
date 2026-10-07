package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.club.PlayerPosition
import com.kikepb.squadfy.domain.type.ClubMemberId
import java.util.UUID

data class MatchGuestDto(
    val guestId: UUID,
    val name: String,
    val position: PlayerPosition?,
    val invitedByMemberId: ClubMemberId?
)

data class AddGuestRequest(
    @field:jakarta.validation.constraints.NotBlank(message = "name is required")
    @field:jakarta.validation.constraints.Size(max = 80, message = "name can have at most 80 characters")
    val name: String,
    val position: PlayerPosition? = null
)
