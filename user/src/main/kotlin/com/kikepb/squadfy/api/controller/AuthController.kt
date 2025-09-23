package com.kikepb.squadfy.api.controller

import com.kikepb.squadfy.api.dto.*
import com.kikepb.squadfy.api.mappers.toAuthenticatedUserDto
import com.kikepb.squadfy.api.mappers.toUserDto
import com.kikepb.squadfy.infrastructure.rate_limiting.EmailRateLimiter
import com.kikepb.squadfy.service.auth.AuthService
import com.kikepb.squadfy.service.auth.EmailVerificationService
import com.kikepb.squadfy.service.auth.PasswordResetService
import jakarta.validation.Valid
import com.kikepb.squadfy.api.config.IpRateLimit
import com.kikepb.squadfy.api.util.requestUserId
import org.springframework.web.bind.annotation.*
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val emailVerificationService: EmailVerificationService,
    private val passwordResetService: PasswordResetService,
    private val emailRateLimiter: EmailRateLimiter
) {

    @PostMapping("/register")
    @IpRateLimit(
        requests = 10,
        duration = 1L,
        unit = TimeUnit.HOURS
    )
    fun register(@Valid @RequestBody body: RegisterRequest): UserDto {
        return authService.register(
            email = body.email,
            username = body.username,
            password = body.password
        ).toUserDto()
    }

    @PostMapping("/login")
    @IpRateLimit(
        requests = 10,
        duration = 1L,
        unit = TimeUnit.HOURS
    )
    fun login(@RequestBody body: LoginRequest): AuthenticatedUserDto {
        return authService.login(
            email = body.email,
            password = body.password
        ).toAuthenticatedUserDto()
    }

    @PostMapping("/refresh")
    @IpRateLimit(
        requests = 10,
        duration = 1L,
        unit = TimeUnit.HOURS
    )
    fun refresh(@RequestBody body: RefreshRequest): AuthenticatedUserDto {
        return authService.refresh(
            refreshToken = body.refreshToken
        ).toAuthenticatedUserDto()
    }

    @PostMapping("/logout")
    fun logout(@RequestBody body: RefreshRequest) {
        authService.logout(refreshToken = body.refreshToken)
    }

    @PostMapping("/resend-verification")
    @IpRateLimit(
        requests = 10,
        duration = 1L,
        unit = TimeUnit.HOURS
    )
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
    @IpRateLimit(
        requests = 10,
        duration = 1L,
        unit = TimeUnit.HOURS
    )
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
        passwordResetService.changePassword(
            userId = requestUserId,
            oldPassword = body.oldPassword,
            newPassword = body.newPassword
        )
    }
}