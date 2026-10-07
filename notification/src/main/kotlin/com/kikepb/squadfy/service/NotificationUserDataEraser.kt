package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.domain.user.UserDataEraser
import com.kikepb.squadfy.infrastructure.database.repositories.ClubNotificationSettingRepository
import com.kikepb.squadfy.infrastructure.database.repositories.DeviceTokenRepository
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Account deletion in the notification module (spec 010 RN-A3): devices and per-club preferences. */
@Service
@Order(4)
class NotificationUserDataEraser(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val clubNotificationSettingRepository: ClubNotificationSettingRepository
) : UserDataEraser {

    @Transactional
    override fun eraseUserData(userId: UserId) {
        deviceTokenRepository.deleteAllByUserId(userId = userId)
        clubNotificationSettingRepository.deleteAllByUserId(userId = userId)
    }
}
