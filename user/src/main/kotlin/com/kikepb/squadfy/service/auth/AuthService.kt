package com.kikepb.squadfy.service.auth

import com.kikepb.squadfy.domain.exception.UserAlreadyExistsException
import com.kikepb.squadfy.domain.model.User
import com.kikepb.squadfy.infrastructure.database.entities.UserEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toUser
import com.kikepb.squadfy.infrastructure.database.repositories.UserRepository
import com.kikepb.squadfy.infrastructure.security.PasswordEncoded
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoded: PasswordEncoded
) {

    fun register(email: String, username: String, password: String): User {
        val user = userRepository.findByEmailOrUsername(
            email = email.trim(),
            username = username.trim()
        )

        if (user != null) throw UserAlreadyExistsException()

        val savedUser = userRepository.save(
            UserEntity(
                email = email.trim(),
                username = username.trim(),
                hashedPassword = passwordEncoded.encode(password)
            )
        ).toUser()

        return savedUser
    }
}