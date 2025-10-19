package com.kikepb.squadfy.infrastructure.mappers

import com.kikepb.squadfy.domain.model.DeviceTokenModel
import com.kikepb.squadfy.domain.model.DeviceTokenModel.Platform.ANDROID
import com.kikepb.squadfy.domain.model.DeviceTokenModel.Platform.IOS
import com.kikepb.squadfy.infrastructure.database.entities.PlatformEntity

fun DeviceTokenModel.Platform.toPlatformEntity(): PlatformEntity =
    when (this) {
        ANDROID -> PlatformEntity.ANDROID
        IOS -> PlatformEntity.IOS
    }

fun PlatformEntity.toPlatformModel(): DeviceTokenModel.Platform =
    when (this) {
        PlatformEntity.ANDROID -> ANDROID
        PlatformEntity.IOS -> IOS
    }