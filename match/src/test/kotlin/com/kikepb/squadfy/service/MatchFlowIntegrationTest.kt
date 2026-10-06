package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.club.ClubMembershipProvider
import com.kikepb.squadfy.domain.club.ClubMembershipSnapshot
import com.kikepb.squadfy.domain.club.ClubRole
import com.kikepb.squadfy.domain.club.PlayerPosition
import com.kikepb.squadfy.domain.exception.ForbiddenException
import com.kikepb.squadfy.domain.exception.InvalidMatchStateException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementAlreadyEnrolledException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementClosedException
import com.kikepb.squadfy.domain.exception.NotClubMemberException
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel
import com.kikepb.squadfy.domain.model.MatchEventType
import com.kikepb.squadfy.domain.model.MatchFormat
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus
import com.kikepb.squadfy.domain.model.PlayerRatingCalculator
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.service.MatchTeamService.TeamGenerationMode
import com.kikepb.squadfy.testing.PostgresTestContainerConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * End-to-end flows of specs 002, 003 and 004 against a real PostgreSQL:
 * schedule → planning → enrollment with waitlist → team draw → completion → ratings.
 */
@DataJpaTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true"
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    PostgresTestContainerConfiguration::class,
    MatchFlowIntegrationTest.TestBeans::class,
    ClubAccessGuard::class,
    ClubMatchScheduleService::class,
    MatchPlanningService::class,
    MatchAnnouncementService::class,
    MatchService::class,
    MatchTeamService::class,
    MatchEventService::class,
    PlayerRatingService::class,
    MatchSchedulerService::class
)
class MatchFlowIntegrationTest {

    companion object {
        val MADRID: ZoneId = ZoneId.of("Europe/Madrid")
    }

    @TestConfiguration
    class TestBeans {
        @Bean
        fun clock() = MutableClock()

        @Bean
        fun clubMembershipProvider() = FakeClubMembershipProvider()
    }

    @Autowired lateinit var clock: MutableClock
    @Autowired lateinit var clubs: FakeClubMembershipProvider
    @Autowired lateinit var scheduleService: ClubMatchScheduleService
    @Autowired lateinit var planningService: MatchPlanningService
    @Autowired lateinit var announcementService: MatchAnnouncementService
    @Autowired lateinit var matchService: MatchService
    @Autowired lateinit var teamService: MatchTeamService
    @Autowired lateinit var eventService: MatchEventService
    @Autowired lateinit var ratingService: PlayerRatingService
    @Autowired lateinit var scheduler: MatchSchedulerService

    private val clubId: ClubId = UUID.randomUUID()
    private val owner: UserId = UUID.randomUUID()

    private fun madrid(value: String): Instant = LocalDateTime.parse(value).atZone(MADRID).toInstant()

    @BeforeEach
    fun setUp() {
        clock.now = madrid("2026-10-05T10:00") // Monday
        clubs.addMember(clubId = clubId, userId = owner, role = ClubRole.OWNER)
    }

    private fun createThursdaySchedule(format: MatchFormat) =
        scheduleService.createSchedule(
            clubId = clubId,
            userId = owner,
            matchDayOfWeek = DayOfWeek.THURSDAY,
            matchTime = LocalTime.of(20, 0),
            timeZone = "Europe/Madrid",
            format = format
        )

    private fun nextMatch(): MatchModel = matchService.getScheduledMatchesByClub(clubId = clubId, userId = owner).first()

    private fun announcementOf(match: MatchModel): MatchAnnouncementModel =
        announcementService.getMatchAnnouncementByMatch(matchId = match.id, userId = owner)

    private fun enrollPlayers(announcement: MatchAnnouncementModel, count: Int): List<UserId> =
        List(count) { index ->
            val userId = UUID.randomUUID()
            clubs.addMember(clubId = clubId, userId = userId, position = PlayerPosition.entries[index % 4])
            announcementService.enroll(matchAnnouncementId = announcement.id, userId = userId)
            userId
        }

