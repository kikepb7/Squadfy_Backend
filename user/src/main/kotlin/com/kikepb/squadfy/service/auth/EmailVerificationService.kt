package com.kikepb.squadfy.service.auth

import com.kikepb.squadfy.domain.exception.InvalidTokenException
import com.kikepb.squadfy.domain.exception.UserNotFoundException
import com.kikepb.squadfy.domain.model.EmailVerificationTokenModel
import com.kikepb.squadfy.infrastructure.database.entities.EmailVerificationTokenEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toEmailVerificationToken
import com.kikepb.squadfy.infrastructure.database.mappers.toUser
import com.kikepb.squadfy.infrastructure.database.repositories.EmailVerificationTokenRepository
import com.kikepb.squadfy.infrastructure.database.repositories.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class EmailVerificationService(
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val userRepository: UserRepository,
    @param:Value("\${squadfy.email.verification.expiry-hours}")private val expiryHours: Long
) {

    @Transactional
    fun createVerificationToken(email: String): EmailVerificationTokenModel {
        val userEntity = userRepository.findByEmail(email = email)
            ?: throw UserNotFoundException()

        val existingTokens = emailVerificationTokenRepository.findByUserAndUsedAtIsNull(user = userEntity)

        val now = Instant.now()
        val usedTokens = existingTokens.map {
            it.apply {
                this.usedAt = now
            }
        }

        emailVerificationTokenRepository.saveAll(usedTokens)

        val token = EmailVerificationTokenEntity(
            expiresAt = now.plus(expiryHours, ChronoUnit.HOURS),
            user = userEntity
        )

        return emailVerificationTokenRepository.save(token).toEmailVerificationToken()
    }

    @Transactional
    fun verifyEmail(token: String) {
        val verificationToken = emailVerificationTokenRepository.findByToken(token = token)
            ?: throw InvalidTokenException("Email verification token is invalid")

        if (verificationToken.isUsed) throw InvalidTokenException("Email verification token is already used")
        if (verificationToken.isExpired) throw InvalidTokenException("Email verification token has already expired")

        emailVerificationTokenRepository.save(
            verificationToken.apply {
                this.usedAt = Instant.now()
            }
        )

        userRepository.save(
            verificationToken.user.apply {
                this.hasVerifiedEmail = true
            }
        ).toUser()
    }

    @Scheduled(cron = "0 0 3 * * *")
    fun cleanUpExpiredTokens() {
        emailVerificationTokenRepository.deleteByExpiresAtLessThan(now = Instant.now())
    }
}