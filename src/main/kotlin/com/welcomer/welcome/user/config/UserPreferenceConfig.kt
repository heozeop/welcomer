package com.welcomer.welcome.user.config

import com.welcomer.welcome.user.model.BehaviorAnalysisConfig
import com.welcomer.welcome.user.service.PreferenceMergingConfig
import com.welcomer.welcome.user.repository.DefaultPreferenceRepository
import com.welcomer.welcome.user.repository.PreferenceRepository
import com.welcomer.welcome.user.service.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

/**
 * Spring configuration for user preference management system
 */
@Configuration
@EnableAsync
class UserPreferenceConfig {

    @Bean
    fun preferenceRepository(): PreferenceRepository {
        return DefaultPreferenceRepository()
    }

    @Bean
    fun behaviorAnalysisConfig(): BehaviorAnalysisConfig {
        return BehaviorAnalysisConfig(
            recentActivityWindowDays = 30,
            minimumEventCountForInference = 10,
            dwellTimeThresholdSeconds = 30,
            decayFactor = 0.95,
            topicConfidenceThreshold = 0.6,
            contentTypeConfidenceThreshold = 0.7
        )
    }

    @Bean
    fun preferenceMergingConfig(): PreferenceMergingConfig {
        return PreferenceMergingConfig(
            explicitWeight = 0.7,
            implicitWeight = 0.3,
            confidenceThreshold = 0.3,
            maxPreferencesPerType = 50,
            decayDays = 90
        )
    }

    @Bean
    fun behaviorAnalysisService(config: BehaviorAnalysisConfig): BehaviorAnalysisService {
        return DefaultBehaviorAnalysisService(config)
    }

    @Bean
    fun userPreferenceService(
        preferenceRepository: PreferenceRepository,
        behaviorAnalysisService: BehaviorAnalysisService,
        eventPublisher: ApplicationEventPublisher
    ): UserPreferenceService {
        return DefaultUserPreferenceService(
            preferenceRepository,
            behaviorAnalysisService,
            eventPublisher
        )
    }
}