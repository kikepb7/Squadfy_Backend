package com.kikepb.squadfy.domain.model

import java.util.UUID

data class PushNotificationModel(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val recipients: List<DeviceTokenModel>,
    val message: String,
    /** Groups notifications on the device (Android collapse key / iOS thread). */
    val collapseKey: String,
    val data: Map<String, String>
)
