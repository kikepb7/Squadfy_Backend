package com.kikepb.squadfy.infrastructure.messaging

import com.kikepb.squadfy.domain.events.club.ClubEvent
import com.kikepb.squadfy.infrastructure.database.ClubMatchDataCleaner
import com.kikepb.squadfy.infrastructure.message_queue.MessageQueues
import com.kikepb.squadfy.service.MatchAnnouncementService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class MatchClubEventListener(
    private val matchAnnouncementService: MatchAnnouncementService,
    private val clubMatchDataCleaner: ClubMatchDataCleaner
) {

    @RabbitListener(queues = [MessageQueues.MATCH_CLUB_EVENTS])
    fun handleClubEvent(event: ClubEvent) {
        when (event) {
            is ClubEvent.MemberLeft ->
                matchAnnouncementService.withdrawFromOpenAnnouncements(clubId = event.clubId, clubMemberId = event.clubMemberId)
            is ClubEvent.MemberKicked ->
                matchAnnouncementService.withdrawFromOpenAnnouncements(clubId = event.clubId, clubMemberId = event.clubMemberId)
            is ClubEvent.ClubDeleted ->
                clubMatchDataCleaner.deleteClubData(clubId = event.clubId)
            else -> Unit
        }
    }
}
