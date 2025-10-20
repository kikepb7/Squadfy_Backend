package com.kikepb.squadfy.service.auth

import com.kikepb.squadfy.domain.events.user.UserEvent
import com.kikepb.squadfy.domain.exception.InvalidTokenException
import com.kikepb.squadfy.domain.exception.UserNotFoundException
import com.kikepb.squadfy.domain.model.UserId
import com.kikepb.squadfy.infrastructure.database.entities.EmailVerificationTokenEntity
import com.kikepb.squadfy.infrastructure.database.entities.UserEntity
import com.kikepb.squadfy.infrastructure.database.repositories.EmailVerificationTokenRepository
import com.kikepb.squadfy.infrastructure.database.repositories.UserRepository
import com.kikepb.squadfy.infrastructure.message_queue.EventPublisher
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.*

class EmailVerificationServiceTest {
    private lateinit var emailVerificationService: EmailVerificationService
    private lateinit var emailVerificationTokenRepository: EmailVerificationTokenRepository
    private lateinit var userRepository: UserRepository
    private lateinit var eventPublisher: EventPublisher

    @BeforeEach
    fun setUp() {
        emailVerificationTokenRepository = mockk()
        userRepository = mockk()
        eventPublisher = mockk()

        emailVerificationService = EmailVerificationService(
            emailVerificationTokenRepository = emailVerificationTokenRepository,
            userRepository = userRepository,
            expiryHours = 24L,
            eventPublisher = eventPublisher
        )
    }

    @Test
    fun `resendVerificationEmail should create new token and publish event`() {
        val email = "test@example.com"
        val userId = UserId.randomUUID()
        val username = "testuser"

        val userEntity = UserEntity(
            email = email,
            username = username,
            hashedPassword = "hashedPassword"
        ).apply {
            id = userId
            hasVerifiedEmail = false
        }

        val tokenEntity = EmailVerificationTokenEntity(
            user = userEntity,
            expiresAt = Instant.now().plusSeconds(86400)
        )

        every { userRepository.findByEmail(email) } returns userEntity
        every { emailVerificationTokenRepository.invalidateActiveTokensForUser(userEntity) } just Runs
        every { emailVerificationTokenRepository.save(any()) } returns tokenEntity
        every { eventPublisher.publish(any()) } just Runs

        emailVerificationService.resendVerificationEmail(email)

        verify {
            emailVerificationTokenRepository.invalidateActiveTokensForUser(userEntity)
            emailVerificationTokenRepository.save(any())
            eventPublisher.publish(match {
                it is UserEvent.RequestResendVerification &&
                it.userId == userId &&
                it.email == email &&
                it.username == username
            })
        }
    }

    @Test
    fun `resendVerificationEmail should throw UserNotFoundException when user not found`() {
        val email = "nonexistent@example.com"

        every { userRepository.findByEmail(email) } returns null

        assertThrows<UserNotFoundException> {
            emailVerificationService.resendVerificationEmail(email)
        }
    }

    @Test
    fun `verifyEmail should verify user email successfully`() {
        val token = "valid-token"
        val userId = UserId.randomUUID()
        val userEntity = UserEntity(
            email = "test@example.com",
            username = "testuser",
            hashedPassword = "hashedPassword"
        ).apply {
            id = userId
            hasVerifiedEmail = false
        }

        val tokenEntity = EmailVerificationTokenEntity(
            user = userEntity,
            expiresAt = Instant.now().plusSeconds(3600)
        )

        every { emailVerificationTokenRepository.findByToken(token) } returns tokenEntity
        every { emailVerificationTokenRepository.save(any()) } returns tokenEntity
        every { userRepository.save(any()) } returns userEntity.apply { hasVerifiedEmail = true }
        every { eventPublisher.publish(any()) } just Runs

        emailVerificationService.verifyEmail(token)

        verify {
            userRepository.save(match { it.hasVerifiedEmail })
            eventPublisher.publish(match {
                it is UserEvent.Verified &&
                it.userId == userId &&
                it.email == userEntity.email &&
                it.username == userEntity.username
            })
        }
    }

    @Test
    fun `verifyEmail should throw InvalidTokenException when token is invalid`() {
        val token = "invalid-token"

        every { emailVerificationTokenRepository.findByToken(token) } returns null

        assertThrows<InvalidTokenException> {
            emailVerificationService.verifyEmail(token)
        }
    }

    @Test
    fun `verifyEmail should throw InvalidTokenException when token is expired`() {
        val token = "expired-token"
        val userEntity = UserEntity(
            email = "test@example.com",
            username = "testuser",
            hashedPassword = "hashedPassword"
        )

        val tokenEntity = EmailVerificationTokenEntity(
            user = userEntity,
            expiresAt = Instant.now().minusSeconds(3600)
        )

        every { emailVerificationTokenRepository.findByToken(token) } returns tokenEntity

        assertThrows<InvalidTokenException> {
            emailVerificationService.verifyEmail(token)
        }
    }

    @Test
    fun `verifyEmail should throw InvalidTokenException when token is already used`() {
        val token = "used-token"
        val userEntity = UserEntity(
            email = "test@example.com",
            username = "testuser",
            hashedPassword = "hashedPassword"
        )

        val tokenEntity = EmailVerificationTokenEntity(
            user = userEntity,
            expiresAt = Instant.now().plusSeconds(3600)
        ).apply {
            usedAt = Instant.now().minusSeconds(1800)
        }

        every { emailVerificationTokenRepository.findByToken(token) } returns tokenEntity

        assertThrows<InvalidTokenException> {
            emailVerificationService.verifyEmail(token)
        }
    }

    @Test
    fun `cleanUpExpiredTokens should delete expired tokens`() {
        every { emailVerificationTokenRepository.deleteByExpiresAtLessThan(any()) } just Runs

        emailVerificationService.cleanUpExpiredTokens()

        verify {
            emailVerificationTokenRepository.deleteByExpiresAtLessThan(any())
        }
    }
}
