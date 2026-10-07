package com.kikepb.squadfy.service.account

import com.kikepb.squadfy.domain.exception.InvalidCredentialsException
import com.kikepb.squadfy.domain.exception.UserNotFoundException
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.domain.user.UserDataEraser
import com.kikepb.squadfy.infrastructure.database.entities.UserEntity
import com.kikepb.squadfy.infrastructure.database.repositories.EmailVerificationTokenRepository
import com.kikepb.squadfy.infrastructure.database.repositories.PasswordResetTokenRepository
import com.kikepb.squadfy.infrastructure.database.repositories.RefreshTokenRepository
import com.kikepb.squadfy.infrastructure.database.repositories.UserRepository
import com.kikepb.squadfy.infrastructure.security.AccessTokenRevocation
import com.kikepb.squadfy.infrastructure.security.PasswordEncoded
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Immediate and irreversible account deletion (spec 010 RN-A). Every module erases or anonymizes
 * its data through [UserDataEraser] in the same transaction as the user row, so a failure leaves
 * the account untouched.
 */
@Service
class AccountDeletionService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val passwordEncoded: PasswordEncoded,
    private val userDataErasers: List<UserDataEraser>,
    private val accessTokenRevocation: AccessTokenRevocation
) {

    private val log = LoggerFactory.getLogger(AccountDeletionService::class.java)

    /** From the app: the authenticated user confirms with their password. */
    @Transactional
    fun deleteOwnAccount(userId: UserId, password: String) {
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException()
        delete(user = user, password = password)
    }

    /** From the web page, without the app: email and password. */
    @Transactional
    fun deleteAccountWithCredentials(email: String, password: String) {
        val user = userRepository.findByEmail(email = email.trim()) ?: throw InvalidCredentialsException()
        delete(user = user, password = password)
    }

    private fun delete(user: UserEntity, password: String) {
        if (!passwordEncoded.matches(rawPassword = password, hashedPassword = user.hashedPassword)) throw InvalidCredentialsException()
        val userId = requireNotNull(user.id)

        userDataErasers.forEach { it.eraseUserData(userId = userId) }
        refreshTokenRepository.deleteByUserId(userId = userId)
        emailVerificationTokenRepository.deleteAllByUser(user = user)
        passwordResetTokenRepository.deleteAllByUser(user = user)
        userRepository.delete(user)
        userRepository.flush()

        accessTokenRevocation.revokeAllAfterCommit(userId = userId)
        log.info("[AccountDeletion] Account of user={} deleted", userId)
    }
}
