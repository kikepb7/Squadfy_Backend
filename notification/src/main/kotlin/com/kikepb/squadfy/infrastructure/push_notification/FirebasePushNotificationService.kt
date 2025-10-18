package com.kikepb.squadfy.infrastructure.push_notification

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import com.google.firebase.messaging.MessagingErrorCode.*
import com.kikepb.squadfy.domain.model.DeviceTokenModel
import com.kikepb.squadfy.domain.model.DeviceTokenModel.Platform.ANDROID
import com.kikepb.squadfy.domain.model.DeviceTokenModel.Platform.IOS
import com.kikepb.squadfy.domain.model.PushNotificationModel
import com.kikepb.squadfy.domain.model.PushNotificationSendResultModel
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service

@Service
class FirebasePushNotificationService(
    @param:Value("\${firebase.credentials-path")
    private val credentialsPath: String,
    private val resourceLoader: ResourceLoader
) {

    private val logger = LoggerFactory.getLogger(FirebasePushNotificationService::class.java)

    @PostConstruct
    fun initialize() {
        try {
            val serviceAccount = resourceLoader.getResource(credentialsPath)

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount.inputStream))
                .build()

            FirebaseApp.initializeApp(options)
            logger.info("Firebase Admin SDK initialized successfully")
        } catch (e: Exception) {
            logger.error("Error initializing Firebase Admin SDK")
            throw e
        }
    }

    fun isValidToken(token: String): Boolean {
        val message = Message.builder()
            .setToken(token)
            .build()

        return try {
            FirebaseMessaging.getInstance().send(message, true)
            true
        } catch (e: FirebaseMessagingException) {
            logger.warn("Failed to validate Firebase token", e)
            false
        }
    }

    fun sendNotification(notification: PushNotificationModel): PushNotificationSendResultModel {
        val messages = notification.recipients.map { recipient ->
            Message.builder()
                .setToken(recipient.token)
                .setNotification(
                    Notification.builder()
                        .setTitle(notification.title)
                        .setBody(notification.message)
                        .build()
                )
                .apply {
                    notification.data.forEach { (key, value) ->
                        putData(key, value)
                    }

                    when (recipient.platform) {
                        ANDROID -> {
                            setAndroidConfig(
                                AndroidConfig.builder()
                                    .setPriority(AndroidConfig.Priority.HIGH)
                                    .setCollapseKey(notification.chatId.toString())
                                    .setRestrictedPackageName("com.kikepb.squadfy")
                                    .build()
                            )
                        }
                        IOS -> {
                            setApnsConfig(
                                ApnsConfig.builder()
                                    .setAps(
                                        Aps.builder()
                                            .setSound("default")
                                            .setThreadId(notification.chatId.toString())
                                            .build()
                                    )
                                    .build()
                            )
                        }
                    }
                }
                .build()
        }

        return FirebaseMessaging
            .getInstance()
            .sendEach(messages)
            .toSendResult(notification.recipients)
    }

    private fun BatchResponse.toSendResult(
        allDeviceTokens: List<DeviceTokenModel>
    ): PushNotificationSendResultModel {
        val succeeded = mutableListOf<DeviceTokenModel>()
        val temporaryFailures = mutableListOf<DeviceTokenModel>()
        val permanentFailures = mutableListOf<DeviceTokenModel>()

        responses.forEachIndexed { index, sendResponse ->
            val deviceToken = allDeviceTokens[index]

            if (sendResponse.isSuccessful) {
                succeeded.add(deviceToken)
            } else {
                val errorCode = sendResponse.exception?.messagingErrorCode

                logger.warn("Failed to send notification to token ${deviceToken.token}: $errorCode")

                when (errorCode) {
                    UNREGISTERED, SENDER_ID_MISMATCH, INVALID_ARGUMENT, THIRD_PARTY_AUTH_ERROR -> permanentFailures.add(deviceToken)

                    INTERNAL, QUOTA_EXCEEDED, UNAVAILABLE, null -> temporaryFailures.add(deviceToken)
                }
            }
        }

        logger.debug("Push notifications sent. Succeeded: ${succeeded.size}, " +
                "temporary failures: ${temporaryFailures.size}, permanent failures: ${permanentFailures.size}")

        return PushNotificationSendResultModel(
            succeeded = succeeded.toList(),
            temporaryFailures = temporaryFailures.toList(),
            permanentFailures = permanentFailures.toList()
        )
    }
}