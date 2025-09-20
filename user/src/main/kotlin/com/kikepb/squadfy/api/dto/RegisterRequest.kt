package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.api.util.Password
import jakarta.validation.constraints.Email
import org.hibernate.validator.constraints.Length

data class RegisterRequest(
    @field:Email(message = "Must be a valid email address")
    val email: String,
    @field:Length(min = 3, max = 20, message = "Username length must be between 3 and 20 characters")
    val username: String,
    @field:Password
    val password: String
)
