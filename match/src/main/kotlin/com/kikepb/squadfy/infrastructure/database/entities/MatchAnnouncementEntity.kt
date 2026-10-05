package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.model.MatchAnnouncementModel.MatchAnnouncementStatus
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel.MatchAnnouncementStatus.OPEN
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
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
    name = "match_announcements",
    schema = "match_service",
    indexes = [
        Index(name = "idx_match_announcements_club_id", columnList = "club_id"),
        Index(name = "idx_match_announcements_status", columnList = "status")
    ]
)
class MatchAnnouncementEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: MatchAnnouncementId? = null,
    @Column(name = "match_id", nullable = false, updatable = false, unique = true)
    var matchId: MatchId,
    @Column(name = "club_id", nullable = false, updatable = false)
    var clubId: ClubId,
    @Column(name = "max_players", nullable = false)
    var maxPlayers: Int,
    @Column(name = "opens_at", nullable = false)
    var opensAt: Instant,
    @Column(name = "closes_at", nullable = false)
    var closesAt: Instant,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MatchAnnouncementStatus = OPEN,
    @CreationTimestamp
    var createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now()
)
