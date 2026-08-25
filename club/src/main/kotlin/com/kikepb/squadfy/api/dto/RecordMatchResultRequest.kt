package com.kikepb.squadfy.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.Min

data class RecordMatchResultRequest(
    @field:Min(value = 0, message = "Score cannot be negative")
    @JsonProperty("teamAScore")
    val teamAScore: Int,
    @field:Min(value = 0, message = "Score cannot be negative")
    @JsonProperty("teamBScore")
    val teamBScore: Int,
    @field:Valid
    @JsonProperty("playerStats")
    val playerStats: List<PlayerStatRequest> = emptyList()
)
