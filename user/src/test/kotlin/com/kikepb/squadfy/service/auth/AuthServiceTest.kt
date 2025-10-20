package com.kikepb.squadfy.service.auth

import com.kikepb.squadfy.domain.events.user.UserEvent
import com.kikepb.squadfy.domain.exception.*
import com.kikepb.squadfy.domain.model.UserId
import com.kikepb.squadfy.infrastructure.database.entities.UserEntity
import com.kikepb.squadfy.infrastructure.database.repositories.RefreshTokenRepository
import com.kikepb.squadfy.infrastructure.database.repositories.UserRepository
import com.kikepb.squadfy.infrastructure.message_queue.EventPublisher
import com.kikepb.squadfy.infrastructure.security.PasswordEncoded
import com.kikepb.squadfy.service.JwtService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AuthServiceTest {
    private lateinit var authService: AuthService
    private lateinit var jwtService: JwtService
    private lateinit var passwordEncoded: PasswordEncoded
    private lateinit var userRepository: UserRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var emailVerificationService: EmailVerificationService
    private lateinit var eventPublisher: EventPublisher

    @BeforeEach
    fun setUp() {
        jwtService = mockk()
        passwordEncoded = mockk()
        userRepository = mockk()
        refreshTokenRepository = mockk()
        emailVerificationService = mockk()
        eventPublisher = mockk()

        authService = AuthService(
            jwtService = jwtService,
            passwordEncoded = passwordEncoded,
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            emailVerificationService = emailVerificationService,
            eventPublisher = eventPublisher
        )
    }

    @Test
    fun `GIVEN valid user data WHEN registering THEN user should be created successfully`() {
        // GIVEN
        val userId = UserId.randomUUID()
        val email = "test@example.com"
        val username = "testuser"
        val password = "password123"
        val hashedPassword = "hashedPassword123"
        val verificationToken = "token123"

        val userEntity = UserEntity(
            email = email,
            username = username,
            hashedPassword = hashedPassword
        ).apply { id = userId }

        every { passwordEncoded.encode(password) } returns hashedPassword
        every { userRepository.findByEmailOrUsername(email, username) } returns null
        every { userRepository.saveAndFlush(any()) } returns userEntity
        every { emailVerificationService.createVerificationToken(email) } returns mockk {
            every { token } returns verificationToken
        }
        every { eventPublisher.publish(any()) } just Runs

        // WHEN
        authService.register(email, username, password)

        // THEN
        verify {
            userRepository.saveAndFlush(match {
                it.email == email &&
                it.username == username &&
                it.hashedPassword == hashedPassword
            })
            eventPublisher.publish(match {
                it is UserEvent.Created &&
                it.email == email &&
                it.username == username &&
                it.verificationToken == verificationToken
            })
        }
    }

    @Test
    fun `GIVEN existing user data WHEN registering THEN should throw UserAlreadyExistsException`() {
        // GIVEN
        val email = "test@example.com"
        val username = "testuser"
        val password = "password123"

        // WHEN
        every { userRepository.findByEmailOrUsername(email, username) } returns mockk()

        // THEN
        assertThrows<UserAlreadyExistsException> {
            authService.register(email, username, password)
        }
    }

    @Test
    fun `GIVEN valid credentials WHEN logging in THEN should return authenticated user model`() {
        // GIVEN
        val userId = UserId.randomUUID()
        val email = "test@example.com"
        val password = "password123"
        val hashedPassword = "hashedPassword123"
        val accessToken = "accessToken123"
        val refreshToken = "refreshToken123"

        val userEntity = UserEntity(
            email = email,
            username = "testuser",
            hashedPassword = hashedPassword
        ).apply {
            id = userId
            hasVerifiedEmail = true
        }

        every { userRepository.findByEmail(email) } returns userEntity
        every { passwordEncoded.matches(password, hashedPassword) } returns true
        every { jwtService.generateAccessToken(userId) } returns accessToken
        every { jwtService.generateRefreshToken(userId) } returns refreshToken
        every { jwtService.refreshTokenValidityMs } returns 3600000L
        every { refreshTokenRepository.save(any()) } returns mockk()

        // WHEN
        val result = authService.login(email, password)

        // THEN
        verify {
            jwtService.generateAccessToken(userId)
            jwtService.generateRefreshToken(userId)
            refreshTokenRepository.save(any())
        }

        assert(result.accessToken == accessToken)
        assert(result.refreshToken == refreshToken)
    }

    @Test
    fun `GIVEN invalid password WHEN logging in THEN should throw InvalidCredentialsException`() {
        // GIVEN
        val email = "test@example.com"
        val password = "wrongPassword"
        val hashedPassword = "correctHashedPassword"

        val userEntity = UserEntity(
            email = email,
            username = "testuser",
            hashedPassword = hashedPassword
        )

        // WHEN
        every { userRepository.findByEmail(email) } returns userEntity
        every { passwordEncoded.matches(password, hashedPassword) } returns false

        // THEN
        assertThrows<InvalidCredentialsException> {
            authService.login(email, password)
        }
    }

    @Test
    fun `GIVEN unverified email WHEN logging in THEN should throw EmailNotVerifiedException`() {
        // GIVEN
        val email = "test@example.com"
        val password = "password123"
        val hashedPassword = "hashedPassword123"

        val userEntity = UserEntity(
            email = email,
            username = "testuser",
            hashedPassword = hashedPassword
        ).apply { hasVerifiedEmail = false }

        // WHEN
        every { userRepository.findByEmail(email) } returns userEntity
        every { passwordEncoded.matches(password, hashedPassword) } returns true

        // THEN
        assertThrows<EmailNotVerifiedException> {
            authService.login(email, password)
        }
    }
}
