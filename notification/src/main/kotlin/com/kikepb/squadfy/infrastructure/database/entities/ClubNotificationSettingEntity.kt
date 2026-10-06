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
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "club_notification_settings",
    schema = "notification_service",
    uniqueConstraints = [UniqueConstraint(name = "idx_club_notification_settings_user_club", columnNames = ["user_id", "club_id"])],
    indexes = [Index(name = "idx_club_notification_settings_club_id", columnList = "club_id")]
)
class ClubNotificationSettingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UserId,
    @Column(name = "club_id", nullable = false, updatable = false)
    var clubId: ClubId,
    @Column(nullable = false)
    var muted: Boolean,
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now()
)
