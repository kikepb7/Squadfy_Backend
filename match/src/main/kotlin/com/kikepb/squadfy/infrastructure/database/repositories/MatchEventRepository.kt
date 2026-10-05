package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.MatchEventId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.infrastructure.database.entities.MatchEventEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MatchEventRepository : JpaRepository<MatchEventEntity, MatchEventId> {

    fun findAllByMatchId(matchId: MatchId): List<MatchEventEntity>

    fun findAllByMatchIdIn(matchIds: Collection<MatchId>): List<MatchEventEntity>
}
