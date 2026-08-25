package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.infrastructure.database.entities.ClubEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ClubRepository : JpaRepository<ClubEntity, ClubId> {
    fun findByInvitationCode(invitationCode: String): ClubEntity?
    fun existsByInvitationCode(invitationCode: String): Boolean
    fun findAllByMatchDayOfWeekIsNotNull(): List<ClubEntity>
}
