package com.welcomer.welcome.diversity.model

import com.welcomer.welcome.feed.model.FeedEntry
import com.welcomer.welcome.ingestion.model.StoredContent
import com.welcomer.welcome.ingestion.model.TopicCategory
import java.time.Instant

/**
 * Result of diversity analysis for content
 */
data class DiversityAnalysisResult(
    val contentId: String,
    val overallDiversityScore: Double,
    val dimensionScores: Map<DiversityDimension, Double>,
    val recommendations: List<DiversityRecommendation>,
    val analysisMetadata: Map<String, Any> = emptyMap(),
    val analyzedAt: Instant = Instant.now()
)

/**
 * Different dimensions of content diversity
 */
enum class DiversityDimension {
    TOPIC,           // Topic/category diversity
    SOURCE,          // Author/source diversity  
    PERSPECTIVE,     // Viewpoint/opinion diversity
    CONTENT_TYPE,    // Text/image/video diversity
    RECENCY,         // Time-based diversity
    SENTIMENT,       // Emotional tone diversity
    LANGUAGE,        // Language diversity
    ENGAGEMENT_TYPE  // Different engagement patterns
}

/**
 * Recommendation for improving diversity
 */
data class DiversityRecommendation(
    val dimension: DiversityDimension,
    val type: RecommendationType,
    val description: String,
    val priority: RecommendationPriority,
    val suggestedAction: String,
    val impactScore: Double
)

/**
 * Type of diversity recommendation
 */
enum class RecommendationType {
    INCREASE_VARIETY,    // Add more diverse content
    REDUCE_CLUSTERING,   // Spread out similar content
    BALANCE_PERSPECTIVES,// Balance different viewpoints
    DIVERSIFY_SOURCES,   // Include more diverse authors
    TEMPORAL_SPACING     // Better time distribution
}

/**
 * Priority level for recommendations
 */
enum class RecommendationPriority {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Content features extracted for diversity analysis
 */
data class ContentFeatures(
    val contentId: String,
    val authorId: String,
    val topics: List<String>,
    val topicCategories: List<TopicCategory>,
    val contentType: String,
    val sentiment: String?,
    val language: String?,
    val source: String,
    val perspective: String = "neutral",
    val createdAt: Instant,
    val engagementPattern: EngagementPattern?
)

/**
 * Engagement pattern classification
 */
data class EngagementPattern(
    val type: EngagementType,
    val intensity: EngagementIntensity,
    val primaryAction: String, // likes, shares, comments
    val timeToEngagement: Long? = null // milliseconds
)

/**
 * Type of engagement
 */
enum class EngagementType {
    HIGH_LIKES,
    HIGH_SHARES,
    HIGH_COMMENTS,
    VIRAL,
    STEADY,
    DECLINING,
    POLARIZING
}

/**
 * Engagement intensity levels
 */
enum class EngagementIntensity {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Historical content distribution for baseline comparison
 */
data class ContentDistribution(
    val userId: String,
    val topicDistribution: Map<String, Double>,
    val categoryDistribution: Map<TopicCategory, Double>,
    val sourceDistribution: Map<String, Double>,
    val contentTypeDistribution: Map<String, Double>,
    val sentimentDistribution: Map<String, Double>,
    val languageDistribution: Map<String, Double>,
    val temporalDistribution: Map<String, Double>, // hour of day, day of week
    val totalItems: Int,
    val timeWindow: TimeWindow,
    val computedAt: Instant = Instant.now()
)

/**
 * Time window for analysis
 */
data class TimeWindow(
    val startTime: Instant,
    val endTime: Instant,
    val duration: Long // milliseconds
)

/**
 * Diversity score calculation configuration
 */
data class DiversityConfig(
    val dimensionWeights: Map<DiversityDimension, Double> = defaultDimensionWeights(),
    val minimumHistorySize: Int = 20,
    val timeWindowDays: Int = 7,
    val similarityThreshold: Double = 0.7,
    val diversityThreshold: Double = 0.6,
    val perspectiveBalanceThreshold: Double = 0.3,
    val enableEchoChamberDetection: Boolean = true,
    val penalizeRepetition: Boolean = true
) {
    companion object {
        fun defaultDimensionWeights() = mapOf(
            DiversityDimension.TOPIC to 0.25,
            DiversityDimension.SOURCE to 0.20,
            DiversityDimension.PERSPECTIVE to 0.20,
            DiversityDimension.CONTENT_TYPE to 0.15,
            DiversityDimension.SENTIMENT to 0.10,
            DiversityDimension.RECENCY to 0.05,
            DiversityDimension.LANGUAGE to 0.03,
            DiversityDimension.ENGAGEMENT_TYPE to 0.02
        )
    }
}

/**
 * Echo chamber detection result
 */
data class EchoChamberAnalysis(
    val isEchoChamber: Boolean,
    val severity: EchoChamberSeverity,
    val dominantPerspectives: List<String>,
    val missingPerspectives: List<String>,
    val topicConcentration: Double, // 0-1, higher means more concentrated
    val sourceConcentration: Double,
    val recommendations: List<String>
)

/**
 * Severity levels for echo chambers
 */
enum class EchoChamberSeverity {
    SEVERE,   // Very limited diversity
    MODERATE, // Somewhat limited diversity
    MILD,     // Minor diversity issues
    NONE      // Good diversity
}

/**
 * Freshness analysis for content
 */
data class FreshnessAnalysis(
    val contentId: String,
    val freshnessScore: Double, // 0-1, higher is fresher
    val factors: Map<FreshnessFactor, Double>,
    val stalenessReason: String?,
    val recommendedAction: FreshnessAction?
)

/**
 * Factors that affect content freshness
 */
enum class FreshnessFactor {
    RECENCY,           // How recent the content is
    NOVELTY,           // How different from recent content
    TRENDING_TOPIC,    // Related to currently trending topics
    ORIGINAL_CONTENT,  // Original vs shared/reposted content
    TIMELY_REFERENCE,  // References current events
    UPDATE_FREQUENCY   // How often similar content appears
}

/**
 * Actions to improve freshness
 */
enum class FreshnessAction {
    BOOST,      // Boost this fresh content
    DEMOTE,     // Reduce priority of stale content
    REPLACE,    // Find fresher alternative
    SCHEDULE    // Schedule for better timing
}