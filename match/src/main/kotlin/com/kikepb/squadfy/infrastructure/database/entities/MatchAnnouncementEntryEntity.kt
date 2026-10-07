package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.EntryStatus
import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.ParticipantType
import com.kikepb.squadfy.domain.type.MatchAnnouncementEntryId
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.domain.type.ClubMemberId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@Entity
@Table(
    name = "match_announcement_entries",
    schema = "match_service",
    indexes = [
        Index(name = "idx_match_announcement_entries_announcement_id", columnList = "match_announcement_id"),
        Index(name = "idx_match_announcement_entries_club_member_id", columnList = "club_member_id"),
        Index(
            name = "idx_match_announcement_entries_announcement_member",
            columnList = "match_announcement_id,club_member_id",
            unique = true
        )
    ]
)
class MatchAnnouncementEntryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: MatchAnnouncementEntryId? = null,
    @Column(name = "match_announcement_id", nullable = false, updatable = false)
    var matchAnnouncementId: MatchAnnouncementId,
    /** Null for guests. */
    @Column(name = "club_member_id", updatable = false)
    var clubMemberId: ClubMemberId? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 16)
    var participantType: ParticipantType = ParticipantType.MEMBER,
    @Column(name = "guest_name", length = 80)
    var guestName: String? = null,
    @Column(name = "guest_position", length = 16)
    var guestPosition: String? = null,
    /** Member who added the guest (spec 008 RN-A1). */
    @Column(name = "invited_by_member_id", updatable = false)
    var invitedByMemberId: ClubMemberId? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: EntryStatus = EntryStatus.CONFIRMED,
    @CreationTimestamp
    var enrolledAt: Instant = Instant.now()
)
