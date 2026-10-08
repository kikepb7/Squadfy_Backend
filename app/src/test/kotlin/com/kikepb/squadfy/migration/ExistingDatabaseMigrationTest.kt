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
 * A database created by Hibernate's ddl-auto before Flyway existed (like the current development
 * database) is baselined at V1, upgraded by the following migrations and keeps its data.
 */
@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=validate"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExistingDatabaseMigrationTest {

    companion object {
        private val ANNOUNCEMENT_ID: UUID = UUID.randomUUID()

        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:16-alpine").also {
            it.start()
            seedPreFlywaySchema(it)
        }

        private fun seedPreFlywaySchema(container: PostgreSQLContainer) {
            DriverManager.getConnection(container.jdbcUrl, container.username, container.password).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(ClassPathResource("db/migration/V1__baseline.sql").getContentAsString(Charsets.UTF_8))
                    statement.execute(
                        "ALTER TABLE match_service.club_match_schedules ADD COLUMN callup_open_days_before_match integer NOT NULL DEFAULT 6"
                    )
                    statement.execute(
                        """
                        INSERT INTO match_service.callups (id, match_id, club_id, max_players, opens_at, closes_at, status, created_at, updated_at)
                        VALUES ('$ANNOUNCEMENT_ID', gen_random_uuid(), gen_random_uuid(), 14, now(), now(), 'OPEN', now(), now())
                        """.trimIndent()
                    )
                    statement.execute(
                        """
                        INSERT INTO match_service.callup_entries (id, callup_id, club_member_id, status, enrolled_at)
                        VALUES (gen_random_uuid(), '$ANNOUNCEMENT_ID', gen_random_uuid(), 'CONFIRMED', now())
                        """.trimIndent()
                    )
                }
            }
        }
    }

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `database is baselined, upgraded and keeps its data`() {
        val history = jdbcTemplate.queryForList(
            "SELECT version, type FROM public.flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank"
        ).map { it["version"] to it["type"] }
        assertEquals(listOf<Pair<Any?, Any?>>("1" to "BASELINE", "2" to "SQL", "3" to "SQL", "4" to "SQL", "5" to "SQL", "6" to "SQL", "7" to "SQL", "8" to "SQL", "9" to "SQL"), history)

        val maxPlayers = jdbcTemplate.queryForObject(
            "SELECT max_players FROM match_service.match_announcements WHERE id = ?",
            Int::class.java,
            ANNOUNCEMENT_ID
        )
        assertEquals(14, maxPlayers)

        val entries = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM match_service.match_announcement_entries WHERE match_announcement_id = ?",
            Int::class.java,
            ANNOUNCEMENT_ID
        )
        assertEquals(1, entries)

        val legacyColumns = jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM information_schema.columns
            WHERE table_schema = 'match_service' AND column_name = 'callup_open_days_before_match'
            """.trimIndent(),
            Int::class.java
        )
        assertEquals(0, legacyColumns)
    }
}
