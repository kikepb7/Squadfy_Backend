package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.events.club.ClubEvent
import com.kikepb.squadfy.domain.exception.ClubMemberBannedException
import com.kikepb.squadfy.domain.exception.ClubOwnerCannotLeaveException
import com.kikepb.squadfy.domain.exception.ForbiddenException
import com.kikepb.squadfy.domain.exception.InvalidClubOperationException
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole
import com.kikepb.squadfy.domain.model.ClubModel
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.domain.user.UserDirectory
import com.kikepb.squadfy.domain.user.UserSummary
import com.kikepb.squadfy.infrastructure.message_queue.EventPublisher
import com.kikepb.squadfy.infrastructure.storage.SupabaseStorageService
import com.kikepb.squadfy.testing.PostgresTestContainerConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Spec 001: leave, remove, roles, ownership transfer, rejoin and club edition.
 * Each service call runs in its own transaction, as in a real request (no test-wide transaction).
 */
@DataJpaTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true"
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(
    PostgresTestContainerConfiguration::class,
    ClubMemberManagementIntegrationTest.TestBeans::class,
    ClubService::class,
    ClubMemberManagementService::class,
    ClubParticipantService::class,
    ClubMemberGuard::class
)
class ClubMemberManagementIntegrationTest {

    @TestConfiguration
    class TestBeans {
        @Bean
        fun clock(): Clock = Clock.systemUTC()

        /** Every user id is a registered user. */
        @Bean
        fun userDirectory() = object : UserDirectory {
            override fun findUser(userId: UserId) =
                UserSummary(userId = userId, username = "user-$userId", email = "$userId@squadfy.test")
        }
    }

    @MockitoBean lateinit var eventPublisher: EventPublisher
    @MockitoBean lateinit var storageService: SupabaseStorageService

    @Autowired lateinit var clubService: ClubService
    @Autowired lateinit var managementService: ClubMemberManagementService

    private val owner: UserId = UUID.randomUUID()
    private lateinit var club: ClubModel

    @BeforeEach
    fun setUp() {
        club = clubService.createClub(userId = owner, name = "Squadfy FC", description = null, clubLogoUrl = null, maxMembers = null)
    }

    private fun join(): UserId = UUID.randomUUID().also {
        clubService.joinClub(userId = it, invitationCode = club.invitationCode, shirtNumber = null, position = null)
    }

    private fun memberIdOf(userId: UserId): ClubMemberId =
        clubService.getMembers(clubId = club.id, userId = owner).single { it.userId == userId }.id

    private fun publishedEvents(): List<Any?> =
        mockingDetails(eventPublisher).invocations
            .filter { it.method.name == "publishAfterCommit" }
            .map { it.arguments.first() }

    private fun roleOf(userId: UserId): ClubMemberRole =
        clubService.getMembers(clubId = club.id, userId = owner).single { it.userId == userId }.role

    @Test
    fun `a member leaves, stops counting and loses access, and an event is published`() {
        val player = join()
        val memberId = memberIdOf(player)

        managementService.leaveClub(clubId = club.id, userId = player)

        assertEquals(1, clubService.getClubById(clubId = club.id, userId = owner).membersCount)
        assertTrue(clubService.getMembers(clubId = club.id, userId = owner).none { it.userId == player })
        assertFailsWith<ForbiddenException> { clubService.getClubById(clubId = club.id, userId = player) }
        assertEquals(1, publishedEvents().filterIsInstance<ClubEvent.MemberLeft>().count { it.clubMemberId == memberId })
    }

    @Test
    fun `the owner cannot leave until ownership is transferred`() {
        val player = join()
        assertFailsWith<ClubOwnerCannotLeaveException> { managementService.leaveClub(clubId = club.id, userId = owner) }

        val updated = managementService.transferOwnership(clubId = club.id, userId = owner, newOwnerMemberId = memberIdOf(player))

        assertEquals(player, updated.ownerId)
        assertEquals(ClubMemberRole.OWNER, roleOf(player))
        assertEquals(ClubMemberRole.ADMIN, roleOf(owner))
        managementService.leaveClub(clubId = club.id, userId = owner)
        assertTrue(clubService.getMembers(clubId = club.id, userId = player).none { it.userId == owner })
    }

    @Test
    fun `admins manage captains and players but not other admins`() {
        val admin = join()
        val otherAdmin = join()
        val player = join()
        managementService.changeMemberRole(club.id, owner, memberIdOf(admin), ClubMemberRole.ADMIN)
        managementService.changeMemberRole(club.id, owner, memberIdOf(otherAdmin), ClubMemberRole.ADMIN)

        managementService.changeMemberRole(club.id, admin, memberIdOf(player), ClubMemberRole.CAPTAIN)
        assertEquals(ClubMemberRole.CAPTAIN, roleOf(player))

        assertFailsWith<ForbiddenException> { managementService.changeMemberRole(club.id, admin, memberIdOf(player), ClubMemberRole.ADMIN) }
        assertFailsWith<ForbiddenException> { managementService.removeMember(club.id, admin, memberIdOf(otherAdmin)) }
        assertFailsWith<ForbiddenException> { managementService.removeMember(club.id, player, memberIdOf(admin)) }

        managementService.removeMember(club.id, admin, memberIdOf(player))
        managementService.removeMember(club.id, owner, memberIdOf(otherAdmin))
        assertEquals(2, clubService.getClubById(clubId = club.id, userId = owner).membersCount)
        assertEquals(2, publishedEvents().filterIsInstance<ClubEvent.MemberKicked>().size)
    }

