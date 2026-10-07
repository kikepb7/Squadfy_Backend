package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.EntryStatus
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchAnnouncementEntryId
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.infrastructure.database.entities.MatchAnnouncementEntryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MatchAnnouncementEntryRepository : JpaRepository<MatchAnnouncementEntryEntity, MatchAnnouncementEntryId> {

    fun findAllByMatchAnnouncementIdOrderByEnrolledAtAsc(matchAnnouncementId: MatchAnnouncementId): List<MatchAnnouncementEntryEntity>

    fun findAllByMatchAnnouncementIdIn(matchAnnouncementIds: Collection<MatchAnnouncementId>): List<MatchAnnouncementEntryEntity>

    fun findByMatchAnnouncementIdAndClubMemberId(
        matchAnnouncementId: MatchAnnouncementId,
        clubMemberId: ClubMemberId
    ): MatchAnnouncementEntryEntity?

    fun existsByMatchAnnouncementIdAndClubMemberId(
        matchAnnouncementId: MatchAnnouncementId,
        clubMemberId: ClubMemberId
    ): Boolean

    fun countByMatchAnnouncementIdAndInvitedByMemberId(matchAnnouncementId: MatchAnnouncementId, invitedByMemberId: ClubMemberId): Int

    fun findAllByMatchAnnouncementIdAndInvitedByMemberId(
        matchAnnouncementId: MatchAnnouncementId,
        invitedByMemberId: ClubMemberId
    ): List<MatchAnnouncementEntryEntity>

    fun countByMatchAnnouncementIdAndStatus(matchAnnouncementId: MatchAnnouncementId, status: EntryStatus): Int

    fun findFirstByMatchAnnouncementIdAndStatusOrderByEnrolledAtAsc(
        matchAnnouncementId: MatchAnnouncementId,
        status: EntryStatus
    ): MatchAnnouncementEntryEntity?
}
