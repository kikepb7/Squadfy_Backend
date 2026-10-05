package com.kikepb.squadfy.infrastructure.database.entities

import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.EntryStatus
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
    name = "callup_entries",
    schema = "match_service",
    indexes = [
        Index(name = "idx_callup_entries_callup_id", columnList = "callup_id"),
        Index(name = "idx_callup_entries_club_member_id", columnList = "club_member_id"),
        Index(
            name = "idx_callup_entries_callup_member",
            columnList = "callup_id,club_member_id",
            unique = true
        )
    ]
)
class MatchAnnouncementEntryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: MatchAnnouncementEntryId? = null,
    @Column(name = "callup_id", nullable = false, updatable = false)
    var matchAnnouncementId: MatchAnnouncementId,
    @Column(name = "club_member_id", nullable = false, updatable = false)
    var clubMemberId: ClubMemberId,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(16) not null default 'CONFIRMED'")
    var status: EntryStatus = EntryStatus.CONFIRMED,
    @CreationTimestamp
    var enrolledAt: Instant = Instant.now()
)
