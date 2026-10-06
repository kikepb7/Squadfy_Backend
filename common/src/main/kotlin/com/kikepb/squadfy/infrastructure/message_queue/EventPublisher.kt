package com.kikepb.squadfy.infrastructure.message_queue

import com.kikepb.squadfy.domain.events.SquadfyEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
class EventPublisher(
    private val rabbitTemplate: RabbitTemplate
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Publishes once the current transaction commits, so listeners never see changes that are
     * rolled back. Without an active transaction it publishes immediately.
     */
    fun <T : SquadfyEvent> publishAfterCommit(event: T) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(event)
            return
        }
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() = publish(event)
        })
    }

    fun <T: SquadfyEvent> publish(event: T) {
        try {
            rabbitTemplate.convertAndSend(
                event.exchange,
                event.eventKey,
                event
            )
            logger.info("Successfully published event: ${event.eventKey}")
        } catch (e: Exception) {
            logger.error("Failed to publish ${event.eventKey}", e)
        }
    }
}