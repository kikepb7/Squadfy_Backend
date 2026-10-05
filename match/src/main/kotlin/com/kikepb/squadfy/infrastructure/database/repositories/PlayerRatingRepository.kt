package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.infrastructure.database.entities.PlayerRatingEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlayerRatingRepository : JpaRepository<PlayerRatingEntity, UUID> {

    fun findAllByClubIdAndClubMemberIdIn(clubId: ClubId, clubMemberIds: Collection<ClubMemberId>): List<PlayerRatingEntity>
}
