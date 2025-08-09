package com.welcomer.welcome.bdd

import com.welcomer.welcome.personalization.service.*
import org.mockito.kotlin.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

/**
 * Test configuration for BDD scenarios
 * 
 * This configuration provides mock or test implementations of services
 * needed for BDD testing to avoid database dependencies.
 */
@TestConfiguration
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