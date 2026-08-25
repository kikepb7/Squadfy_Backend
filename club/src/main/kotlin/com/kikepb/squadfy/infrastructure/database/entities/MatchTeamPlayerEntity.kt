package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.domain.type.MatchSignupId
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

@Entity
@Table(
    name = "match_team_players",
    schema = "club_service",
    indexes = [
        Index(name = "idx_match_team_players_match_id", columnList = "match_id"),
        Index(name = "idx_match_team_players_signup_id", columnList = "match_signup_id"),
        Index(name = "idx_match_team_players_match_side", columnList = "match_id,team_side"),
        Index(name = "idx_match_team_players_unique_signup_per_match", columnList = "match_id,match_signup_id", unique = true)
    ]
)
class MatchTeamPlayerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: MatchTeamPlayerId? = null,
    @Column(name = "match_id", nullable = false, updatable = false)
    var matchId: ClubMatchId,
    @Column(name = "match_signup_id", nullable = false, updatable = false)
    var matchSignupId: MatchSignupId,
    @Enumerated(EnumType.STRING)
    @Column(name = "team_side", nullable = false)
    var teamSide: TeamSideEntity,
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
