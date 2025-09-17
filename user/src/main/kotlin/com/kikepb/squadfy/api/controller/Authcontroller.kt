package com.kikepb.squadfy.api.controller

import com.kikepb.squadfy.api.dto.RegisterRequest
import com.kikepb.squadfy.api.dto.UserDto
import com.kikepb.squadfy.api.mappers.toUserDto
import com.kikepb.squadfy.service.auth.AuthService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController("/api/auth")
class Authcontroller(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody body: RegisterRequest
    ): UserDto {
        return authService.register(
            email = body.email,
            username = body.username,
            password = body.password
        ).toUserDto()
    }
}