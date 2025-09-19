package com.kikepb.squadfy.api.controller

import com.kikepb.squadfy.api.dto.*
import com.kikepb.squadfy.api.mappers.toAuthenticatedUserDto
import com.kikepb.squadfy.api.mappers.toUserDto
import com.kikepb.squadfy.service.auth.AuthService
import com.kikepb.squadfy.service.auth.EmailVerificationService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val emailVerificationService: EmailVerificationService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody body: RegisterRequest): UserDto {
        return authService.register(
            email = body.email,
            username = body.username,
            password = body.password
        ).toUserDto()
    }

    @PostMapping("/login")
    fun login(@RequestBody body: LoginRequest): AuthenticatedUserDto {
        return authService.login(
            email = body.email,
            password = body.password
        ).toAuthenticatedUserDto()
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody body: RefreshRequest): AuthenticatedUserDto {
        return authService.refresh(
            refreshToken = body.refreshToken
        ).toAuthenticatedUserDto()
    }

    @GetMapping("/verify")
    fun verifyEmail(@RequestParam token: String) {
        emailVerificationService.verifyEmail(token = token)
    }
}