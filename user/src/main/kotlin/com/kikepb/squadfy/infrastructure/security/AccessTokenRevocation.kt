package com.kikepb.squadfy.infrastructure.security

import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.service.JwtService
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration

/**
 * Access tokens are stateless, so those of a deleted account are rejected through a Redis mark that
 * lives as long as an access token (spec 010 RN-A4). No database query per request.
 */
@Component
class AccessTokenRevocation(
    private val redisTemplate: StringRedisTemplate,
    private val jwtService: JwtService
) {

    private val log = LoggerFactory.getLogger(AccessTokenRevocation::class.java)

    /** Marks the user once the surrounding transaction commits (immediately without one). */
    fun revokeAllAfterCommit(userId: UserId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            revokeAll(userId)
            return
        }
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() = revokeAll(userId)
        })
    }

    /** Fails open if Redis is unavailable: the account is already gone and tokens expire soon. */
    fun isRevoked(userId: UserId): Boolean =
        try {
            redisTemplate.hasKey(key(userId)) == true
        } catch (e: Exception) {
            log.warn("[Auth] Could not check revoked tokens of user={}: {}", userId, e.message)
            false
        }

    private fun revokeAll(userId: UserId) {
        redisTemplate.opsForValue().set(key(userId), "1", Duration.ofMillis(jwtService.accessTokenValidityMs))
    }

    private fun key(userId: UserId) = "$KEY_PREFIX:$userId"

    private companion object {
        const val KEY_PREFIX = "revoked_user"
    }
}
