package com.kikepb.squadfy.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.kikepb.squadfy.domain.type.MatchSignupId
import jakarta.validation.constraints.NotNull

data class GenerateTeamsRequest(
    @field:NotNull(message = "Mode is required")
    @JsonProperty("mode")
    val mode: TeamGenerationModeDto,
    @JsonProperty("manualTeamA")
    val manualTeamA: List<MatchSignupId>? = null,
    @JsonProperty("manualTeamB")
    val manualTeamB: List<MatchSignupId>? = null
)
