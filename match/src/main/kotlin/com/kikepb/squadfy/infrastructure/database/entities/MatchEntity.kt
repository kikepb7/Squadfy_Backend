package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.SCHEDULED
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.MatchId
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
import java.time.Instant

@Entity
@Table(
    name = "matches",
    schema = "match_service",
    indexes = [
        Index(name = "idx_matches_club_id", columnList = "club_id"),
        Index(name = "idx_matches_scheduled_at", columnList = "scheduled_at"),
        Index(name = "idx_matches_status", columnList = "status")
    ]
)
class MatchEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: MatchId? = null,
    @Column(name = "club_id", nullable = false, updatable = false)
    var clubId: ClubId,
    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: Instant,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MatchStatus = SCHEDULED,
    @CreationTimestamp
    var createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now()
)
