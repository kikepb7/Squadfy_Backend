package com.kikepb.squadfy.domain.model

data class EmailVerificationTokenModel(
    val id: Long,
    val token: String,
    val user: UserModel
)