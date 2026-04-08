package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ClubParticipantEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ClubParticipantRepository : JpaRepository<ClubParticipantEntity, UserId> {
    fun findAllByUserIdIn(userIds: Collection<UserId>): Set<ClubParticipantEntity>
}
