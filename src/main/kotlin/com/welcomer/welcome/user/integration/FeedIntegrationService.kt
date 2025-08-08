package com.welcomer.welcome.user.integration

import com.welcomer.welcome.feed.model.UserPreferences as FeedUserPreferences
import com.welcomer.welcome.user.model.UserPreferenceProfile
import com.welcomer.welcome.user.service.UserPreferenceService
import com.welcomer.welcome.feed.service.FeedCacheService
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Integration service that bridges the user preference management system
 * with the existing feed system
 */
@Service
class FeedIntegrationService(
    private val userPreferenceService: UserPreferenceService,
    private val feedCacheService: FeedCacheService
) {

    /**
     * Get user preferences in the format expected by the feed system
     */
    suspend fun getFeedUserPreferences(userId: String): FeedUserPreferences {
        // Try to get from cache first
        val cachedPreferences = feedCacheService.getCachedUserPreferences(userId)
        if (cachedPreferences != null) {
            return cachedPreferences
        }

        // Get from user preference service
        val preferenceProfile = userPreferenceService.getPreferences(userId)
        
        val feedPreferences = if (preferenceProfile != null) {
            convertToFeedPreferences(preferenceProfile)
        } else {
            // Return default preferences if none exist
            createDefaultFeedPreferences(userId)
        }

        // Cache the result
        feedCacheService.cacheUserPreferences(feedPreferences)

        return feedPreferences
    }

    /**
     * Invalidate cached preferences when they're updated
     */
    suspend fun invalidateUserPreferenceCache(userId: String) {
        // Invalidate all feed types for this user
        com.welcomer.welcome.feed.model.FeedType.values().forEach { feedType ->
            feedCacheService.invalidateFeed(userId, feedType)
        }
    }

    /**
     * Refresh user preferences in feed system when they change
     */
    suspend fun refreshUserPreferencesInFeedSystem(userId: String) {
        // Get latest preferences
        val preferenceProfile = userPreferenceService.getPreferences(userId)
        
        if (preferenceProfile != null) {
            val feedPreferences = convertToFeedPreferences(preferenceProfile)
            
            // Update cache with fresh data
            feedCacheService.cacheUserPreferences(feedPreferences)
            
            // Invalidate any existing feeds to force regeneration
            com.welcomer.welcome.feed.model.FeedType.values().forEach { feedType ->
                feedCacheService.invalidateFeed(userId, feedType)
            }
        }
    }

    /**
     * Convert UserPreferenceProfile to FeedUserPreferences
     */
    private fun convertToFeedPreferences(profile: UserPreferenceProfile): FeedUserPreferences {
        // Convert topic interests to a simple list of interests
        val interests = profile.topicInterests.entries
            .filter { it.value > 0.3 } // Only include interests above threshold
            .sortedByDescending { it.value }
            .map { it.key }

        // Convert content type preferences to a set
        val preferredContentTypes = profile.contentTypePreferences.entries
            .filter { it.value > 0.3 }
            .map { it.key }
            .toSet()

        // Create engagement history from topic and content type preferences
        val engagementHistory = mutableMapOf<String, Double>()
        
        // Add topic engagement scores
        profile.topicInterests.forEach { (topic, score) ->
            engagementHistory["topic:$topic"] = score
        }
        
        // Add content type engagement scores  
        profile.contentTypePreferences.forEach { (contentType, score) ->
            engagementHistory["content_type:$contentType"] = score
        }

        return FeedUserPreferences(
            userId = profile.userId,
            interests = interests,
            preferredContentTypes = preferredContentTypes,
            blockedUsers = profile.blockedUsers,
            blockedTopics = profile.blockedTopics,
            languagePreferences = profile.languagePreferences,
            engagementHistory = engagementHistory,
            lastActiveAt = profile.lastUpdated,
            accountAge = calculateAccountAge(profile.userId) // Would calculate from user creation date
        )
    }

    /**
     * Create default preferences for new users
     */
    private fun createDefaultFeedPreferences(userId: String): FeedUserPreferences {
        return FeedUserPreferences(
            userId = userId,
            interests = listOf("technology", "news", "entertainment"), // Default interests
            preferredContentTypes = setOf("text", "image"),
            blockedUsers = emptySet(),
            blockedTopics = emptySet(),
            languagePreferences = listOf("en"),
            engagementHistory = emptyMap(),
            lastActiveAt = Instant.now(),
            accountAge = 0L
        )
    }

    /**
     * Calculate account age in days
     * In production, this would query the user creation date from the database
     */
    private fun calculateAccountAge(@Suppress("UNUSED_PARAMETER") userId: String): Long {
        // Placeholder implementation
        // In reality, would query: SELECT DATEDIFF(NOW(), created_at) FROM users WHERE id = ?
        return 1L // Default to 1 day for new users
    }

    /**
     * Update feed system when user preferences change
     * This should be called from the preference event handler
     */
    suspend fun handlePreferenceUpdate(userId: String, updatedPreferences: List<com.welcomer.welcome.user.model.UserPreference>) {
        try {
            // Refresh preferences in feed system
            refreshUserPreferencesInFeedSystem(userId)
            
            // Log the update for monitoring
            println("Updated feed preferences for user: $userId, changes: ${updatedPreferences.size}")
            
        } catch (e: Exception) {
            // Log error but don't let it bubble up
            println("Error updating feed preferences for user $userId: ${e.message}")
        }
    }
}