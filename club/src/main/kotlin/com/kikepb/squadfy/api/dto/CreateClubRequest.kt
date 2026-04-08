package com.kikepb.squadfy.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreateClubRequest(
    @field:NotBlank(message = "Club name is required")
    @field:Size(max = 120, message = "Club name can have at most 120 characters")
    @JsonProperty("name")
    val name: String,
    @field:Size(max = 2000, message = "Club description can have at most 2000 characters")
    @JsonProperty("description")
    val description: String? = null,
    @field:Positive(message = "Max members must be a positive number")
    @JsonProperty("maxMembers")
    val maxMembers: Int? = null,
    @field:Size(max = 2048, message = "Club logo URL can have at most 2048 characters")
    @JsonProperty("clubLogoUrl")
    val clubLogoUrl: String? = null
)
