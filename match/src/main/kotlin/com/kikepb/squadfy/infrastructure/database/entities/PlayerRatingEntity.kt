package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.model.PlayerRatingCalculator
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "player_ratings",
    schema = "match_service",
    indexes = [
        Index(name = "idx_player_ratings_club_member", columnList = "club_id,club_member_id", unique = true)
    ]
)
class PlayerRatingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "club_id", nullable = false, updatable = false)
    var clubId: ClubId,
    @Column(name = "club_member_id", nullable = false, updatable = false)
    var clubMemberId: ClubMemberId,
    @Column(nullable = false)
    var rating: Double = PlayerRatingCalculator.INITIAL_RATING,
    @Column(name = "matches_rated", nullable = false)
    var matchesRated: Int = 0,
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now()
)
