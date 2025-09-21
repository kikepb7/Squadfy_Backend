package com.kikepb.squadfy.service.auth

import com.kikepb.squadfy.domain.exception.*
import com.kikepb.squadfy.domain.model.AuthenticatedUserModel
import com.kikepb.squadfy.domain.model.UserModel
import com.kikepb.squadfy.domain.model.UserId
import com.kikepb.squadfy.infrastructure.database.entities.RefreshTokenEntity
import com.kikepb.squadfy.infrastructure.database.entities.UserEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toUser
import com.kikepb.squadfy.infrastructure.database.repositories.RefreshTokenRepository
import com.kikepb.squadfy.infrastructure.database.repositories.UserRepository
import com.kikepb.squadfy.infrastructure.security.PasswordEncoded
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

@Service
class AuthService(
    private val jwtService: JwtService,
    private val passwordEncoded: PasswordEncoded,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailVerificationService: EmailVerificationService
) {

    @Transactional
    fun register(email: String, username: String, password: String): UserModel {
        val trimmedEmail = email.trim()

        val user = userRepository.findByEmailOrUsername(
            email = trimmedEmail,
            username = username.trim()
        )

        if (user != null) throw UserAlreadyExistsException()

        val savedUser = userRepository.saveAndFlush(
            UserEntity(
                email = trimmedEmail,
                username = username.trim(),
                hashedPassword = passwordEncoded.encode(password)
            )
        ).toUser()

        val token = emailVerificationService.createVerificationToken(email = trimmedEmail)

        return savedUser
    }

    fun login(email: String, password: String): AuthenticatedUserModel {
        val user = userRepository.findByEmail(email = email.trim())
            ?: throw InvalidCredentialsException()

        if (!passwordEncoded.matches(password, user.hashedPassword)) throw InvalidCredentialsException()
        if (!user.hasVerifiedEmail) throw EmailNotVerifiedException()

        return user.id?.let { userId ->
            val accessToken = jwtService.generateAccessToken(userId = userId)
            val refreshToken = jwtService.generateRefreshToken(userId = userId)

            storeRefreshToken(userId = userId, token = refreshToken)

            AuthenticatedUserModel(
                user = user.toUser(),
                accessToken = accessToken,
                refreshToken = refreshToken
            )
        } ?: throw UserNotFoundException()
    }

    @Transactional
    fun refresh(refreshToken: String): AuthenticatedUserModel {
        if (!jwtService.validateRefreshToken(token = refreshToken)) {
            throw InvalidTokenException(
                message = "Invalid refresh token"
            )
        }

        val userId = jwtService.getUserIdFromToken(token = refreshToken)
        val user = userRepository.findByIdOrNull(id = userId)
            ?: throw UserNotFoundException()

        val hashed = hashToken(token = refreshToken)

        return user.id?.let { userId ->
            refreshTokenRepository.findByUserIdAndHashedToken(
                userId = userId,
                hashedToken = hashed
            ) ?: throw InvalidTokenException("Invalid refresh token")

            refreshTokenRepository.deleteByUserIdAndHashedToken(
                userId = userId,
                hashedToken = hashed
            )

            val newAccessToken = jwtService.generateAccessToken(userId = userId)
            val newRefreshToken = jwtService.generateRefreshToken(userId = userId)

            storeRefreshToken(userId = userId, token = newRefreshToken)

            AuthenticatedUserModel(
                user = user.toUser(),
                accessToken = newAccessToken,
                refreshToken = newRefreshToken
            )
        } ?: throw UserNotFoundException()
    }

    @Transactional
    fun logout(refreshToken: String) {
        val userId = jwtService.getUserIdFromToken(token = refreshToken)
        val hashed = hashToken(token = refreshToken)

        refreshTokenRepository.deleteByUserIdAndHashedToken(userId = userId, hashedToken = hashed)
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