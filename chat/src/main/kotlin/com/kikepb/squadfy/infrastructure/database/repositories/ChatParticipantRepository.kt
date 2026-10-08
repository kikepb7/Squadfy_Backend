package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ChatParticipantEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ChatParticipantRepository: JpaRepository<ChatParticipantEntity, UserId> {

    fun findByUserIdIn(userIds: Set<UserId>): Set<ChatParticipantEntity>

    @Query("""
       SELECT p
       FROM ChatParticipantEntity p
       WHERE LOWER(p.username) = :query OR LOWER(p.email) = :query
    """)
    fun findByEmailOrUsername(query: String): ChatParticipantEntity?

    /** Usernames containing [pattern] (a LIKE pattern escaped with `!`) or the exact [email]; prefix matches first. */
    @Query("""
       SELECT p
       FROM ChatParticipantEntity p
       WHERE p.userId <> :excludedUserId
       AND (LOWER(p.username) LIKE :pattern ESCAPE '!' OR LOWER(p.email) = :email)
       ORDER BY CASE WHEN LOWER(p.username) LIKE :prefixPattern ESCAPE '!' THEN 0 ELSE 1 END, LOWER(p.username)
    """)
    fun search(pattern: String, prefixPattern: String, email: String, excludedUserId: UserId, pageable: Pageable): List<ChatParticipantEntity>
}