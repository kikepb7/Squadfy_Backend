package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.club.ClubRole
import com.kikepb.squadfy.domain.club.PlayerPosition
import com.kikepb.squadfy.domain.events.match.MatchEvent
import com.kikepb.squadfy.domain.exception.ForbiddenException
import com.kikepb.squadfy.domain.exception.InvalidClubMatchScheduleException
import com.kikepb.squadfy.domain.exception.InvalidMatchStateException
import com.kikepb.squadfy.domain.exception.InvalidScheduleExceptionException
import com.kikepb.squadfy.domain.exception.ScheduleExceptionAlreadyExistsException
import com.kikepb.squadfy.domain.exception.TooManyGuestsException
import com.kikepb.squadfy.domain.model.DeadlineRule
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel.MatchAnnouncementStatus
import com.kikepb.squadfy.domain.model.MatchEventType
import com.kikepb.squadfy.domain.model.MatchFormat
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus
import com.kikepb.squadfy.domain.model.PlayerRatingCalculator
import com.kikepb.squadfy.domain.model.ScheduleExceptionModel.ExceptionType
import com.kikepb.squadfy.domain.model.StatsSortBy
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.message_queue.EventPublisher
import com.kikepb.squadfy.service.MatchTeamService.TeamGenerationMode
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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Acceptance criteria of specs/008-app-parity against a real PostgreSQL. */
@DataJpaTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true"
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    PostgresTestContainerConfiguration::class,
    AppParityIntegrationTest.TestBeans::class,
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
    ScheduleExceptionService::class
)
class AppParityIntegrationTest {

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
    @Autowired lateinit var absenceService: MemberAbsenceService
    @Autowired lateinit var exceptionService: ScheduleExceptionService
    @MockitoBean lateinit var eventPublisher: EventPublisher

    private val madridZone: ZoneId = ZoneId.of("Europe/Madrid")
    private val clubId: ClubId = UUID.randomUUID()
    private val owner: UserId = UUID.randomUUID()
    private val thursday: LocalDate = LocalDate.parse("2026-10-08")

    private fun madrid(value: String): Instant = LocalDateTime.parse(value).atZone(madridZone).toInstant()

    private fun publishedEvents(): List<MatchEvent> =
        mockingDetails(eventPublisher).invocations
            .filter { it.method.name == "publishAfterCommit" }
            .map { it.arguments.first() as MatchEvent }

    @BeforeEach
    fun setUp() {
        clock.now = madrid("2026-10-05T10:00") // Monday
        clubs.addMember(clubId = clubId, userId = owner, role = ClubRole.OWNER)
    }

    private fun createSchedule(close: DeadlineRule = DeadlineRule.DEFAULT, draw: DeadlineRule? = null) =
        scheduleService.createSchedule(
            clubId = clubId,
            userId = owner,
            matchDayOfWeek = DayOfWeek.THURSDAY,
            matchTime = LocalTime.of(20, 0),
            timeZone = "Europe/Madrid",
            format = MatchFormat.FIVE_A_SIDE,
            close = close,
            draw = draw
        )

    private fun scheduledMatches(): List<MatchModel> =
        matchService.getMatchesByClub(clubId = clubId, userId = owner, status = MatchStatus.SCHEDULED).sortedBy { it.scheduledAt }

    private fun nextMatch(): MatchModel = scheduledMatches().first()

    private fun announcementOf(match: MatchModel): MatchAnnouncementModel =
        announcementService.getMatchAnnouncementByMatch(matchId = match.id, userId = owner)

    private fun newMember(): UserId = UUID.randomUUID().also { clubs.addMember(clubId = clubId, userId = it) }

    private fun enrollPlayers(announcement: MatchAnnouncementModel, count: Int): List<UserId> =
        List(count) { index ->
            val userId = UUID.randomUUID()
            clubs.addMember(clubId = clubId, userId = userId, position = PlayerPosition.entries[index % 4])
            announcementService.enroll(matchAnnouncementId = announcement.id, userId = userId)
            userId
        }

