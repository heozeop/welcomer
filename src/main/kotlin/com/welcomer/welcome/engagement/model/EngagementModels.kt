package com.welcomer.welcome.engagement.model

import java.time.Duration
import java.time.Instant

/**
 * Types of user engagement events
 */
enum class EngagementType {
    VIEW,        // User viewed content
    CLICK,       // User clicked on content
    LIKE,        // User liked content
    UNLIKE,      // User removed like
    COMMENT,     // User commented on content
    SHARE,       // User shared content
    BOOKMARK,    // User bookmarked content
    UNBOOKMARK,  // User removed bookmark
    DWELL_TIME,  // Time spent viewing content
    SCROLL,      // User scrolled through content
    EXPAND,      // User expanded content (e.g., "read more")
    REPORT,      // User reported content
    HIDE         // User hid content from feed
}

/**
 * Engagement event data model
 */
data class EngagementEvent(
    val id: String,
    val userId: String,
    val contentId: String,
    val type: EngagementType,
    val metadata: Map<String, Any> = emptyMap(),
    val timestamp: Instant,
    val sessionId: String? = null,
    val deviceType: String? = null,
    val clientIp: String? = null,
    val userAgent: String? = null
)

/**
 * Request model for tracking engagement
 */
data class TrackEngagementRequest(
    val contentId: String,
    val engagementType: EngagementType,
    val metadata: Map<String, Any> = emptyMap(),
    val sessionId: String? = null
)

/**
 * Response model for engagement tracking
 */
data class EngagementResponse(
    val success: Boolean,
    val engagementId: String? = null,
    val message: String? = null,
    val timestamp: Instant = Instant.now()
)

/**
 * Aggregated engagement metrics for content
 */
data class ContentEngagementMetrics(
    val contentId: String,
    val totalViews: Long,
    val uniqueViews: Long,
    val totalLikes: Long,
    val totalComments: Long,
    val totalShares: Long,
    val totalBookmarks: Long,
    val averageDwellTime: Duration?,
    val engagementRate: Double, // (likes + comments + shares) / views
    val clickThroughRate: Double, // clicks / impressions
    val hideRate: Double, // hides / views
    val reportCount: Long,
    val lastEngagementAt: Instant?,
    val periodStart: Instant,
    val periodEnd: Instant
)

/**
 * User engagement summary
 */
data class UserEngagementSummary(
    val userId: String,
    val totalEngagements: Long,
    val engagementsByType: Map<EngagementType, Long>,
    val topEngagedContent: List<String>, // content IDs
    val averageSessionDuration: Duration?,
    val lastActivityAt: Instant?,
    val periodStart: Instant,
    val periodEnd: Instant
)

/**
 * Time window for aggregation
 */
enum class TimeWindow {
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    ALL_TIME
}

/**
 * Aggregated metrics for a time window
 */
data class WindowedMetrics(
    val window: TimeWindow,
    val windowStart: Instant,
    val windowEnd: Instant,
    val metrics: Map<String, Any>,
    val computedAt: Instant
)

/**
 * Configuration for engagement tracking
 */
data class EngagementTrackingConfig(
    val enableRealTimeProcessing: Boolean = true,
    val batchSize: Int = 100,
    val flushIntervalSeconds: Long = 10,
    val maxRetries: Int = 3,
    val retryDelayMillis: Long = 1000,
    val enableDuplicateDetection: Boolean = true,
    val duplicateWindowMinutes: Long = 5,
    val enableAnonymousTracking: Boolean = false,
    val minDwellTimeSeconds: Long = 3, // Minimum time to count as meaningful engagement
    val maxDwellTimeSeconds: Long = 600 // Cap for dwell time to filter outliers
)

/**
 * Validation result for engagement data
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)

/**
 * Batch of engagement events for processing
 */
data class EngagementBatch(
    val batchId: String,
    val events: List<EngagementEvent>,
    val createdAt: Instant,
    val processedAt: Instant? = null,
    val status: BatchStatus = BatchStatus.PENDING
)

enum class BatchStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    PARTIAL
}

/**
 * Analytics query parameters
 */
data class EngagementAnalyticsQuery(
    val contentIds: List<String>? = null,
    val userIds: List<String>? = null,
    val engagementTypes: List<EngagementType>? = null,
    val startTime: Instant,
    val endTime: Instant,
    val timeWindow: TimeWindow = TimeWindow.DAILY,
    val limit: Int = 100,
    val offset: Int = 0
)

/**
 * Analytics response
 */
data class EngagementAnalyticsResponse(
    val query: EngagementAnalyticsQuery,
    val results: List<ContentEngagementMetrics>,
    val totalResults: Long,
    val hasMore: Boolean,
    val computedAt: Instant
)