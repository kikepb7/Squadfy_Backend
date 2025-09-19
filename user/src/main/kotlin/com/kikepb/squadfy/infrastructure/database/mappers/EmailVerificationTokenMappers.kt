package com.kikepb.squadfy.infrastructure.database.mappers

import com.kikepb.squadfy.domain.model.EmailVerificationTokenModel
import com.kikepb.squadfy.infrastructure.database.entities.EmailVerificationTokenEntity

fun EmailVerificationTokenEntity.toEmailVerificationToken(): EmailVerificationTokenModel {
    return EmailVerificationTokenModel(
        id = id,
        token = token,
        user = user.toUser()
    )
}