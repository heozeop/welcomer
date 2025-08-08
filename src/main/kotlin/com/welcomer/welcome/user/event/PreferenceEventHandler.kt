package com.welcomer.welcome.user.event

import com.welcomer.welcome.user.service.PreferencesUpdatedEvent
import com.welcomer.welcome.user.integration.FeedIntegrationService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Event handler for user preference changes
 * Notifies other services (particularly the feed system) when preferences are updated
 */
@Component
class PreferenceEventHandler(
    private val feedIntegrationService: FeedIntegrationService
) {

    /**
     * Handle preference update events by notifying relevant services
     */
    @EventListener
    @Async
    suspend fun handlePreferencesUpdated(event: PreferencesUpdatedEvent) {
        try {
            // Update feed system with new preferences
            feedIntegrationService.handlePreferenceUpdate(event.userId, event.updatedPreferences)
            
            // Notify recommendation service to update user model
            updateRecommendationModel(event.userId, event.updatedPreferences)
            
            // Log event for analytics
            logPreferenceUpdateEvent(event)
            
            // Trigger feed regeneration (async)
            triggerFeedRegeneration(event.userId)
            
        } catch (e: Exception) {
            // Log error but don't let it bubble up
            println("Error handling preference update event for user ${event.userId}: ${e.message}")
        }
    }

    private fun invalidateFeedCache(userId: String) {
        // In production, this would call the actual feed cache service
        println("Invalidating feed cache for user: $userId")
        
        // Example implementation:
        // feedCacheService.invalidateUserFeeds(userId)
        // 
        // This would clear all cached feeds for the user so that the next
        // feed request will generate fresh content based on updated preferences
    }

    private fun updateRecommendationModel(userId: String, updatedPreferences: List<com.welcomer.welcome.user.model.UserPreference>) {
        // In production, this would update ML models or recommendation engines
        println("Updating recommendation model for user: $userId with ${updatedPreferences.size} preference changes")
        
        // Example implementation:
        // recommendationService.updateUserModel(userId, updatedPreferences)
        // 
        // This would:
        // 1. Update user embeddings or feature vectors
        // 2. Retrain personalization models if needed
        // 3. Update similarity calculations with other users
        // 4. Refresh content recommendations
    }

    private fun logPreferenceUpdateEvent(event: PreferencesUpdatedEvent) {
        // In production, this would send to analytics/monitoring system
        println("Preference update event logged: userId=${event.userId}, " +
                "preferenceCount=${event.updatedPreferences.size}, " +
                "timestamp=${event.timestamp}")
        
        // Example implementation:
        // analyticsService.track("preference_updated", mapOf(
        //     "userId" to event.userId,
        //     "preferenceCount" to event.updatedPreferences.size,
        //     "preferenceTypes" to event.updatedPreferences.map { it.type }.toSet(),
        //     "timestamp" to event.timestamp
        // ))
    }

    private fun triggerFeedRegeneration(userId: String) {
        // In production, this would queue a background job to regenerate feeds
        println("Triggering feed regeneration for user: $userId")
        
        // Example implementation:
        // backgroundJobService.scheduleJob("regenerate_user_feed", mapOf(
        //     "userId" to userId,
        //     "priority" to "normal",
        //     "delay" to "1m" // Small delay to batch multiple preference changes
        // ))
    }
}

/**
 * Service for publishing preference-related events to external systems
 */
@Component
class PreferenceEventPublisher {

    /**
     * Publish event to message queue/event bus for external consumption
     */
    fun publishPreferenceUpdate(userId: String, eventType: String, metadata: Map<String, Any> = emptyMap()) {
        // In production, this would publish to Kafka, RabbitMQ, or other message system
        val event = PreferenceChangeEvent(
            userId = userId,
            eventType = eventType,
            metadata = metadata,
            timestamp = Instant.now()
        )
        
        println("Publishing preference event: $event")
        
        // Example implementation:
        // kafkaTemplate.send("preference-updates", event)
        // or
        // eventBus.publish("preferences.updated", event)
    }

    /**
     * Publish bulk preference update event
     */
    fun publishBulkPreferenceUpdate(userId: String, updateCount: Int, preferenceTypes: Set<String>) {
        publishPreferenceUpdate(
            userId = userId,
            eventType = "bulk_update",
            metadata = mapOf(
                "updateCount" to updateCount,
                "preferenceTypes" to preferenceTypes
            )
        )
    }

    /**
     * Publish preference deletion event
     */
    fun publishPreferenceDeletion(userId: String, deletedCount: Int) {
        publishPreferenceUpdate(
            userId = userId,
            eventType = "deleted",
            metadata = mapOf(
                "deletedCount" to deletedCount
            )
        )
    }

    /**
     * Publish implicit preference refresh event
     */
    fun publishImplicitPreferenceRefresh(userId: String, newPreferenceCount: Int) {
        publishPreferenceUpdate(
            userId = userId,
            eventType = "implicit_refresh",
            metadata = mapOf(
                "newPreferenceCount" to newPreferenceCount
            )
        )
    }
}

/**
 * Event data class for external systems
 */
data class PreferenceChangeEvent(
    val userId: String,
    val eventType: String,
    val metadata: Map<String, Any>,
    val timestamp: Instant
)

/**
 * Configuration for preference event publishing
 */
@Component
class PreferenceEventConfig {
    
    companion object {
        const val TOPIC_PREFERENCE_UPDATES = "preference-updates"
        const val TOPIC_FEED_INVALIDATION = "feed-cache-invalidation"
        const val TOPIC_RECOMMENDATION_UPDATES = "recommendation-updates"
        
        // Event types
        const val EVENT_TYPE_UPDATED = "updated"
        const val EVENT_TYPE_DELETED = "deleted"
        const val EVENT_TYPE_BULK_UPDATE = "bulk_update"
        const val EVENT_TYPE_IMPLICIT_REFRESH = "implicit_refresh"
    }
    
    // Configuration properties for event publishing
    val enableEventPublishing: Boolean = true
    val batchEventUpdates: Boolean = true
    val eventBatchSize: Int = 10
    val eventBatchTimeoutMs: Long = 5000
    val retryFailedEvents: Boolean = true
    val maxRetries: Int = 3
}