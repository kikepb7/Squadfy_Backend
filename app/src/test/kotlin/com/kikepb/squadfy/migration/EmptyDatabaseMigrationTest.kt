package com.kikepb.squadfy.migration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import com.kikepb.squadfy.testing.PostgresTestContainerConfiguration
import kotlin.test.assertEquals

/** Flyway builds the whole schema on an empty database and it matches the JPA entities (ddl-auto=validate). */
@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=validate"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestContainerConfiguration::class)
class EmptyDatabaseMigrationTest {

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `all migrations run and match the entities`() {
        val versions = jdbcTemplate.queryForList(
            "SELECT version FROM public.flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank",
            String::class.java
        )
        assertEquals(listOf("1", "2", "3", "4", "5"), versions)
    }
}
