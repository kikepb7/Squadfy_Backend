package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.UserId

data class UserDto(
    val id: UserId,
    val email: String,
    val username: String,
    val hasVerifiedEmail: Boolean
)
