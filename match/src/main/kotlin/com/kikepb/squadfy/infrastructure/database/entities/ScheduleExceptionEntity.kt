package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.model.ScheduleExceptionModel.ExceptionType
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.MatchId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** One-off change of the weekly schedule for a given date (spec 008 RN-B). */
@Entity
@Table(
    name = "schedule_exceptions",
    schema = "match_service",
    uniqueConstraints = [UniqueConstraint(name = "idx_schedule_exceptions_club_date", columnNames = ["club_id", "schedule_date"])]
)
class ScheduleExceptionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "club_id", nullable = false, updatable = false)
    var clubId: ClubId,
    @Column(name = "schedule_date", nullable = false, updatable = false)
    var scheduleDate: LocalDate,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    var type: ExceptionType,
    @Column(name = "new_scheduled_at")
    var newScheduledAt: Instant? = null,
    @Column(length = 200)
    var reason: String? = null,
    /** Already planned match that this exception cancelled or moved (to undo it). */
    @Column(name = "affected_match_id")
    var affectedMatchId: MatchId? = null,
    @CreationTimestamp
    var createdAt: Instant = Instant.now()
)
