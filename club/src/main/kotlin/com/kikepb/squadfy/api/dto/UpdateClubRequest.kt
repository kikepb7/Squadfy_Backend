package com.kikepb.squadfy.api.dto

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class UpdateClubRequest(
    @field:Size(min = 1, max = 120, message = "Club name must have between 1 and 120 characters")
    val name: String? = null,
    @field:Size(max = 2000, message = "Club description can have at most 2000 characters")
    val description: String? = null,
    @field:Positive(message = "Max members must be a positive number")
    val maxMembers: Int? = null
)
