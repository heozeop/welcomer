package com.welcomer.welcome.feed.model

import com.welcomer.welcome.ingestion.model.StoredContent
import java.time.Instant
import java.util.*

/**
 * Feed generation request containing user context and preferences
 */
data class FeedGenerationRequest(
    val userId: String,
    val feedType: FeedType = FeedType.HOME,
    val limit: Int = 50,
    val cursor: String? = null,
    val algorithmId: String? = null,
    val parameters: Map<String, Any> = emptyMap(),
    val refreshForced: Boolean = false
)

/**
 * Generated feed response with scored and ranked content
 */
data class GeneratedFeed(
    val userId: String,
    val feedType: FeedType,
    val entries: List<FeedEntry>,
    val metadata: FeedMetadata,
    val nextCursor: String? = null,
    val hasMore: Boolean = false
)

/**
 * Individual feed entry with scoring and ranking information
 */
data class FeedEntry(
    val id: String,
    val content: StoredContent,
    val score: Double,
    val rank: Int,
    val reasons: List<FeedReason>,
    val sourceType: FeedSourceType,
    val boosted: Boolean = false,
    val algorithmId: String? = null,
    val generatedAt: Instant = Instant.now()
)

/**
 * Feed generation metadata and performance metrics
 */
data class FeedMetadata(
    val algorithmId: String,
    val algorithmVersion: String,
    val generationDuration: Long, // milliseconds
    val contentCount: Int,
    val candidateCount: Int,
    val parameters: Map<String, Any>,
    val generatedAt: Instant = Instant.now(),
    val expiresAt: Instant? = null
)

/**
 * Reason why content was included in feed
 */
data class FeedReason(
    val type: FeedReasonType,
    val description: String,
    val weight: Double,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Types of feeds available in the system
 */
enum class FeedType {
    HOME,           // Main personalized feed
    FOLLOWING,      // Content from followed users only
    EXPLORE,        // Discovery content
    TRENDING,       // Currently trending content
    PERSONALIZED    // ML-based personalized recommendations
}

/**
 * How content was sourced for the feed
 */
enum class FeedSourceType {
    FOLLOWING,      // From users the current user follows
    TRENDING,       // From trending content
    RECOMMENDATION, // From recommendation algorithm
    PROMOTED,       // Sponsored/promoted content
    MANUAL          // Manually curated content
}

/**
 * Reasons why content appears in feed
 */
enum class FeedReasonType {
    RECENCY,        // Recent content
    POPULARITY,     // Popular content
    RELEVANCE,      // Relevant to user preferences
    FOLLOWING,      // From followed users
    ENGAGEMENT,     // High engagement content
    TRENDING,       // Currently trending
    SIMILAR_USERS,  // Liked by similar users
    TOPIC_INTEREST, // Matches user interests
    DIVERSITY,      // Added for feed diversity
    COLD_START      // Cold start recommendation
}

/**
 * Content scoring weights configuration
 */
data class ScoringWeights(
    val recency: Double = 0.5,
    val popularity: Double = 0.3,
    val relevance: Double = 0.2,
    val following: Double = 0.0, // Additional weight if from followed users
    val engagement: Double = 0.0, // Additional weight for high engagement
    val customWeights: Map<String, Double> = emptyMap()
)

/**
 * User preferences for feed generation
 */
data class UserPreferences(
    val userId: String,
    val interests: List<String> = emptyList(),
    val preferredContentTypes: Set<String> = emptySet(),
    val blockedUsers: Set<String> = emptySet(),
    val blockedTopics: Set<String> = emptySet(),
    val languagePreferences: List<String> = emptyList(),
    val engagementHistory: Map<String, Double> = emptyMap(), // contentId -> engagement score
    val lastActiveAt: Instant? = null,
    val accountAge: Long? = null // days since account creation
)

/**
 * Content candidate with pre-computed metrics
 */
data class ContentCandidate(
    val content: StoredContent,
    val popularityScore: Double = 0.0,
    val engagementMetrics: EngagementMetrics? = null,
    val authorFollowingStatus: Boolean = false,
    val topicRelevance: Double = 0.0,
    val languageMatch: Boolean = true
)

/**
 * Engagement metrics for content
 */
data class EngagementMetrics(
    val likes: Long = 0,
    val shares: Long = 0,
    val comments: Long = 0,
    val views: Long = 0,
    val clickThroughRate: Double = 0.0,
    val engagementRate: Double = 0.0,
    val averageTimeSpent: Long = 0, // milliseconds
    val completionRate: Double = 0.0 // for video/audio content
)

/**
 * Diversity rules configuration
 */
data class DiversityConfig(
    val maxSameAuthor: Int = 3,        // Max content from same author
    val maxSameTopic: Int = 5,         // Max content from same topic
    val maxSameContentType: Int = 10,  // Max content of same type
    val authorSpacing: Int = 3,        // Min positions between same author
    val topicSpacing: Int = 2,         // Min positions between same topic
    val enforceContentTypeBalance: Boolean = true
)

/**
 * Cold start configuration for new users
 */
data class ColdStartConfig(
    val newUserThresholdDays: Long = 7,     // Consider user "new" for this many days
    val minEngagementActions: Int = 10,      // Min actions before personalizing
    val trendingContentWeight: Double = 0.7, // Weight for trending content
    val diverseTopicSampling: Boolean = true, // Sample from diverse topics
    val popularContentFallback: Boolean = true // Use popular content as fallback
)

/**
 * A/B test experiment configuration
 */
data class ExperimentConfig(
    val experimentId: String,
    val variantId: String,
    val parameters: Map<String, Any> = emptyMap(),
    val isControl: Boolean = false
)

/**
 * Performance metrics for feed generation
 */
data class FeedPerformanceMetrics(
    val generationTime: Long,           // milliseconds
    val databaseQueries: Int,
    val cacheHits: Int,
    val cacheMisses: Int,
    val candidatesEvaluated: Int,
    val scoringTime: Long,              // milliseconds
    val diversityRulesTime: Long,       // milliseconds
    val totalMemoryUsed: Long           // bytes
)