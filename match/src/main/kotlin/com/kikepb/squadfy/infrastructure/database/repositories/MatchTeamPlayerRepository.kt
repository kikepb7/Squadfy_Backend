package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.domain.type.MatchTeamPlayerId
import com.kikepb.squadfy.infrastructure.database.entities.MatchTeamPlayerEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface MatchTeamPlayerRepository : JpaRepository<MatchTeamPlayerEntity, MatchTeamPlayerId> {

    fun findAllByMatchId(matchId: MatchId): List<MatchTeamPlayerEntity>

    fun findAllByMatchIdIn(matchIds: Collection<MatchId>): List<MatchTeamPlayerEntity>

    /**
     * Bulk delete executed immediately. A derived deleteBy would be flushed after the new
     * inserts and violate the (match_id, club_member_id) unique index when teams are regenerated.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM MatchTeamPlayerEntity p WHERE p.matchId = :matchId")
    fun deleteAllByMatchIdInBulk(matchId: MatchId)
}
