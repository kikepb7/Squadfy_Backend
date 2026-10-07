package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.domain.type.MatchTeamPlayerId
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
import java.util.UUID

@Entity
@Table(
    name = "match_team_players",
    schema = "match_service",
    indexes = [
        Index(name = "idx_mtp_match_id", columnList = "match_id"),
        Index(name = "idx_mtp_club_member_id", columnList = "club_member_id"),
        Index(name = "idx_mtp_match_side", columnList = "match_id,team_side"),
        Index(
            name = "idx_mtp_unique_member_per_match",
            columnList = "match_id,club_member_id",
            unique = true
        )
    ]
)
class MatchTeamPlayerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: MatchTeamPlayerId? = null,
    @Column(name = "match_id", nullable = false, updatable = false)
    var matchId: MatchId,
    /** Null for guests (then [guestEntryId] is set). */
    @Column(name = "club_member_id", updatable = false)
    var clubMemberId: ClubMemberId? = null,
    /** Announcement entry id of a guest player (spec 008 RN-A6). */
    @Column(name = "guest_entry_id", updatable = false)
    var guestEntryId: UUID? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "team_side", nullable = false)
    var teamSide: TeamSideEntity,
    /** Minutes set by a manager; null means the whole match (spec 004 RN-6). */
    @Column(name = "minutes_played")
    var minutesPlayed: Int? = null,
    @CreationTimestamp
    var createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now()
) {
    enum class TeamSideEntity {
        TEAM_A,
        TEAM_B
    }
}