    @Test
    fun `nobody removes themselves or changes their own role, and owner is only transferred`() {
        val player = join()

        assertFailsWith<InvalidClubOperationException> { managementService.removeMember(club.id, owner, memberIdOf(owner)) }
        assertFailsWith<InvalidClubOperationException> { managementService.changeMemberRole(club.id, owner, memberIdOf(owner), ClubMemberRole.PLAYER) }
        assertFailsWith<InvalidClubOperationException> { managementService.changeMemberRole(club.id, owner, memberIdOf(player), ClubMemberRole.OWNER) }
    }

    @Test
    fun `a removed player who rejoins gets the same membership back as player`() {
        val player = join()
        val memberId = memberIdOf(player)
        managementService.changeMemberRole(club.id, owner, memberId, ClubMemberRole.CAPTAIN)
        managementService.removeMember(club.id, owner, memberId)

        clubService.joinClub(userId = player, invitationCode = club.invitationCode, shirtNumber = 9, position = null)

        val member = clubService.getMembers(clubId = club.id, userId = owner).single { it.userId == player }
        assertEquals(memberId, member.id)
        assertEquals(ClubMemberRole.PLAYER, member.role)
        assertEquals(9, member.shirtNumber)
    }

    @Test
    fun `managers edit the club but the member limit cannot drop below the current members`() {
        join()
        join()

        val updated = clubService.updateClub(clubId = club.id, userId = owner, name = "  New name ", description = "Weekly 5v5", maxMembers = 3)
        assertEquals("New name", updated.name)
        assertEquals(3, updated.maxMembers)

        assertFailsWith<InvalidClubOperationException> {
            clubService.updateClub(clubId = club.id, userId = owner, name = null, description = null, maxMembers = 2)
        }
    }

    @Test
    fun `a banned member is removed and cannot rejoin until the ban is lifted`() {
        val player = join()
        val memberId = memberIdOf(player)

        managementService.banMember(clubId = club.id, userId = owner, memberId = memberId)

        assertTrue(clubService.getMembers(clubId = club.id, userId = owner).none { it.userId == player })
        assertEquals(1, publishedEvents().filterIsInstance<ClubEvent.MemberKicked>().count { it.clubMemberId == memberId })
        assertEquals(listOf(memberId), managementService.getBans(clubId = club.id, userId = owner).map { it.clubMemberId })
        assertFailsWith<ClubMemberBannedException> {
            clubService.joinClub(userId = player, invitationCode = club.invitationCode, shirtNumber = null, position = null)
        }

        managementService.unbanMember(clubId = club.id, userId = owner, memberId = memberId)
        clubService.joinClub(userId = player, invitationCode = club.invitationCode, shirtNumber = null, position = null)

        assertEquals(memberId, memberIdOf(player))
        assertEquals(ClubMemberRole.PLAYER, roleOf(player))
        assertTrue(managementService.getBans(clubId = club.id, userId = owner).isEmpty())
    }

    @Test
    fun `former members can be banned too`() {
        val player = join()
        val memberId = memberIdOf(player)
        managementService.leaveClub(clubId = club.id, userId = player)

        managementService.banMember(clubId = club.id, userId = owner, memberId = memberId)

        assertFailsWith<ClubMemberBannedException> {
            clubService.joinClub(userId = player, invitationCode = club.invitationCode, shirtNumber = null, position = null)
        }
        assertEquals(0, publishedEvents().filterIsInstance<ClubEvent.MemberKicked>().size)
    }

    @Test
    fun `bans follow the same hierarchy as removals`() {
        val admin = join()
        val otherAdmin = join()
        val player = join()
        managementService.changeMemberRole(club.id, owner, memberIdOf(admin), ClubMemberRole.ADMIN)
        managementService.changeMemberRole(club.id, owner, memberIdOf(otherAdmin), ClubMemberRole.ADMIN)
        val otherAdminMemberId = memberIdOf(otherAdmin)

        assertFailsWith<ForbiddenException> { managementService.banMember(club.id, admin, otherAdminMemberId) }
        assertFailsWith<InvalidClubOperationException> { managementService.banMember(club.id, admin, memberIdOf(admin)) }
        assertFailsWith<ForbiddenException> { managementService.getBans(clubId = club.id, userId = player) }

        managementService.banMember(club.id, owner, otherAdminMemberId)
        assertFailsWith<ForbiddenException> { managementService.unbanMember(club.id, admin, otherAdminMemberId) }
        managementService.unbanMember(club.id, owner, otherAdminMemberId)
    }
}
