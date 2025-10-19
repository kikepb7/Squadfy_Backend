package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.DeviceTokenDto
import com.kikepb.squadfy.api.dto.RegisterDeviceRequest
import com.kikepb.squadfy.api.mappers.toDeviceTokenDto
import com.kikepb.squadfy.api.mappers.toPlatformDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.service.PushNotificationService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notification")
class DeviceTokenController(
    private val pushNotificationService: PushNotificationService
) {

    @PostMapping("/register")
    fun registerDeviceToken(@Valid @RequestBody body: RegisterDeviceRequest): DeviceTokenDto =
        pushNotificationService.registerDevice(
            userId = requestUserId,
            token = body.token,
            platform = body.platform.toPlatformDto(),
        ).toDeviceTokenDto()

    @DeleteMapping("/{token}")
    fun unregisterDeviceToken(@PathVariable("token") token: String) =
        pushNotificationService.unregisterDevice(token = token)
}