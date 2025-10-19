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
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListMap

@Service
class PushNotificationService(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val firebasePushNotificationService: FirebasePushNotificationService
) {

    companion object {
        private val RETRY_DELAYS_SECONDS = listOf(30L, 60L, 120L, 300L, 600L)
        const val MAX_RETRY_AGE_MINUTES = 30L
    }

    private val retryQueue = ConcurrentSkipListMap<Long, MutableList<RetryData>>()
    private val logger = LoggerFactory.getLogger(PushNotificationService::class.java)

    private fun scheduleRetry(notification: PushNotificationModel, attempt: Int) {
        val delay = RETRY_DELAYS_SECONDS.getOrElse(attempt - 1) {
            RETRY_DELAYS_SECONDS.last()
        }

        val executeAt = Instant.now().plusSeconds(delay)
        val executeAtMillis = executeAt.toEpochMilli()

        val retryData = RetryData(
            notification = notification,
            attempt = attempt,
            createdAt = Instant.now()
        )

        retryQueue.compute(executeAtMillis) { _, retries ->
            (retries ?: mutableListOf()).apply { add(retryData) }
        }

        logger.info("Scheduled retry $attempt for ${notification.id} in $delay seconds ")
    }

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

        sendWithRetry(notification = notification)
    }

    fun sendWithRetry(notification: PushNotificationModel, attempt: Int = 0) {
        val result = firebasePushNotificationService.sendNotification(notification = notification)

        result.permanentFailures.forEach {
            deviceTokenRepository.deleteByToken(token = it.token)
        }

        if (result.temporaryFailures.isNotEmpty() && attempt < RETRY_DELAYS_SECONDS.size) {
            val retryNotification = notification.copy(
                recipients = result.temporaryFailures
            )

            scheduleRetry(retryNotification, attempt + 1)
        }

        if (result.succeeded.isNotEmpty()) logger.info("Successfully sent notification to ${result.succeeded.size} devices")
    }

    @Scheduled(fixedDelay = 15_000L)
    fun processRetries() {
        val now = Instant.now()
        val nowMillis = now.toEpochMilli()
        val toProcess = retryQueue.headMap(nowMillis, true)

        if (toProcess.isEmpty()) return

        val entries = toProcess.entries.toList()
        entries.forEach { (timeMillis, retries) ->
            retryQueue.remove(timeMillis)

            retries.forEach { retry ->
                try {
                    val age = Duration.between(retry.createdAt, now)
                    if (age.toMinutes() > MAX_RETRY_AGE_MINUTES) {
                        logger.warn("Dropping old retry (${age.toMinutes()} old")
                        return@forEach
                    }

                    sendWithRetry(
                        notification = retry.notification,
                        attempt = retry.attempt
                    )
                } catch (e: Exception) {
                    logger.warn("Error processing retry ${retry.notification.id}", e)
                }
            }
        }
    }

    private data class RetryData(
        val notification: PushNotificationModel,
        val attempt: Int,
        val createdAt: Instant
    )
}