package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.club.ClubRole
import com.kikepb.squadfy.domain.club.PlayerPosition
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubParticipantEntity
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMemberRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ClubParticipantRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DataJpaTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true"
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(ClubMembershipQueryService::class)
class ClubMembershipQueryServiceIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired lateinit var service: ClubMembershipQueryService
    @Autowired lateinit var memberRepository: ClubMemberRepository
    @Autowired lateinit var participantRepository: ClubParticipantRepository

    private fun member(clubId: UUID, role: ClubMemberRoleEntity, position: String?): ClubMemberEntity {
        val userId = UUID.randomUUID()
        participantRepository.save(ClubParticipantEntity(userId = userId, username = "user-$userId", email = "$userId@test.dev"))
        return memberRepository.saveAndFlush(ClubMemberEntity(clubId = clubId, userId = userId, role = role, position = position))
    }

    @Test
    fun `maps memberships with role and legacy positions`() {
        val clubId = UUID.randomUUID()
        val admin = member(clubId, ClubMemberRoleEntity.ADMIN, position = "Portero")

        val snapshot = service.findMembership(clubId = clubId, userId = admin.userId)!!

        assertEquals(admin.id, snapshot.memberId)
        assertEquals(ClubRole.ADMIN, snapshot.role)
        assertTrue(snapshot.role.canManageClub)
        assertEquals(PlayerPosition.GOALKEEPER, snapshot.position)
        assertNull(service.findMembership(clubId = UUID.randomUUID(), userId = admin.userId))
    }

    @Test
    fun `finds only the requested members of the given club`() {
        val clubId = UUID.randomUUID()
        val first = member(clubId, ClubMemberRoleEntity.PLAYER, position = "FORWARD")
        val second = member(clubId, ClubMemberRoleEntity.PLAYER, position = null)
        member(clubId, ClubMemberRoleEntity.PLAYER, position = null)
        val otherClub = member(UUID.randomUUID(), ClubMemberRoleEntity.PLAYER, position = null)

        val found = service.findMembers(clubId = clubId, memberIds = listOf(first.id!!, second.id!!, otherClub.id!!))

        assertEquals(setOf(first.id, second.id), found.map { it.memberId }.toSet())
        assertTrue(service.findMembers(clubId = clubId, memberIds = emptyList()).isEmpty())
    }
}
