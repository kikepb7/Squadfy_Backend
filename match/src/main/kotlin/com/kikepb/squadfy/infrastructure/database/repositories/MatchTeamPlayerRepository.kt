package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.domain.type.MatchTeamPlayerId
import com.kikepb.squadfy.infrastructure.database.entities.MatchTeamPlayerEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MatchTeamPlayerRepository : JpaRepository<MatchTeamPlayerEntity, MatchTeamPlayerId> {

    fun findAllByMatchId(matchId: MatchId): List<MatchTeamPlayerEntity>

    fun deleteByMatchId(matchId: MatchId)

    fun existsByMatchIdAndClubMemberId(matchId: MatchId, clubMemberId: ClubMemberId): Boolean
}
