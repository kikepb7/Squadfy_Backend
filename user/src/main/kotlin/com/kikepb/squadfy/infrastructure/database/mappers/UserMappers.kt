package com.kikepb.squadfy.infrastructure.database.mappers

import com.kikepb.squadfy.domain.model.UserModel
import com.kikepb.squadfy.infrastructure.database.entities.UserEntity

fun UserEntity.toUser(): UserModel {
    return UserModel(
        id = requireNotNull(id) { "ID should not be null in UserEntity" },
        username = username,
        email = email,
        hasEmailVerified = hasVerifiedEmail,
        isActive = isActive
    )
}