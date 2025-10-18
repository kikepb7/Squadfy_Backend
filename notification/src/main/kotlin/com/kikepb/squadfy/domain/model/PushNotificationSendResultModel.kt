package com.kikepb.squadfy.domain.model

data class PushNotificationSendResultModel(
    val succeeded: List<DeviceTokenModel>,
    val temporaryFailures: List<DeviceTokenModel>,
    val permanentFailures: List<DeviceTokenModel>
)
