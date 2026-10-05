package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.ClubMemberId

data class GenerateTeamsRequest(
    val mode: TeamGenerationModeDto = TeamGenerationModeDto.AUTO,
    val manualTeamA: List<ClubMemberId>? = null,
    val manualTeamB: List<ClubMemberId>? = null
)

enum class TeamGenerationModeDto {
    AUTO,
    MANUAL
}
