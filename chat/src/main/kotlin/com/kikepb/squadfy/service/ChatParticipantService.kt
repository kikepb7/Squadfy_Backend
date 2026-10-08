package com.kikepb.squadfy.service

import com.kikepb.squadfy.api.mappers.toChatParticipantDto
import com.kikepb.squadfy.domain.exception.InvalidSearchQueryException
import com.kikepb.squadfy.domain.model.ChatParticipantModel
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.domain.user.ProfilePictureProvider
import com.kikepb.squadfy.infrastructure.database.mappers.toChatParticipantEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toChatParticipantModel
import com.kikepb.squadfy.infrastructure.database.repositories.ChatParticipantRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ChatParticipantService(
    private val chatParticipantRepository: ChatParticipantRepository
) : ProfilePictureProvider {

    override fun findProfilePictures(userIds: Collection<UserId>): Map<UserId, String> =
        if (userIds.isEmpty()) emptyMap()
        else chatParticipantRepository.findByUserIdIn(userIds = userIds.toSet())
            .mapNotNull { participant -> participant.profilePictureUrl?.let { participant.userId to it } }
            .toMap()


    fun createChatParticipant(chatParticipant: ChatParticipantModel) {
        chatParticipantRepository.save(
            chatParticipant.toChatParticipantEntity()
        )
    }

    fun findChatParticipantById(userId: UserId): ChatParticipantModel? =
        chatParticipantRepository.findByIdOrNull(userId)?.toChatParticipantModel()

    /** Spec 012 RN-D: usernames containing [query] or the exact email, without the requester. */
    fun searchParticipants(query: String, requestUserId: UserId): List<ChatParticipantModel> {
        val normalized = query.trim().lowercase()
        if (normalized.length < MIN_SEARCH_LENGTH) throw InvalidSearchQueryException(MIN_SEARCH_LENGTH)
        val escaped = normalized.replace("!", "!!").replace("%", "!%").replace("_", "!_")

        return chatParticipantRepository.search(
            pattern = "%$escaped%",
            prefixPattern = "$escaped%",
            email = normalized,
            excludedUserId = requestUserId,
            pageable = PageRequest.of(0, MAX_SEARCH_RESULTS)
        ).map { it.toChatParticipantModel() }
    }

    fun findChatParticipantByEmailOrUsername(query: String): ChatParticipantModel? {
        val normalizedQuery = query.lowercase().trim()
        return chatParticipantRepository.findByEmailOrUsername(query = normalizedQuery)?.toChatParticipantModel()
    }

    private companion object {
        const val MIN_SEARCH_LENGTH = 2
        const val MAX_SEARCH_RESULTS = 20
    }
}
