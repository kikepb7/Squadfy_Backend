package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchSignupId
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
    name = "match_signups",
    schema = "club_service",
    indexes = [
        Index(name = "idx_match_signups_match_id", columnList = "match_id"),
        Index(name = "idx_match_signups_match_member", columnList = "match_id,club_member_id", unique = true)
    ]
)
class MatchSignupEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: MatchSignupId? = null,
    @Column(name = "match_id", nullable = false, updatable = false)
    var matchId: ClubMatchId,
    @Column(name = "club_member_id", nullable = true, updatable = false)
    var clubMemberId: ClubMemberId? = null,
    @Column(name = "guest_name", nullable = true, length = 120)
    var guestName: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "guest_position", nullable = true)
    var guestPosition: ClubMemberEntity.PlayerPositionEntity? = null,
    @Column(name = "guest_rating", nullable = true)
    var guestRating: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SignupStatusEntity = SignupStatusEntity.CONFIRMED,
    @CreationTimestamp
    var signedUpAt: Instant = Instant.now()
) {
    enum class SignupStatusEntity {
        CONFIRMED,
        CANCELLED
    }
}