    private fun MatchAnnouncementModel.confirmedGuests() = confirmedEntries.filter { it.isGuest }.map { it.guestName }
    private fun MatchAnnouncementModel.waitingGuests() = waitlistEntries.filter { it.isGuest }.map { it.guestName }
    private fun MatchAnnouncementModel.confirmedMembers() = confirmedEntries.count { !it.isGuest }

    @Test
    fun `CA-1 and CA-2 members have priority over guests for the places`() {
        createSchedule()
        val announcement = announcementOf(nextMatch())
        val members = enrollPlayers(announcement, count = 8)

        announcementService.addGuest(announcement.id, members[0], "Guest 1", null)
        announcementService.addGuest(announcement.id, members[0], "Guest 2", PlayerPosition.GOALKEEPER)
        val withGuests = announcementService.addGuest(announcement.id, members[1], "Guest 3", null)
        assertEquals(8, withGuests.confirmedMembers())
        assertEquals(listOf("Guest 1", "Guest 2"), withGuests.confirmedGuests())
        assertEquals(listOf("Guest 3"), withGuests.waitingGuests())

        enrollPlayers(announcement, count = 1)
        val displaced = announcementService.getMatchAnnouncementById(announcement.id, owner)
        assertEquals(9, displaced.confirmedMembers())
        assertEquals(listOf("Guest 1"), displaced.confirmedGuests())
        assertEquals(listOf("Guest 2", "Guest 3"), displaced.waitingGuests())

        // CA-2: a waiting member goes up before the waiting guests.
        enrollPlayers(announcement, count = 1)
        val waitingMember = enrollPlayers(announcement, count = 1).single()
        val full = announcementService.getMatchAnnouncementById(announcement.id, owner)
        assertEquals(10, full.confirmedMembers())
        assertEquals(clubs.memberIdOf(clubId, waitingMember), full.waitlistEntries.first().clubMemberId)

        val afterWithdraw = announcementService.withdraw(announcement.id, members[5])
        assertTrue(afterWithdraw.confirmedEntries.any { it.clubMemberId == clubs.memberIdOf(clubId, waitingMember) })
        assertEquals(listOf("Guest 1", "Guest 2", "Guest 3"), afterWithdraw.waitingGuests())
        assertEquals(waitingMember, publishedEvents().filterIsInstance<MatchEvent.PromotedFromWaitlist>().single().userId)
    }

    @Test
    fun `CA-3 at most two guests per member, removed by their host or a manager`() {
        createSchedule()
        val announcement = announcementOf(nextMatch())
        val host = newMember()
        val other = newMember()

        announcementService.addGuest(announcement.id, host, "Guest 1", null)
        val withTwo = announcementService.addGuest(announcement.id, host, "Guest 2", null)
        assertFailsWith<TooManyGuestsException> { announcementService.addGuest(announcement.id, host, "Guest 3", null) }

        val guests = withTwo.confirmedEntries.filter { it.isGuest }
        assertFailsWith<ForbiddenException> { announcementService.removeGuest(announcement.id, other, guests[0].id) }
        announcementService.removeGuest(announcement.id, host, guests[0].id)
        val afterManager = announcementService.removeGuest(announcement.id, owner, guests[1].id)
        assertTrue(afterManager.entries.none { it.isGuest })

        // A member leaving the club takes their guests out of open announcements (RN-A5).
        announcementService.addGuest(announcement.id, host, "Guest 4", null)
        announcementService.withdrawFromOpenAnnouncements(clubId = clubId, clubMemberId = clubs.memberIdOf(clubId, host))
        assertTrue(announcementService.getMatchAnnouncementById(announcement.id, owner).entries.none { it.isGuest })
    }

