package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.infrastructure.database.entities.ScheduleExceptionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface ScheduleExceptionRepository : JpaRepository<ScheduleExceptionEntity, UUID> {

    fun findAllByClubIdOrderByScheduleDateAsc(clubId: ClubId): List<ScheduleExceptionEntity>

    fun findByClubIdAndScheduleDate(clubId: ClubId, scheduleDate: LocalDate): ScheduleExceptionEntity?

    fun existsByClubIdAndScheduleDate(clubId: ClubId, scheduleDate: LocalDate): Boolean
}
