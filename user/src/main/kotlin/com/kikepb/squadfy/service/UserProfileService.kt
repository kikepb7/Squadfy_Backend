package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.UserNotFoundException
import com.kikepb.squadfy.domain.model.UserModel
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.domain.user.UserDirectory
import com.kikepb.squadfy.domain.user.UserSummary
import com.kikepb.squadfy.infrastructure.database.mappers.toUser
import com.kikepb.squadfy.infrastructure.database.repositories.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class UserProfileService(
    private val userRepository: UserRepository
) : UserDirectory {

    fun getProfile(userId: UserId): UserModel =
        userRepository.findByIdOrNull(userId)?.toUser() ?: throw UserNotFoundException()

    override fun findUser(userId: UserId): UserSummary? =
        userRepository.findByIdOrNull(userId)?.let {
            UserSummary(userId = userId, username = it.username, email = it.email)
        }
}
