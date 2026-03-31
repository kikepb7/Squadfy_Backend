package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.domain.type.UserId
import jakarta.persistence.Column
import jakarta.persistence.Entity
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
    name = "club_matches",
    schema = "club_service",
    indexes = [
        Index(name = "idx_club_matches_club_id", columnList = "club_id"),
        Index(name = "idx_club_matches_scheduled_at", columnList = "scheduled_at")
    ]
)
class ClubMatchEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: ClubMatchId? = null,
    @Column(name = "club_id", nullable = false, updatable = false)
    var clubId: ClubId,
    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    var createdByUserId: UserId,
    @Column(nullable = false)
    var scheduledAt: Instant,
    @Column(nullable = true)
    var teamAScore: Int? = null,
    @Column(nullable = true)
    var teamBScore: Int? = null,
    @CreationTimestamp
    var createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now()
)
