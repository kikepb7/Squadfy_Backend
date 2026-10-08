package com.kikepb.squadfy

import com.kikepb.squadfy.infrastructure.storage.SupabaseStorageService
import com.kikepb.squadfy.testing.InfrastructureTestContainersConfiguration
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 012 over HTTP: club pictures, profile pictures in clubs, user search, error code and the message cache. */
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = [
        "jwt.secret=c3F1YWRmeS10ZXN0LXNlY3JldC1zcXVhZGZ5LXRlc3Qtc2VjcmV0LTEyMzQ1Ng==",
        "firebase.enabled=false"
    ]
)
@Import(InfrastructureTestContainersConfiguration::class)
class MvpCompletionIntegrationTest : ApiIntegrationTestSupport() {

    @LocalServerPort private var serverPort: Int = 0
    @MockitoBean lateinit var storageService: SupabaseStorageService

    private fun uploadClubPicture(clubId: String, token: String, mimeType: String): ApiResponse {
        val boundary = "squadfy-${UUID.randomUUID()}"
        val body = "--$boundary\r\nContent-Disposition: form-data; name=\"picture\"; filename=\"me.img\"\r\n" +
            "Content-Type: $mimeType\r\n\r\nfake-image-bytes\r\n--$boundary--\r\n"
        val request = HttpRequest.newBuilder(URI.create("http://localhost:$serverPort/api/v1/clubs/$clubId/members/me/picture"))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "multipart/form-data; boundary=$boundary")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
        return ApiResponse(response.statusCode(), response.body())
    }

    private fun myMember(clubId: String, user: RegisteredUser) =
        call("GET", "/api/v1/clubs/$clubId/members", user.accessToken).json.single { it["userId"].asText() == user.userId.toString() }

    @Test
    fun `CA-3 and CA-4 a member shows their club picture, otherwise their current profile picture`() {
        val (clubId, code, _) = newClub("Pictures FC")
        val player = registerUser()
        call("POST", "/api/v1/clubs/join", player.accessToken, """{"invitationCode":"$code"}""")
        val clubPicture = "http://localhost:54321/storage/v1/object/public/profile-pictures/club-members/a.png"
        `when`(storageService.uploadImage(anyString(), anyString(), any(ByteArray::class.java) ?: ByteArray(0), anyString()))
            .thenReturn(clubPicture)

        // CA-4: the profile picture set in the chat module reaches the club
        val profilePicture = "http://localhost:54321/storage/v1/object/public/profile-pictures/profile/p.jpg"
        val confirmed = await().atMost(Duration.ofSeconds(20)).until(
            { call("PUT", "/api/v1/me/profile-picture", player.accessToken, """{"publicUrl":"$profilePicture"}""").status },
            { it == 200 }
        )
        assertEquals(200, confirmed)
        assertEquals(profilePicture, myMember(clubId, player)["profilePictureUrl"].asText())
        assertEquals(profilePicture, myMember(clubId, player)["pictureUrl"].asText())

        // CA-3: the club picture wins while it exists
        assertEquals(400, uploadClubPicture(clubId, player.accessToken, "image/gif").status)
        val uploaded = uploadClubPicture(clubId, player.accessToken, "image/png")
        assertEquals(200, uploaded.status, uploaded.body)
        assertEquals(clubPicture, uploaded.json["clubPictureUrl"].asText())
        assertEquals(clubPicture, myMember(clubId, player)["pictureUrl"].asText())

        val removed = call("DELETE", "/api/v1/clubs/$clubId/members/me/picture", player.accessToken)
        assertEquals(200, removed.status)
        assertTrue(removed.json["clubPictureUrl"].isNull)
        assertEquals(profilePicture, removed.json["pictureUrl"].asText())
        verify(storageService).deleteFile(clubPicture)
    }

    @Test
    fun `CA-5 users are found by part of their username or by their exact email`() {
        val tag = UUID.randomUUID().toString().filter { it.isLetter() }.take(4) + "x"
        fun register(name: String): String {
            val email = "$name@squadfy.test"
            assertEquals(200, call("POST", "/api/v1/auth/register", body = """{"email":"$email","username":"$name","password":"$TEST_PASSWORD"}""").status)
            return email
        }
        register("${tag}carlos")
        register("${tag}marcos")
        val anaEmail = register("${tag}ana")
        val searcher = registerUser()

        await().atMost(Duration.ofSeconds(20)).untilAsserted {
            val all = call("GET", "/api/v1/users/search?q=$tag", searcher.accessToken).json.map { it["username"].asText() }
            assertEquals(listOf("${tag}ana", "${tag}carlos", "${tag}marcos"), all)
        }
        val ar = call("GET", "/api/v1/users/search?q=AR", searcher.accessToken).json.map { it["username"].asText() }
        assertTrue("${tag}carlos" in ar && "${tag}marcos" in ar && "${tag}ana" !in ar, ar.toString())
        assertEquals(400, call("GET", "/api/v1/users/search?q=a", searcher.accessToken).status)

        assertEquals(listOf("${tag}ana"), call("GET", "/api/v1/users/search?q=$anaEmail", searcher.accessToken).json.map { it["username"].asText() })
        assertTrue(call("GET", "/api/v1/users/search?q=${anaEmail.take(8)}@squadfy", searcher.accessToken).json.isEmpty)
        val me = call("GET", "/api/v1/users/search?q=${searcher.email}", searcher.accessToken).json
        assertTrue(me.isEmpty, "The requester is never returned")
        assertTrue(call("GET", "/api/v1/users/search?q=$tag", searcher.accessToken).json.none { it.has("email") })
    }

    @Test
    fun `CA-6 registering an existing email answers USER_EXISTS`() {
        val user = registerUser()
        val duplicated = call("POST", "/api/v1/auth/register", body = """{"email":"${user.email}","username":"other${UUID.randomUUID().toString().take(6)}","password":"$TEST_PASSWORD"}""")

        assertEquals(409, duplicated.status)
        assertEquals("USER_EXISTS", duplicated.json["code"].asText())
    }

    @Test
    fun `cached message pages are read back with Jackson 3 and stats reject an inverted period`() {
        val sender = registerUser()
        val reader = registerUser()
        val chat = await().atMost(Duration.ofSeconds(20)).until(
            { call("POST", "/api/v1/chats", sender.accessToken, """{"otherUserIds":["${reader.userId}"]}""") },
            { it.status == 201 }
        )
        val chatId = UUID.fromString(chat.json["id"].asText())
        jdbcTemplate.update(
            "INSERT INTO chat_service.chat_messages (id, chat_id, sender_id, content, created_at) VALUES (?, ?, ?, 'hola', now() - interval '1 minute')",
            UUID.randomUUID(), chatId, sender.userId
        )

        val first = call("GET", "/api/v1/chats/$chatId/messages", reader.accessToken)
        val cached = call("GET", "/api/v1/chats/$chatId/messages", reader.accessToken)
        assertEquals(200, cached.status, cached.body)
        assertEquals(first.json, cached.json)
        assertEquals("hola", cached.json[0]["content"].asText())
        assertEquals(sender.userId.toString(), cached.json[0]["senderId"].asText())

        val (clubId, _, ownerToken) = newClub("Period FC")
        assertEquals(400, call("GET", "/api/v1/clubs/$clubId/stats?from=2026-12-01&to=2026-11-01", ownerToken).status)
        assertEquals(200, call("GET", "/api/v1/clubs/$clubId/stats/me?from=2026-11-01&to=2026-12-01", ownerToken).status)
    }
}