    @Test
    fun `creating a schedule plans the first match in the club time zone`() {
        createThursdaySchedule(format = MatchFormat.SEVEN_A_SIDE)

        val match = nextMatch()
        val announcement = announcementOf(match)

        assertEquals(madrid("2026-10-08T20:00"), match.scheduledAt)
        assertEquals(14, announcement.maxPlayers)
        assertEquals(clock.now, announcement.opensAt)
        assertEquals(madrid("2026-10-07T22:00"), announcement.closesAt)
    }

    @Test
    fun `planning is idempotent and the next match is planned once the current one has started`() {
        val schedule = createThursdaySchedule(format = MatchFormat.ELEVEN_A_SIDE)

        assertNull(planningService.planNextMatch(schedule = schedule))
        assertEquals(1, matchService.getMatchesByClub(clubId = clubId, userId = owner).size)

        clock.now = madrid("2026-10-08T21:05")
        val next = planningService.planNextMatch(schedule = schedule)!!
        val announcement = announcementOf(next)

        assertEquals(madrid("2026-10-15T20:00"), next.scheduledAt)
        assertEquals(madrid("2026-10-09T00:00"), announcement.opensAt)
        assertEquals(madrid("2026-10-14T22:00"), announcement.closesAt)
    }

    @Test
    fun `players beyond the format capacity are waitlisted and promoted when a confirmed player withdraws`() {
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        val announcement = announcementOf(nextMatch())

        val players = enrollPlayers(announcement, count = 12)
        val full = announcementService.getMatchAnnouncementById(matchAnnouncementId = announcement.id, userId = owner)
        assertEquals(10, full.confirmedEntries.size)
        assertEquals(2, full.waitlistEntries.size)
        val firstWaitlisted = full.waitlistEntries.first().clubMemberId

        assertFailsWith<MatchAnnouncementAlreadyEnrolledException> {
            announcementService.enroll(matchAnnouncementId = announcement.id, userId = players.last())
        }

        val afterWithdraw = announcementService.withdraw(matchAnnouncementId = announcement.id, userId = players.first())

        assertEquals(10, afterWithdraw.confirmedEntries.size)
        assertEquals(1, afterWithdraw.waitlistEntries.size)
        assertTrue(afterWithdraw.confirmedEntries.any { it.clubMemberId == firstWaitlisted })
    }

    @Test
    fun `nobody can enroll or withdraw after 22h the day before the match`() {
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        val announcement = announcementOf(nextMatch())
        val players = enrollPlayers(announcement, count = 2)

        clock.now = madrid("2026-10-07T22:00")
        val lateUser = UUID.randomUUID().also { clubs.addMember(clubId = clubId, userId = it) }

        assertFailsWith<MatchAnnouncementClosedException> {
            announcementService.enroll(matchAnnouncementId = announcement.id, userId = lateUser)
        }
        assertFailsWith<MatchAnnouncementClosedException> {
            announcementService.withdraw(matchAnnouncementId = announcement.id, userId = players.first())
        }
    }

    @Test
    fun `permissions are enforced`() {
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        val announcement = announcementOf(nextMatch())
        val player = UUID.randomUUID().also { clubs.addMember(clubId = clubId, userId = it) }

        assertFailsWith<NotClubMemberException> {
            announcementService.enroll(matchAnnouncementId = announcement.id, userId = UUID.randomUUID())
        }
        assertFailsWith<ForbiddenException> {
            matchService.createMatch(clubId = clubId, userId = player, scheduledAt = madrid("2026-10-20T20:00"), format = null)
        }
        assertFailsWith<ForbiddenException> {
            teamService.generateTeams(nextMatch().id, player, TeamGenerationMode.AUTO, null, null)
        }
    }

