package com.kikepb.squadfy.service.auth

import com.kikepb.squadfy.domain.events.user.UserEvent
import com.kikepb.squadfy.domain.exception.*
import com.kikepb.squadfy.domain.model.UserId
import com.kikepb.squadfy.infrastructure.database.entities.PasswordResetTokenEntity
import com.kikepb.squadfy.infrastructure.database.entities.UserEntity
import com.kikepb.squadfy.infrastructure.database.repositories.PasswordResetTokenRepository
import com.kikepb.squadfy.infrastructure.database.repositories.RefreshTokenRepository
import com.kikepb.squadfy.infrastructure.database.repositories.UserRepository
import com.kikepb.squadfy.infrastructure.message_queue.EventPublisher
import com.kikepb.squadfy.infrastructure.security.PasswordEncoded
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import java.time.Instant

class PasswordResetServiceTest {
    private lateinit var passwordResetService: PasswordResetService
    private lateinit var userRepository: UserRepository
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var passwordEncoded: PasswordEncoded
    private lateinit var eventPublisher: EventPublisher

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        passwordResetTokenRepository = mockk()
        refreshTokenRepository = mockk()
        passwordEncoded = mockk()
        eventPublisher = mockk()

        passwordResetService = PasswordResetService(
            userRepository = userRepository,
            passwordResetTokenRepository = passwordResetTokenRepository,
            refreshTokenRepository = refreshTokenRepository,
            passwordEncoded = passwordEncoded,
            expiryMinutes = 30L,
            eventPublisher = eventPublisher
        )
    }

    @Test
    fun `requestPasswordReset should create token and publish event`() {
        val email = "test@example.com"
        val userId = UserId.randomUUID()
        val userEntity = UserEntity(
            email = email,
            username = "testuser",
            hashedPassword = "hashedPassword"
        ).apply { id = userId }

        val tokenEntity = PasswordResetTokenEntity(
            user = userEntity,
            expiresAt = Instant.now().plusSeconds(1800)
        )

        every { userRepository.findByEmail(email) } returns userEntity
        every { passwordResetTokenRepository.invalidateActiveTokensForUser(userEntity) } just Runs
        every { passwordResetTokenRepository.save(any()) } returns tokenEntity
        every { eventPublisher.publish(any()) } just Runs

        passwordResetService.requestPasswordReset(email)

        verify {
            passwordResetTokenRepository.invalidateActiveTokensForUser(userEntity)
            passwordResetTokenRepository.save(any())
            eventPublisher.publish(match {
                it is UserEvent.RequestResetPassword &&
                it.userId == userId &&
                it.email == email
            })
        }
    }

    @Test
    fun `resetPassword should change password successfully`() {
        val token = "valid-token"
        val newPassword = "newPassword123"
        val userId = UserId.randomUUID()
        val userEntity = UserEntity(
            email = "test@example.com",
            username = "testuser",
            hashedPassword = "oldHashedPassword"
        ).apply { id = userId }

        val tokenEntity = PasswordResetTokenEntity(
            user = userEntity,
            expiresAt = Instant.now().plusSeconds(1800)
        )

        every { passwordResetTokenRepository.findByToken(token) } returns tokenEntity
        every { passwordEncoded.matches(newPassword, userEntity.hashedPassword) } returns false
        every { passwordEncoded.encode(newPassword) } returns "newHashedPassword"
        every { userRepository.save(any()) } returns userEntity
        every { passwordResetTokenRepository.save(any()) } returns tokenEntity
        every { refreshTokenRepository.deleteByUserId(userId) } just Runs

        passwordResetService.resetPassword(token, newPassword)

        verify {
            userRepository.save(match { it.hashedPassword == "newHashedPassword" })
            refreshTokenRepository.deleteByUserId(userId)
        }
    }

    @Test
    fun `resetPassword should throw InvalidTokenException when token is invalid`() {
        val token = "invalid-token"
        val newPassword = "newPassword123"

        every { passwordResetTokenRepository.findByToken(token) } returns null

        assertThrows<InvalidTokenException> {
            passwordResetService.resetPassword(token, newPassword)
        }
    }

    @Test
    fun `resetPassword should throw SamePasswordException when new password is same as old`() {
        val token = "valid-token"
        val newPassword = "samePassword123"
        val userEntity = UserEntity(
            email = "test@example.com",
            username = "testuser",
            hashedPassword = "hashedPassword123"
        )

        val tokenEntity = PasswordResetTokenEntity(
            user = userEntity,
            expiresAt = Instant.now().plusSeconds(1800)
        )

        every { passwordResetTokenRepository.findByToken(token) } returns tokenEntity
        every { passwordEncoded.matches(newPassword, userEntity.hashedPassword) } returns true

        assertThrows<SamePasswordException> {
            passwordResetService.resetPassword(token, newPassword)
        }
    }

    @Test
    fun `changePassword should update password successfully`() {
        val userId = UserId.randomUUID()
        val oldPassword = "oldPassword123"
        val newPassword = "newPassword123"

        val userEntity = UserEntity(
            email = "test@example.com",
            username = "testuser",
            hashedPassword = "oldHashedPassword"
        ).apply { id = userId }

        every { userRepository.findByIdOrNull(userId) } returns userEntity
        every { passwordEncoded.matches(oldPassword, userEntity.hashedPassword) } returns true
        every { passwordEncoded.encode(newPassword) } returns "newHashedPassword"
        every { userRepository.save(any()) } returns userEntity
        every { refreshTokenRepository.deleteByUserId(userId) } just Runs

        passwordResetService.changePassword(userId, oldPassword, newPassword)

        verify {
            userRepository.save(match { it.hashedPassword == "newHashedPassword" })
            refreshTokenRepository.deleteByUserId(userId)
        }
    }

    @Test
    fun `changePassword should throw InvalidCredentialsException when old password is incorrect`() {
        val userId = UserId.randomUUID()
        val oldPassword = "wrongPassword"
        val newPassword = "newPassword123"

        val userEntity = UserEntity(
            email = "test@example.com",
            username = "testuser",
            hashedPassword = "correctHashedPassword"
        )

        every { userRepository.findByIdOrNull(userId) } returns userEntity
        every { passwordEncoded.matches(oldPassword, userEntity.hashedPassword) } returns false

        assertThrows<InvalidCredentialsException> {
            passwordResetService.changePassword(userId, oldPassword, newPassword)
        }
    }

    @Test
    fun `changePassword should throw SamePasswordException when new password is same as old`() {
        val userId = UserId.randomUUID()
        val password = "samePassword123"

        val userEntity = UserEntity(
            email = "test@example.com",
            username = "testuser",
            hashedPassword = "hashedPassword"
        )

        every { userRepository.findByIdOrNull(userId) } returns userEntity
        every { passwordEncoded.matches(password, userEntity.hashedPassword) } returns true

        assertThrows<SamePasswordException> {
            passwordResetService.changePassword(userId, password, password)
        }
    }

    @Test
    fun `cleanUpExpiredTokens should delete expired tokens`() {
        every { passwordResetTokenRepository.deleteByExpiresAtLessThan(any()) } just Runs

        passwordResetService.cleanUpExpiredTokens()

        verify {
            passwordResetTokenRepository.deleteByExpiresAtLessThan(any())
        }
    }
}
