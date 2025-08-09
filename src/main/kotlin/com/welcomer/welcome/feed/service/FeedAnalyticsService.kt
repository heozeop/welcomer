package com.welcomer.welcome.feed.service

import com.welcomer.welcome.engagement.model.*
import com.welcomer.welcome.feed.model.*
import com.welcomer.welcome.feed.repository.AnalyticsRepository
import com.welcomer.welcome.engagement.repository.EngagementRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service for tracking and analyzing feed performance and user engagement metrics.
 * Implements event recording, metrics aggregation, and analytics reporting functionality.
 */
@Service
class FeedAnalyticsService(
    private val analyticsRepository: AnalyticsRepository,
    private val engagementRepository: EngagementRepository
) {

    /**
     * Record a feed view event with associated metadata
     */
    suspend fun recordFeedView(userId: String, feedItems: List<FeedEntry>, metadata: Map<String, Any> = emptyMap()) {
        val event = FeedAnalyticsEvent(
            eventType = FeedAnalyticsEventType.FEED_VIEW,
            userId = userId,
            timestamp = Instant.now(),
            metadata = mapOf(
                "feedSize" to feedItems.size,
                "feedItemIds" to feedItems.map { it.id },
                "algorithmId" to (feedItems.firstOrNull()?.algorithmId ?: "unknown")
            ) + metadata
        )
        
        analyticsRepository.recordEvent(event)
        updateDailyMetrics("feed_views")
    }

    /**
     * Record an item interaction event
     */
    suspend fun recordItemInteraction(
        userId: String, 
        itemId: String, 
        interactionType: String, 
        metadata: Map<String, Any> = emptyMap()
    ) {
        val event = FeedAnalyticsEvent(
            eventType = FeedAnalyticsEventType.ITEM_INTERACTION,
            userId = userId,
            itemId = itemId,
            timestamp = Instant.now(),
            metadata = mapOf(
                "interactionType" to interactionType
            ) + metadata
        )
        
        analyticsRepository.recordEvent(event)
        
        // Update engagement metrics
        when (interactionType) {
            "click" -> updateDailyMetrics("item_clicks")
            "like" -> updateDailyMetrics("item_likes")
            "share" -> updateDailyMetrics("item_shares")
            "comment" -> updateDailyMetrics("item_comments")
            "bookmark" -> updateDailyMetrics("item_bookmarks")
        }
    }

    /**
     * Record user session metrics
     */
    suspend fun recordSessionMetrics(
        userId: String,
        sessionDuration: Long,
        itemsViewed: Int,
        interactionsCount: Int,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val event = FeedAnalyticsEvent(
            eventType = FeedAnalyticsEventType.SESSION_METRICS,
            userId = userId,
            timestamp = Instant.now(),
            metadata = mapOf(
                "sessionDuration" to sessionDuration,
                "itemsViewed" to itemsViewed,
                "interactionsCount" to interactionsCount
            ) + metadata
        )
        
        analyticsRepository.recordEvent(event)
        updateDailyMetrics("user_sessions")
    }

    /**
     * Record feed generation performance metrics
     */
    suspend fun recordFeedGeneration(
        userId: String,
        algorithmId: String,
        generationDuration: Long,
        candidateCount: Int,
        finalCount: Int,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val event = FeedAnalyticsEvent(
            eventType = FeedAnalyticsEventType.FEED_GENERATION,
            userId = userId,
            timestamp = Instant.now(),
            metadata = mapOf(
                "algorithmId" to algorithmId,
                "generationDuration" to generationDuration,
                "candidateCount" to candidateCount,
                "finalCount" to finalCount
            ) + metadata
        )
        
        analyticsRepository.recordEvent(event)
        updateDailyMetrics("feed_generations")
    }

    /**
     * Generate comprehensive daily analytics report
     */
    suspend fun generateDailyReport(): DailyAnalyticsReport {
        val today = Instant.now().truncatedTo(ChronoUnit.DAYS)
        val tomorrow = today.plus(1, ChronoUnit.DAYS)
        
        val metrics = analyticsRepository.getAggregatedMetrics(today, tomorrow)
        
        return DailyAnalyticsReport(
            date = today,
            totalViews = metrics["feed_views"] as? Long ?: 0,
            averageEngagementRate = calculateEngagementRate(metrics),
            contentDiversityScore = calculateDiversityScore(),
            topPerformingContent = getTopPerformingContent(10),
            userRetentionRate = calculateRetentionRate()
        )
    }

    /**
     * Get analytics for a specific time period
     */
    suspend fun getAnalytics(startTime: Instant, endTime: Instant): FeedAnalyticsData {
        val events = analyticsRepository.getEvents(startTime, endTime)
        val engagementMetrics = engagementRepository.getTopEngagedContent(startTime, endTime, 50)
        
        val feedViews = events.count { it.eventType == FeedAnalyticsEventType.FEED_VIEW }
        val interactions = events.count { it.eventType == FeedAnalyticsEventType.ITEM_INTERACTION }
        val uniqueUsers = events.map { it.userId }.toSet().size
        
        return FeedAnalyticsData(
            periodStart = startTime,
            periodEnd = endTime,
            totalFeedViews = feedViews.toLong(),
            totalInteractions = interactions.toLong(),
            uniqueActiveUsers = uniqueUsers.toLong(),
            averageEngagementRate = if (feedViews > 0) interactions.toDouble() / feedViews else 0.0,
            topContent = engagementMetrics.take(10),
            userActivityPattern = analyzeUserActivityPattern(events)
        )
    }

    /**
     * Get real-time analytics dashboard data
     */
    suspend fun getDashboardData(): FeedAnalyticsDashboard {
        val now = Instant.now()
        val lastHour = now.minus(1, ChronoUnit.HOURS)
        val last24Hours = now.minus(24, ChronoUnit.HOURS)
        
        val hourlyEvents = analyticsRepository.getEvents(lastHour, now)
        val dailyEvents = analyticsRepository.getEvents(last24Hours, now)
        
        return FeedAnalyticsDashboard(
            currentActiveUsers = getCurrentActiveUsers(),
            hourlyMetrics = calculateHourlyMetrics(hourlyEvents),
            dailyMetrics = calculateDailyMetrics(dailyEvents),
            trendingContent = getTrendingContent(20),
            systemHealth = assessSystemHealth()
        )
    }

    // Private helper methods

    /**
     * Update daily aggregated metrics
     */
    private suspend fun updateDailyMetrics(metricName: String) {
        val today = Instant.now().truncatedTo(ChronoUnit.DAYS)
        analyticsRepository.incrementMetric(today, metricName, 1)
    }

    private fun calculateEngagementRate(metrics: Map<String, Any>): Double {
        val views = metrics["feed_views"] as? Long ?: 0
        val interactions = listOf("item_clicks", "item_likes", "item_shares", "item_comments")
            .sumOf { metrics[it] as? Long ?: 0 }
        
        return if (views > 0) interactions.toDouble() / views else 0.0
    }

    private suspend fun calculateDiversityScore(): Double {
        // Calculate content diversity based on recent feeds
        // This is a simplified implementation
        return 0.75 // Placeholder for diversity calculation
    }

    private suspend fun getTopPerformingContent(limit: Int): List<ContentPerformance> {
        val now = Instant.now()
        val yesterday = now.minus(24, ChronoUnit.HOURS)
        
        return engagementRepository.getTopEngagedContent(yesterday, now, limit)
            .map { (contentId, engagementCount) ->
                ContentPerformance(
                    contentId = contentId,
                    engagementScore = engagementCount.toDouble(),
                    views = engagementCount, // Simplified - would need more detailed tracking
                    interactions = engagementCount
                )
            }
    }

    private suspend fun calculateRetentionRate(): Double {
        val now = Instant.now()
        val sevenDaysAgo = now.minus(7, ChronoUnit.DAYS)
        val fourteenDaysAgo = now.minus(14, ChronoUnit.DAYS)
        
        val thisWeekUsers = analyticsRepository.getUniqueUsers(sevenDaysAgo, now)
        val lastWeekUsers = analyticsRepository.getUniqueUsers(fourteenDaysAgo, sevenDaysAgo)
        
        val retainedUsers = thisWeekUsers.intersect(lastWeekUsers).size
        
        return if (lastWeekUsers.isNotEmpty()) {
            retainedUsers.toDouble() / lastWeekUsers.size
        } else {
            0.0
        }
    }

    private fun analyzeUserActivityPattern(events: List<FeedAnalyticsEvent>): Map<String, Double> {
        val hourlyActivity = events.groupBy { event ->
            event.timestamp.atZone(java.time.ZoneOffset.UTC).hour
        }.mapValues { it.value.size.toDouble() }
        
        return hourlyActivity.mapKeys { it.key.toString() }
    }

    private suspend fun getCurrentActiveUsers(): Long {
        val now = Instant.now()
        val fiveMinutesAgo = now.minus(5, ChronoUnit.MINUTES)
        
        return analyticsRepository.getUniqueUsers(fiveMinutesAgo, now).size.toLong()
    }

    private fun calculateHourlyMetrics(events: List<FeedAnalyticsEvent>): HourlyMetrics {
        val feedViews = events.count { it.eventType == FeedAnalyticsEventType.FEED_VIEW }
        val interactions = events.count { it.eventType == FeedAnalyticsEventType.ITEM_INTERACTION }
        
        return HourlyMetrics(
            feedViews = feedViews.toLong(),
            interactions = interactions.toLong(),
            uniqueUsers = events.map { it.userId }.toSet().size.toLong()
        )
    }

    private fun calculateDailyMetrics(events: List<FeedAnalyticsEvent>): DailyMetrics {
        val feedViews = events.count { it.eventType == FeedAnalyticsEventType.FEED_VIEW }
        val interactions = events.count { it.eventType == FeedAnalyticsEventType.ITEM_INTERACTION }
        val sessions = events.count { it.eventType == FeedAnalyticsEventType.SESSION_METRICS }
        
        return DailyMetrics(
            feedViews = feedViews.toLong(),
            interactions = interactions.toLong(),
            sessions = sessions.toLong(),
            uniqueUsers = events.map { it.userId }.toSet().size.toLong()
        )
    }

    private suspend fun getTrendingContent(limit: Int): List<String> {
        val now = Instant.now()
        val sixHoursAgo = now.minus(6, ChronoUnit.HOURS)
        
        return engagementRepository.getTopEngagedContent(sixHoursAgo, now, limit)
            .map { it.first }
    }

    private fun assessSystemHealth(): SystemHealthStatus {
        // Simple health assessment - could be more sophisticated
        return SystemHealthStatus.HEALTHY
    }
}

