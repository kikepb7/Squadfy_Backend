package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchPlayerStatId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@Entity
@Table(
    name = "match_player_stats",
    schema = "club_service",
    indexes = [
        Index(name = "idx_match_player_stats_match_id", columnList = "match_id"),
        Index(name = "idx_match_player_stats_member_id", columnList = "club_member_id"),
        Index(name = "idx_match_player_stats_match_member", columnList = "match_id,club_member_id", unique = true)
    ]
)
class MatchPlayerStatEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: MatchPlayerStatId? = null,
    @Column(name = "match_id", nullable = false, updatable = false)
    var matchId: ClubMatchId,
    @Column(name = "club_member_id", nullable = false, updatable = false)
    var clubMemberId: ClubMemberId,
    @Column(nullable = false)
    var goals: Int = 0,
    @Column(nullable = false)
    var assists: Int = 0,
    @Column(name = "yellow_cards", nullable = false)
    var yellowCards: Int = 0,
    @Column(name = "red_cards", nullable = false)
    var redCards: Int = 0,
    @Column(name = "minutes_played", nullable = false)
    var minutesPlayed: Int = 0,
    @CreationTimestamp
    var createdAt: Instant = Instant.now()
)