    @Test
    fun `teams can be regenerated, the match completed and ratings reverted on reopen`() {
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        val match = nextMatch()
        enrollPlayers(announcementOf(match), count = 10)

        teamService.generateTeams(match.id, owner, TeamGenerationMode.AUTO, null, null)
        val teams = teamService.generateTeams(match.id, owner, TeamGenerationMode.AUTO, null, null)
        assertEquals(5, teams.teamA.size)
        assertEquals(5, teams.teamB.size)

        assertFailsWith<InvalidMatchStateException> { matchService.completeMatch(matchId = match.id, userId = owner) }

        val scorer = teams.teamA.first()
        repeat(2) { eventService.addEvent(match.id, owner, scorer, MatchEventType.GOAL, null) }
        clock.now = madrid("2026-10-08T21:30")

        val completed = matchService.completeMatch(matchId = match.id, userId = owner)
        assertEquals(MatchStatus.COMPLETED, completed.status)
        assertEquals(2, completed.teamAScore)

        val ratings = ratingService.ratingsFor(clubId = clubId, memberIds = teams.teamA + teams.teamB)
        teams.teamA.forEach { assertTrue(ratings.getValue(it) > PlayerRatingCalculator.INITIAL_RATING) }
        teams.teamB.forEach { assertTrue(ratings.getValue(it) < PlayerRatingCalculator.INITIAL_RATING) }
        assertTrue(ratings.getValue(scorer) == ratings.values.max())

        val reopened = matchService.reopenMatch(matchId = match.id, userId = owner)
        assertEquals(MatchStatus.SCHEDULED, reopened.status)
        ratingService.ratingsFor(clubId = clubId, memberIds = teams.teamA + teams.teamB).values.forEach {
            assertTrue(abs(it - PlayerRatingCalculator.INITIAL_RATING) < 1e-9)
        }
    }

    @Test
    fun `teams are published automatically when the announcement closes and managers can rectify them`() {
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        val match = nextMatch()
        enrollPlayers(announcementOf(match), count = 11)

        clock.now = madrid("2026-10-07T21:55")
        scheduler.closeExpiredMatchAnnouncements()
        assertTrue(matchService.getMatchById(matchId = match.id, userId = owner).teamA.isEmpty())

        clock.now = madrid("2026-10-07T22:00")
        scheduler.closeExpiredMatchAnnouncements()

        val published = matchService.getMatchById(matchId = match.id, userId = owner)
        assertEquals(MatchAnnouncementModel.MatchAnnouncementStatus.CLOSED, announcementOf(match).status)
        assertEquals(5, published.teamA.size)
        assertEquals(5, published.teamB.size)
        assertEquals(published.enrolledPlayers.toSet(), (published.teamA + published.teamB).toSet())

        val rectified = teamService.generateTeams(
            matchId = match.id,
            userId = owner,
            mode = TeamGenerationMode.MANUAL,
            manualTeamA = published.teamB,
            manualTeamB = published.teamA
        )
        assertEquals(published.teamB, rectified.teamA)
    }

    @Test
    fun `teams already drawn with the confirmed players are kept when the announcement closes`() {
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        val match = nextMatch()
        enrollPlayers(announcementOf(match), count = 10)
        val drawn = teamService.generateTeams(match.id, owner, TeamGenerationMode.AUTO, null, null)

        clock.now = madrid("2026-10-07T22:00")
        scheduler.closeExpiredMatchAnnouncements()

        val afterClose = matchService.getMatchById(matchId = match.id, userId = owner)
        assertEquals(drawn.teamA.toSet(), afterClose.teamA.toSet())
        assertEquals(drawn.teamB.toSet(), afterClose.teamB.toSet())
    }

