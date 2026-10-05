package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.infrastructure.database.entities.PlayerRatingChangeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface PlayerRatingChangeRepository : JpaRepository<PlayerRatingChangeEntity, UUID> {

    fun findAllByMatchId(matchId: MatchId): List<PlayerRatingChangeEntity>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM PlayerRatingChangeEntity c WHERE c.matchId = :matchId")
    fun deleteAllByMatchIdInBulk(matchId: MatchId)
}