    @Test
    fun `CA-4 and CA-11 guests play the draw but get no rating, members get their rating change`() {
        createSchedule()
        val match = nextMatch()
        val announcement = announcementOf(match)
        val members = enrollPlayers(announcement, count = 8)
        announcementService.addGuest(announcement.id, members[0], "Guest 1", PlayerPosition.GOALKEEPER)
        announcementService.addGuest(announcement.id, members[1], "Guest 2", null)

        clock.now = madrid("2026-10-07T22:00")
        scheduler.closeExpiredMatchAnnouncements()

        val drawn = matchService.getMatchById(matchId = match.id, userId = owner)
        assertEquals(2, drawn.enrolledGuests.size)
        assertEquals(5, drawn.teamA.size + drawn.teamAGuests.size)
        assertEquals(5, drawn.teamB.size + drawn.teamBGuests.size)
        // Players with equal rating are shuffled, so the guests may end up in either team
        assertEquals(2, drawn.teamAGuests.size + drawn.teamBGuests.size)
        val balance = teamService.getTeamBalance(matchId = match.id, userId = owner)
        assertEquals(2, (balance.teamA.playerRatings + balance.teamB.playerRatings).count { it.isGuest })

        // Managers can move a guest by its guestId: swap one guest with a member of the other team.
        val guestInA = drawn.teamAGuests.isNotEmpty()
        val guestA = (if (guestInA) drawn.teamAGuests else drawn.teamBGuests).first().guestId
        val memberB = if (guestInA) drawn.teamB.first() else drawn.teamA.first()
        val idsA = drawn.teamA + drawn.teamAGuests.map { it.guestId }
        val idsB = drawn.teamB + drawn.teamBGuests.map { it.guestId }
        val swapped = teamService.generateTeams(
            matchId = match.id,
            userId = owner,
            mode = TeamGenerationMode.MANUAL,
            manualTeamA = if (guestInA) idsA - guestA + memberB else idsA - memberB + guestA,
            manualTeamB = if (guestInA) idsB - memberB + guestA else idsB - guestA + memberB
        )
        val movedTo = if (guestInA) swapped.teamBGuests else swapped.teamAGuests
        assertTrue(movedTo.any { it.guestId == guestA })

        eventService.addEvent(match.id, owner, swapped.teamA.first(), MatchEventType.GOAL, null)
        clock.now = madrid("2026-10-08T21:30")
        val completed = matchService.completeMatch(matchId = match.id, userId = owner)

        assertEquals((completed.teamA + completed.teamB).toSet(), completed.ratingChanges.keys)
        completed.teamA.forEach { assertTrue(completed.ratingChanges.getValue(it) > 0) }
        val stats = statsService.getClubStats(clubId = clubId, userId = owner, sortBy = StatsSortBy.MATCHES)
        assertEquals(8, stats.count { it.stats.matchesPlayed == 1 })
        assertTrue(ratingService.getLeaderboard(clubId = clubId, userId = owner).none { it.clubMemberId == guestA })
    }

    @Test
    fun `CA-5 planning skips a cancelled week that was not planned yet`() {
        val schedule = createSchedule()
        exceptionService.createException(clubId, owner, LocalDate.parse("2026-10-15"), ExceptionType.CANCELLED, null, "Holidays")

        clock.now = madrid("2026-10-08T21:05")
        val next = assertNotNull(planningService.planNextMatch(schedule = schedule))

        assertEquals(madrid("2026-10-22T20:00"), next.scheduledAt)
        assertEquals(LocalDate.parse("2026-10-22"), next.scheduleDate)
    }

