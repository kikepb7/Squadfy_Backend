package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.ClubParticipantNotFoundException
import com.kikepb.squadfy.domain.model.ClubParticipantModel
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.domain.user.UserDirectory
import com.kikepb.squadfy.infrastructure.database.mappers.toClubParticipantEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toClubParticipantModel
import com.kikepb.squadfy.infrastructure.database.repositories.ClubParticipantRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ClubParticipantService(
    private val clubParticipantRepository: ClubParticipantRepository,
    private val userDirectory: UserDirectory
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

        val user = userDirectory.findUser(userId = userId)
            ?: throw ClubParticipantNotFoundException(userId = userId)

        return createClubParticipant(
            clubParticipantModel = ClubParticipantModel(userId = user.userId, username = user.username, email = user.email)
        )
    }
}
