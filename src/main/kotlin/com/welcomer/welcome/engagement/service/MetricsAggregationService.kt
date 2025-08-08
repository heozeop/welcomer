package com.welcomer.welcome.engagement.service

import com.welcomer.welcome.engagement.model.*
import com.welcomer.welcome.engagement.repository.EngagementRepository
import kotlinx.coroutines.*
import org.springframework.scheduling.annotation.Scheduled
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for aggregating engagement metrics across different time windows
 */
interface MetricsAggregationService {
    /**
     * Update metrics for a content item after an engagement
     */
    suspend fun updateMetrics(contentId: String, engagementType: EngagementType)

    /**
     * Compute aggregated metrics for a time window
     */
    suspend fun computeWindowedMetrics(
        contentId: String,
        window: TimeWindow,
        windowStart: Instant
    ): WindowedMetrics

    /**
     * Get cached or compute metrics for content
     */
    suspend fun getMetrics(
        contentId: String,
        window: TimeWindow
    ): WindowedMetrics?

    /**
     * Batch compute metrics for multiple content items
     */
    suspend fun batchComputeMetrics(
        contentIds: List<String>,
        window: TimeWindow
    ): Map<String, WindowedMetrics>

    /**
     * Get trending content based on engagement metrics
     */
    suspend fun getTrendingContent(
        window: TimeWindow,
        limit: Int = 10
    ): List<Pair<String, Double>>

    /**
     * Perform periodic re-aggregation of metrics
     */
    suspend fun performPeriodicAggregation()
}

/**
 * Default implementation of metrics aggregation service
 */