    @Test
    fun `CA-6 cancelling a planned week cancels its match and undoing it brings it back`() {
        createSchedule()
        val match = nextMatch()
        val players = enrollPlayers(announcementOf(match), count = 3)

        assertFailsWith<InvalidScheduleExceptionException> {
            exceptionService.createException(clubId, owner, LocalDate.parse("2026-10-09"), ExceptionType.CANCELLED, null, null)
        }
        assertFailsWith<ForbiddenException> {
            exceptionService.createException(clubId, players[0], thursday, ExceptionType.CANCELLED, null, null)
        }
        val exception = exceptionService.createException(clubId, owner, thursday, ExceptionType.CANCELLED, null, null)
        assertFailsWith<ScheduleExceptionAlreadyExistsException> {
            exceptionService.createException(clubId, owner, thursday, ExceptionType.CANCELLED, null, null)
        }

        assertEquals(MatchStatus.CANCELLED, matchService.getMatchById(match.id, owner).status)
        assertEquals(players.toSet(), publishedEvents().filterIsInstance<MatchEvent.MatchCancelled>().single().recipientUserIds.toSet())
        assertEquals(madrid("2026-10-15T20:00"), nextMatch().scheduledAt)
        assertEquals(listOf(exception.id), exceptionService.getExceptions(clubId, players[0]).map { it.id })

        exceptionService.deleteException(clubId = clubId, userId = owner, exceptionId = exception.id)

        val restored = matchService.getMatchById(match.id, owner)
        assertEquals(MatchStatus.SCHEDULED, restored.status)
        assertEquals(MatchAnnouncementStatus.OPEN, announcementOf(restored).status)
        assertEquals(3, announcementOf(restored).entries.size)
        assertEquals(match.id, nextMatch().id)
    }

    @Test
    fun `CA-7 moving a planned week keeps enrollments and recalculates close and draw`() {
        createSchedule()
        val match = nextMatch()
        enrollPlayers(announcementOf(match), count = 3)
        val members = clubs.findAllMembers(clubId).map { it.userId }.toSet()

        val exception = exceptionService.createException(clubId, owner, thursday, ExceptionType.RESCHEDULED, madrid("2026-10-09T21:00"), "Field busy")

        val moved = matchService.getMatchById(match.id, owner)
        val announcement = announcementOf(moved)
        assertEquals(madrid("2026-10-09T21:00"), moved.scheduledAt)
        assertEquals(madrid("2026-10-08T22:00"), announcement.closesAt)
        assertEquals(madrid("2026-10-08T22:00"), announcement.drawAt)
        assertEquals(3, announcement.entries.size)
        val rescheduled = publishedEvents().filterIsInstance<MatchEvent.MatchRescheduled>().single()
        assertEquals(madrid("2026-10-08T20:00"), rescheduled.previousScheduledAt)
        assertEquals(members, rescheduled.recipientUserIds.toSet())

        exceptionService.deleteException(clubId = clubId, userId = owner, exceptionId = exception.id)
        val back = matchService.getMatchById(match.id, owner)
        assertEquals(madrid("2026-10-08T20:00"), back.scheduledAt)
        assertEquals(madrid("2026-10-07T22:00"), announcementOf(back).closesAt)
    }

    @Test
    fun `CA-8 an absence withdraws the member, promotes the waitlist and skips their pushes`() {
        val awayAllWeek = newMember()
        absenceService.createAbsence(clubId, awayAllWeek, LocalDate.parse("2026-10-06"), LocalDate.parse("2026-10-09"), "Trip")
        createSchedule()
        notificationService.notifyOpenedAnnouncements()
        assertTrue(awayAllWeek !in publishedEvents().filterIsInstance<MatchEvent.AnnouncementOpened>().single().recipientUserIds)

        val announcement = announcementOf(nextMatch())
        val players = enrollPlayers(announcement, count = 11)
        absenceService.createAbsence(clubId, players[0], thursday, thursday, null)

        val after = announcementService.getMatchAnnouncementById(announcement.id, owner)
        assertTrue(after.entries.none { it.clubMemberId == clubs.memberIdOf(clubId, players[0]) })
        assertEquals(10, after.confirmedEntries.size)
        assertEquals(players.last(), publishedEvents().filterIsInstance<MatchEvent.PromotedFromWaitlist>().single().userId)

        // RN-C4: the absence is informative, the member can still enroll.
        announcementService.enroll(announcement.id, players[0])
        announcementService.withdraw(announcement.id, players[0])
        announcementService.withdraw(announcement.id, players[1])

        clock.now = madrid("2026-10-06T23:00")
        assertEquals(1, notificationService.sendClosingReminders())
        val reminder = publishedEvents().filterIsInstance<MatchEvent.AnnouncementClosingSoon>().single()
        assertTrue(players[0] !in reminder.recipientUserIds)
        assertTrue(awayAllWeek !in reminder.recipientUserIds)
        assertTrue(players[1] in reminder.recipientUserIds)
        assertEquals(2, absenceService.getAbsences(clubId, players[2], thursday, thursday).size)
    }

