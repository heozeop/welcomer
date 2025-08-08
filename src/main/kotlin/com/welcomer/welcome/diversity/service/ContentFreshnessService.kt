package com.welcomer.welcome.diversity.service

import com.welcomer.welcome.diversity.model.*
import com.welcomer.welcome.feed.model.FeedEntry
import com.welcomer.welcome.ingestion.model.StoredContent
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.*

/**
 * Service for identifying and scoring content freshness
 */
interface ContentFreshnessService {
    /**
     * Calculate freshness scores for candidate content items
     */
    suspend fun calculateFreshnessScores(
        candidateItems: List<StoredContent>,
        currentTime: Instant = Instant.now()
    ): List<FreshnessAnalysis>

    /**
     * Identify stale content that should be deprioritized
     */
    suspend fun identifyStaleContent(
        userFeedHistory: List<FeedEntry>,
        timeThresholdHours: Long = 72,
        currentTime: Instant = Instant.now()
    ): List<StaleContentAnalysis>

    /**
     * Get trending boost for content based on current trends
     */
    suspend fun getTrendingBoost(contentId: String): Double

    /**
     * Calculate time-based decay score for content age
     */
    fun calculateRecencyScore(ageInHours: Double): Double

    /**
     * Identify content that references current events or timely topics
     */
    suspend fun identifyTimelyContent(content: StoredContent): TimelinessAnalysis
}

/**
 * Analysis result for stale content identification
 */
data class StaleContentAnalysis(
    val contentId: String,
    val stalenessScore: Double, // 0-1, higher means more stale
    val reasons: List<StalenessReason>,
    val overexposureMetrics: Map<String, Double>,
    val recommendedAction: FreshnessAction,
    val analysisTimestamp: Instant = Instant.now()
)

/**
 * Reasons why content is considered stale
 */
enum class StalenessReason {
    OLD_CONTENT,         // Content is old
    OVEREXPOSED_TOPIC,   // Topic has been heavily featured
    OVEREXPOSED_AUTHOR,  // Author's content is overrepresented
    DECLINING_ENGAGEMENT,// Engagement is decreasing
    REPETITIVE_TYPE,     // Content type is overused
    OUTDATED_REFERENCE,  // References outdated events
    SEASONAL_MISMATCH    // Content doesn't match current season/time
}

/**
 * Analysis of content timeliness
 */
data class TimelinessAnalysis(
    val isTimely: Boolean,
    val timeRelevanceScore: Double, // 0-1, higher means more time-relevant
    val timelyFactors: List<TimelyFactor>,
    val expirationPrediction: Instant?, // When content might become stale
    val seasonalRelevance: SeasonalRelevance?
)

/**
 * Factors that make content timely
 */
enum class TimelyFactor {
    BREAKING_NEWS,       // References breaking news
    CURRENT_EVENT,       // Related to current events
    SEASONAL_TOPIC,      // Seasonal relevance
    TREND_REFERENCE,     // References current trends
    HOLIDAY_RELATED,     // Related to upcoming holidays
    WEATHER_DEPENDENT,   // Weather-related content
    MARKET_RELEVANT      // Financial/market timing relevance
}

/**
 * Seasonal relevance information
 */
data class SeasonalRelevance(
    val season: Season,
    val relevanceScore: Double,
    val peakPeriod: Pair<Int, Int>, // month/day ranges
    val isCurrentlyRelevant: Boolean
)

/**
 * Season enumeration
 */
enum class Season {
    SPRING, SUMMER, AUTUMN, WINTER, HOLIDAY, ANY
}

/**
 * Configuration for freshness calculations
 */
data class FreshnessConfig(
    val recencyDecayRate: Double = 0.05,           // Hourly decay rate
    val trendingBoostMultiplier: Double = 0.5,     // Max boost for trending content
    val timeRelevanceMultiplier: Double = 0.2,     // Boost for time-relevant content
    val staleThresholdScore: Double = 0.3,         // Below this is considered stale
    val maxContentAge: Long = 168,                 // Max age in hours (1 week)
    val overexposureThreshold: Double = 0.4,       // Topic/author overexposure limit
    val engagementDecayThreshold: Double = 0.5     // Engagement decline threshold
)

/**
 * Default implementation of ContentFreshnessService
 */
