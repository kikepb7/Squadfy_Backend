package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.infrastructure.database.entities.MatchTeamPlayerEntity
import com.kikepb.squadfy.domain.type.MatchTeamPlayerId
import org.springframework.data.jpa.repository.JpaRepository

interface MatchTeamPlayerRepository : JpaRepository<MatchTeamPlayerEntity, MatchTeamPlayerId> {

    fun findAllByMatchId(matchId: ClubMatchId): List<MatchTeamPlayerEntity>
    fun deleteByMatchId(matchId: ClubMatchId)
}
