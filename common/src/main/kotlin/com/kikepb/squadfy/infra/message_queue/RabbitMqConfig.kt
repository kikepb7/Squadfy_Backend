@file:Suppress("DEPRECATION")

package com.kikepb.squadfy.infra.message_queue

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.kikepb.squadfy.domain.events.SquadfyEvent
import com.kikepb.squadfy.domain.events.user.UserEventConstants
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
class RabbitMqConfig {

    @Bean
    fun messageConverter(): Jackson2JsonMessageConverter {
        val objectMapper = ObjectMapper().apply {
            registerModules(KotlinModule.Builder().build())
            findAndRegisterModules()

            val polymorphicTypeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(SquadfyEvent::class.java)
                .allowIfSubType("java.util.") // Allow Java list
                .allowIfBaseType("kotlin.collections.") // Kotlin collection
                .build()

            activateDefaultTyping(
                polymorphicTypeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL
            )
        }

        return Jackson2JsonMessageConverter(objectMapper).apply {
            typePrecedence = Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID
        }
    }

    /*@Bean
    fun rabbitListenerContainerFactory(connectionFactory: ConnectionFactory, transactionManager: PlatformTransactionManager): SimpleRabbitListenerContainerFactory {
        return SimpleRabbitListenerContainerFactory().apply {
            this.setConnectionFactory(connectionFactory)
            this.setTransactionManager(transactionManager)
            this.setChannelTransacted(true)
        }
    }*/

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory, messageConverter: Jackson2JsonMessageConverter): RabbitTemplate {
        return RabbitTemplate(connectionFactory).apply {
            this.messageConverter = messageConverter
        }
    }

    @Bean
    fun userExchange() = TopicExchange(
        UserEventConstants.USER_EXCHANGE,
        true,
        false
    )

    @Bean
    fun notificationUserEventQueue() = Queue(
        MessageQueues.NOTIFICATION_USER_EVENTS,
        true
    )

    @Bean
    fun notificationUserEventsBinding(notificationUserEventQueue: Queue, userExchange: TopicExchange): Binding {
        return BindingBuilder
            .bind(notificationUserEventQueue)
            .to(userExchange)
            .with("user.*")
    }
}