    @Test
    fun `managers can see how even the teams are, players cannot`() {
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        val match = nextMatch()
        val players = enrollPlayers(announcementOf(match), count = 10)

        assertFailsWith<InvalidMatchStateException> { teamService.getTeamBalance(matchId = match.id, userId = owner) }

        teamService.generateTeams(match.id, owner, TeamGenerationMode.AUTO, null, null)
        val balance = teamService.getTeamBalance(matchId = match.id, userId = owner)

        assertEquals(5, balance.teamA.players)
        assertEquals(5, balance.teamB.players)
        assertEquals(PlayerRatingCalculator.INITIAL_RATING, balance.teamA.averageRating)
        assertEquals(0.0, balance.averageRatingDifference)
        assertEquals(0.5, balance.teamAExpectedScore)
        assertEquals(balance.teamA.playerRatings.map { it.clubMemberId }.toSet(), nextMatch().teamA.toSet())
        assertFailsWith<ForbiddenException> { teamService.getTeamBalance(matchId = match.id, userId = players.first()) }
    }

    @Test
    fun `every member sees the club classification by rating`() {
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        val match = nextMatch()
        val players = enrollPlayers(announcementOf(match), count = 4)
        val teams = teamService.generateTeams(match.id, owner, TeamGenerationMode.AUTO, null, null)
        repeat(2) { eventService.addEvent(match.id, owner, teams.teamA.first(), MatchEventType.GOAL, null) }
        clock.now = madrid("2026-10-08T21:30")
        matchService.completeMatch(matchId = match.id, userId = owner)

        val leaderboard = ratingService.getLeaderboard(clubId = clubId, userId = players.last())

        assertEquals(5, leaderboard.size) // owner + 4 players
        assertEquals(1, leaderboard.first().rank)
        assertTrue(leaderboard.first().clubMemberId in teams.teamA)
        val worstWinner = leaderboard.filter { it.clubMemberId in teams.teamA }.maxOf { it.rank }
        val bestLoser = leaderboard.filter { it.clubMemberId in teams.teamB }.minOf { it.rank }
        assertTrue(worstWinner < bestLoser)

        val mine = ratingService.getMyRating(clubId = clubId, userId = players.last())
        assertEquals(5, mine.totalPlayers)
        assertEquals(leaderboard.single { it.clubMemberId == mine.clubMemberId }.rank, mine.rank)
        assertFailsWith<NotClubMemberException> { ratingService.getLeaderboard(clubId = clubId, userId = UUID.randomUUID()) }
    }

    @Test
    fun `each player can read their own rating`() {
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        val player = UUID.randomUUID().also { clubs.addMember(clubId = clubId, userId = it) }

        val rating = ratingService.getMyRating(clubId = clubId, userId = player)

        assertEquals(PlayerRatingCalculator.INITIAL_RATING, rating.rating)
        assertTrue(rating.isProvisional)
        assertFailsWith<NotClubMemberException> { ratingService.getMyRating(clubId = clubId, userId = UUID.randomUUID()) }
    }
}

class MutableClock(var now: Instant = Instant.now()) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = now
}

class FakeClubMembershipProvider : ClubMembershipProvider {
    private val members = mutableListOf<ClubMembershipSnapshot>()

    fun addMember(
        clubId: ClubId,
        userId: UserId,
        role: ClubRole = ClubRole.PLAYER,
        position: PlayerPosition? = null
    ): ClubMemberId {
        val memberId = UUID.randomUUID()
        members += ClubMembershipSnapshot(memberId = memberId, clubId = clubId, userId = userId, role = role, position = position)
        return memberId
    }

    override fun findMembership(clubId: ClubId, userId: UserId): ClubMembershipSnapshot? =
        members.firstOrNull { it.clubId == clubId && it.userId == userId }

    override fun findMembers(clubId: ClubId, memberIds: Collection<ClubMemberId>): List<ClubMembershipSnapshot> =
        members.filter { it.clubId == clubId && it.memberId in memberIds }

    override fun findAllMembers(clubId: ClubId): List<ClubMembershipSnapshot> =
        members.filter { it.clubId == clubId }
}
