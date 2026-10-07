package com.kikepb.squadfy.api.dto

import jakarta.validation.constraints.NotBlank

/** `DELETE /me`: the password confirms the deletion (spec 010 RN-A1). */
data class DeleteAccountRequest(
    @field:NotBlank(message = "password is required")
    val password: String
)

/** `POST /auth/delete-account`: deletion from the web page, without the app. */
data class DeleteAccountWithCredentialsRequest(
    @field:NotBlank(message = "email is required")
    val email: String,
    @field:NotBlank(message = "password is required")
    val password: String
)
