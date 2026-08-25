package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubScheduleExceptionId
import com.kikepb.squadfy.infrastructure.database.entities.ClubScheduleExceptionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ClubScheduleExceptionRepository : JpaRepository<ClubScheduleExceptionEntity, ClubScheduleExceptionId> {

    fun findAllByClubIdOrderByDateAsc(clubId: ClubId): List<ClubScheduleExceptionEntity>
    fun existsByClubIdAndDate(clubId: ClubId, date: LocalDate): Boolean
    fun findByIdAndClubId(id: ClubScheduleExceptionId, clubId: ClubId): ClubScheduleExceptionEntity?
}
