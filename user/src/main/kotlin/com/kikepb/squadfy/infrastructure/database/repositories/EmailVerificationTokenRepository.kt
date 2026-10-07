package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.infrastructure.database.entities.EmailVerificationTokenEntity
import com.kikepb.squadfy.infrastructure.database.entities.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface EmailVerificationTokenRepository: JpaRepository<EmailVerificationTokenEntity, Long> {
    fun findByToken(token: String): EmailVerificationTokenEntity?
    fun deleteByExpiresAtLessThan(now: Instant)
    fun deleteAllByUser(user: UserEntity)

    @Modifying
    @Query("""
        UPDATE EmailVerificationTokenEntity e
        SET e.usedAt = CURRENT_TIMESTAMP
        WHERE e.user = :user
    """)
    fun invalidateActiveTokensForUser(user: UserEntity)
}