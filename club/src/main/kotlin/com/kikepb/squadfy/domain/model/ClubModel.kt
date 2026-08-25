package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.UserId
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

data class ClubModel(
    val id: ClubId,
    val name: String,
    val description: String?,
    val clubLogoUrl: String?,
    val ownerId: UserId,
    val invitationCode: String,
    val maxMembers: Int?,
    val membersCount: Int,
    val matchDayOfWeek: DayOfWeek?,
    val matchStartTime: LocalTime?,
    val matchEndTime: LocalTime?,
    val seasonStartMonth: Int,
    val seasonStartDay: Int,
    val drawTime: LocalTime,
    val createdAt: Instant,
    val updatedAt: Instant
)
