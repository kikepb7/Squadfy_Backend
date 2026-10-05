package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.model.MatchFormat
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchScheduleId
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
    name = "club_match_schedules",
    schema = "match_service",
    indexes = [
        Index(name = "idx_club_match_schedules_club_id", columnList = "club_id", unique = true)
    ]
)
class ClubMatchScheduleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: ClubMatchScheduleId? = null,
    @Column(name = "club_id", nullable = false, unique = true, updatable = false)
    var clubId: ClubId,
    @Enumerated(EnumType.STRING)
    @Column(name = "match_day_of_week", nullable = false)
    var matchDayOfWeek: DayOfWeek,
    @Column(name = "match_time", nullable = false)
    var matchTime: LocalTime,
    @Column(name = "time_zone", nullable = false, length = 64)
    var timeZone: String = DEFAULT_CLUB_TIME_ZONE,
    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 16)
    var format: MatchFormat = MatchFormat.ELEVEN_A_SIDE,
    /** Derived from [format]; kept as a column so announcements can copy it. */
    @Column(name = "max_players", nullable = false)
    var maxPlayers: Int = format.maxPlayers,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @CreationTimestamp
    var createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now()
)

const val DEFAULT_CLUB_TIME_ZONE = "Europe/Madrid"
