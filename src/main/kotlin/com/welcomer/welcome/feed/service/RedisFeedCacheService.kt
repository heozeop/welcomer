package com.welcomer.welcome.feed.service

import com.welcomer.welcome.cache.config.CacheConfigProperties
import com.welcomer.welcome.cache.provider.CacheProvider
import com.welcomer.welcome.feed.model.*
import kotlinx.coroutines.*
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/**
 * Redis-based implementation of FeedCacheService with advanced caching features
 */
@Service
@Primary
class RedisFeedCacheService(
    private val cacheProvider: CacheProvider,
    @Lazy private val feedGenerationService: FeedGenerationService,
    private val cacheConfig: CacheConfigProperties
) : FeedCacheService {
    
    // Statistics tracking
    private val feedHits = AtomicLong(0)
    private val feedMisses = AtomicLong(0)
    private val preferencesHits = AtomicLong(0)
    private val preferencesMisses = AtomicLong(0)
    private val popularityHits = AtomicLong(0)
    private val popularityMisses = AtomicLong(0)
    
    // Background coroutine scope for cache operations
    private val cacheScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val FEED_KEY_PREFIX = "feed:"
        private const val USER_PREFS_KEY_PREFIX = "user_prefs:"
        private const val POPULARITY_KEY_PREFIX = "popularity:"
        private const val CACHE_VERSION = "v1:"
    }
    
    override suspend fun getCachedFeed(userId: String, feedType: FeedType): GeneratedFeed? {
        return try {
            val key = generateFeedCacheKey(userId, feedType)
            val cachedFeed = cacheProvider.get(key, GeneratedFeed::class.java)
            
            if (cachedFeed != null) {
                feedHits.incrementAndGet()
                // Check if feed is still fresh enough
                if (isFeedFresh(cachedFeed, feedType)) {
                    cachedFeed
                } else {
                    // Feed exists but is stale, remove it
                    cacheProvider.delete(key)
                    feedMisses.incrementAndGet()
                    null
                }
            } else {
                feedMisses.incrementAndGet()
                null
            }
        } catch (e: Exception) {
            feedMisses.incrementAndGet()
            println("Error getting cached feed: ${e.message}")
            null
        }
    }
    
    override suspend fun cacheFeed(feed: GeneratedFeed, ttl: Duration) {
        try {
            val key = generateFeedCacheKey(feed.userId, feed.feedType)
            val adjustedTtl = calculateOptimalTtl(feed, ttl)
            
            cacheProvider.set(key, feed, adjustedTtl)
            
            // Also cache feed metadata for analytics
            cacheFeedMetadata(feed)
        } catch (e: Exception) {
            println("Error caching feed: ${e.message}")
        }
    }
    
    override suspend fun invalidateFeed(userId: String, feedType: FeedType) {
        try {
            val key = generateFeedCacheKey(userId, feedType)
            cacheProvider.delete(key)
            
            // Also invalidate related metadata
            invalidateFeedMetadata(userId, feedType)
        } catch (e: Exception) {
            println("Error invalidating feed: ${e.message}")
        }
    }
    
    override suspend fun cacheUserPreferences(preferences: UserPreferences, ttl: Duration) {
        try {
            val key = generateUserPrefsKey(preferences.userId)
            val adjustedTtl = Duration.ofHours(cacheConfig.userPreferencesTtlHours)
            
            cacheProvider.set(key, preferences, adjustedTtl)
        } catch (e: Exception) {
            println("Error caching user preferences: ${e.message}")
        }
    }
    
    override suspend fun getCachedUserPreferences(userId: String): UserPreferences? {
        return try {
            val key = generateUserPrefsKey(userId)
            val cached = cacheProvider.get(key, UserPreferences::class.java)
            
            if (cached != null) {
                preferencesHits.incrementAndGet()
                cached
            } else {
                preferencesMisses.incrementAndGet()
                null
            }
        } catch (e: Exception) {
            preferencesMisses.incrementAndGet()
            println("Error getting cached user preferences: ${e.message}")
            null
        }
    }
    
    override suspend fun cachePopularityScores(scores: Map<String, Double>, ttl: Duration) {
        try {
            val adjustedTtl = Duration.ofMinutes(cacheConfig.popularityScoresTtlMinutes)
            
            // Cache scores in batch for efficiency
            coroutineScope {
                scores.map { (contentId, score) ->
                    async {
                        val key = generatePopularityKey(contentId)
                        cacheProvider.set(key, score, adjustedTtl)
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            println("Error caching popularity scores: ${e.message}")
        }
    }
    
    override suspend fun getCachedPopularityScore(contentId: String): Double? {
        return try {
            val key = generatePopularityKey(contentId)
            val score = cacheProvider.get(key, Double::class.java)
            
            if (score != null) {
                popularityHits.incrementAndGet()
                score
            } else {
                popularityMisses.incrementAndGet()
                null
            }
        } catch (e: Exception) {
            popularityMisses.incrementAndGet()
            println("Error getting cached popularity score: ${e.message}")
            null
        }
    }
    
    override suspend fun preWarmCache(userIds: List<String>, feedTypes: List<FeedType>) {
        if (!cacheConfig.preWarmingEnabled) {
            return
        }
        
        // Launch pre-warming in background to not block the caller
        cacheScope.launch {
            try {
                val batches = userIds.chunked(cacheConfig.preWarmingBatchSize)
                
                batches.forEach { batch ->
                    // Process batch of users concurrently
                    coroutineScope {
                        batch.map { userId ->
                            async {
                                feedTypes.forEach { feedType ->
                                    preWarmUserFeed(userId, feedType)
                                }
                            }
                        }.awaitAll()
                    }
                    
                    // Small delay between batches to avoid overwhelming the system
                    delay(100)
                }
                
                println("Pre-warming completed for ${userIds.size} users and ${feedTypes.size} feed types")
            } catch (e: Exception) {
                println("Error during cache pre-warming: ${e.message}")
            }
        }
    }
    
    override fun getCacheStats(): CacheStats {
        val feedHitRate = if (feedHits.get() + feedMisses.get() > 0) {
            feedHits.get().toDouble() / (feedHits.get() + feedMisses.get())
        } else 0.0
        
        val preferencesHitRate = if (preferencesHits.get() + preferencesMisses.get() > 0) {
            preferencesHits.get().toDouble() / (preferencesHits.get() + preferencesMisses.get())
        } else 0.0
        
        val popularityHitRate = if (popularityHits.get() + popularityMisses.get() > 0) {
            popularityHits.get().toDouble() / (popularityHits.get() + popularityMisses.get())
        } else 0.0
        
        return CacheStats(
            feedCacheSize = 0, // Would need to query Redis for actual count
            userPreferencesCacheSize = 0,
            popularityScoresCacheSize = 0,
            feedHitRate = feedHitRate,
            preferencesHitRate = preferencesHitRate,
            popularityHitRate = popularityHitRate,
            totalMemoryUsed = 0 // Would need Redis memory info
        )
    }
    
    /**
     * Get comprehensive feed from cache or generate new one
     */
    suspend fun getFeed(userId: String, feedType: FeedType, options: FeedOptions = FeedOptions()): GeneratedFeed {
        // Try cache first
        val cachedFeed = getCachedFeed(userId, feedType)
        if (cachedFeed != null) {
            return cachedFeed
        }
        
        // Generate new feed if not in cache
        val request = FeedGenerationRequest(
            userId = userId,
            feedType = feedType,
            limit = options.limit,
            parameters = options.customFilters
        )
        val feed = feedGenerationService.generateFeed(request)
        
        // Cache the generated feed
        val ttl = calculateTtlForFeedType(feedType)
        cacheFeed(feed, ttl)
        
        return feed
    }
    
    /**
     * Invalidate all feeds for a user across all feed types
     */
    suspend fun invalidateUserCache(userId: String) {
        try {
            val pattern = "${CACHE_VERSION}${FEED_KEY_PREFIX}${userId}:*"
            val keys = cacheProvider.findKeys(pattern)
            
            if (keys.isNotEmpty()) {
                cacheProvider.deleteAll(keys)
            }
            
            // Also invalidate user preferences
            val prefsKey = generateUserPrefsKey(userId)
            cacheProvider.delete(prefsKey)
            
        } catch (e: Exception) {
            println("Error invalidating user cache: ${e.message}")
        }
    }
    
    /**
     * Batch get multiple cached feeds
     */
    suspend fun getBatchCachedFeeds(requests: List<Pair<String, FeedType>>): Map<Pair<String, FeedType>, GeneratedFeed?> {
        return coroutineScope {
            requests.associateWith { (userId, feedType) ->
                async { getCachedFeed(userId, feedType) }
            }.mapValues { (_, deferred) ->
                deferred.await()
            }
        }
    }
    
    // Private helper methods
    
    private fun generateFeedCacheKey(userId: String, feedType: FeedType): String {
        return "${CACHE_VERSION}${FEED_KEY_PREFIX}${userId}:${feedType.name}"
    }
    
    private fun generateCacheKey(userId: String, feedType: FeedType, options: FeedOptions): String {
        val optionsHash = options.hashCode()
        return "${generateFeedCacheKey(userId, feedType)}:${optionsHash}"
    }
    
    private fun generateUserPrefsKey(userId: String): String {
        return "${CACHE_VERSION}${USER_PREFS_KEY_PREFIX}${userId}"
    }
    
    private fun generatePopularityKey(contentId: String): String {
        return "${CACHE_VERSION}${POPULARITY_KEY_PREFIX}${contentId}"
    }
    
    private fun isFeedFresh(feed: GeneratedFeed, feedType: FeedType): Boolean {
        val age = Duration.between(feed.metadata.generatedAt, Instant.now())
        val maxAge = calculateTtlForFeedType(feedType)
        
        return age < maxAge
    }
    
    private fun calculateTtlForFeedType(feedType: FeedType): Duration {
        return when (feedType) {
            FeedType.HOME -> Duration.ofMinutes(cacheConfig.feedTtlMinutes)
            FeedType.TRENDING -> Duration.ofMinutes(5) // Trending content changes quickly
            FeedType.FOLLOWING -> Duration.ofMinutes(cacheConfig.feedTtlMinutes)
            FeedType.PERSONALIZED -> Duration.ofMinutes(cacheConfig.feedTtlMinutes * 2) // Personalized can be cached longer
            FeedType.EXPLORE -> Duration.ofMinutes(cacheConfig.feedTtlMinutes)
        }
    }
    
    private fun calculateOptimalTtl(feed: GeneratedFeed, @Suppress("UNUSED_PARAMETER") defaultTtl: Duration): Duration {
        // Adjust TTL based on feed characteristics
        val baseTtl = calculateTtlForFeedType(feed.feedType)
        
        // Shorter TTL for feeds with highly dynamic content (trending, recent)
        val dynamicContentRatio = feed.entries.count { 
            it.reasons.any { reason -> 
                reason.type in setOf(FeedReasonType.TRENDING, FeedReasonType.RECENCY) 
            } 
        } / feed.entries.size.toFloat()
        if (dynamicContentRatio > 0.5) {
            return baseTtl.dividedBy(2)
        }
        
        // Longer TTL for feeds with mostly evergreen content (similar users, topic interest)
        val evergreenRatio = feed.entries.count { 
            it.reasons.any { reason -> 
                reason.type in setOf(FeedReasonType.SIMILAR_USERS, FeedReasonType.TOPIC_INTEREST) 
            } 
        } / feed.entries.size.toFloat()
        if (evergreenRatio > 0.7) {
            return baseTtl.multipliedBy(2)
        }
        
        return baseTtl
    }
    
    private suspend fun preWarmUserFeed(userId: String, feedType: FeedType) {
        try {
            // Check if feed is already cached and fresh
            val existing = getCachedFeed(userId, feedType)
            if (existing != null && isFeedFresh(existing, feedType)) {
                return // Already cached and fresh
            }
            
            // Generate and cache the feed
            val request = FeedGenerationRequest(
                userId = userId,
                feedType = feedType,
                limit = 20
            )
            val feed = feedGenerationService.generateFeed(request)
            val ttl = calculateTtlForFeedType(feedType)
            cacheFeed(feed, ttl)
            
        } catch (e: Exception) {
            println("Error pre-warming feed for user $userId, type $feedType: ${e.message}")
        }
    }
    
    private suspend fun cacheFeedMetadata(feed: GeneratedFeed) {
        try {
            val metadata = CachedFeedMetadata(
                userId = feed.userId,
                feedType = feed.feedType,
                itemCount = feed.entries.size,
                generatedAt = feed.metadata.generatedAt,
                categories = feed.entries.groupingBy { it.content.contentType.toString() }.eachCount()
            )
            
            val key = "${CACHE_VERSION}feed_metadata:${feed.userId}:${feed.feedType.name}"
            cacheProvider.set(key, metadata, Duration.ofHours(24))
            
        } catch (e: Exception) {
            println("Error caching feed metadata: ${e.message}")
        }
    }
    
    private suspend fun invalidateFeedMetadata(userId: String, feedType: FeedType) {
        try {
            val key = "${CACHE_VERSION}feed_metadata:${userId}:${feedType.name}"
            cacheProvider.delete(key)
        } catch (e: Exception) {
            println("Error invalidating feed metadata: ${e.message}")
        }
    }
}

/**
 * Feed metadata for analytics and optimization
 */
data class CachedFeedMetadata(
    val userId: String,
    val feedType: FeedType,
    val itemCount: Int,
    val generatedAt: Instant,
    val categories: Map<String, Int>
)

/**
 * Feed options that can affect caching behavior
 */
data class FeedOptions(
    val limit: Int = 20,
    val offset: Int = 0,
    val includePromoted: Boolean = true,
    val freshness: FeedFreshness = FeedFreshness.NORMAL,
    val customFilters: Map<String, Any> = emptyMap()
)

enum class FeedFreshness {
    STALE_OK,    // Accept older cached content
    NORMAL,      // Standard cache behavior
    FRESH_ONLY   // Force fresh content generation
}