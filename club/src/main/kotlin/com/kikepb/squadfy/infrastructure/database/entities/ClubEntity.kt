package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.UserId
import jakarta.persistence.Column
import jakarta.persistence.Entity
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
    name = "clubs",
    schema = "club_service",
    indexes = [
        Index(name = "idx_clubs_owner_id", columnList = "owner_id"),
        Index(name = "idx_clubs_invitation_code", columnList = "invitation_code", unique = true)
    ]
)
class ClubEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: ClubId? = null,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = true, length = 2000)
    var description: String? = null,
    @Column(nullable = true)
    var clubLogoUrl: String? = null,
    @Column(name = "owner_id", nullable = false)
    var ownerId: UserId,
    @Column(nullable = false, unique = true)
    var invitationCode: String,
    @Column(nullable = true)
    var maxMembers: Int? = null,
    @CreationTimestamp
    var createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now()
)
