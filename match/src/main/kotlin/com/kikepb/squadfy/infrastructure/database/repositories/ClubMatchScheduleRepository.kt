package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchScheduleId
import com.kikepb.squadfy.infrastructure.database.entities.ClubMatchScheduleEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ClubMatchScheduleRepository : JpaRepository<ClubMatchScheduleEntity, ClubMatchScheduleId> {

    fun findByClubId(clubId: ClubId): ClubMatchScheduleEntity?

    fun existsByClubId(clubId: ClubId): Boolean

    fun findAllByIsActive(isActive: Boolean): List<ClubMatchScheduleEntity>
}
