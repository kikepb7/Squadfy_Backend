package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchSignupId
import com.kikepb.squadfy.infrastructure.database.entities.MatchSignupEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchSignupEntity.SignupStatusEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MatchSignupRepository : JpaRepository<MatchSignupEntity, MatchSignupId> {

    fun findAllByMatchId(matchId: ClubMatchId): List<MatchSignupEntity>
    fun findAllByMatchIdAndStatus(matchId: ClubMatchId, status: SignupStatusEntity): List<MatchSignupEntity>
    fun findByMatchIdAndClubMemberId(matchId: ClubMatchId, clubMemberId: ClubMemberId): MatchSignupEntity?
    fun findAllByIdIn(ids: Collection<MatchSignupId>): List<MatchSignupEntity>
}
