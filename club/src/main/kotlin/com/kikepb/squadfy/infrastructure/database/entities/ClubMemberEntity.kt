package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.PLAYER
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(
    name = "club_members",
    schema = "club_service",
    indexes = [
        Index(name = "idx_club_members_club_id", columnList = "club_id"),
        Index(name = "idx_club_members_user_id", columnList = "user_id"),
        Index(name = "idx_club_members_club_user", columnList = "club_id,user_id", unique = true)
    ]
)
class ClubMemberEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: ClubMemberId? = null,
    @Column(name = "club_id", nullable = false, updatable = false)
    var clubId: ClubId,
    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UserId,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    var userParticipant: ClubParticipantEntity? = null,
    @Column(name = "shirt_number")
    var shirtNumber: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var position: PlayerPositionEntity? = null,
    @Column(nullable = false)
    var rating: Int = 50,
    @Column(nullable = false)
    var goals: Int = 0,
    @Column(nullable = false)
    var assists: Int = 0,
    @Column(nullable = false)
    var yellowCards: Int = 0,
    @Column(nullable = false)
    var redCards: Int = 0,
    @Column(nullable = false)
    var minutesPlayed: Int = 0,
    @Column(nullable = false)
    var matchesPlayed: Int = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: ClubMemberRoleEntity = PLAYER,
    @CreationTimestamp
    var createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now()
) {
    enum class ClubMemberRoleEntity {
        OWNER,
        ADMIN,
        CAPTAIN,
        PLAYER
    }

    enum class PlayerPositionEntity {
        GOALKEEPER,
        DEFENDER,
        MIDFIELDER,
        FORWARD
    }
}
