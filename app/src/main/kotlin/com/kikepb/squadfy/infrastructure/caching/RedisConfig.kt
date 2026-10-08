package com.kikepb.squadfy.infrastructure.caching

import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import tools.jackson.module.kotlin.KotlinModule
import java.time.Duration

@Configuration
@EnableCaching
class RedisConfig {

    /** Cached values are JSON written with Jackson 3 (spec 012 RN-F1); only our classes and collections are typed. */
    @Bean
    fun cacheManager(connectionFactory: LettuceConnectionFactory): RedisCacheManager {
        val typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("java.util.")
            .allowIfSubType("java.time.")
            .allowIfBaseType("kotlin.collections.")
            .allowIfSubType("com.kikepb.squadfy.")
            .build()

        val serializer = GenericJacksonJsonRedisSerializer.builder()
            .customize { it.addModule(KotlinModule.Builder().build()) }
            .enableDefaultTyping(typeValidator)
            .enableSpringCacheNullValueSupport()
            .build()

        val cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1L))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfig)
            .withCacheConfiguration(
                "messages",
                cacheConfig.entryTtl(Duration.ofMinutes(30))
            )
            .transactionAware()
            .build()
    }
}
