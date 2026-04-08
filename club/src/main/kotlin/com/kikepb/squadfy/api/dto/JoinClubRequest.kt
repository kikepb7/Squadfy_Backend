package com.kikepb.squadfy.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class JoinClubRequest(
    @field:NotBlank(message = "Invitation code is required")
    @field:Pattern(
        regexp = "^[A-Za-z0-9]{6,12}$",
        message = "Invitation code must be alphanumeric and have between 6 and 12 characters"
    )
    @JsonProperty("invitationCode")
    val invitationCode: String,
    @field:Min(value = 1, message = "Shirt number must be greater than 0")
    @field:Max(value = 999, message = "Shirt number must be lower than 1000")
    @JsonProperty("shirtNumber")
    val shirtNumber: Int? = null,
    @field:Size(max = 120, message = "Position can have at most 120 characters")
    @JsonProperty("position")
    val position: String? = null
)
