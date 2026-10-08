package com.kikepb.squadfy

import com.kikepb.squadfy.testing.InfrastructureTestContainersConfiguration
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 010 BE-GAP-1: account deletion from the app and from the web page. */
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = [
        "jwt.secret=c3F1YWRmeS10ZXN0LXNlY3JldC1zcXVhZGZ5LXRlc3Qtc2VjcmV0LTEyMzQ1Ng==",
        "firebase.enabled=false"
    ]
)
@Import(InfrastructureTestContainersConfiguration::class)
class AccountDeletionIntegrationTest : ApiIntegrationTestSupport() {

    private val timeout: Duration = Duration.ofSeconds(20)

    private fun count(sql: String, vararg args: Any): Int = jdbcTemplate.queryForObject(sql, Int::class.java, *args)!!

    private fun join(code: String, token: String) =
        assertEquals(200, call("POST", "/api/v1/clubs/join", token, """{"invitationCode":"$code"}""").status)

    private fun memberIdOf(clubId: String, userId: UUID, token: String): String =
        call("GET", "/api/v1/clubs/$clubId/members", token).json.single { it["userId"].asText() == userId.toString() }["id"].asText()

    @Test
    fun `CA-1 to CA-5 deleting the account erases personal data and keeps the clubs working`() {
        val deleted = registerUser()
        val admin = registerUser()
        val olderPlayer = registerUser()

        // Club X: the oldest member is a player, but the admin inherits it (CA-4)
        val clubX = call("POST", "/api/v1/clubs", deleted.accessToken, """{"name":"Inherited FC"}""").json
        val clubXId = clubX["id"].asText()
        join(clubX["invitationCode"].asText(), olderPlayer.accessToken)
        join(clubX["invitationCode"].asText(), admin.accessToken)
        val adminMemberId = memberIdOf(clubXId, admin.userId, deleted.accessToken)
        call("PATCH", "/api/v1/clubs/$clubXId/members/$adminMemberId/role", deleted.accessToken, """{"role":"ADMIN"}""")
        val deletedMemberId = memberIdOf(clubXId, deleted.userId, deleted.accessToken)

        // Full announcement with a waitlist (CA-3)
        call("POST", "/api/v1/clubs/$clubXId/schedule", deleted.accessToken, """{"matchDayOfWeek":"THURSDAY","matchTime":"20:00:00","format":"FIVE_A_SIDE"}""")
        val announcementId = call("GET", "/api/v1/clubs/$clubXId/announcements/current", deleted.accessToken).json["announcement"]["id"].asText()
        val fillers = List(8) { newUser().second }
        fillers.forEach { join(clubX["invitationCode"].asText(), it) }
        (listOf(deleted.accessToken, admin.accessToken) + fillers).forEach {
            assertEquals(200, call("POST", "/api/v1/announcements/$announcementId/enrollment", it).status)
        }
        val waiting = olderPlayer
        call("POST", "/api/v1/announcements/$announcementId/enrollment", waiting.accessToken)
        assertEquals(1, call("GET", "/api/v1/announcements/$announcementId", admin.accessToken).json["waitlistCount"].asInt())
        call("POST", "/api/v1/clubs/$clubXId/members/me/absences", deleted.accessToken, """{"fromDate":"2030-01-01","toDate":"2030-01-05"}""")
        call("PUT", "/api/v1/clubs/$clubXId/notification-settings", deleted.accessToken, """{"muted":true}""")

        // Club Y: the user is its only member, so it disappears with its match data (CA-4)
        val clubYId = call("POST", "/api/v1/clubs", deleted.accessToken, """{"name":"Lonely FC"}""").json["id"].asText()
        call("POST", "/api/v1/clubs/$clubYId/schedule", deleted.accessToken, """{"matchDayOfWeek":"FRIDAY","matchTime":"20:00:00","format":"FIVE_A_SIDE"}""")
        assertEquals(1, count("SELECT count(*) FROM match_service.matches WHERE club_id = ?", UUID.fromString(clubYId)))

        // Chat created by the user with two other participants, with a message of each (CA-5)
        val chat = await().atMost(timeout).until(
            { call("POST", "/api/v1/chats", deleted.accessToken, """{"otherUserIds":["${admin.userId}","${olderPlayer.userId}"]}""") },
            { it.status == 201 }
        )
        val chatId = UUID.fromString(chat.json["id"].asText())
        listOf(deleted.userId to "bye", admin.userId to "see you").forEach { (sender, content) ->
            jdbcTemplate.update(
                "INSERT INTO chat_service.chat_messages (id, chat_id, sender_id, content, created_at) VALUES (?, ?, ?, ?, now())",
                UUID.randomUUID(), chatId, sender, content
            )
        }
        jdbcTemplate.update(
            "INSERT INTO notification_service.device_token (user_id, token, platform, created_at) VALUES (?, ?, 'ANDROID', now())",
            deleted.userId, "token-${deleted.userId}"
        )

        // CA-1: a wrong password deletes nothing
        assertEquals(401, call("DELETE", "/api/v1/me", deleted.accessToken, """{"password":"wrong-password1"}""").status)
        assertEquals(400, call("DELETE", "/api/v1/me", deleted.accessToken, """{"password":""}""").status)
        assertEquals(200, call("GET", "/api/v1/me", deleted.accessToken).status)

        assertEquals(204, call("DELETE", "/api/v1/me", deleted.accessToken, """{"password":"$TEST_PASSWORD"}""").status)

        // CA-2: sessions, login and personal data are gone; the email is free again
        assertEquals(401, call("GET", "/api/v1/me", deleted.accessToken).status)
        assertEquals(401, call("POST", "/api/v1/auth/login", body = """{"email":"${deleted.email}","password":"$TEST_PASSWORD"}""").status)
        assertEquals(401, call("POST", "/api/v1/auth/refresh", body = """{"refreshToken":"${deleted.refreshToken}"}""").status)
        assertEquals(0, count("SELECT count(*) FROM notification_service.device_token WHERE user_id = ?", deleted.userId))
        assertEquals(0, count("SELECT count(*) FROM notification_service.club_notification_settings WHERE user_id = ?", deleted.userId))
        assertEquals(0, count("SELECT count(*) FROM match_service.member_absences WHERE club_member_id = ?", UUID.fromString(deletedMemberId)))
        assertEquals(0, count("SELECT count(*) FROM chat_service.chat_participants WHERE user_id = ?", deleted.userId))
        val registeredAgain = call(
            "POST", "/api/v1/auth/register",
            body = """{"email":"${deleted.email}","username":"again${UUID.randomUUID().toString().take(6)}","password":"$TEST_PASSWORD"}"""
        )
        assertEquals(200, registeredAgain.status, registeredAgain.body)

        // CA-3: the membership stays anonymized, the user is out of the club and the waitlist moves up
        val anonymousName = jdbcTemplate.queryForObject("SELECT username FROM club_service.club_participants WHERE user_id = ?", String::class.java, deleted.userId)
        assertTrue(anonymousName!!.startsWith("Usuario eliminado"), anonymousName)
        assertEquals(1, count("SELECT count(*) FROM club_service.club_members WHERE id = ? AND left_at IS NOT NULL", UUID.fromString(deletedMemberId)))
        val members = call("GET", "/api/v1/clubs/$clubXId/members", admin.accessToken).json
        assertTrue(members.none { it["userId"].asText() == deleted.userId.toString() })
        await().atMost(timeout).untilAsserted {
            val announcement = call("GET", "/api/v1/announcements/$announcementId", admin.accessToken).json
            assertEquals(0, announcement["waitlistCount"].asInt())
            assertTrue(announcement["entries"].any { it["clubMemberId"].asText() == memberIdOf(clubXId, waiting.userId, admin.accessToken) })
        }

        // CA-4: the admin owns club X; club Y and its match data are gone
        assertEquals(admin.userId.toString(), call("GET", "/api/v1/clubs/$clubXId", admin.accessToken).json["ownerId"].asText())
        assertEquals("OWNER", members.single { it["userId"].asText() == admin.userId.toString() }["role"].asText())
        assertEquals(0, count("SELECT count(*) FROM club_service.clubs WHERE id = ?", UUID.fromString(clubYId)))
        await().atMost(timeout).untilAsserted {
            assertEquals(0, count("SELECT count(*) FROM match_service.matches WHERE club_id = ?", UUID.fromString(clubYId)))
            assertEquals(0, count("SELECT count(*) FROM match_service.club_match_schedules WHERE club_id = ?", UUID.fromString(clubYId)))
        }

        // CA-5: the chat goes on with the other two, without the deleted user's messages
        val chatAfter = call("GET", "/api/v1/chats/$chatId", admin.accessToken)
        assertEquals(200, chatAfter.status, chatAfter.body)
        assertEquals(setOf(admin.userId.toString(), olderPlayer.userId.toString()), chatAfter.json["participants"].map { it["userId"].asText() }.toSet())
        assertTrue(chatAfter.json["creator"]["userId"].asText() in setOf(admin.userId.toString(), olderPlayer.userId.toString()))
        val messages = call("GET", "/api/v1/chats/$chatId/messages", admin.accessToken).json
        assertEquals(listOf("see you"), messages.map { it["content"].asText() })
    }

    @Test
    fun `CA-6 the account can be deleted from the public web page`() {
        val page = call("GET", "/account/delete")
        assertEquals(200, page.status)
        assertTrue("Borrar tu cuenta de Squadfy" in page.body)
        assertTrue("/api/v1/auth/delete-account" in page.body)

        val user = registerUser()
        val wrong = call("POST", "/api/v1/auth/delete-account", body = """{"email":"${user.email}","password":"not-my-password1"}""")
        assertEquals(401, wrong.status)
        assertEquals("INVALID_CREDENTIALS", wrong.json["code"].asText())
        assertEquals(401, call("POST", "/api/v1/auth/delete-account", body = """{"email":"nobody@squadfy.test","password":"$TEST_PASSWORD"}""").status)
        assertEquals(200, call("GET", "/api/v1/me", user.accessToken).status)

        assertEquals(204, call("POST", "/api/v1/auth/delete-account", body = """{"email":"${user.email}","password":"$TEST_PASSWORD"}""").status)
        assertEquals(401, call("POST", "/api/v1/auth/login", body = """{"email":"${user.email}","password":"$TEST_PASSWORD"}""").status)
        assertEquals(401, call("GET", "/api/v1/me", user.accessToken).status)
    }
}