@Service
class DefaultContentFreshnessService(
    private val trendingService: TrendingAnalysisService? = null
) : ContentFreshnessService {

    private val config = FreshnessConfig()

    override suspend fun calculateFreshnessScores(
        candidateItems: List<StoredContent>,
        currentTime: Instant
    ): List<FreshnessAnalysis> {
        return candidateItems.map { content ->
            calculateSingleContentFreshness(content, currentTime)
        }
    }

    override suspend fun identifyStaleContent(
        userFeedHistory: List<FeedEntry>,
        timeThresholdHours: Long,
        currentTime: Instant
    ): List<StaleContentAnalysis> {
        if (userFeedHistory.isEmpty()) return emptyList()

        // Filter to recent history within threshold
        val recentHistory = userFeedHistory.filter { entry ->
            val ageInHours = ChronoUnit.HOURS.between(entry.generatedAt, currentTime)
            ageInHours <= timeThresholdHours
        }

        // Calculate overexposure metrics
        val topicOverexposure = calculateTopicOverexposure(recentHistory)
        val authorOverexposure = calculateAuthorOverexposure(recentHistory)
        val typeOverexposure = calculateContentTypeOverexposure(recentHistory)

        // Identify stale content
        return recentHistory.mapNotNull { entry ->
            val staleness = calculateStalenessScore(
                entry, topicOverexposure, authorOverexposure, typeOverexposure, currentTime
            )
            
            if (staleness.stalenessScore > config.staleThresholdScore) {
                staleness
            } else null
        }
    }

    override suspend fun getTrendingBoost(contentId: String): Double {
        // Mock implementation - would integrate with actual trending service
        return trendingService?.getTrendingScore(contentId) ?: 0.0
    }

    override fun calculateRecencyScore(ageInHours: Double): Double {
        // Exponential decay function - very fresh content gets highest score
        return exp(-config.recencyDecayRate * ageInHours)
    }

    override suspend fun identifyTimelyContent(content: StoredContent): TimelinessAnalysis {
        val timelyFactors = mutableListOf<TimelyFactor>()
        var timeRelevanceScore = 0.0

        // Check for time-sensitive keywords in content
        val contentText = (content.textContent ?: "") + " " + content.tags.joinToString(" ")
        val lowerText = contentText.lowercase()

        // Breaking news indicators
        if (lowerText.contains("breaking") || lowerText.contains("urgent") || 
            lowerText.contains("just in") || lowerText.contains("developing")) {
            timelyFactors.add(TimelyFactor.BREAKING_NEWS)
            timeRelevanceScore += 0.3
        }

        // Current events indicators
        if (lowerText.contains("today") || lowerText.contains("now") || 
            lowerText.contains("current") || lowerText.contains("latest")) {
            timelyFactors.add(TimelyFactor.CURRENT_EVENT)
            timeRelevanceScore += 0.2
        }

        // Seasonal content detection
        val seasonalRelevance = detectSeasonalRelevance(lowerText)
        if (seasonalRelevance != null) {
            timelyFactors.add(TimelyFactor.SEASONAL_TOPIC)
            timeRelevanceScore += seasonalRelevance.relevanceScore * 0.15
        }

        // Holiday-related content
        if (detectHolidayContent(lowerText)) {
            timelyFactors.add(TimelyFactor.HOLIDAY_RELATED)
            timeRelevanceScore += 0.25
        }

        // Trend references
        if (detectTrendReferences(lowerText)) {
            timelyFactors.add(TimelyFactor.TREND_REFERENCE)
            timeRelevanceScore += 0.2
        }

        val isTimely = timeRelevanceScore > 0.1
        val expirationPrediction = if (isTimely) {
            predictContentExpiration(content, timelyFactors)
        } else null

        return TimelinessAnalysis(
            isTimely = isTimely,
            timeRelevanceScore = min(timeRelevanceScore, 1.0),
            timelyFactors = timelyFactors,
            expirationPrediction = expirationPrediction,
            seasonalRelevance = seasonalRelevance
        )
    }

    // Private helper methods

    private suspend fun calculateSingleContentFreshness(
        content: StoredContent,
        currentTime: Instant
    ): FreshnessAnalysis {
        val ageInHours = ChronoUnit.HOURS.between(content.createdAt, currentTime).toDouble()
        
        // Base freshness from recency
        var freshnessScore = calculateRecencyScore(ageInHours)
        val factors = mutableMapOf<FreshnessFactor, Double>()
        factors[FreshnessFactor.RECENCY] = freshnessScore

        // Apply trending boost
        val trendingBoost = getTrendingBoost(content.id)
        if (trendingBoost > 0) {
            freshnessScore *= (1 + trendingBoost * config.trendingBoostMultiplier)
            factors[FreshnessFactor.TRENDING_TOPIC] = trendingBoost
        }

        // Check for timeliness
        val timelinessAnalysis = identifyTimelyContent(content)
        if (timelinessAnalysis.isTimely) {
            freshnessScore *= (1 + timelinessAnalysis.timeRelevanceScore * config.timeRelevanceMultiplier)
            factors[FreshnessFactor.TIMELY_REFERENCE] = timelinessAnalysis.timeRelevanceScore
        }

        // Check for novelty (original vs shared content)
        val noveltyScore = calculateNoveltyScore(content)
        if (noveltyScore > 0) {
            freshnessScore *= (1 + noveltyScore * 0.1)
            factors[FreshnessFactor.ORIGINAL_CONTENT] = noveltyScore
        }

        // Determine recommended action
        val recommendedAction = when {
            freshnessScore > 0.8 -> FreshnessAction.BOOST
            freshnessScore < 0.3 -> FreshnessAction.DEMOTE
            freshnessScore < 0.5 && ageInHours > 48 -> FreshnessAction.REPLACE
            else -> FreshnessAction.SCHEDULE
        }

        // Identify staleness reasons if score is low
        val stalenessReason = if (freshnessScore < config.staleThresholdScore) {
            when {
                ageInHours > config.maxContentAge -> "Content is too old (${ageInHours}h)"
                !timelinessAnalysis.isTimely && trendingBoost == 0.0 -> "Content lacks timeliness and trending signals"
                else -> "Low overall freshness score"
            }
        } else null

        return FreshnessAnalysis(
            contentId = content.id,
            freshnessScore = min(freshnessScore, 1.0),
            factors = factors,
            stalenessReason = stalenessReason,
            recommendedAction = recommendedAction
        )
    }

    private fun calculateStalenessScore(
        entry: FeedEntry,
        topicOverexposure: Map<String, Double>,
        authorOverexposure: Map<String, Double>,
        typeOverexposure: Map<String, Double>,
        currentTime: Instant
    ): StaleContentAnalysis {
        val content = entry.content
        var stalenessScore = 0.0
        val reasons = mutableListOf<StalenessReason>()
        val overexposureMetrics = mutableMapOf<String, Double>()

        // Age-based staleness
        val ageInHours = ChronoUnit.HOURS.between(content.createdAt, currentTime).toDouble()
        val ageScore = 1.0 - calculateRecencyScore(ageInHours)
        if (ageScore > 0.7) {
            stalenessScore += ageScore * 0.4
            reasons.add(StalenessReason.OLD_CONTENT)
        }

        // Topic overexposure
        val topicExposure = content.tags.maxOfOrNull { topicOverexposure[it] ?: 0.0 } ?: 0.0
        if (topicExposure > config.overexposureThreshold) {
            stalenessScore += topicExposure * 0.3
            reasons.add(StalenessReason.OVEREXPOSED_TOPIC)
            overexposureMetrics["topic"] = topicExposure
        }

        // Author overexposure
        val authorExposure = authorOverexposure[content.authorId] ?: 0.0
        if (authorExposure > config.overexposureThreshold) {
            stalenessScore += authorExposure * 0.2
            reasons.add(StalenessReason.OVEREXPOSED_AUTHOR)
            overexposureMetrics["author"] = authorExposure
        }

        // Content type overexposure
        val typeExposure = typeOverexposure[content.contentType.name] ?: 0.0
        if (typeExposure > config.overexposureThreshold) {
            stalenessScore += typeExposure * 0.1
            reasons.add(StalenessReason.REPETITIVE_TYPE)
            overexposureMetrics["type"] = typeExposure
        }

        val recommendedAction = when {
            stalenessScore > 0.8 -> FreshnessAction.REPLACE
            stalenessScore > 0.6 -> FreshnessAction.DEMOTE
            stalenessScore > 0.4 -> FreshnessAction.SCHEDULE
            else -> FreshnessAction.BOOST
        }

        return StaleContentAnalysis(
            contentId = content.id,
            stalenessScore = min(stalenessScore, 1.0),
            reasons = reasons,
            overexposureMetrics = overexposureMetrics,
            recommendedAction = recommendedAction
        )
    }

    private fun calculateTopicOverexposure(history: List<FeedEntry>): Map<String, Double> {
        val topicCounts = mutableMapOf<String, Int>()
        val totalCount = history.size
        
        history.forEach { entry ->
            entry.content.tags.forEach { topic ->
                topicCounts[topic] = topicCounts.getOrDefault(topic, 0) + 1
            }
        }
        
        return topicCounts.mapValues { it.value.toDouble() / totalCount }
    }

    private fun calculateAuthorOverexposure(history: List<FeedEntry>): Map<String, Double> {
        val authorCounts = history.groupingBy { it.content.authorId }.eachCount()
        val totalCount = history.size
        return authorCounts.mapValues { it.value.toDouble() / totalCount }
    }

    private fun calculateContentTypeOverexposure(history: List<FeedEntry>): Map<String, Double> {
        val typeCounts = history.groupingBy { it.content.contentType.name }.eachCount()
        val totalCount = history.size
        return typeCounts.mapValues { it.value.toDouble() / totalCount }
    }

    private fun calculateNoveltyScore(content: StoredContent): Double {
        // Simple novelty calculation - would be enhanced with duplicate detection
        return when {
            content.replyToId != null -> 0.3 // Reply/comment has lower novelty
            content.linkUrl != null -> 0.7  // Shared content has medium novelty
            else -> 1.0                     // Original content has highest novelty
        }
    }

    private fun detectSeasonalRelevance(text: String): SeasonalRelevance? {
        val currentMonth = java.time.LocalDate.now().monthValue
        val currentSeason = when (currentMonth) {
            12, 1, 2 -> Season.WINTER
            3, 4, 5 -> Season.SPRING
            6, 7, 8 -> Season.SUMMER
            9, 10, 11 -> Season.AUTUMN
            else -> Season.ANY
        }

        val seasonKeywords = mapOf(
            Season.WINTER to listOf("winter", "snow", "cold", "holiday", "christmas", "new year"),
            Season.SPRING to listOf("spring", "bloom", "easter", "renewal", "fresh"),
            Season.SUMMER to listOf("summer", "vacation", "beach", "hot", "travel"),
            Season.AUTUMN to listOf("fall", "autumn", "harvest", "thanksgiving", "leaves")
        )

        seasonKeywords[currentSeason]?.let { keywords ->
            val matches = keywords.count { text.contains(it) }
            if (matches > 0) {
                return SeasonalRelevance(
                    season = currentSeason,
                    relevanceScore = min(matches * 0.2, 1.0),
                    peakPeriod = Pair(currentMonth, currentMonth),
                    isCurrentlyRelevant = true
                )
            }
        }

        return null
    }

    private fun detectHolidayContent(text: String): Boolean {
        val holidayKeywords = listOf(
            "holiday", "christmas", "thanksgiving", "easter", "valentine", 
            "halloween", "new year", "independence day", "memorial day"
        )
        return holidayKeywords.any { text.contains(it) }
    }

    private fun detectTrendReferences(text: String): Boolean {
        val trendKeywords = listOf(
            "viral", "trending", "#", "meme", "challenge", "latest trend"
        )
        return trendKeywords.any { text.contains(it) }
    }

    private fun predictContentExpiration(content: StoredContent, timelyFactors: List<TimelyFactor>): Instant {
        val baseExpiration = content.createdAt.plus(7, ChronoUnit.DAYS) // Default 1 week
        
        return when {
            TimelyFactor.BREAKING_NEWS in timelyFactors -> content.createdAt.plus(6, ChronoUnit.HOURS)
            TimelyFactor.CURRENT_EVENT in timelyFactors -> content.createdAt.plus(24, ChronoUnit.HOURS)
            TimelyFactor.SEASONAL_TOPIC in timelyFactors -> content.createdAt.plus(30, ChronoUnit.DAYS)
            else -> baseExpiration
        }
    }
}

/**
 * Mock trending analysis service for demonstration
 */
interface TrendingAnalysisService {
    suspend fun getTrendingScore(contentId: String): Double
}