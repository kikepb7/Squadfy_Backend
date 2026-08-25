package com.kikepb.squadfy.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.kikepb.squadfy.domain.type.ClubMemberId
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class PlayerStatRequest(
    @field:NotNull(message = "Club member id is required")
    @JsonProperty("clubMemberId")
    val clubMemberId: ClubMemberId,
    @field:Min(value = 0, message = "Goals cannot be negative")
    @JsonProperty("goals")
    val goals: Int = 0,
    @field:Min(value = 0, message = "Assists cannot be negative")
    @JsonProperty("assists")
    val assists: Int = 0,
    @field:Min(value = 0, message = "Yellow cards cannot be negative")
    @JsonProperty("yellowCards")
    val yellowCards: Int = 0,
    @field:Min(value = 0, message = "Red cards cannot be negative")
    @JsonProperty("redCards")
    val redCards: Int = 0,
    @field:Min(value = 0, message = "Minutes played cannot be negative")
    @JsonProperty("minutesPlayed")
    val minutesPlayed: Int = 0
)
