package com.kikepb.squadfy.api.websocket

import com.fasterxml.jackson.databind.JsonMappingException
import com.kikepb.squadfy.api.dto.websocket.*
import com.kikepb.squadfy.api.mappers.toChatMessageDto
import com.kikepb.squadfy.domain.type.ChatId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.service.ChatMessageService
import com.kikepb.squadfy.service.ChatService
import com.kikepb.squadfy.service.JwtService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.mutableSetOf
import kotlin.concurrent.read
import kotlin.concurrent.write

class ChatWebSocketHandler(
    private val chatService: ChatService,
    private val chatMessageService: ChatMessageService,
    private val objectMapper: ObjectMapper,
    private val jwtService: JwtService
): TextWebSocketHandler() {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val connectionLock = ReentrantReadWriteLock()
    private val sessions = ConcurrentHashMap<String, UserSession>()
    private val userToSessions = ConcurrentHashMap<UserId, MutableSet<String>>()
    private val userChatIds = ConcurrentHashMap<UserId, MutableSet<ChatId>>()
    private val chatToSessions = ConcurrentHashMap<ChatId, MutableSet<String>>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val authHeader = session
            .handshakeHeaders
            .getFirst(HttpHeaders.AUTHORIZATION)
            ?: run {
                logger.warn("Session ${session.id} was closed due to missing Authorization header")
                session.close(CloseStatus.SERVER_ERROR.withReason("Authentication failed"))
                return
            }

        val userId = jwtService.getUserIdFromToken(token = authHeader)

        val userSession = UserSession(
            userId = userId,
            session = session
        )

        connectionLock.write {
            sessions[session.id] = userSession

            userToSessions.compute(userId) { _, existingSessions ->
                (existingSessions ?: mutableSetOf()).apply {
                    add(session.id)
                }
            }

            val chatIds = userChatIds.computeIfAbsent(userId) {
                val chatIds = chatService.findChatByUser(userId = userId).map { it.id }
                ConcurrentHashMap.newKeySet<ChatId>().apply {
                    addAll(chatIds)
                }
            }

            chatIds.forEach { chatId ->
                chatToSessions.compute(chatId) { _, sessions ->
                    (sessions ?: mutableSetOf()).apply {
                        add(session.id)
                    }
                }
            }
        }

        logger.info("Websocket connection established for user $userId")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        logger.debug("Received message ${message.payload}")

        val userSession = connectionLock.read {
            sessions[session.id] ?: return
        }

        try {
            val webSocketMessage = objectMapper.readValue(
                message.payload,
                IncomingWebSocketMessage::class.java
            )

            when (webSocketMessage.type) {
                IncomingWebSocketMessageType.NEW_MESSAGE -> {
                    val senderMessageDto = objectMapper.readValue(
                        webSocketMessage.payload,
                        SendMessageDto::class.java
                    )

                    handleSendMessage(sendMessageDto = senderMessageDto, senderId = userSession.userId)
                }
            }

        } catch (e: JsonMappingException) {
            logger.warn("Couldn't parse message ${message.payload}", e)
            sendError(
                session = userSession.session,
                error = WebSocketErrorDto(
                    code = "INVALID_JSON",
                    message = "Incoming JSON or UUID is invalid"
                )
            )
        }
    }

    private fun sendError(session: WebSocketSession, error: WebSocketErrorDto) {
        val webSocketMessage = objectMapper.writeValueAsString(
            OutgoingWebSocketMessage(
                type = OutgoingWebSocketMessageType.ERROR,
                payload = objectMapper.writeValueAsString(error)
            )
        )

        try {
            session.sendMessage(TextMessage(webSocketMessage))
        } catch (e: Exception) {
            logger.warn("Couldn't send error message")
        }
    }

    private fun broadcastToChat(chatId: ChatId, message: OutgoingWebSocketMessage) {
        val chatSessions = connectionLock.read {
            chatToSessions[chatId]?.toList() ?: emptyList()
        }

        chatSessions.forEach { sessionId ->
            val userSession = connectionLock.read {
                sessions[sessionId]
            } ?: return@forEach

            sendToUser(userId = userSession.userId, message = message)
        }
    }

    private fun handleSendMessage(sendMessageDto: SendMessageDto, senderId: UserId) {
        val userChatIds = connectionLock.read { userChatIds[senderId] } ?: return

        if (sendMessageDto.chatId !in userChatIds) return

        val savedMessage = chatMessageService.sendMessage(
            chatId = sendMessageDto.chatId,
            senderId = senderId,
            content = sendMessageDto.content,
            messageId = sendMessageDto.messageId
        )

        broadcastToChat(
            chatId = sendMessageDto.chatId,
            message = OutgoingWebSocketMessage(
                type = OutgoingWebSocketMessageType.NEW_MESSAGE,
                payload = objectMapper.writeValueAsString(savedMessage.toChatMessageDto())
            )
        )
    }

    private fun sendToUser(userId: UserId, message: OutgoingWebSocketMessage) {
        val userSessions = connectionLock.read {
            userToSessions[userId] ?: emptySet()
        }

        userSessions.forEach { sessionId ->
            val userSession = connectionLock.read {
                sessions[sessionId] ?: return@forEach
            }

            if (userSession.session.isOpen) {
                try {
                    val messageJson = objectMapper.writeValueAsString(message)
                    userSession.session.sendMessage(TextMessage(messageJson))
                    logger.debug("Sent message to user {}: {}", userId, messageJson)
                } catch (e: Exception) {
                    logger.error("Error while sending message to $userId", e)
                }
            }
        }
    }

    private data class UserSession(
        val userId: UserId,
        val session: WebSocketSession
    )
}