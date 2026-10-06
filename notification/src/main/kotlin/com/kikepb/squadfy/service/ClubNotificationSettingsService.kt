package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.club.ClubMembershipProvider
import com.kikepb.squadfy.domain.exception.ForbiddenException
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ClubNotificationSettingEntity
import com.kikepb.squadfy.infrastructure.database.repositories.ClubNotificationSettingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Per user and club mute preference (spec 005 RN-7). */
@Service
class ClubNotificationSettingsService(
    private val clubNotificationSettingRepository: ClubNotificationSettingRepository,
    private val clubMembershipProvider: ClubMembershipProvider
) {

    fun isMuted(clubId: ClubId, userId: UserId): Boolean {
        requireMember(clubId = clubId, userId = userId)
        return clubNotificationSettingRepository.findByUserIdAndClubId(userId = userId, clubId = clubId)?.muted ?: false
    }

    @Transactional
    fun setMuted(clubId: ClubId, userId: UserId, muted: Boolean): Boolean {
        requireMember(clubId = clubId, userId = userId)
        val setting = clubNotificationSettingRepository.findByUserIdAndClubId(userId = userId, clubId = clubId)
            ?.apply { this.muted = muted }
            ?: ClubNotificationSettingEntity(userId = userId, clubId = clubId, muted = muted)
        clubNotificationSettingRepository.saveAndFlush(setting)
        return muted
    }

    fun mutedUsers(clubId: ClubId, userIds: Collection<UserId>): Set<UserId> {
        if (userIds.isEmpty()) return emptySet()
        return clubNotificationSettingRepository.findAllByClubIdAndMutedIsTrueAndUserIdIn(clubId = clubId, userIds = userIds)
            .map { it.userId }
            .toSet()
    }

    private fun requireMember(clubId: ClubId, userId: UserId) {
        clubMembershipProvider.findMembership(clubId = clubId, userId = userId) ?: throw ForbiddenException()
    }
}
