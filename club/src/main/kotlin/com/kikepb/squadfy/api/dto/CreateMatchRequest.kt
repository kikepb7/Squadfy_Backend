package com.kikepb.squadfy.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class CreateMatchRequest(
    @JsonProperty("scheduledAt")
    val scheduledAt: Instant? = null,
    @JsonProperty("signupOpensAt")
    val signupOpensAt: Instant? = null,
    @JsonProperty("signupClosesAt")
    val signupClosesAt: Instant? = null
)
