package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.club.ClubMembershipProvider
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.domain.user.UserDataEraser
import com.kikepb.squadfy.infrastructure.database.repositories.MemberAbsenceRepository
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Account deletion in the match module (spec 010 RN-A3): absences are personal and are deleted.
 * Ratings, stats and team history stay with the anonymized membership; leaving the open
 * announcements happens through `ClubEvent.MemberLeft`.
 */
@Service
@Order(2)
class MatchUserDataEraser(
    private val clubMembershipProvider: ClubMembershipProvider,
    private val memberAbsenceRepository: MemberAbsenceRepository
) : UserDataEraser {

    @Transactional
    override fun eraseUserData(userId: UserId) {
        val memberIds = clubMembershipProvider.findMemberIdsOfUser(userId = userId)
        if (memberIds.isNotEmpty()) memberAbsenceRepository.deleteAllByClubMemberIdIn(clubMemberIds = memberIds)
    }
}
