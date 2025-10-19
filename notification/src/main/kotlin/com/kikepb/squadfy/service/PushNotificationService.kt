package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.InvalidDeviceTokenException
import com.kikepb.squadfy.domain.model.DeviceTokenModel
import com.kikepb.squadfy.domain.model.PushNotificationModel
import com.kikepb.squadfy.domain.type.ChatId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.DeviceTokenEntity
import com.kikepb.squadfy.infrastructure.database.repositories.DeviceTokenRepository
import com.kikepb.squadfy.infrastructure.mappers.toDeviceTokenModel
import com.kikepb.squadfy.infrastructure.mappers.toPlatformEntity
import com.kikepb.squadfy.infrastructure.push_notification.FirebasePushNotificationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PushNotificationService(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val firebasePushNotificationService: FirebasePushNotificationService
) {

    private val logger = LoggerFactory.getLogger(PushNotificationService::class.java)

    @Transactional
    fun registerDevice(userId: UserId, token: String, platform: DeviceTokenModel.Platform): DeviceTokenModel {
        val existingToken = deviceTokenRepository.findByToken(token = token)
        val trimmedToken = token.trim()

        if (existingToken == null && !firebasePushNotificationService.isValidToken(token = trimmedToken)) throw InvalidDeviceTokenException()

        val entity = if (existingToken != null) {
            deviceTokenRepository.save(
                existingToken.apply {
                    this.userId = userId
                }
            )
        } else {
            deviceTokenRepository.save(
                DeviceTokenEntity(
                    userId = userId,
                    token = trimmedToken,
                    platform = platform.toPlatformEntity()
                )
            )
        }

        return entity.toDeviceTokenModel()
    }

    @Transactional
    fun unregisterDevice(token: String) = deviceTokenRepository.deleteByToken(token = token.trim())

    @Transactional
    fun sendNewMessageNotifications(
        recipientUserIds: List<UserId>,
        senderUserId: UserId,
        senderUsername: String,
        message: String,
        chatId: ChatId
    ) {
        val deviceToken = deviceTokenRepository.findByUserIdIn(userIds = recipientUserIds)

        if (deviceToken.isEmpty()) {
            logger.info("No device tokens found for $recipientUserIds")
            return
        }

        val recipients = deviceToken
            .filter { it.userId != senderUserId }
            .map { it.toDeviceTokenModel() }

        val notification = PushNotificationModel(
            title = "new message from $senderUserId",
            recipients = recipients,
            message = message,
            chatId = chatId,
            data = mapOf(
                "chatId" to chatId.toString(),
                "type" to "new_message"
            )
        )

        firebasePushNotificationService.sendNotification(notification = notification)
    }
}