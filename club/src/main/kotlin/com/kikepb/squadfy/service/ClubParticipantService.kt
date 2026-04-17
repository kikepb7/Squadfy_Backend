package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.ClubParticipantNotFoundException
import com.kikepb.squadfy.domain.model.ClubParticipantModel
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.mappers.toClubParticipantEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toClubParticipantModel
import com.kikepb.squadfy.infrastructure.database.repositories.ClubParticipantRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ClubParticipantService(
    private val clubParticipantRepository: ClubParticipantRepository,
    private val jdbcTemplate: JdbcTemplate
) {

    fun createClubParticipant(clubParticipantModel: ClubParticipantModel): ClubParticipantModel =
        clubParticipantRepository
            .save(clubParticipantModel.toClubParticipantEntity())
            .toClubParticipantModel()

    fun findById(userId: UserId): ClubParticipantModel? =
        clubParticipantRepository.findByIdOrNull(id = userId)?.toClubParticipantModel()

    fun findByIds(userIds: Collection<UserId>): Map<UserId, ClubParticipantModel> =
        clubParticipantRepository.findAllByUserIdIn(userIds = userIds)
            .associate { it.userId to it.toClubParticipantModel() }

    fun ensureExists(userId: UserId): ClubParticipantModel {
        findById(userId = userId)?.let { return it }

        val userParticipant = findInUserService(userId = userId)
            ?: throw ClubParticipantNotFoundException(userId = userId)

        return createClubParticipant(clubParticipantModel = userParticipant)
    }

    private fun findInUserService(userId: UserId): ClubParticipantModel? {
        val sql = """
            SELECT id, username, email
            FROM user_service.users
            WHERE id = ?
        """.trimIndent()

        return jdbcTemplate.query(sql, { rs, _ ->
            ClubParticipantModel(
                userId = rs.getObject("id", java.util.UUID::class.java),
                username = rs.getString("username"),
                email = rs.getString("email"),
                profilePictureUrl = null
            )
        }, userId).firstOrNull()
    }
}
