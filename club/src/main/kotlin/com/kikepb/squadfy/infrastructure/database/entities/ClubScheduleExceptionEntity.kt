package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubScheduleExceptionId
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

@Entity
@Table(
    name = "club_schedule_exceptions",
    schema = "club_service",
    indexes = [
        Index(name = "idx_club_schedule_exceptions_club_id", columnList = "club_id"),
        Index(name = "idx_club_schedule_exceptions_club_date", columnList = "club_id,exception_date", unique = true)
    ]
)
class ClubScheduleExceptionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: ClubScheduleExceptionId? = null,
    @Column(name = "club_id", nullable = false, updatable = false)
    var clubId: ClubId,
    @Column(name = "exception_date", nullable = false, updatable = false)
    var date: LocalDate,
    @Column(nullable = true, length = 500)
    var reason: String? = null,
    @CreationTimestamp
    var createdAt: Instant = Instant.now()
)
