package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ClubNotificationSettingEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ClubNotificationSettingRepository : JpaRepository<ClubNotificationSettingEntity, UUID> {

    fun findByUserIdAndClubId(userId: UserId, clubId: ClubId): ClubNotificationSettingEntity?

    fun findAllByClubIdAndMutedIsTrueAndUserIdIn(clubId: ClubId, userIds: Collection<UserId>): List<ClubNotificationSettingEntity>
}
