package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.type.ClubOwnerId
import com.kikepb.squadfy.domain.type.UserId
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "club_owners", schema = "club_service")
class ClubOwnerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: ClubOwnerId? = null,
    @ManyToOne
    @JoinColumn(name = "club_id")
    var club: ClubEntity,
    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: UserId
)