package com.kikepb.squadfy.infrastructure.mappers

import com.kikepb.squadfy.domain.model.DeviceTokenModel
import com.kikepb.squadfy.infrastructure.database.entities.DeviceTokenEntity

fun DeviceTokenEntity.toDeviceTokenModel(): DeviceTokenModel {
    return DeviceTokenModel(
        id = id,
        userId = userId,
        token = token,
        platform = platform.toPlatformModel(),
        createdAt = createdAt
    )
}