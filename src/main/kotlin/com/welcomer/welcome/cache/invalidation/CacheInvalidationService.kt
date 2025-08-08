package com.welcomer.welcome.cache.invalidation

import com.welcomer.welcome.cache.provider.CacheProvider
import com.welcomer.welcome.feed.model.FeedType
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Service for managing cache invalidation strategies across the feed system
 */
@Service
class CacheInvalidationService(
    private val cacheProvider: CacheProvider
) {
    
    private val invalidationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Event processing channels
    private val invalidationEventChannel = Channel<InvalidationEvent>(Channel.UNLIMITED)
    
    // Metrics
    private val invalidationCount = AtomicLong(0)
    private val batchInvalidationCount = AtomicLong(0)
    private val patternInvalidationCount = AtomicLong(0)
    
    // Cache of recently invalidated items to avoid duplicate work
    private val recentlyInvalidated = ConcurrentHashMap<String, Instant>()
    
    companion object {
        private const val FEED_CACHE_PREFIX = "v1:feed:"
        private const val USER_PREFS_PREFIX = "v1:user_prefs:"
        private const val POPULARITY_PREFIX = "v1:popularity:"
        private const val RECENT_INVALIDATION_TTL_MINUTES = 5L
    }
    
    init {
        // Start background invalidation processor
        startInvalidationProcessor()
    }
    
    /**
     * Invalidate feed cache for a specific user and feed type
     */
    suspend fun invalidateUserFeed(userId: String, feedType: FeedType) {
        val key = "${FEED_CACHE_PREFIX}${userId}:${feedType.name}"
        
        if (shouldSkipInvalidation(key)) {
            return
        }
        
        try {
            cacheProvider.delete(key)
            markAsRecentlyInvalidated(key)
            invalidationCount.incrementAndGet()
            
            // Also invalidate feed metadata
            val metadataKey = "v1:feed_metadata:${userId}:${feedType.name}"
            cacheProvider.delete(metadataKey)
            
        } catch (e: Exception) {
            println("Error invalidating user feed cache: ${e.message}")
        }
    }
    
    /**
     * Invalidate all feeds for a specific user across all feed types
     */
    suspend fun invalidateAllUserFeeds(userId: String) {
        try {
            val pattern = "${FEED_CACHE_PREFIX}${userId}:*"
            val keys = cacheProvider.findKeys(pattern)
            
            if (keys.isNotEmpty()) {
                cacheProvider.deleteAll(keys)
                keys.forEach { markAsRecentlyInvalidated(it) }
                batchInvalidationCount.incrementAndGet()
            }
            
            // Also invalidate user preferences and metadata
            val prefsKey = "${USER_PREFS_PREFIX}${userId}"
            cacheProvider.delete(prefsKey)
            
            val metadataPattern = "v1:feed_metadata:${userId}:*"
            val metadataKeys = cacheProvider.findKeys(metadataPattern)
            if (metadataKeys.isNotEmpty()) {
                cacheProvider.deleteAll(metadataKeys)
            }
            
        } catch (e: Exception) {
            println("Error invalidating all user feeds: ${e.message}")
        }
    }
    
    /**
     * Invalidate feeds for multiple users (e.g., followers of a content creator)
     */
    suspend fun invalidateUserFeeds(userIds: List<String>, feedTypes: List<FeedType>? = null) {
        invalidationScope.launch {
            val targetFeedTypes = feedTypes ?: FeedType.values().toList()
            
            // Process in batches to avoid overwhelming the cache
            userIds.chunked(50).forEach { batch ->
                coroutineScope {
                    batch.map { userId ->
                        async {
                            if (targetFeedTypes.size == FeedType.values().size) {
                                // Invalidate all feeds for user
                                invalidateAllUserFeeds(userId)
                            } else {
                                // Invalidate specific feed types
                                targetFeedTypes.forEach { feedType ->
                                    invalidateUserFeed(userId, feedType)
                                }
                            }
                        }
                    }.awaitAll()
                }
                
                // Small delay between batches
                delay(10)
            }
        }
    }
    
    /**
     * Invalidate popularity scores for content items
     */
    suspend fun invalidatePopularityScores(contentIds: List<String>) {
        try {
            val keys = contentIds.map { "${POPULARITY_PREFIX}${it}" }
            
            if (keys.isNotEmpty()) {
                cacheProvider.deleteAll(keys)
                keys.forEach { markAsRecentlyInvalidated(it) }
                batchInvalidationCount.incrementAndGet()
            }
            
        } catch (e: Exception) {
            println("Error invalidating popularity scores: ${e.message}")
        }
    }
    
    /**
     * Time-based invalidation for content that changes frequently
     */
    suspend fun performTimeBasedInvalidation() {
        try {
            val now = Instant.now()
            
            // Invalidate trending feeds older than 5 minutes
            val trendingPattern = "${FEED_CACHE_PREFIX}*:TRENDING"
            invalidateByPattern(trendingPattern, "trending feeds")
            
            // Invalidate main feeds older than cache TTL would suggest they need refresh
            // This is more complex and would typically involve checking TTL of existing keys
            
        } catch (e: Exception) {
            println("Error in time-based invalidation: ${e.message}")
        }
    }
    
    /**
     * Content-based invalidation when new content is published
     */
    suspend fun invalidateForNewContent(
        contentCreatorId: String, 
        contentTopics: List<String>,
        affectedUsers: List<String>? = null
    ) {
        invalidationScope.launch {
            try {
                // If we have a list of affected users, use it
                if (!affectedUsers.isNullOrEmpty()) {
                    invalidateUserFeeds(
                        affectedUsers, 
                        listOf(FeedType.HOME, FeedType.FOLLOWING, FeedType.PERSONALIZED)
                    )
                    return@launch
                }
                
                // Otherwise, we need a broader invalidation strategy
                // This could involve:
                // 1. Finding followers of the content creator
                // 2. Finding users interested in the content topics
                // 3. Invalidating trending feeds since new content might affect trending
                
                // For now, invalidate trending feeds as new content can affect what's trending
                val trendingPattern = "${FEED_CACHE_PREFIX}*:TRENDING"
                invalidateByPattern(trendingPattern, "trending feeds for new content")
                
            } catch (e: Exception) {
                println("Error invalidating for new content: ${e.message}")
            }
        }
    }
    
    /**
     * Preference-based invalidation when user preferences change
     */
    suspend fun invalidateForPreferenceChange(userId: String, changedPreferences: Set<PreferenceType>) {
        try {
            // Invalidate user preferences cache
            val prefsKey = "${USER_PREFS_PREFIX}${userId}"
            cacheProvider.delete(prefsKey)
            
            // Determine which feed types are affected by the preference changes
            val affectedFeedTypes = determineAffectedFeedTypes(changedPreferences)
            
            // Invalidate affected feeds
            affectedFeedTypes.forEach { feedType ->
                invalidateUserFeed(userId, feedType)
            }
            
        } catch (e: Exception) {
            println("Error invalidating for preference change: ${e.message}")
        }
    }
    
    /**
     * Selective invalidation to avoid clearing entire cache
     */
    suspend fun selectiveInvalidation(criteria: InvalidationCriteria) {
        invalidationScope.launch {
            try {
                when (criteria) {
                    is InvalidationCriteria.ByUserActivity -> {
                        // Invalidate feeds for users who were recently active
                        val pattern = "${FEED_CACHE_PREFIX}{${criteria.userIds.joinToString(",")}}:*"
                        invalidateByPattern(pattern, "user activity based")
                    }
                    
                    is InvalidationCriteria.ByContentAge -> {
                        // More complex: would need to examine cache entries and their timestamps
                        // For now, invalidate feeds that are likely to contain old content
                        performTimeBasedInvalidation()
                    }
                    
                    is InvalidationCriteria.ByEngagement -> {
                        // Invalidate feeds that might be affected by engagement changes
                        val patterns = listOf(
                            "${FEED_CACHE_PREFIX}*:TRENDING",
                            "${FEED_CACHE_PREFIX}*:EXPLORE"
                        )
                        patterns.forEach { pattern ->
                            invalidateByPattern(pattern, "engagement based")
                        }
                    }
                }
                
            } catch (e: Exception) {
                println("Error in selective invalidation: ${e.message}")
            }
        }
    }
    
    /**
     * Event-driven invalidation through Spring events
     */
    @EventListener
    fun handleContentPublishedEvent(event: ContentPublishedEvent) {
        invalidationScope.launch {
            invalidateForNewContent(
                contentCreatorId = event.creatorId,
                contentTopics = event.topics,
                affectedUsers = event.followerIds
            )
        }
    }
    
    @EventListener
    fun handleUserPreferencesChangedEvent(event: UserPreferencesChangedEvent) {
        invalidationScope.launch {
            invalidateForPreferenceChange(event.userId, event.changedPreferences)
        }
    }
    
    @EventListener
    fun handleUserFollowEvent(event: UserFollowEvent) {
        invalidationScope.launch {
            // When user follows someone, their FOLLOWING feed should be invalidated
            invalidateUserFeed(event.followerId, FeedType.FOLLOWING)
            // Also potentially their HOME and PERSONALIZED feeds
            invalidateUserFeed(event.followerId, FeedType.HOME)
            invalidateUserFeed(event.followerId, FeedType.PERSONALIZED)
        }
    }
    
    /**
     * Get invalidation statistics for monitoring
     */
    fun getInvalidationStats(): InvalidationStats {
        return InvalidationStats(
            totalInvalidations = invalidationCount.get(),
            batchInvalidations = batchInvalidationCount.get(),
            patternInvalidations = patternInvalidationCount.get(),
            recentlyInvalidatedCount = recentlyInvalidated.size.toLong()
        )
    }
    
    /**
     * Cleanup old entries from recently invalidated cache
     */
    suspend fun cleanupRecentlyInvalidated() {
        val cutoff = Instant.now().minusSeconds(RECENT_INVALIDATION_TTL_MINUTES * 60)
        val toRemove = recentlyInvalidated.entries.filter { (_, timestamp) ->
            timestamp.isBefore(cutoff)
        }.map { it.key }
        
        toRemove.forEach { key ->
            recentlyInvalidated.remove(key)
        }
    }
    
    // Private helper methods
    
    private fun startInvalidationProcessor() {
        invalidationScope.launch {
            invalidationEventChannel.consumeAsFlow().collect { event ->
                processInvalidationEvent(event)
            }
        }
    }
    
    private suspend fun processInvalidationEvent(event: InvalidationEvent) {
        try {
            when (event) {
                is InvalidationEvent.UserFeed -> {
                    invalidateUserFeed(event.userId, event.feedType)
                }
                is InvalidationEvent.AllUserFeeds -> {
                    invalidateAllUserFeeds(event.userId)
                }
                is InvalidationEvent.BatchUsers -> {
                    invalidateUserFeeds(event.userIds, event.feedTypes)
                }
                is InvalidationEvent.PopularityScores -> {
                    invalidatePopularityScores(event.contentIds)
                }
            }
        } catch (e: Exception) {
            println("Error processing invalidation event: ${e.message}")
        }
    }
    
    private fun shouldSkipInvalidation(key: String): Boolean {
        val lastInvalidated = recentlyInvalidated[key]
        return lastInvalidated != null && 
               lastInvalidated.isAfter(Instant.now().minusSeconds(RECENT_INVALIDATION_TTL_MINUTES * 60))
    }
    
    private fun markAsRecentlyInvalidated(key: String) {
        recentlyInvalidated[key] = Instant.now()
    }
    
    private suspend fun invalidateByPattern(pattern: String, description: String) {
        try {
            val keys = cacheProvider.findKeys(pattern)
            if (keys.isNotEmpty()) {
                cacheProvider.deleteAll(keys)
                keys.forEach { markAsRecentlyInvalidated(it) }
                patternInvalidationCount.incrementAndGet()
                println("Invalidated ${keys.size} keys for $description")
            }
        } catch (e: Exception) {
            println("Error invalidating by pattern '$pattern': ${e.message}")
        }
    }
    
    private fun determineAffectedFeedTypes(changedPreferences: Set<PreferenceType>): List<FeedType> {
        return when {
            changedPreferences.contains(PreferenceType.INTERESTS) -> {
                listOf(FeedType.HOME, FeedType.PERSONALIZED, FeedType.EXPLORE)
            }
            changedPreferences.contains(PreferenceType.BLOCKED_USERS) -> {
                FeedType.values().toList() // All feeds could be affected
            }
            changedPreferences.contains(PreferenceType.CONTENT_TYPES) -> {
                listOf(FeedType.HOME, FeedType.PERSONALIZED)
            }
            else -> {
                listOf(FeedType.PERSONALIZED) // Default to personalized feed
            }
        }
    }
}

