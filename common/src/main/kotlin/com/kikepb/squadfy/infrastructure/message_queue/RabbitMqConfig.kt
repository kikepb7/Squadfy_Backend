@file:Suppress("DEPRECATION")

package com.kikepb.squadfy.infrastructure.message_queue

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.kikepb.squadfy.domain.events.SquadfyEvent
import com.kikepb.squadfy.domain.events.chat.ChatEvent
import com.kikepb.squadfy.domain.events.club.ClubEvent
import com.kikepb.squadfy.domain.events.club.ClubEventConstants
import com.kikepb.squadfy.domain.events.match.MatchEvent
import com.kikepb.squadfy.domain.events.match.MatchEventConstants
import com.kikepb.squadfy.domain.events.user.UserEvent
import com.kikepb.squadfy.domain.events.chat.ChatEventConstant
import com.kikepb.squadfy.domain.events.user.UserEventConstants
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
class RabbitMqConfig {

    @Bean
    fun messageConverter(): Jackson2JsonMessageConverter {
        val objectMapper = ObjectMapper().apply {
            registerModules(KotlinModule.Builder().build())
            registerModule(JavaTimeModule())
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

        // Only our own event classes may be instantiated from the __TypeId__ header (the default
        // only trusts java.util / java.lang, which rejected every event). Matching is per exact
        // package, so every event family is listed.
        val typeMapper = DefaultJackson2JavaTypeMapper().apply {
            setTrustedPackages(
                SquadfyEvent::class.java.packageName,
                UserEvent::class.java.packageName,
                ChatEvent::class.java.packageName,
                ClubEvent::class.java.packageName,
                MatchEvent::class.java.packageName
            )
            typePrecedence = Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID
        }

        return Jackson2JsonMessageConverter(objectMapper).apply {
            javaTypeMapper = typeMapper
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
    fun chatExchange() = TopicExchange(
        ChatEventConstant.CHAT_EXCHANGE,
        true,
        false
    )

    @Bean
    fun clubExchange() = TopicExchange(
        ClubEventConstants.CLUB_EXCHANGE,
        true,
        false
    )

    @Bean
    fun matchExchange() = TopicExchange(
        MatchEventConstants.MATCH_EXCHANGE,
        true,
        false
    )

    @Bean
    fun notificationMatchEventQueue() = Queue(
        MessageQueues.NOTIFICATION_MATCH_EVENTS,
        true
    )

    /** notification sends the push notifications of the match cycle (spec 005). */
    @Bean
    fun notificationMatchEventsBinding(notificationMatchEventQueue: Queue, matchExchange: TopicExchange): Binding =
        BindingBuilder.bind(notificationMatchEventQueue).to(matchExchange).with("match.#")

    @Bean
    fun matchClubEventQueue() = Queue(
        MessageQueues.MATCH_CLUB_EVENTS,
        true
    )

    /** match removes former members from open announcements (spec 001 RN-10). */
    @Bean
    fun matchClubMemberLeftBinding(matchClubEventQueue: Queue, clubExchange: TopicExchange): Binding =
        BindingBuilder.bind(matchClubEventQueue).to(clubExchange).with(ClubEventConstants.CLUB_MEMBER_LEFT)

    @Bean
    fun matchClubMemberKickedBinding(matchClubEventQueue: Queue, clubExchange: TopicExchange): Binding =
        BindingBuilder.bind(matchClubEventQueue).to(clubExchange).with(ClubEventConstants.CLUB_MEMBER_KICKED)

    @Bean
    fun notificationUserEventQueue() = Queue(
        MessageQueues.NOTIFICATION_USER_EVENTS,
        true
    )

    @Bean
    fun notificationChatEventsQueue() = Queue(
        MessageQueues.NOTIFICATION_CHAT_EVENTS,
        true
    )

    @Bean
    fun chatUserEventQueue() = Queue(
        MessageQueues.CHAT_USER_EVENTS,
        true
    )

    @Bean
    fun clubUserEventQueue() = Queue(
        MessageQueues.CLUB_USER_EVENTS,
        true
    )

    @Bean
    fun notificationUserEventsBinding(notificationUserEventQueue: Queue, userExchange: TopicExchange): Binding {
        return BindingBuilder
            .bind(notificationUserEventQueue)
            .to(userExchange)
            .with("user.*")
    }

    @Bean
    fun notificationChatEventsBinding(@Qualifier("notificationChatEventsQueue") notificationChatEventQueue: Queue, chatExchange: TopicExchange): Binding {
        return BindingBuilder
            .bind(notificationChatEventQueue)
            .to(chatExchange)
            .with(ChatEventConstant.CHAT_NEW_MESSAGE)
    }

    @Bean
    fun chatUserEventsBinding(chatUserEventQueue: Queue, userExchange: TopicExchange): Binding {
        return BindingBuilder
            .bind(chatUserEventQueue)
            .to(userExchange)
            .with("user.*")
    }

    @Bean
    fun clubUserEventsBinding(clubUserEventQueue: Queue, userExchange: TopicExchange): Binding {
        return BindingBuilder
            .bind(clubUserEventQueue)
            .to(userExchange)
            .with("user.*")
    }
}
