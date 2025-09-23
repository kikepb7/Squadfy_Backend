package com.kikepb.squadfy.service.auth

import com.kikepb.squadfy.domain.events.user.UserEvent
import com.kikepb.squadfy.domain.exception.InvalidCredentialsException
import com.kikepb.squadfy.domain.exception.InvalidTokenException
import com.kikepb.squadfy.domain.exception.SamePasswordException
import com.kikepb.squadfy.domain.exception.UserNotFoundException
import com.kikepb.squadfy.domain.model.UserId
import com.kikepb.squadfy.infrastructure.message_queue.EventPublisher
import com.kikepb.squadfy.infrastructure.database.entities.PasswordResetTokenEntity
import com.kikepb.squadfy.infrastructure.database.repositories.PasswordResetTokenRepository
import com.kikepb.squadfy.infrastructure.database.repositories.RefreshTokenRepository
import com.kikepb.squadfy.infrastructure.database.repositories.UserRepository
import com.kikepb.squadfy.infrastructure.security.PasswordEncoded
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class PasswordResetService(
    private val userRepository: UserRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoded: PasswordEncoded,
    @param:Value("\${squadfy.email.reset-password.expiry-minutes}")
    private val expiryMinutes: Long,
    private val eventPublisher: EventPublisher

) {
    @Transactional
    fun requestPasswordReset(email: String) {
        val user = userRepository.findByEmail(email = email) ?: return

        passwordResetTokenRepository.invalidateActiveTokensForUser(user = user)

        val token = PasswordResetTokenEntity(
            user = user,
            expiresAt = Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES)
        )
        passwordResetTokenRepository.save(token)

        user.id?.let { userId ->
            eventPublisher.publish(
                UserEvent.RequestResetPassword(
                    userId = userId,
                    email = user.email,
                    username = user.username,
                    passwordResetToken = token.token,
                    expiresInMinutes = expiryMinutes
                )
            )
        }
    }

    @Transactional
    fun resetPassword(token: String, newPassword: String) {
        val resetToken = passwordResetTokenRepository.findByToken(token = token)
            ?: throw InvalidTokenException("Invalid password reset token")

        if (resetToken.isUsed) throw InvalidTokenException("Email verification token is already used")
        if (resetToken.isExpired) throw InvalidTokenException("Email verification token has already expired")

        val user = resetToken.user

        if (passwordEncoded.matches(rawPassword = newPassword, hashedPassword = user.hashedPassword)) throw SamePasswordException()

        val hashedNewPassword = passwordEncoded.encode(rawPassword = newPassword)

        userRepository.save(
            user.apply {
                this.hashedPassword = hashedNewPassword
            }
        )

        passwordResetTokenRepository.save(
            resetToken.apply {
                this.usedAt = Instant.now()
            }
        )

        user.id?.let { refreshTokenRepository.deleteByUserId(userId = it) }
    }

    @Transactional
    fun changePassword(userId: UserId, oldPassword: String, newPassword: String) {
        val user = userRepository.findByIdOrNull(id = userId)
            ?: throw UserNotFoundException()

        if (!passwordEncoded.matches(rawPassword = oldPassword, hashedPassword = user.hashedPassword)) throw InvalidCredentialsException()
        if (oldPassword == newPassword) throw SamePasswordException()

        user.id?.let { refreshTokenRepository.deleteByUserId(userId = it) }

        val newHashedPassword = passwordEncoded.encode(rawPassword = newPassword)

        userRepository.save(
            user.apply {
                this.hashedPassword = newHashedPassword
            }
        )
    }

    @Scheduled(cron = "0 0 3 * * *")
    fun cleanUpExpiredTokens() {
        passwordResetTokenRepository.deleteByExpiresAtLessThan(now = Instant.now())
    }
}