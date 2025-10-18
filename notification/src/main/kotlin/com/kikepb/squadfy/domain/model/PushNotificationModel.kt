package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ChatId
import java.util.UUID

data class PushNotificationModel(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val recipients: List<DeviceTokenModel>,
    val message: String,
    val chatId: ChatId,
    val data: Map<String, String>
)
