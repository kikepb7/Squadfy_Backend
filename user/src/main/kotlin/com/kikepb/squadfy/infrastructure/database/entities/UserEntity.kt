package com.kikepb.squadfy.infrastructure.database.entities

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import com.kikepb.squadfy.domain.model.UserId
import java.time.Instant

@Entity
@Table(
    name = "users",
    schema = "user_service",
    indexes = [
        Index(name = "idx_users_email", columnList = "email"),
        Index(name = "idx_users_username", columnList = "username"),
    ]
)
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UserId? = null,
    @Column(nullable = false, unique = true)
    var email: String,
    @Column(nullable = false, unique = true)
    var username: String,
    @Column(nullable = false)
    var hashedPassword: String,
    @Column(nullable = false)
    var hasVerifiedEmail: Boolean = false,
    @Column(nullable = false)
    var isActive: Boolean = true,
    @CreationTimestamp
    var createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now()
)