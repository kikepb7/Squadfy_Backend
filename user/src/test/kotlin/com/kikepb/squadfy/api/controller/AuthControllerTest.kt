package com.kikepb.squadfy.api.controller

import com.kikepb.squadfy.api.dto.*
import com.kikepb.squadfy.domain.model.AuthenticatedUserModel
import com.kikepb.squadfy.domain.model.UserModel
import com.kikepb.squadfy.domain.model.UserId
import com.kikepb.squadfy.infrastructure.rate_limiting.EmailRateLimiter
import com.kikepb.squadfy.service.auth.AuthService
import com.kikepb.squadfy.service.auth.EmailVerificationService
import com.kikepb.squadfy.service.auth.PasswordResetService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*


@SpringBootTest(classes = [AuthController::class])
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var authService: AuthService

    @MockkBean
    private lateinit var emailVerificationService: EmailVerificationService

    @MockkBean
    private lateinit var passwordResetService: PasswordResetService

    @MockkBean
    private lateinit var emailRateLimiter: EmailRateLimiter

    @Test
    fun `register should create new user`() {
        val request = RegisterRequest(
            email = "test@example.com",
            username = "testuser",
            password = "password123"
        )

        val userId = UserId.randomUUID()
        val userModel = UserModel(
            id = userId,
            email = request.email,
            username = request.username,
            hasEmailVerified = false,
            isActive = true
        )

        every {
            authService.register(
                email = request.email,
                username = request.username,
                password = request.password
            )
        } returns userModel

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "${request.email}",
                        "username": "${request.username}",
                        "password": "${request.password}"
                    }
                """.trimIndent())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(userId.toString()))
            .andExpect(jsonPath("$.email").value(request.email))
            .andExpect(jsonPath("$.username").value(request.username))
    }

    @Test
    fun `login should return authenticated user`() {
        val request = LoginRequest(
            email = "test@example.com",
            password = "password123"
        )

        val userId = UserId.randomUUID()
        val authenticatedUser = AuthenticatedUserModel(
            user = UserModel(
                id = userId,
                email = request.email,
                username = "testuser",
                hasEmailVerified = true,
                isActive = true
            ),
            accessToken = "access-token",
            refreshToken = "refresh-token"
        )

        every {
            authService.login(
                email = request.email,
                password = request.password
            )
        } returns authenticatedUser

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "${request.email}",
                        "password": "${request.password}"
                    }
                """.trimIndent())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.id").value(userId.toString()))
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
    }

    @Test
    fun `refresh should return new tokens`() {
        val request = RefreshRequest(refreshToken = "old-refresh-token")

        val userId = UserId.randomUUID()
        val authenticatedUser = AuthenticatedUserModel(
            user = UserModel(
                id = userId,
                email = "test@example.com",
                username = "testuser",
                hasEmailVerified = true,
                isActive = true
            ),
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token"
        )

        every {
            authService.refresh(refreshToken = request.refreshToken)
        } returns authenticatedUser

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "refreshToken": "${request.refreshToken}"
                    }
                """.trimIndent())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
    }

    @Test
    fun `logout should invalidate refresh token`() {
        val request = RefreshRequest(refreshToken = "refresh-token")

        every {
            authService.logout(refreshToken = request.refreshToken)
        } just runs

        mockMvc.perform(
            post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "refreshToken": "${request.refreshToken}"
                    }
                """.trimIndent())
        )
            .andExpect(status().isOk)

        verify {
            authService.logout(refreshToken = request.refreshToken)
        }
    }

    @Test
    fun `resend verification should send new verification email`() {
        val request = EmailRequest(email = "test@example.com")

        every {
            emailRateLimiter.withRateLimit(
                email = request.email,
                action = any()
            )
        } just runs

        every {
            emailVerificationService.resendVerificationEmail(email = request.email)
        } just runs

        mockMvc.perform(
            post("/api/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "${request.email}"
                    }
                """.trimIndent())
        )
            .andExpect(status().isOk)

        verify {
            emailVerificationService.resendVerificationEmail(email = request.email)
        }
    }

    @Test
    fun `verify email should validate token`() {
        val token = "verification-token"

        every {
            emailVerificationService.verifyEmail(token = token)
        } just runs

        mockMvc.perform(
            get("/api/auth/verify")
                .param("token", token)
        )
            .andExpect(status().isOk)

        verify {
            emailVerificationService.verifyEmail(token = token)
        }
    }

    @Test
    fun `forgot password should initiate password reset`() {
        val request = EmailRequest(email = "test@example.com")

        every {
            passwordResetService.requestPasswordReset(email = request.email)
        } just runs

        mockMvc.perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "${request.email}"
                    }
                """.trimIndent())
        )
            .andExpect(status().isOk)

        verify {
            passwordResetService.requestPasswordReset(email = request.email)
        }
    }

    @Test
    fun `reset password should change password with token`() {
        val request = ResetPasswordRequest(
            token = "reset-token",
            newPassword = "newPassword123"
        )

        every {
            passwordResetService.resetPassword(
                token = request.token,
                newPassword = request.newPassword
            )
        } just runs

        mockMvc.perform(
            post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "token": "${request.token}",
                        "newPassword": "${request.newPassword}"
                    }
                """.trimIndent())
        )
            .andExpect(status().isOk)

        verify {
            passwordResetService.resetPassword(
                token = request.token,
                newPassword = request.newPassword
            )
        }
    }
}
