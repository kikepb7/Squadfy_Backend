package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.domain.type.MatchPlayerStatId
import com.kikepb.squadfy.infrastructure.database.entities.MatchPlayerStatEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MatchPlayerStatRepository : JpaRepository<MatchPlayerStatEntity, MatchPlayerStatId> {

    fun findAllByMatchId(matchId: ClubMatchId): List<MatchPlayerStatEntity>
    fun existsByMatchId(matchId: ClubMatchId): Boolean
}
