package com.kikepb.squadfy.api.mappers

import com.kikepb.squadfy.api.dto.ChatDto
import com.kikepb.squadfy.api.dto.ChatMessageDto
import com.kikepb.squadfy.api.dto.ChatParticipantDto
import com.kikepb.squadfy.domain.model.ChatMessageModel
import com.kikepb.squadfy.domain.model.ChatModel
import com.kikepb.squadfy.domain.model.ChatParticipantModel

fun ChatModel.toChatDto(): ChatDto {
    return ChatDto(
        id = id,
        participants = participants.map {
            it.toChatParticipantDto()
        },
        lastActivityAt = lastActivityAt,
        lastMessage = lastMessage?.toChatMessageDto(),
        creator = creator.toChatParticipantDto()
    )
}

fun ChatMessageModel.toChatMessageDto(): ChatMessageDto {
    return ChatMessageDto(
        id = id,
        chatId = chatId,
        content = content,
        createdAt = createdAt,
        senderId = sender.userId
    )
}

fun ChatParticipantModel.toChatParticipantDto(): ChatParticipantDto {
    return ChatParticipantDto(
        userId = userId,
        username = username,
        email = email,
        profilePictureUrl = profilePictureUrl
    )
}