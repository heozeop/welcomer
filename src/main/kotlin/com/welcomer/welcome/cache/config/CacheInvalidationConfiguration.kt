package com.welcomer.welcome.cache.config

import com.welcomer.welcome.cache.invalidation.CacheInvalidationService
import com.welcomer.welcome.cache.provider.CacheProvider
import kotlinx.coroutines.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Configuration for cache invalidation system
 */
@Configuration
@EnableScheduling
class CacheInvalidationConfiguration {

    @Bean
    fun cacheInvalidationProperties(): CacheInvalidationProperties {
        return CacheInvalidationProperties(
            enableTimeBasedInvalidation = true,
            timeBasedInvalidationIntervalMinutes = 5,
            enableEventDrivenInvalidation = true,
            batchInvalidationSize = 50,
            maxInvalidationRetries = 3,
            invalidationTimeoutSeconds = 30,
            recentlyInvalidatedCleanupIntervalMinutes = 15
        )
    }
}

/**
 * Configuration properties for cache invalidation
 */
data class CacheInvalidationProperties(
    val enableTimeBasedInvalidation: Boolean,
    val timeBasedInvalidationIntervalMinutes: Long,
    val enableEventDrivenInvalidation: Boolean,
    val batchInvalidationSize: Int,
    val maxInvalidationRetries: Int,
    val invalidationTimeoutSeconds: Long,
    val recentlyInvalidatedCleanupIntervalMinutes: Long
)

/**
 * Scheduled tasks for cache invalidation maintenance
 */
@Component
class CacheInvalidationScheduler(
    private val cacheInvalidationService: CacheInvalidationService,
    private val invalidationProperties: CacheInvalidationProperties
) {
    
    private val schedulerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Scheduled(fixedDelayString = "#{@cacheInvalidationProperties.timeBasedInvalidationIntervalMinutes * 60 * 1000}")
    fun performTimeBasedInvalidation() {
        if (!invalidationProperties.enableTimeBasedInvalidation) {
            return
        }
        
        schedulerScope.launch {
            try {
                cacheInvalidationService.performTimeBasedInvalidation()
            } catch (e: Exception) {
                println("Error in scheduled time-based invalidation: ${e.message}")
            }
        }
    }

    @Scheduled(fixedDelayString = "#{@cacheInvalidationProperties.recentlyInvalidatedCleanupIntervalMinutes * 60 * 1000}")
    fun cleanupRecentlyInvalidated() {
        schedulerScope.launch {
            try {
                cacheInvalidationService.cleanupRecentlyInvalidated()
            } catch (e: Exception) {
                println("Error in recently invalidated cleanup: ${e.message}")
            }
        }
    }
}