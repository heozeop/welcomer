package com.welcomer.welcome.bdd

import com.welcomer.welcome.personalization.service.*
import org.mockito.kotlin.mock
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration
import org.springframework.cloud.autoconfigure.LifecycleMvcEndpointAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

/**
 * Test configuration for BDD scenarios
 * 
 * This configuration provides mock or test implementations of services
 * needed for BDD testing to avoid database dependencies and external services.
 */
@Configuration
@EnableAutoConfiguration(exclude = [
    FlywayAutoConfiguration::class,
    JooqAutoConfiguration::class,
    KafkaAutoConfiguration::class,
    RefreshAutoConfiguration::class,
    LifecycleMvcEndpointAutoConfiguration::class
])
@ComponentScan(basePackages = ["com.welcomer.welcome.bdd.steps"])
@Profile("bdd-test")
class BddTestConfiguration {

    @Bean
    @Primary
    fun feedPersonalizationService(): FeedPersonalizationService {
        // Return a mock since FeedPersonalizationService is an interface
        return mock()
    }

    @Bean
    @Primary
    fun topicRelevanceService(): TopicRelevanceService {
        return mock()
    }

    @Bean  
    @Primary
    fun sourceAffinityService(): SourceAffinityService {
        return mock()
    }

    @Bean
    @Primary
    fun contextualRelevanceService(): ContextualRelevanceService {
        return mock()
    }
}