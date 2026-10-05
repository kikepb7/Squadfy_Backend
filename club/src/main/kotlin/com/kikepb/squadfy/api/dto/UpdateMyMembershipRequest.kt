package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.club.PlayerPosition
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class UpdateMyMembershipRequest(
    @field:Min(value = 1, message = "Shirt number must be greater than 0")
    @field:Max(value = 999, message = "Shirt number must be lower than 1000")
    val shirtNumber: Int? = null,
    val position: PlayerPosition? = null
)
