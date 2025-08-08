package com.welcomer.welcome.user.model

import java.time.Duration
import java.time.Instant

/**
 * Types of user behavior events that can be tracked
 */
enum class BehaviorEventType {
    CONTENT_VIEW,
    CONTENT_LIKE,
    CONTENT_SHARE,
    CONTENT_COMMENT,
    CONTENT_CLICK,
    CONTENT_BOOKMARK,
    CONTENT_HIDE,
    CONTENT_REPORT,
    USER_FOLLOW,
    USER_UNFOLLOW,
    USER_BLOCK,
    TOPIC_SEARCH,
    FEED_REFRESH,
    DWELL_TIME // Time spent viewing content
}

/**
 * Represents a user behavior event
 */
data class BehaviorEvent(
    val id: String,
    val userId: String,
    val eventType: BehaviorEventType,
    val contentId: String? = null,
    val targetUserId: String? = null, // For follow/unfollow events
    val topic: String? = null,
    val contentType: String? = null,
    val duration: Duration? = null, // For dwell time events
    val metadata: Map<String, Any> = emptyMap(),
    val timestamp: Instant,
    val sessionId: String? = null
)

/**
 * Aggregated behavior metrics for a user
 */
data class UserBehaviorProfile(
    val userId: String,
    val totalEvents: Long,
    val recentEvents: Long, // Last 30 days
    val topicInteractionCounts: Map<String, Long>,
    val contentTypeInteractionCounts: Map<String, Long>,
    val averageDwellTime: Duration?,
    val engagementRate: Double, // (likes + shares + comments) / total_views
    val preferredContentTypes: List<String>,
    val preferredTopics: List<String>,
    val activeHours: List<Int>, // Hours of day when most active (0-23)
    val lastActivityAt: Instant?
)

/**
 * Configuration for behavior analysis algorithms
 */
data class BehaviorAnalysisConfig(
    val recentActivityWindowDays: Int = 30,
    val minimumEventCountForInference: Int = 10,
    val dwellTimeThresholdSeconds: Long = 30, // Minimum dwell time to count as engagement
    val decayFactor: Double = 0.95, // How much older events are discounted
    val topicConfidenceThreshold: Double = 0.6,
    val contentTypeConfidenceThreshold: Double = 0.7,
    val engagementWeights: Map<BehaviorEventType, Double> = mapOf(
        BehaviorEventType.CONTENT_VIEW to 1.0,
        BehaviorEventType.CONTENT_LIKE to 3.0,
        BehaviorEventType.CONTENT_SHARE to 5.0,
        BehaviorEventType.CONTENT_COMMENT to 4.0,
        BehaviorEventType.CONTENT_BOOKMARK to 3.5,
        BehaviorEventType.CONTENT_HIDE to -2.0,
        BehaviorEventType.CONTENT_REPORT to -5.0
    )
)

/**
 * Result of implicit preference analysis
 */
data class ImplicitPreferenceResult(
    val userId: String,
    val inferredPreferences: List<UserPreference>,
    val confidence: Double,
    val analysisMetadata: Map<String, Any>,
    val generatedAt: Instant
)