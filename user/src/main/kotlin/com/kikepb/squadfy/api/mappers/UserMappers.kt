package com.kikepb.squadfy.api.mappers

import com.kikepb.squadfy.api.dto.AuthenticatedUserDto
import com.kikepb.squadfy.api.dto.UserDto
import com.kikepb.squadfy.domain.model.AuthenticatedUser
import com.kikepb.squadfy.domain.model.User

fun AuthenticatedUser.toAuthenticatedUserDto(): AuthenticatedUserDto {
    return AuthenticatedUserDto(
        user = user.toUserDto(),
        accessToken = accessToken,
        refreshToken = refreshToken
    )
}

fun User.toUserDto(): UserDto {
    return UserDto(
        id = id,
        email = email,
        username = username,
        hasVerifiedEmail = hasEmailVerified
    )
}