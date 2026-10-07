package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.infrastructure.database.entities.MemberAbsenceEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface MemberAbsenceRepository : JpaRepository<MemberAbsenceEntity, UUID> {

    fun findAllByClubIdOrderByFromDateAsc(clubId: ClubId): List<MemberAbsenceEntity>

    /** Absences overlapping [from, to]. */
    fun findAllByClubIdAndFromDateLessThanEqualAndToDateGreaterThanEqualOrderByFromDateAsc(
        clubId: ClubId,
        to: LocalDate,
        from: LocalDate
    ): List<MemberAbsenceEntity>

    fun findAllByClubIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(clubId: ClubId, date: LocalDate, sameDate: LocalDate): List<MemberAbsenceEntity>
}
