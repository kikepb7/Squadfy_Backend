package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Period in which a member will not play (spec 008 RN-C). Dates are local to the club. */
@Entity
@Table(
    name = "member_absences",
    schema = "match_service",
    indexes = [Index(name = "idx_member_absences_club_member", columnList = "club_id,club_member_id")]
)
class MemberAbsenceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "club_id", nullable = false, updatable = false)
    var clubId: ClubId,
    @Column(name = "club_member_id", nullable = false, updatable = false)
    var clubMemberId: ClubMemberId,
    @Column(name = "from_date", nullable = false)
    var fromDate: LocalDate,
    @Column(name = "to_date", nullable = false)
    var toDate: LocalDate,
    @Column(length = 200)
    var reason: String? = null,
    @CreationTimestamp
    var createdAt: Instant = Instant.now()
)
