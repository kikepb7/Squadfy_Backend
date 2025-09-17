package com.kikepb.squadfy.infrastructure.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class PasswordEncoded {

    private val bcrypt = BCryptPasswordEncoder()

    fun encode(rawPassword: String): String = requireNotNull(bcrypt.encode(rawPassword))
    fun matches(rawPassword: String, hashedPassword: String): Boolean {
        return bcrypt.matches(rawPassword, hashedPassword)
    }
}