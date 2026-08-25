package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.UserId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

@Entity
@Table(
    name = "clubs",
    schema = "club_service",
    indexes = [
        Index(name = "idx_clubs_owner_id", columnList = "owner_id"),
        Index(name = "idx_clubs_invitation_code", columnList = "invitation_code", unique = true)
    ]
)
class ClubEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: ClubId? = null,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = true, length = 2000)
    var description: String? = null,
    @Column(nullable = true)
    var clubLogoUrl: String? = null,
    @Column(name = "owner_id", nullable = false, updatable = false)
    var ownerId: UserId,
    @Column(nullable = false, unique = true)
    var invitationCode: String,
    @Column(nullable = true)
    var maxMembers: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "match_day_of_week", nullable = true)
    var matchDayOfWeek: DayOfWeek? = null,
    @Column(name = "match_start_time", nullable = true)
    var matchStartTime: LocalTime? = null,
    @Column(name = "match_end_time", nullable = true)
    var matchEndTime: LocalTime? = null,
    @Column(name = "season_start_month", nullable = false)
    var seasonStartMonth: Int = 9,
    @Column(name = "season_start_day", nullable = false)
    var seasonStartDay: Int = 1,
    @Column(name = "draw_time", nullable = false)
    var drawTime: LocalTime = LocalTime.of(18, 0),
    @CreationTimestamp
    var createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now()
)
