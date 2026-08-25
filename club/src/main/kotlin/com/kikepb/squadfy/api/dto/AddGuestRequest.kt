package com.kikepb.squadfy.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.kikepb.squadfy.domain.model.PlayerPositionModel
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AddGuestRequest(
    @field:NotBlank(message = "Guest name is required")
    @field:Size(max = 120, message = "Guest name can have at most 120 characters")
    @JsonProperty("guestName")
    val guestName: String,
    @JsonProperty("position")
    val position: PlayerPositionModel? = null,
    @field:Min(value = 1, message = "Rating must be between 1 and 99")
    @field:Max(value = 99, message = "Rating must be between 1 and 99")
    @JsonProperty("rating")
    val rating: Int? = null
)
