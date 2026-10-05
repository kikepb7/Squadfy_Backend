package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.model.MatchEventType
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchEventId
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
import java.time.Instant

@Entity
@Table(
    name = "match_events",
    schema = "match_service",
    indexes = [
        Index(name = "idx_match_events_match_id", columnList = "match_id"),
        Index(name = "idx_match_events_member_id", columnList = "club_member_id"),
        Index(name = "idx_match_events_type", columnList = "type")
    ]
)
class MatchEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: MatchEventId? = null,
    @Column(name = "match_id", nullable = false, updatable = false)
    var matchId: MatchId,
    @Column(name = "club_member_id", nullable = false, updatable = false)
    var clubMemberId: ClubMemberId,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    var type: MatchEventType,
    @Column(nullable = true, updatable = false)
    var minute: Int? = null,
    @CreationTimestamp
    var createdAt: Instant = Instant.now()
)
