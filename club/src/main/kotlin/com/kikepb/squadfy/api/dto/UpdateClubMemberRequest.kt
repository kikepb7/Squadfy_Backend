package com.kikepb.squadfy.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.kikepb.squadfy.domain.model.PlayerPositionModel
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class UpdateClubMemberRequest(
    @field:Min(value = 1, message = "Shirt number must be greater than 0")
    @field:Max(value = 999, message = "Shirt number must be lower than 1000")
    @JsonProperty("shirtNumber")
    val shirtNumber: Int? = null,
    @JsonProperty("position")
    val position: PlayerPositionModel? = null,
    @field:Min(value = 1, message = "Rating must be between 1 and 99")
    @field:Max(value = 99, message = "Rating must be between 1 and 99")
    @JsonProperty("rating")
    val rating: Int? = null
)
