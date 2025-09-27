package com.kikepb.squadfy.infrastructure.database.mappers

import com.kikepb.squadfy.domain.model.ChatMessageModel
import com.kikepb.squadfy.domain.model.ChatModel
import com.kikepb.squadfy.domain.model.ChatParticipantModel
import com.kikepb.squadfy.infrastructure.database.entities.ChatEntity
import com.kikepb.squadfy.infrastructure.database.entities.ChatParticipantEntity

fun ChatEntity.toChatModel(lastMessage: ChatMessageModel? = null): ChatModel {
    return ChatModel(
        id = requireNotNull(id),
        participants = participants.map {
            it.toChatParticipantModel()
        }.toSet(),
        creator = creator.toChatParticipantModel(),
        lastActivityAt = lastMessage?.createdAt ?: createdAt,
        createdAt = createdAt,
        lastMessage = lastMessage
    )
}

fun ChatParticipantEntity.toChatParticipantModel(): ChatParticipantModel {
    return ChatParticipantModel(
        userId = userId,
        username = username,
        email = email,
        profilePictureUrl = profilePictureUrl
    )
}