package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.events.live.ClubDataChangedEvent
import com.kikepb.squadfy.domain.events.live.ClubDataChangedEvent.Scope
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.MatchId
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Tells connected members what to reload (spec 012 RN-A). Changes of one transaction are collected
 * without duplicates and published once it commits, so a rolled back change is never announced.
 */
@Component
class LiveUpdatePublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun matchChanged(clubId: ClubId, matchId: MatchId) =
        changed(ClubDataChangedEvent(clubId = clubId, scope = Scope.MATCH, matchId = matchId))

    fun scheduleChanged(clubId: ClubId) = changed(ClubDataChangedEvent(clubId = clubId, scope = Scope.SCHEDULE))

    fun absencesChanged(clubId: ClubId) = changed(ClubDataChangedEvent(clubId = clubId, scope = Scope.ABSENCES))

    private fun changed(event: ClubDataChangedEvent) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            applicationEventPublisher.publishEvent(event)
            return
        }

        @Suppress("UNCHECKED_CAST")
        val pending = TransactionSynchronizationManager.getResource(RESOURCE_KEY) as? MutableSet<ClubDataChangedEvent>
            ?: linkedSetOf<ClubDataChangedEvent>().also { events ->
                TransactionSynchronizationManager.bindResource(RESOURCE_KEY, events)
                TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                    override fun afterCommit() = events.forEach(applicationEventPublisher::publishEvent)

                    override fun afterCompletion(status: Int) {
                        TransactionSynchronizationManager.unbindResourceIfPossible(RESOURCE_KEY)
                    }
                })
            }
        pending += event
    }

    private companion object {
        val RESOURCE_KEY = Any()
    }
}
