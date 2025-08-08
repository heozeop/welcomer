package com.welcomer.welcome.feed.service

import com.welcomer.welcome.feed.model.*
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for caching feeds and related data to optimize performance
 */
interface FeedCacheService {
    /**
     * Get cached feed for user if available and not expired
     */
    suspend fun getCachedFeed(userId: String, feedType: FeedType): GeneratedFeed?

    /**
     * Cache generated feed with TTL
     */
    suspend fun cacheFeed(feed: GeneratedFeed, ttl: Duration = Duration.ofMinutes(30))

    /**
     * Invalidate cached feed for user
     */
    suspend fun invalidateFeed(userId: String, feedType: FeedType)

    /**
     * Cache user preferences for faster access
     */
    suspend fun cacheUserPreferences(preferences: UserPreferences, ttl: Duration = Duration.ofHours(1))

    /**
     * Get cached user preferences
     */
    suspend fun getCachedUserPreferences(userId: String): UserPreferences?

    /**
     * Cache content popularity scores
     */
    suspend fun cachePopularityScores(scores: Map<String, Double>, ttl: Duration = Duration.ofMinutes(15))

    /**
     * Get cached popularity score for content
     */
    suspend fun getCachedPopularityScore(contentId: String): Double?

    /**
     * Pre-warm cache for active users
     */
    suspend fun preWarmCache(userIds: List<String>, feedTypes: List<FeedType>)

    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats
}

/**
 * Cache statistics for monitoring
 */
data class CacheStats(
    val feedCacheSize: Int,
    val userPreferencesCacheSize: Int,
    val popularityScoresCacheSize: Int,
    val feedHitRate: Double,
    val preferencesHitRate: Double,
    val popularityHitRate: Double,
    val totalMemoryUsed: Long // estimated bytes
)

/**
 * Cached item with TTL
 */
private data class CachedItem<T>(
    val data: T,
    val cachedAt: Instant,
    val ttl: Duration
) {
    fun isExpired(now: Instant = Instant.now()): Boolean {
        return now.isAfter(cachedAt.plus(ttl))
    }
}

@Service
class DefaultFeedCacheService : FeedCacheService {

    // In-memory caches (in production, use Redis)
    private val feedCache = ConcurrentHashMap<String, CachedItem<GeneratedFeed>>()
    private val userPreferencesCache = ConcurrentHashMap<String, CachedItem<UserPreferences>>()
    private val popularityScoresCache = ConcurrentHashMap<String, CachedItem<Double>>()

    // Cache statistics
    private var feedHits = 0L
    private var feedMisses = 0L
    private var preferencesHits = 0L
    private var preferencesMisses = 0L
    private var popularityHits = 0L
    private var popularityMisses = 0L

    override suspend fun getCachedFeed(userId: String, feedType: FeedType): GeneratedFeed? {
        val key = "$userId:${feedType.name}"
        val cached = feedCache[key]

        return if (cached != null && !cached.isExpired()) {
            feedHits++
            cached.data
        } else {
            feedMisses++
            if (cached != null) {
                feedCache.remove(key) // Remove expired entry
            }
            null
        }
    }

    override suspend fun cacheFeed(feed: GeneratedFeed, ttl: Duration) {
        val key = "${feed.userId}:${feed.feedType.name}"
        feedCache[key] = CachedItem(feed, Instant.now(), ttl)

        // Clean up expired entries periodically (simple cleanup)
        if (feedCache.size % 100 == 0) {
            cleanupExpiredEntries()
        }
    }

    override suspend fun invalidateFeed(userId: String, feedType: FeedType) {
        val key = "$userId:${feedType.name}"
        feedCache.remove(key)
    }

    override suspend fun cacheUserPreferences(preferences: UserPreferences, ttl: Duration) {
        userPreferencesCache[preferences.userId] = CachedItem(preferences, Instant.now(), ttl)
    }

    override suspend fun getCachedUserPreferences(userId: String): UserPreferences? {
        val cached = userPreferencesCache[userId]

        return if (cached != null && !cached.isExpired()) {
            preferencesHits++
            cached.data
        } else {
            preferencesMisses++
            if (cached != null) {
                userPreferencesCache.remove(userId) // Remove expired entry
            }
            null
        }
    }

    override suspend fun cachePopularityScores(scores: Map<String, Double>, ttl: Duration) {
        val now = Instant.now()
        scores.forEach { (contentId, score) ->
            popularityScoresCache[contentId] = CachedItem(score, now, ttl)
        }
    }

    override suspend fun getCachedPopularityScore(contentId: String): Double? {
        val cached = popularityScoresCache[contentId]

        return if (cached != null && !cached.isExpired()) {
            popularityHits++
            cached.data
        } else {
            popularityMisses++
            if (cached != null) {
                popularityScoresCache.remove(contentId) // Remove expired entry
            }
            null
        }
    }