// Supporting data classes and enums

sealed class InvalidationEvent {
    data class UserFeed(val userId: String, val feedType: FeedType) : InvalidationEvent()
    data class AllUserFeeds(val userId: String) : InvalidationEvent()
    data class BatchUsers(val userIds: List<String>, val feedTypes: List<FeedType>? = null) : InvalidationEvent()
    data class PopularityScores(val contentIds: List<String>) : InvalidationEvent()
}

sealed class InvalidationCriteria {
    data class ByUserActivity(val userIds: List<String>) : InvalidationCriteria()
    data class ByContentAge(val maxAgeHours: Int) : InvalidationCriteria()
    data class ByEngagement(val minEngagementThreshold: Double) : InvalidationCriteria()
}

data class InvalidationStats(
    val totalInvalidations: Long,
    val batchInvalidations: Long,
    val patternInvalidations: Long,
    val recentlyInvalidatedCount: Long
)

enum class PreferenceType {
    INTERESTS,
    BLOCKED_USERS,
    BLOCKED_TOPICS,
    CONTENT_TYPES,
    LANGUAGE,
    ENGAGEMENT_HISTORY
}

// Event data classes
data class ContentPublishedEvent(
    val contentId: String,
    val creatorId: String,
    val topics: List<String>,
    val followerIds: List<String>? = null
)

data class UserPreferencesChangedEvent(
    val userId: String,
    val changedPreferences: Set<PreferenceType>
)

data class UserFollowEvent(
    val followerId: String,
    val followedUserId: String
)