    @Test
    fun `CA-9 the announcement closes and the teams are drawn at the configured times`() {
        assertFailsWith<InvalidClubMatchScheduleException> {
            createSchedule(close = DeadlineRule(1, LocalTime.of(22, 0)), draw = DeadlineRule(2, LocalTime.of(12, 0)))
        }
        createSchedule(close = DeadlineRule(1, LocalTime.of(21, 0)), draw = DeadlineRule(0, LocalTime.of(12, 0)))
        val match = nextMatch()
        val announcement = announcementOf(match)
        assertEquals(madrid("2026-10-07T21:00"), announcement.closesAt)
        assertEquals(madrid("2026-10-08T12:00"), announcement.drawAt)
        enrollPlayers(announcement, count = 10)

        clock.now = madrid("2026-10-07T21:00")
        scheduler.closeExpiredMatchAnnouncements()
        assertEquals(MatchAnnouncementStatus.CLOSED, announcementOf(match).status)
        assertTrue(matchService.getMatchById(match.id, owner).teamA.isEmpty())

        clock.now = madrid("2026-10-08T12:00")
        scheduler.closeExpiredMatchAnnouncements()
        assertEquals(5, matchService.getMatchById(match.id, owner).teamA.size)
        scheduler.closeExpiredMatchAnnouncements()
        assertEquals(1, publishedEvents().filterIsInstance<MatchEvent.TeamsPublished>().size)

        assertFailsWith<InvalidClubMatchScheduleException> {
            scheduleService.updateSchedule(clubId, owner, null, null, null, null, null, drawDaysBefore = 2)
        }
    }

    @Test
    fun `CA-10 the manual score is the official result`() {
        createSchedule()
        val match = nextMatch()
        val players = enrollPlayers(announcementOf(match), count = 4)
        val teams = teamService.generateTeams(match.id, owner, TeamGenerationMode.AUTO, null, null)
        val scorer = teams.teamB.first()
        eventService.addEvent(match.id, owner, scorer, MatchEventType.GOAL, null)

        assertFailsWith<ForbiddenException> { matchService.setScore(match.id, players[0], 3, 2) }
        val scored = matchService.setScore(match.id, owner, 3, 2)
        assertTrue(scored.isManualScore)
        assertEquals(3, scored.teamAScore)
        assertEquals(2, scored.teamBScore)

        clock.now = madrid("2026-10-08T21:30")
        val completed = matchService.completeMatch(match.id, owner)
        assertFailsWith<InvalidMatchStateException> { matchService.clearScore(match.id, owner) }

        teams.teamA.forEach { assertTrue(completed.ratingChanges.getValue(it) > 0) }
        val scorerStats = statsService.getClubStats(clubId, owner, StatsSortBy.GOALS).first { it.stats.clubMemberId == scorer }.stats
        assertEquals(1, scorerStats.goals)
        assertEquals(1, scorerStats.losses)

        matchService.reopenMatch(match.id, owner)
        val cleared = matchService.clearScore(match.id, owner)
        assertEquals(0, cleared.teamAScore)
        assertEquals(1, cleared.teamBScore)
        assertTrue(ratingService.ratingsFor(clubId, teams.teamA).values.all { abs(it - PlayerRatingCalculator.INITIAL_RATING) < 1e-9 })
    }
}
