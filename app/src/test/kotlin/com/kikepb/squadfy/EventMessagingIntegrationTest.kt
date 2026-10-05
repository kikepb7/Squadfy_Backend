package com.kikepb.squadfy

import com.kikepb.squadfy.domain.events.user.UserEvent
import com.kikepb.squadfy.infrastructure.message_queue.EventPublisher
import com.kikepb.squadfy.testing.InfrastructureTestContainersConfiguration
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals

/**
 * Events travel through RabbitMQ between modules: user events published by `user` must reach the
 * listeners of `club` (created) and `chat` (verified). Regression: the message converter rejected
 * every event type because our packages were not trusted.
 */
@SpringBootTest(
    properties = [
        "jwt.secret=c3F1YWRmeS10ZXN0LXNlY3JldC1zcXVhZGZ5LXRlc3Qtc2VjcmV0LTEyMzQ1Ng==",
        "firebase.enabled=false"
    ]
)
@Import(InfrastructureTestContainersConfiguration::class)
class EventMessagingIntegrationTest {

    @Autowired lateinit var eventPublisher: EventPublisher
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `user events reach the club and chat modules`() {
        val userId = UUID.randomUUID()
        val email = "$userId@squadfy.test"
        val username = "user-${userId.toString().take(8)}"

        eventPublisher.publish(
            UserEvent.Created(userId = userId, email = email, username = username, verificationToken = "token")
        )
        await().atMost(Duration.ofSeconds(15)).untilAsserted {
            assertEquals(1, count("club_service.club_participants", userId))
        }

        eventPublisher.publish(UserEvent.Verified(userId = userId, email = email, username = username))
        await().atMost(Duration.ofSeconds(15)).untilAsserted {
            assertEquals(1, count("chat_service.chat_participants", userId))
        }
    }

    private fun count(table: String, userId: UUID): Int =
        jdbcTemplate.queryForObject("SELECT count(*) FROM $table WHERE user_id = ?", Int::class.java, userId)!!
}
