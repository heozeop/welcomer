package com.welcomer.welcome.bdd

import com.welcomer.welcome.personalization.service.*
import com.welcomer.welcome.feed.service.*
import com.welcomer.welcome.user.service.*
import com.welcomer.welcome.diversity.service.*
import com.welcomer.welcome.engagement.service.*
import org.mockito.kotlin.mock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate

/**
 * Minimal Spring Boot configuration for BDD tests
 * This provides all necessary beans without complex auto-configurations
 */
@SpringBootApplication(
    scanBasePackages = ["com.welcomer.welcome.bdd"],
    exclude = [
        org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration::class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration::class,
        org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration::class,
        org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration::class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration::class
    ]
)
class MinimalBddConfiguration {

    // Personalization Services
    @Bean
    @Primary
    fun feedPersonalizationService(): FeedPersonalizationService = mock()

    @Bean
    @Primary
    fun topicRelevanceService(): TopicRelevanceService = mock()

    @Bean
    @Primary
    fun sourceAffinityService(): SourceAffinityService = mock()

    @Bean
    @Primary
    fun contextualRelevanceService(): ContextualRelevanceService = mock()

    // User Services
    @Bean
    fun userPreferenceService(): UserPreferenceService = mock()

    @Bean
    fun userHistoryService(): UserHistoryService = mock()

    @Bean
    fun userContextService(): UserContextService = mock()

    @Bean
    fun behaviorAnalysisService(): BehaviorAnalysisService = mock()

    // Feed Services
    @Bean
    fun contentScoringService(): ContentScoringService = mock()

    @Bean
    fun feedAnalyticsService(): FeedAnalyticsService = mock()

    @Bean
    fun abTestingService(): ABTestingService = mock()

    @Bean
    fun feedGenerationService(): FeedGenerationService = mock()

    @Bean
    fun coldStartService(): ColdStartService = mock()

    @Bean
    fun feedDiversityService(): FeedDiversityService = mock()

    @Bean
    fun feedCacheService(): FeedCacheService = mock()

    // Diversity Services
    @Bean
    fun echoChamberPreventionService(): EchoChamberPreventionService = mock()

    @Bean
    fun contentFreshnessService(): ContentFreshnessService = mock()

    @Bean
    fun contentBalancingService(): ContentBalancingService = mock()

    @Bean
    fun contentDiversityIntegrationService(): ContentDiversityIntegrationService = mock()

    // Engagement Service
    @Bean
    fun engagementTrackingService(): EngagementTrackingService = mock()

    @Bean
    fun metricsAggregationService(): MetricsAggregationService = mock()

    // Kafka Template - required by some services
    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> = mock()

    // Additional infrastructure beans for step definitions
    @Bean
    fun dataSource(): javax.sql.DataSource = mock()

    @Bean 
    fun jdbcTemplate(): org.springframework.jdbc.core.JdbcTemplate = mock()

    @Bean
    fun redisTemplate(): org.springframework.data.redis.core.RedisTemplate<String, Any> = mock()

    @Bean
    fun stringRedisTemplate(): org.springframework.data.redis.core.StringRedisTemplate = mock()

    @Bean
    fun transactionManager(): org.springframework.transaction.PlatformTransactionManager = mock()

    @Bean
    fun taskExecutor(): org.springframework.core.task.TaskExecutor = mock()

    @Bean
    fun applicationEventPublisher(): org.springframework.context.ApplicationEventPublisher = mock()
}