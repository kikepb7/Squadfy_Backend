package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.MatchAnnouncementEntryId
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.infrastructure.database.entities.MatchAnnouncementEntryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MatchAnnouncementEntryRepository : JpaRepository<MatchAnnouncementEntryEntity, MatchAnnouncementEntryId> {

    fun findAllByMatchAnnouncementId(matchAnnouncementId: MatchAnnouncementId): List<MatchAnnouncementEntryEntity>

    fun findByMatchAnnouncementIdAndClubMemberId(
        matchAnnouncementId: MatchAnnouncementId,
        clubMemberId: ClubMemberId
    ): MatchAnnouncementEntryEntity?

    fun existsByMatchAnnouncementIdAndClubMemberId(
        matchAnnouncementId: MatchAnnouncementId,
        clubMemberId: ClubMemberId
    ): Boolean

    fun countByMatchAnnouncementId(matchAnnouncementId: MatchAnnouncementId): Int

    fun deleteByMatchAnnouncementIdAndClubMemberId(matchAnnouncementId: MatchAnnouncementId, clubMemberId: ClubMemberId)
}