    override suspend fun preWarmCache(userIds: List<String>, feedTypes: List<FeedType>) {
        // In a real implementation, this would generate feeds for active users
        // in the background to warm the cache
        
        // Placeholder implementation
        println("Pre-warming cache for ${userIds.size} users and ${feedTypes.size} feed types")
    }

    override fun getCacheStats(): CacheStats {
        val feedHitRate = if (feedHits + feedMisses > 0) {
            feedHits.toDouble() / (feedHits + feedMisses)
        } else 0.0

        val preferencesHitRate = if (preferencesHits + preferencesMisses > 0) {
            preferencesHits.toDouble() / (preferencesHits + preferencesMisses)
        } else 0.0

        val popularityHitRate = if (popularityHits + popularityMisses > 0) {
            popularityHits.toDouble() / (popularityHits + popularityMisses)
        } else 0.0

        // Rough memory estimation (would be more accurate in production)
        val estimatedMemory = (feedCache.size * 10000) + // ~10KB per feed
                             (userPreferencesCache.size * 1000) + // ~1KB per preferences
                             (popularityScoresCache.size * 100) // ~100B per score

        return CacheStats(
            feedCacheSize = feedCache.size,
            userPreferencesCacheSize = userPreferencesCache.size,
            popularityScoresCacheSize = popularityScoresCache.size,
            feedHitRate = feedHitRate,
            preferencesHitRate = preferencesHitRate,
            popularityHitRate = popularityHitRate,
            totalMemoryUsed = estimatedMemory.toLong()
        )
    }

    private fun cleanupExpiredEntries() {
        val now = Instant.now()
        
        // Clean feed cache
        feedCache.entries.removeIf { it.value.isExpired(now) }
        
        // Clean user preferences cache
        userPreferencesCache.entries.removeIf { it.value.isExpired(now) }
        
        // Clean popularity scores cache
        popularityScoresCache.entries.removeIf { it.value.isExpired(now) }
    }
}

/**
 * Performance optimization service for feed generation
 */
interface FeedPerformanceService {
    /**
     * Execute feed generation with performance monitoring
     */
    suspend fun <T> withPerformanceMonitoring(
        operation: String,
        block: suspend () -> T
    ): T

    /**
     * Batch score content candidates for better performance
     */
    suspend fun batchScoreContent(
        candidates: List<ContentCandidate>,
        userPreferences: UserPreferences,
        weights: ScoringWeights,
        batchSize: Int = 50
    ): List<Pair<ContentCandidate, Double>>

    /**
     * Get performance metrics
     */
    fun getPerformanceMetrics(): Map<String, Any>
}

@Service
class DefaultFeedPerformanceService(
    private val scoringService: ContentScoringService
) : FeedPerformanceService {

    private val operationTimes = ConcurrentHashMap<String, MutableList<Long>>()
    private val operationCounts = ConcurrentHashMap<String, Long>()

    override suspend fun <T> withPerformanceMonitoring(operation: String, block: suspend () -> T): T {
        val startTime = System.currentTimeMillis()
        
        return try {
            val result = block()
            val duration = System.currentTimeMillis() - startTime
            
            // Record performance metrics
            operationTimes.computeIfAbsent(operation) { mutableListOf() }.add(duration)
            operationCounts.merge(operation, 1L, Long::plus)
            
            // Keep only recent measurements (last 1000)
            operationTimes[operation]?.let { times ->
                if (times.size > 1000) {
                    times.removeAt(0)
                }
            }
            
            result
        } catch (exception: Exception) {
            val duration = System.currentTimeMillis() - startTime
            operationTimes.computeIfAbsent("${operation}_error") { mutableListOf() }.add(duration)
            throw exception
        }
    }

    override suspend fun batchScoreContent(
        candidates: List<ContentCandidate>,
        userPreferences: UserPreferences,
        weights: ScoringWeights,
        batchSize: Int
    ): List<Pair<ContentCandidate, Double>> {
        return withPerformanceMonitoring("batch_score_content") {
            candidates.chunked(batchSize).flatMap { batch ->
                // Process batch concurrently (if scoring service supports it)
                batch.map { candidate ->
                    candidate to scoringService.scoreContent(candidate, userPreferences, weights)
                }
            }
        }
    }

    override fun getPerformanceMetrics(): Map<String, Any> {
        val metrics = mutableMapOf<String, Any>()
        
        operationTimes.forEach { (operation, times) ->
            if (times.isNotEmpty()) {
                metrics["${operation}_count"] = operationCounts[operation] ?: 0
                metrics["${operation}_avg_ms"] = times.average()
                metrics["${operation}_min_ms"] = times.minOrNull() ?: 0
                metrics["${operation}_max_ms"] = times.maxOrNull() ?: 0
                metrics["${operation}_p95_ms"] = times.sorted().let { sorted ->
                    sorted[(sorted.size * 0.95).toInt()]
                }
            }
        }
        
        return metrics
    }
}