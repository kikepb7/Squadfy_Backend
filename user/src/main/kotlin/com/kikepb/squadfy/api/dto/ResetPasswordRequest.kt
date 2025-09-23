package com.kikepb.squadfy.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.kikepb.squadfy.api.util.Password
import jakarta.validation.constraints.NotBlank

data class ResetPasswordRequest(
    @field:NotBlank
    @JsonProperty("token")
    val token: String,
    @field:Password
    @JsonProperty("newPassword")
    val newPassword: String
)
