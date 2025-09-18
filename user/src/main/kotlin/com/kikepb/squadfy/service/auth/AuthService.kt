package com.kikepb.squadfy.service.auth

import com.kikepb.squadfy.domain.exception.InvalidCredentialsException
import com.kikepb.squadfy.domain.exception.UserAlreadyExistsException
import com.kikepb.squadfy.domain.exception.UserNotFoundException
import com.kikepb.squadfy.domain.model.AuthenticatedUser
import com.kikepb.squadfy.domain.model.User
import com.kikepb.squadfy.domain.model.UserId
import com.kikepb.squadfy.infrastructure.database.entities.RefreshTokenEntity
import com.kikepb.squadfy.infrastructure.database.entities.UserEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toUser
import com.kikepb.squadfy.infrastructure.database.repositories.RefreshTokenRepository
import com.kikepb.squadfy.infrastructure.database.repositories.UserRepository
import com.kikepb.squadfy.infrastructure.security.PasswordEncoded
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoded: PasswordEncoded,
    private val jwtService: JwtService,
    private val refreshTokenRepository: RefreshTokenRepository
) {

    fun register(email: String, username: String, password: String): User {
        val user = userRepository.findByEmailOrUsername(
            email = email.trim(),
            username = username.trim()
        )

        if (user != null) throw UserAlreadyExistsException()

        val savedUser = userRepository.save(
            UserEntity(
                email = email.trim(),
                username = username.trim(),
                hashedPassword = passwordEncoded.encode(password)
            )
        ).toUser()

        return savedUser
    }

    fun login(email: String, password: String): AuthenticatedUser {
        val user = userRepository.findByEmail(email = email.trim())
            ?: throw InvalidCredentialsException()

        if (!passwordEncoded.matches(password, user.hashedPassword)) {
            throw InvalidCredentialsException()
        }

        // TODO: Check for verified email

        return user.id?.let { userId ->
            val accessToken = jwtService.generateAccessToken(userId = userId)
            val refreshToken = jwtService.generateRefreshToken(userId = userId)

            storeRefreshToken(userId = userId, token = refreshToken)

            AuthenticatedUser(
                user = user.toUser(),
                accessToken = accessToken,
                refreshToken = refreshToken
            )
        } ?: throw UserNotFoundException()
    }

    private fun storeRefreshToken(userId: UserId, token: String) {
        val hashedToken = hashToken(token = token)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepository.save(
            RefreshTokenEntity(
                userId = userId,
                expiresAt = expiresAt,
                hashedToken = hashedToken
            )
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}