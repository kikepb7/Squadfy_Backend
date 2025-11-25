package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
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
)
class ClubMemberEntity(
    @Id
    var id: ClubMemberId? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    var club: ClubId,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserId,
    @Enumerated(EnumType.STRING)
    var role: ClubRole = ClubRole.MEMBER,
    @Column(nullable = false)
    var status: ClubMemberStatus = ClubMemberStatus.ACTIVE,
    @CreationTimestamp
    var joinedAt: Instant = Instant.now(),
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now()
) {
    enum class ClubRole { ADMIN, MEMBER }
    enum class ClubMemberStatus { ACTIVE, PENDING, BANNED}
}