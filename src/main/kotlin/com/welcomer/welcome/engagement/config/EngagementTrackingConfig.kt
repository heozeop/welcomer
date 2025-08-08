package com.welcomer.welcome.engagement.config

import com.welcomer.welcome.engagement.model.EngagementTrackingConfig
import com.welcomer.welcome.engagement.repository.EngagementRepository
import com.welcomer.welcome.engagement.repository.InMemoryEngagementRepository
import com.welcomer.welcome.engagement.service.*
import com.welcomer.welcome.engagement.event.EngagementEventPublisher
import com.welcomer.welcome.user.service.BehaviorAnalysisService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Spring configuration for engagement tracking system
 */
@Configuration
@EnableScheduling
class EngagementTrackingConfiguration {

    @Bean
    fun engagementTrackingConfig(): EngagementTrackingConfig {
        return EngagementTrackingConfig(
            enableRealTimeProcessing = true,
            batchSize = 100,
            flushIntervalSeconds = 10,
            maxRetries = 3,
            retryDelayMillis = 1000,
            enableDuplicateDetection = true,
            duplicateWindowMinutes = 5,
            enableAnonymousTracking = false,
            minDwellTimeSeconds = 3,
            maxDwellTimeSeconds = 600
        )
    }

    @Bean
    fun engagementRepository(): EngagementRepository {
        return InMemoryEngagementRepository()
    }

    @Bean
    fun engagementCoroutineScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Default)
    }

    @Bean
    fun metricsAggregationService(
        engagementRepository: EngagementRepository,
        scope: CoroutineScope
    ): MetricsAggregationService {
        return DefaultMetricsAggregationService(engagementRepository, scope)
    }

    @Bean
    fun engagementEventPublisher(
        applicationEventPublisher: ApplicationEventPublisher,
        behaviorAnalysisService: BehaviorAnalysisService,
        scope: CoroutineScope
    ): EngagementEventPublisher {
        return EngagementEventPublisher(
            applicationEventPublisher,
            behaviorAnalysisService,
            scope
        )
    }

    @Bean
    fun engagementTrackingService(
        engagementRepository: EngagementRepository,
        metricsAggregationService: MetricsAggregationService,
        engagementEventPublisher: EngagementEventPublisher,
        config: EngagementTrackingConfig
    ): EngagementTrackingService {
        return DefaultEngagementTrackingService(
            engagementRepository,
            metricsAggregationService,
            engagementEventPublisher,
            config
        )
    }

    @Bean
    fun pipelineConfig(): PipelineConfig {
        return PipelineConfig(
            processingWorkers = 4,
            batchSize = 50,
            eventBufferSize = 1000,
            metricsBufferSize = 100,
            aggregationWindowSize = 10,
            healthCheckIntervalMs = 5000,
            maxEventAgeMinutes = 60,
            maxDropRate = 0.01,
            circuitBreakerThreshold = 5,
            circuitBreakerResetSeconds = 30,
            pipelineVersion = "1.0.0"
        )
    }

    @Bean
    fun realTimeProcessingPipeline(
        engagementTrackingService: EngagementTrackingService,
        metricsAggregationService: MetricsAggregationService,
        pipelineConfig: PipelineConfig
    ): RealTimeProcessingPipeline {
        return RealTimeProcessingPipeline(
            engagementTrackingService,
            metricsAggregationService,
            pipelineConfig
        )
    }
}