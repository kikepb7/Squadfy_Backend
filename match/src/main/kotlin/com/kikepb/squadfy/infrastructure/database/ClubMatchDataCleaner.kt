package com.kikepb.squadfy.infrastructure.database

import com.kikepb.squadfy.domain.type.ClubId
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** Deletes everything the match module stores about a club that no longer exists (spec 010 RN-A6). */
@Component
class ClubMatchDataCleaner(
    private val entityManager: EntityManager
) {

    private val log = LoggerFactory.getLogger(ClubMatchDataCleaner::class.java)

    @Transactional
    fun deleteClubData(clubId: ClubId) {
        val deleted = STATEMENTS.sumOf { sql ->
            entityManager.createNativeQuery(sql).setParameter("clubId", clubId).executeUpdate()
        }
        log.info("[MatchCleanup] Deleted {} rows of club={}", deleted, clubId)
    }

    private companion object {
        const val MATCHES_OF_CLUB = "(SELECT id FROM match_service.matches WHERE club_id = :clubId)"

        /** Children first; the schema has no foreign keys between these tables. */
        val STATEMENTS = listOf(
            "DELETE FROM match_service.match_announcement_entries WHERE match_announcement_id IN " +
                "(SELECT id FROM match_service.match_announcements WHERE club_id = :clubId)",
            "DELETE FROM match_service.match_announcements WHERE club_id = :clubId",
            "DELETE FROM match_service.match_events WHERE match_id IN $MATCHES_OF_CLUB",
            "DELETE FROM match_service.match_team_players WHERE match_id IN $MATCHES_OF_CLUB",
            "DELETE FROM match_service.player_rating_changes WHERE match_id IN $MATCHES_OF_CLUB",
            "DELETE FROM match_service.matches WHERE club_id = :clubId",
            "DELETE FROM match_service.player_ratings WHERE club_id = :clubId",
            "DELETE FROM match_service.schedule_exceptions WHERE club_id = :clubId",
            "DELETE FROM match_service.member_absences WHERE club_id = :clubId",
            "DELETE FROM match_service.club_match_schedules WHERE club_id = :clubId"
        )
    }
}
