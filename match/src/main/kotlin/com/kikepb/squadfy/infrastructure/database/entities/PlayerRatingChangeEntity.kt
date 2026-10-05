package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

/** Rating delta applied by a completed match, kept so the match can be reopened and reverted. */
@Entity
@Table(
    name = "player_rating_changes",
    schema = "match_service",
    indexes = [
        Index(name = "idx_player_rating_changes_match_member", columnList = "match_id,club_member_id", unique = true)
    ]
)
class PlayerRatingChangeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "match_id", nullable = false, updatable = false)
    var matchId: MatchId,
    @Column(name = "club_member_id", nullable = false, updatable = false)
    var clubMemberId: ClubMemberId,
    @Column(nullable = false, updatable = false)
    var delta: Double,
    @CreationTimestamp
    var createdAt: Instant = Instant.now()
)