class DefaultMetricsAggregationService(
    private val engagementRepository: EngagementRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : MetricsAggregationService {

    // In-memory cache for aggregated metrics (in production, use Redis)
    private val metricsCache = ConcurrentHashMap<String, WindowedMetrics>()
    
    // Real-time counters for fast updates
    private val realtimeCounters = ConcurrentHashMap<String, ContentCounters>()

    data class ContentCounters(
        var views: Long = 0,
        var likes: Long = 0,
        var comments: Long = 0,
        var shares: Long = 0,
        var bookmarks: Long = 0,
        var lastUpdated: Instant = Instant.now()
    )

    override suspend fun updateMetrics(contentId: String, engagementType: EngagementType) {
        // Update real-time counters
        val counters = realtimeCounters.computeIfAbsent(contentId) { ContentCounters() }
        
        when (engagementType) {
            EngagementType.VIEW -> counters.views++
            EngagementType.LIKE -> counters.likes++
            EngagementType.COMMENT -> counters.comments++
            EngagementType.SHARE -> counters.shares++
            EngagementType.BOOKMARK -> counters.bookmarks++
            else -> { /* Other types don't affect basic counters */ }
        }
        
        counters.lastUpdated = Instant.now()

        // Invalidate cached metrics for this content
        invalidateCache(contentId)

        // Schedule background aggregation if needed
        if (shouldTriggerAggregation(counters)) {
            scope.launch {
                recomputeMetrics(contentId)
            }
        }
    }

    override suspend fun computeWindowedMetrics(
        contentId: String,
        window: TimeWindow,
        windowStart: Instant
    ): WindowedMetrics {
        val windowEnd = calculateWindowEnd(windowStart, window)
        
        // Get engagement data for the window
        val metrics = engagementRepository.getContentMetrics(contentId, windowStart, windowEnd)
        
        // Create windowed metrics
        val metricsMap = mutableMapOf<String, Any>()
        
        if (metrics != null) {
            metricsMap["total_views"] = metrics.totalViews
            metricsMap["unique_views"] = metrics.uniqueViews
            metricsMap["total_likes"] = metrics.totalLikes
            metricsMap["total_comments"] = metrics.totalComments
            metricsMap["total_shares"] = metrics.totalShares
            metricsMap["total_bookmarks"] = metrics.totalBookmarks
            metricsMap["engagement_rate"] = metrics.engagementRate
            metricsMap["click_through_rate"] = metrics.clickThroughRate
            metricsMap["hide_rate"] = metrics.hideRate
            metricsMap["report_count"] = metrics.reportCount
            
            metrics.averageDwellTime?.let {
                metricsMap["average_dwell_time_seconds"] = it.seconds
            }
            
            // Calculate trending score
            val trendingScore = calculateTrendingScore(metrics, window)
            metricsMap["trending_score"] = trendingScore
            
            // Add velocity metrics (change rate)
            val velocity = calculateVelocity(contentId, metrics, window)
            metricsMap["engagement_velocity"] = velocity
        }
        
        return WindowedMetrics(
            window = window,
            windowStart = windowStart,
            windowEnd = windowEnd,
            metrics = metricsMap,
            computedAt = Instant.now()
        )
    }

    override suspend fun getMetrics(
        contentId: String,
        window: TimeWindow
    ): WindowedMetrics? {
        val cacheKey = "$contentId:${window.name}"
        
        // Check cache first
        val cached = metricsCache[cacheKey]
        if (cached != null && !isCacheExpired(cached)) {
            return cached
        }
        
        // Compute new metrics
        val windowStart = getWindowStart(window)
        val metrics = computeWindowedMetrics(contentId, window, windowStart)
        
        // Cache the result
        metricsCache[cacheKey] = metrics
        
        return metrics
    }

    override suspend fun batchComputeMetrics(
        contentIds: List<String>,
        window: TimeWindow
    ): Map<String, WindowedMetrics> {
        return coroutineScope {
            contentIds.associateWith { contentId ->
                async {
                    getMetrics(contentId, window)
                }
            }.mapValues { (_, deferred) ->
                deferred.await() ?: WindowedMetrics(
                    window = window,
                    windowStart = getWindowStart(window),
                    windowEnd = Instant.now(),
                    metrics = emptyMap(),
                    computedAt = Instant.now()
                )
            }
        }
    }

    override suspend fun getTrendingContent(
        window: TimeWindow,
        limit: Int
    ): List<Pair<String, Double>> {
        val windowStart = getWindowStart(window)
        val windowEnd = Instant.now()
        
        // Get top engaged content from repository
        val topContent = engagementRepository.getTopEngagedContent(windowStart, windowEnd, limit * 2)
        
        // Calculate trending scores for each
        val scoredContent = topContent.map { (contentId, engagementCount) ->
            val metrics = engagementRepository.getContentMetrics(contentId, windowStart, windowEnd)
            val trendingScore = if (metrics != null) {
                calculateTrendingScore(metrics, window)
            } else {
                engagementCount.toDouble()
            }
            contentId to trendingScore
        }
        
        // Sort by trending score and return top N
        return scoredContent
            .sortedByDescending { it.second }
            .take(limit)
    }

    @Scheduled(fixedDelay = 60000) // Run every minute
    fun performPeriodicAggregationTask() {
        scope.launch {
            performPeriodicAggregation()
        }
    }

    override suspend fun performPeriodicAggregation() {
        // Get content that needs re-aggregation
        val contentToAggregate = realtimeCounters.entries
            .filter { shouldReaggregate(it.value) }
            .map { it.key }
        
        if (contentToAggregate.isNotEmpty()) {
            contentToAggregate.forEach { contentId ->
                recomputeMetrics(contentId)
            }
        }
        
        // Clean up old cache entries
        cleanupExpiredCache()
    }

    // Private helper methods

    private fun calculateWindowEnd(windowStart: Instant, window: TimeWindow): Instant {
        return when (window) {
            TimeWindow.HOURLY -> windowStart.plus(1, ChronoUnit.HOURS)
            TimeWindow.DAILY -> windowStart.plus(1, ChronoUnit.DAYS)
            TimeWindow.WEEKLY -> windowStart.plus(7, ChronoUnit.DAYS)
            TimeWindow.MONTHLY -> windowStart.plus(30, ChronoUnit.DAYS)
            TimeWindow.ALL_TIME -> Instant.now()
        }
    }

    private fun getWindowStart(window: TimeWindow): Instant {
        val now = Instant.now()
        return when (window) {
            TimeWindow.HOURLY -> now.minus(1, ChronoUnit.HOURS)
            TimeWindow.DAILY -> now.minus(1, ChronoUnit.DAYS)
            TimeWindow.WEEKLY -> now.minus(7, ChronoUnit.DAYS)
            TimeWindow.MONTHLY -> now.minus(30, ChronoUnit.DAYS)
            TimeWindow.ALL_TIME -> Instant.EPOCH
        }
    }

    private fun calculateTrendingScore(metrics: ContentEngagementMetrics, window: TimeWindow): Double {
        // Trending score algorithm combining recency, engagement rate, and velocity
        val baseScore = (metrics.totalLikes * 3) + 
                       (metrics.totalComments * 5) + 
                       (metrics.totalShares * 7) +
                       metrics.totalViews
        
        // Apply time decay
        val ageHours = Duration.between(metrics.periodStart, Instant.now()).toHours()
        val decayFactor = when (window) {
            TimeWindow.HOURLY -> 1.0
            TimeWindow.DAILY -> Math.exp(-ageHours / 24.0)
            TimeWindow.WEEKLY -> Math.exp(-ageHours / 168.0)
            else -> Math.exp(-ageHours / 720.0)
        }
        
        // Boost for high engagement rate
        val engagementBoost = if (metrics.engagementRate > 0.1) 1.5 else 1.0
        
        // Penalize for high hide/report rate
        val qualityFactor = 1.0 - (metrics.hideRate * 0.5) - 
                           (if (metrics.reportCount > 0) 0.2 else 0.0)
        
        return baseScore * decayFactor * engagementBoost * qualityFactor
    }

    private fun calculateVelocity(
        contentId: String,
        currentMetrics: ContentEngagementMetrics,
        window: TimeWindow
    ): Double {
        // Calculate rate of change in engagement
        val previousWindow = when (window) {
            TimeWindow.HOURLY -> getWindowStart(window).minus(1, ChronoUnit.HOURS)
            TimeWindow.DAILY -> getWindowStart(window).minus(1, ChronoUnit.DAYS)
            TimeWindow.WEEKLY -> getWindowStart(window).minus(7, ChronoUnit.DAYS)
            else -> getWindowStart(window).minus(30, ChronoUnit.DAYS)
        }
        
        // This is simplified - in production, would compare with previous period metrics
        val hoursSincePeriodStart = Duration.between(currentMetrics.periodStart, Instant.now()).toHours()
        if (hoursSincePeriodStart > 0) {
            return (currentMetrics.totalLikes + currentMetrics.totalComments + currentMetrics.totalShares).toDouble() / hoursSincePeriodStart
        }
        return 0.0
    }

    private fun shouldTriggerAggregation(counters: ContentCounters): Boolean {
        // Trigger aggregation if significant changes or time passed
        val timeSinceUpdate = Duration.between(counters.lastUpdated, Instant.now())
        return (counters.views % 100 == 0L) || 
               (counters.likes % 10 == 0L) ||
               timeSinceUpdate.toMinutes() > 5
    }

    private fun shouldReaggregate(counters: ContentCounters): Boolean {
        val timeSinceUpdate = Duration.between(counters.lastUpdated, Instant.now())
        return timeSinceUpdate.toMinutes() > 1
    }

    private suspend fun recomputeMetrics(contentId: String) {
        TimeWindow.values().forEach { window ->
            val cacheKey = "$contentId:${window.name}"
            val windowStart = getWindowStart(window)
            val metrics = computeWindowedMetrics(contentId, window, windowStart)
            metricsCache[cacheKey] = metrics
        }
    }

    private fun invalidateCache(contentId: String) {
        TimeWindow.values().forEach { window ->
            val cacheKey = "$contentId:${window.name}"
            metricsCache.remove(cacheKey)
        }
    }

    private fun isCacheExpired(metrics: WindowedMetrics): Boolean {
        val age = Duration.between(metrics.computedAt, Instant.now())
        return when (metrics.window) {
            TimeWindow.HOURLY -> age.toMinutes() > 5
            TimeWindow.DAILY -> age.toMinutes() > 15
            TimeWindow.WEEKLY -> age.toHours() > 1
            TimeWindow.MONTHLY -> age.toHours() > 6
            TimeWindow.ALL_TIME -> age.toHours() > 24
        }
    }

    private fun cleanupExpiredCache() {
        val expiredKeys = metricsCache.entries
            .filter { isCacheExpired(it.value) }
            .map { it.key }
        
        expiredKeys.forEach { metricsCache.remove(it) }
    }
}