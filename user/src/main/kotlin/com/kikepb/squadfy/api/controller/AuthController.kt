package com.kikepb.squadfy.api.controller

import com.kikepb.squadfy.api.dto.*
import com.kikepb.squadfy.api.mappers.toAuthenticatedUserDto
import com.kikepb.squadfy.api.mappers.toUserDto
import com.kikepb.squadfy.infrastructure.rate_limiting.EmailRateLimiter
import com.kikepb.squadfy.service.auth.AuthService
import com.kikepb.squadfy.service.auth.EmailVerificationService
import com.kikepb.squadfy.service.auth.PasswordResetService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val emailVerificationService: EmailVerificationService,
    private val passwordResetService: PasswordResetService,
    private val emailRateLimiter: EmailRateLimiter
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

    @PostMapping("/resend-verification")
    fun resendVerification(@Valid @RequestBody body: EmailRequest) {
        emailRateLimiter.withRateLimit(email = body.email) {
            emailVerificationService.resendVerificationEmail(email = body.email)
        }
    }

    @GetMapping("/verify")
    fun verifyEmail(@RequestParam token: String) {
        emailVerificationService.verifyEmail(token = token)
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody body: EmailRequest) {
        passwordResetService.requestPasswordReset(email = body.email)
    }

    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody body: ResetPasswordRequest) {
        passwordResetService.resetPassword(
            token = body.token,
            newPassword = body.newPassword
        )
    }

    @PostMapping("/change-password")
    fun changePassword(@Valid @RequestBody body: ChangePasswordRequest) {
        // TODO: Extract request user ID and call service
    }
}