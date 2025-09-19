package com.kikepb.squadfy.api.mappers

import com.kikepb.squadfy.api.dto.AuthenticatedUserDto
import com.kikepb.squadfy.api.dto.UserDto
import com.kikepb.squadfy.domain.model.AuthenticatedUserModel
import com.kikepb.squadfy.domain.model.UserModel

fun AuthenticatedUserModel.toAuthenticatedUserDto(): AuthenticatedUserDto {
    return AuthenticatedUserDto(
        user = user.toUserDto(),
        accessToken = accessToken,
        refreshToken = refreshToken
    )
}

fun UserModel.toUserDto(): UserDto {
    return UserDto(
        id = id,
        email = email,
        username = username,
        hasVerifiedEmail = hasEmailVerified
    )
}