// Data classes for analytics

/**
 * Feed analytics event model
 */
data class FeedAnalyticsEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val eventType: FeedAnalyticsEventType,
    val userId: String,
    val itemId: String? = null,
    val timestamp: Instant,
    val metadata: Map<String, Any> = emptyMap()
)

enum class FeedAnalyticsEventType {
    FEED_VIEW,
    ITEM_INTERACTION,
    SESSION_METRICS,
    FEED_GENERATION
}

data class DailyAnalyticsReport(
    val date: Instant,
    val totalViews: Long,
    val averageEngagementRate: Double,
    val contentDiversityScore: Double,
    val topPerformingContent: List<ContentPerformance>,
    val userRetentionRate: Double
)

data class ContentPerformance(
    val contentId: String,
    val engagementScore: Double,
    val views: Long,
    val interactions: Long
)

data class FeedAnalyticsData(
    val periodStart: Instant,
    val periodEnd: Instant,
    val totalFeedViews: Long,
    val totalInteractions: Long,
    val uniqueActiveUsers: Long,
    val averageEngagementRate: Double,
    val topContent: List<Pair<String, Long>>,
    val userActivityPattern: Map<String, Double>
)

data class FeedAnalyticsDashboard(
    val currentActiveUsers: Long,
    val hourlyMetrics: HourlyMetrics,
    val dailyMetrics: DailyMetrics,
    val trendingContent: List<String>,
    val systemHealth: SystemHealthStatus
)

data class HourlyMetrics(
    val feedViews: Long,
    val interactions: Long,
    val uniqueUsers: Long
)

data class DailyMetrics(
    val feedViews: Long,
    val interactions: Long,
    val sessions: Long,
    val uniqueUsers: Long
)

enum class SystemHealthStatus {
    HEALTHY,
    WARNING,
    CRITICAL
}