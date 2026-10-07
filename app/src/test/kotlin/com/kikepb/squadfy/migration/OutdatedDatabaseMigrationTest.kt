package com.kikepb.squadfy.migration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.assertEquals

/**
 * A database created by Hibernate's ddl-auto with code older than the V1 baseline (before the
 * match feature added schedule format/time zone, entry status and ratings), like the development
 * database: V7 must bring it to the current model with sensible values for existing rows.
 */
@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=validate"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OutdatedDatabaseMigrationTest {

    companion object {
        private val SCHEDULE_ID: UUID = UUID.randomUUID()
        private val ENTRY_ID: UUID = UUID.randomUUID()

        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:16-alpine").also {
            it.start()
            seedPreMatchFeatureSchema(it)
        }

        private fun seedPreMatchFeatureSchema(container: PostgreSQLContainer) {
            DriverManager.getConnection(container.jdbcUrl, container.username, container.password).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(ClassPathResource("db/migration/V1__baseline.sql").getContentAsString(Charsets.UTF_8))
                    statement.execute(
                        """
                        ALTER TABLE match_service.club_match_schedules DROP COLUMN format, DROP COLUMN time_zone;
                        ALTER TABLE match_service.club_match_schedules ADD COLUMN callup_open_days_before_match integer NOT NULL DEFAULT 6;
                        ALTER TABLE match_service.callup_entries DROP COLUMN status;
                        DROP TABLE match_service.player_ratings;
                        DROP TABLE match_service.player_rating_changes;
                        ALTER TABLE match_service.matches DROP CONSTRAINT idx_matches_club_scheduled_at;
                        """.trimIndent()
                    )
                    statement.execute(
                        """
                        INSERT INTO match_service.club_match_schedules (id, club_id, match_day_of_week, match_time, max_players, is_active, created_at, updated_at)
                        VALUES ('$SCHEDULE_ID', gen_random_uuid(), 'THURSDAY', '20:00', 14, true, now(), now())
                        """.trimIndent()
                    )
                    statement.execute(
                        """
                        INSERT INTO match_service.callups (id, match_id, club_id, max_players, opens_at, closes_at, status, created_at, updated_at)
                        VALUES (gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), 14, now(), now(), 'OPEN', now(), now())
                        """.trimIndent()
                    )
                    statement.execute(
                        """
                        INSERT INTO match_service.callup_entries (id, callup_id, club_member_id, enrolled_at)
                        SELECT '$ENTRY_ID', id, gen_random_uuid(), now() FROM match_service.callups LIMIT 1
                        """.trimIndent()
                    )
                }
            }
        }
    }

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `missing columns and tables are created and existing rows get sensible values`() {
        val schedule = jdbcTemplate.queryForMap(
            "SELECT format, time_zone, match_duration_minutes FROM match_service.club_match_schedules WHERE id = ?",
            SCHEDULE_ID
        )
        assertEquals("SEVEN_A_SIDE", schedule["format"])
        assertEquals("Europe/Madrid", schedule["time_zone"])
        assertEquals(60, schedule["match_duration_minutes"])

        assertEquals(
            "CONFIRMED",
            jdbcTemplate.queryForObject("SELECT status FROM match_service.match_announcement_entries WHERE id = ?", String::class.java, ENTRY_ID)
        )
        assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM match_service.player_ratings", Int::class.java))
        assertEquals(
            1,
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE schemaname = 'match_service' AND indexname = 'idx_matches_club_scheduled_at'",
                Int::class.java
            )
        )
    }
}
