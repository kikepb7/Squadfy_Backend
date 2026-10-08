package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.club.ClubMembershipProvider
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.mockito.Mockito.mockingDetails
import com.kikepb.squadfy.infrastructure.message_queue.EventPublisher
import com.kikepb.squadfy.domain.events.match.MatchEvent
import com.kikepb.squadfy.domain.club.ClubMembershipSnapshot
import com.kikepb.squadfy.domain.club.ClubRole
import com.kikepb.squadfy.domain.club.PlayerPosition
import com.kikepb.squadfy.domain.exception.ForbiddenException
import com.kikepb.squadfy.domain.exception.InvalidMatchStateException
import com.kikepb.squadfy.domain.exception.InvalidPlayerMinutesException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementAlreadyEnrolledException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementClosedException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementNotFoundException
import com.kikepb.squadfy.domain.exception.NotClubMemberException
import com.kikepb.squadfy.domain.model.CurrentMatchAnnouncementModel
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel
import com.kikepb.squadfy.domain.model.MatchEventType
import com.kikepb.squadfy.domain.model.MatchFormat
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus
import com.kikepb.squadfy.domain.model.PlayerRatingCalculator
import com.kikepb.squadfy.domain.model.PlayerStatsModel
import com.kikepb.squadfy.domain.model.StatsPeriod
import com.kikepb.squadfy.domain.model.StatsSortBy
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
import java.time.LocalDate
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
    MatchSchedulerService::class,
    MatchNotificationPublisher::class,
    MatchAnnouncementNotificationService::class,
    PlayerStatsService::class,
    MemberAbsenceService::class,
    ScheduleExceptionService::class,
    LiveUpdatePublisher::class
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
    @Autowired lateinit var notificationService: MatchAnnouncementNotificationService
    @Autowired lateinit var statsService: PlayerStatsService
    @MockitoBean lateinit var eventPublisher: EventPublisher

    private fun publishedEvents(): List<MatchEvent> =
        mockingDetails(eventPublisher).invocations
            .filter { it.method.name == "publishAfterCommit" }
            .map { it.arguments.first() as MatchEvent }

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

    private fun nextMatch(): MatchModel = matchService.getMatchesByClub(clubId = clubId, userId = owner, status = MatchStatus.SCHEDULED).first()

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
    fun `the current announcement tells each member where they stand`() {
        assertFailsWith<MatchAnnouncementNotFoundException> { announcementService.getCurrentForClub(clubId = clubId, userId = owner) }

        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        val announcement = announcementOf(nextMatch())
        val players = enrollPlayers(announcement, count = 12)

        val confirmed = announcementService.getCurrentForClub(clubId = clubId, userId = players.first())
        val secondWaiting = announcementService.getCurrentForClub(clubId = clubId, userId = players.last())
        val notEnrolled = announcementService.getCurrentForClub(clubId = clubId, userId = owner)

        assertEquals(CurrentMatchAnnouncementModel.MyEnrollmentStatus.CONFIRMED, confirmed.myStatus)
        assertEquals(CurrentMatchAnnouncementModel.MyEnrollmentStatus.WAITLISTED, secondWaiting.myStatus)
        assertEquals(2, secondWaiting.myWaitlistPosition)
        assertEquals(CurrentMatchAnnouncementModel.MyEnrollmentStatus.NOT_ENROLLED, notEnrolled.myStatus)
        assertEquals(madrid("2026-10-08T20:00"), notEnrolled.matchScheduledAt)
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
        val teamsEvent = publishedEvents().filterIsInstance<MatchEvent.TeamsPublished>().single()
        assertEquals(5, teamsEvent.teamAUserIds.size)
        assertEquals(5, teamsEvent.teamBUserIds.size)
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
    fun `the opening push is emitted once to every member`() {
        val members = List(3) { UUID.randomUUID().also { clubs.addMember(clubId = clubId, userId = it) } }
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)

        assertEquals(1, notificationService.notifyOpenedAnnouncements())
        assertEquals(0, notificationService.notifyOpenedAnnouncements())

        val opened = publishedEvents().filterIsInstance<MatchEvent.AnnouncementOpened>().single()
        assertEquals((members + owner).toSet(), opened.recipientUserIds.toSet())
        assertEquals("Test FC", opened.clubName)
        assertEquals("Europe/Madrid", opened.timeZone)
    }

    @Test
    fun `the closing reminder goes once to members not enrolled while places are left`() {
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        val enrolled = enrollPlayers(announcementOf(nextMatch()), count = 3)
        val idle = List(2) { UUID.randomUUID().also { clubs.addMember(clubId = clubId, userId = it) } }

        clock.now = madrid("2026-10-06T21:00") // more than 24 h before closing (Wed 22:00)
        assertEquals(0, notificationService.sendClosingReminders())

        clock.now = madrid("2026-10-06T23:00")
        assertEquals(1, notificationService.sendClosingReminders())
        assertEquals(0, notificationService.sendClosingReminders())

        val reminder = publishedEvents().filterIsInstance<MatchEvent.AnnouncementClosingSoon>().single()
        assertEquals(7, reminder.freePlaces)
        assertEquals((idle + owner).toSet(), reminder.recipientUserIds.toSet())
        assertTrue(enrolled.none { it in reminder.recipientUserIds })
    }

    @Test
    fun `no closing reminder when the announcement is full or opened within the last 24 hours`() {
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        enrollPlayers(announcementOf(nextMatch()), count = 10)
        clock.now = madrid("2026-10-06T23:00")
        assertEquals(0, notificationService.sendClosingReminders())

        val lateClub = UUID.randomUUID()
        clubs.addMember(clubId = lateClub, userId = owner, role = ClubRole.OWNER)
        clock.now = madrid("2026-10-07T10:00") // opens inside the reminder window of Thursday's match
        scheduleService.createSchedule(lateClub, owner, DayOfWeek.THURSDAY, LocalTime.of(20, 0), "Europe/Madrid", MatchFormat.FIVE_A_SIDE)
        clock.now = madrid("2026-10-07T11:00")
        assertEquals(0, notificationService.sendClosingReminders())
        assertTrue(publishedEvents().none { it is MatchEvent.AnnouncementClosingSoon })
    }

    @Test
    fun `cancelling notifies every enrolled player and a promotion notifies the promoted player`() {
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        val match = nextMatch()
        val announcement = announcementOf(match)
        val players = enrollPlayers(announcement, count = 11)

        announcementService.withdraw(matchAnnouncementId = announcement.id, userId = players.first())
        val promoted = publishedEvents().filterIsInstance<MatchEvent.PromotedFromWaitlist>().single()
        assertEquals(players.last(), promoted.userId)

        matchService.cancelMatch(matchId = match.id, userId = owner)
        val cancelled = publishedEvents().filterIsInstance<MatchEvent.MatchCancelled>().single()
        assertEquals(players.drop(1).toSet(), cancelled.recipientUserIds.toSet())
    }

    @Test
    fun `players add the match duration unless a manager sets their minutes`() {
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        val match = nextMatch()
        enrollPlayers(announcementOf(match), count = 10)
        val teams = teamService.generateTeams(match.id, owner, TeamGenerationMode.AUTO, null, null)
        val substitute = teams.teamB.first()

        assertEquals(60, teams.durationMinutes)
        val adjusted = matchService.setPlayerMinutes(matchId = match.id, userId = owner, clubMemberId = substitute, minutes = 30)
        assertEquals(30, adjusted.minutesPlayed[substitute])
        assertEquals(60, adjusted.minutesPlayed[teams.teamA.first()])
        assertFailsWith<InvalidPlayerMinutesException> { matchService.setPlayerMinutes(match.id, owner, substitute, 70) }

        clock.now = madrid("2026-10-08T21:30")
        matchService.completeMatch(matchId = match.id, userId = owner)
        assertFailsWith<InvalidMatchStateException> { matchService.setPlayerMinutes(match.id, owner, substitute, 20) }
        assertEquals(30, statsService.getClubStats(clubId = clubId, userId = owner, sortBy = StatsSortBy.MINUTES)
            .single { it.stats.clubMemberId == substitute }.stats.minutesPlayed)
    }

    @Test
    fun `statistics come from completed matches and follow corrections after reopening`() {
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        val match = nextMatch()
        val players = enrollPlayers(announcementOf(match), count = 10)
        val teams = teamService.generateTeams(match.id, owner, TeamGenerationMode.AUTO, null, null)
        val scorer = teams.teamA.first()
        val goals = List(2) { eventService.addEvent(match.id, owner, scorer, MatchEventType.GOAL, null).events.last().id }

        assertTrue(statsService.getClubStats(clubId, owner, StatsSortBy.GOALS).all { it.stats.matchesPlayed == 0 })

        clock.now = madrid("2026-10-08T21:30")
        matchService.completeMatch(matchId = match.id, userId = owner)
        matchService.reopenMatch(matchId = match.id, userId = owner)
        eventService.removeEvent(matchId = match.id, userId = owner, eventId = goals.first())
        matchService.completeMatch(matchId = match.id, userId = owner)

        val ranking = statsService.getClubStats(clubId = clubId, userId = players.last(), sortBy = StatsSortBy.GOALS)
        assertEquals(11, ranking.size) // owner (no matches) + 10 players
        assertEquals(scorer, ranking.first().stats.clubMemberId)
        assertEquals(PlayerStatsModel(scorer, matchesPlayed = 1, wins = 1, goals = 1, minutesPlayed = 60), ranking.first().stats)
        val loser = statsService.getMyStats(clubId = clubId, userId = players.first { p -> clubs.memberIdOf(clubId, p) in teams.teamB })
        assertEquals(1, loser.losses)
        assertEquals(0, statsService.getMyStats(clubId = clubId, userId = owner).matchesPlayed)
        assertFailsWith<NotClubMemberException> { statsService.getClubStats(clubId, UUID.randomUUID(), StatsSortBy.GOALS) }
    }

    @Test
    fun `statistics can be limited to a period of the club calendar (spec 012 CA-2)`() {
        createThursdaySchedule(format = MatchFormat.FIVE_A_SIDE)
        val october = nextMatch()
        val players = enrollPlayers(announcementOf(october), count = 2)
        teamService.generateTeams(october.id, owner, TeamGenerationMode.AUTO, null, null)
        clock.now = madrid("2026-10-08T21:30")
        matchService.completeMatch(matchId = october.id, userId = owner)

        clock.now = madrid("2026-11-02T10:00")
        val november = matchService.createMatch(clubId = clubId, userId = owner, scheduledAt = madrid("2026-11-12T20:00"), format = null)
        players.forEach { announcementService.enroll(matchAnnouncementId = announcementOf(november).id, userId = it) }
        teamService.generateTeams(november.id, owner, TeamGenerationMode.AUTO, null, null)
        clock.now = madrid("2026-11-12T21:30")
        matchService.completeMatch(matchId = november.id, userId = owner)

        val player = players.first()
        fun matchesPlayed(period: StatsPeriod) = statsService.getMyStats(clubId = clubId, userId = player, period = period).matchesPlayed

        assertEquals(2, matchesPlayed(StatsPeriod.ALL))
        assertEquals(1, matchesPlayed(StatsPeriod(from = LocalDate.parse("2026-10-01"), to = LocalDate.parse("2026-10-31"))))
        assertEquals(1, matchesPlayed(StatsPeriod(from = LocalDate.parse("2026-11-01"), to = null)))
        assertEquals(0, matchesPlayed(StatsPeriod(from = null, to = LocalDate.parse("2026-10-07"))))
        val octoberRanking = statsService.getClubStats(clubId, owner, StatsSortBy.MATCHES, StatsPeriod(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31")))
        assertEquals(1, octoberRanking.first().stats.matchesPlayed)
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

    fun memberIdOf(clubId: ClubId, userId: UserId): ClubMemberId = findMembership(clubId, userId)!!.memberId

    override fun findMembership(clubId: ClubId, userId: UserId): ClubMembershipSnapshot? =
        members.firstOrNull { it.clubId == clubId && it.userId == userId }

    override fun findMembers(clubId: ClubId, memberIds: Collection<ClubMemberId>): List<ClubMembershipSnapshot> =
        members.filter { it.clubId == clubId && it.memberId in memberIds }

    override fun findAllMembers(clubId: ClubId): List<ClubMembershipSnapshot> =
        members.filter { it.clubId == clubId }

    override fun findClubName(clubId: ClubId): String? = "Test FC"

    override fun findMemberIdsOfUser(userId: UserId): List<ClubMemberId> =
        members.filter { it.userId == userId }.map { it.memberId }
}
