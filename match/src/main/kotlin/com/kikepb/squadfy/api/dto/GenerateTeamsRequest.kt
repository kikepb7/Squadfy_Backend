package com.kikepb.squadfy.api.dto

import java.util.UUID

data class GenerateTeamsRequest(
    val mode: TeamGenerationModeDto = TeamGenerationModeDto.AUTO,
    /** Member ids or guest ids of the confirmed participants (MANUAL mode). */
    val manualTeamA: List<UUID>? = null,
    val manualTeamB: List<UUID>? = null
)

enum class TeamGenerationModeDto {
    AUTO,
    MANUAL
}
