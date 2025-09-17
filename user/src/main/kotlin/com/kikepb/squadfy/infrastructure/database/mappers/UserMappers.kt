package com.kikepb.squadfy.infrastructure.database.mappers

import com.kikepb.squadfy.domain.model.User
import com.kikepb.squadfy.infrastructure.database.entities.UserEntity

fun UserEntity.toUser(): User {
    return User(
        id = requireNotNull(id) { "ID should not be null in UserEntity" },
        username = username,
        email = email,
        hasEmailVerified = hasVerifiedEmail,
        isActive = isActive
    )
}