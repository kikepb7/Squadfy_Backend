package com.kikepb.squadfy.api.mappers

import com.kikepb.squadfy.api.dto.DeviceTokenDto
import com.kikepb.squadfy.api.dto.PlatformDto
import com.kikepb.squadfy.domain.model.DeviceTokenModel
import com.kikepb.squadfy.domain.model.DeviceTokenModel.Platform.ANDROID
import com.kikepb.squadfy.domain.model.DeviceTokenModel.Platform.IOS

fun DeviceTokenModel.toDeviceTokenDto(): DeviceTokenDto =
    DeviceTokenDto(
        userId = userId,
        token = token,
        createdAt = createdAt
    )

fun PlatformDto.toPlatformDto(): DeviceTokenModel.Platform =
    when (this) {
        PlatformDto.ANDROID -> ANDROID
        PlatformDto.IOS -> IOS
    }