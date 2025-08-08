package com.welcomer.welcome.cache.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.DefaultClientResources
import java.time.Duration

/**
 * Redis cache configuration with connection pooling and serialization setup
 */
@Configuration
class CacheConfiguration {

    @Value("\${spring.data.redis.host:localhost}")
    private val redisHost: String = "localhost"

    @Value("\${spring.data.redis.port:6379}")
    private val redisPort: Int = 6379

    @Value("\${spring.data.redis.password:}")
    private val redisPassword: String? = null

    @Value("\${spring.data.redis.database:0}")
    private val redisDatabase: Int = 0

    @Value("\${spring.data.redis.timeout:5000}")
    private val redisTimeout: Long = 5000

    @Value("\${spring.data.redis.lettuce.pool.max-active:8}")
    private val maxActive: Int = 8

    @Value("\${spring.data.redis.lettuce.pool.max-idle:8}")
    private val maxIdle: Int = 8

    @Value("\${spring.data.redis.lettuce.pool.min-idle:0}")
    private val minIdle: Int = 0

    @Value("\${spring.data.redis.lettuce.pool.max-wait:-1}")
    private val maxWait: Long = -1

    @Bean
    fun clientResources(): ClientResources {
        return DefaultClientResources.builder()
            .ioThreadPoolSize(4)
            .computationThreadPoolSize(4)
            .build()
    }

    @Bean
    @Primary
    fun redisConnectionFactory(clientResources: ClientResources): RedisConnectionFactory {
        val redisConfig = RedisStandaloneConfiguration().apply {
            hostName = redisHost
            port = redisPort
            database = redisDatabase
            if (!redisPassword.isNullOrBlank()) {
                setPassword(redisPassword)
            }
        }

        val clientConfig = LettuceClientConfiguration.builder()
            .clientResources(clientResources)
            .commandTimeout(Duration.ofMillis(redisTimeout))
            .build()

        return LettuceConnectionFactory(redisConfig, clientConfig)
    }

    @Bean
    fun reactiveStringRedisTemplate(redisConnectionFactory: RedisConnectionFactory): ReactiveStringRedisTemplate {
        return ReactiveStringRedisTemplate(redisConnectionFactory as org.springframework.data.redis.connection.ReactiveRedisConnectionFactory)
    }

    @Bean
    fun stringRedisTemplate(redisConnectionFactory: RedisConnectionFactory): StringRedisTemplate {
        val template = StringRedisTemplate(redisConnectionFactory)
        
        // Configure serializers
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = StringRedisSerializer()
        
        return template
    }

    @Bean
    @Primary
    fun cacheObjectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerKotlinModule()
            .findAndRegisterModules()
    }

    @Bean
    fun jsonRedisSerializer(objectMapper: ObjectMapper): GenericJackson2JsonRedisSerializer {
        return GenericJackson2JsonRedisSerializer(objectMapper)
    }

    /**
     * Cache configuration properties
     */
    @Bean
    fun cacheConfigProperties(): CacheConfigProperties {
        return CacheConfigProperties(
            defaultTtlMinutes = 30,
            feedTtlMinutes = 15,
            userPreferencesTtlHours = 2,
            popularityScoresTtlMinutes = 10,
            preWarmingEnabled = true,
            preWarmingBatchSize = 50,
            cleanupIntervalMinutes = 60,
            maxCacheSize = 10000,
            enableMetrics = true
        )
    }
}

/**
 * Cache configuration properties
 */
data class CacheConfigProperties(
    val defaultTtlMinutes: Long,
    val feedTtlMinutes: Long,
    val userPreferencesTtlHours: Long,
    val popularityScoresTtlMinutes: Long,
    val preWarmingEnabled: Boolean,
    val preWarmingBatchSize: Int,
    val cleanupIntervalMinutes: Long,
    val maxCacheSize: Long,
    val enableMetrics: Boolean
)