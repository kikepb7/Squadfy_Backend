package com.kikepb.squadfy.domain.model

data class AuthenticatedUserModel(
    val user: UserModel,
    val accessToken: String,
    val refreshToken: String,
)
