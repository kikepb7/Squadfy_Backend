package com.kikepb.squadfy.api.exception_handling

import com.kikepb.squadfy.domain.exception.InvalidDeviceTokenException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class NotificationExceptionHandler {

    @ExceptionHandler(InvalidDeviceTokenException::class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    fun onInvalidDeviceToken(e: InvalidDeviceTokenException) = mapOf(
        "code" to "INVALID_DEVICE_TOKEN",
        "message" to e.message
    )
}