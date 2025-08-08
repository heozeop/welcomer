package com.welcomer.welcome.feed.service

import com.welcomer.welcome.feed.model.*
import com.welcomer.welcome.ingestion.model.StoredContent
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import kotlin.math.*

/**
 * Service for scoring content based on various factors for feed ranking
 */
interface ContentScoringService {
    /**
     * Calculate recency score based on content age
     * Returns score between 0.0 and 1.0, higher for newer content
     */
    fun calculateRecencyScore(createdAt: Instant, baselineTime: Instant = Instant.now()): Double

    /**
     * Calculate popularity score based on engagement metrics
     * Returns score between 0.0 and 1.0, higher for more popular content
     */
    fun calculatePopularityScore(metrics: EngagementMetrics, contentAge: Duration): Double

    /**
     * Calculate relevance score based on user preferences and content attributes
     * Returns score between 0.0 and 1.0, higher for more relevant content
     */
    fun calculateRelevanceScore(content: StoredContent, userPreferences: UserPreferences): Double

    /**
     * Calculate composite score using weighted combination of individual scores
     */
    fun calculateCompositeScore(
        recencyScore: Double,
        popularityScore: Double,
        relevanceScore: Double,
        weights: ScoringWeights,
        bonusFactors: Map<String, Double> = emptyMap()
    ): Double

    /**
     * Score a content candidate with all factors
     */
    suspend fun scoreContent(
        candidate: ContentCandidate,
        userPreferences: UserPreferences,
        weights: ScoringWeights
    ): Double
}

@Service
class DefaultContentScoringService : ContentScoringService {

    companion object {
        // Recency scoring parameters
        private const val RECENCY_HALF_LIFE_HOURS = 24.0 // Content loses half relevance after 24h
        private const val RECENCY_MIN_SCORE = 0.1 // Minimum recency score
        
        // Popularity scoring parameters
        private const val POPULARITY_ENGAGEMENT_WEIGHT = 0.6
        private const val POPULARITY_VIEW_WEIGHT = 0.4
        private const val POPULARITY_TIME_DECAY_FACTOR = 0.8 // Older content needs higher engagement
        
        // Relevance scoring parameters
        private const val RELEVANCE_EXACT_MATCH_BONUS = 0.3
        private const val RELEVANCE_PARTIAL_MATCH_WEIGHT = 0.2
        private const val RELEVANCE_LANGUAGE_WEIGHT = 0.1
        private const val RELEVANCE_CONTENT_TYPE_WEIGHT = 0.1
        private const val RELEVANCE_AUTHOR_FOLLOW_BONUS = 0.2
    }

    override fun calculateRecencyScore(createdAt: Instant, baselineTime: Instant): Double {
        val ageHours = Duration.between(createdAt, baselineTime).toMillis() / (1000.0 * 3600.0)
        
        // Exponential decay with configurable half-life
        val decayFactor = exp(-ageHours * ln(2.0) / RECENCY_HALF_LIFE_HOURS)
        
        // Ensure minimum score and normalize to 0-1 range
        return maxOf(RECENCY_MIN_SCORE, decayFactor)
    }

    override fun calculatePopularityScore(metrics: EngagementMetrics, contentAge: Duration): Double {
        // Calculate engagement score (likes + comments + shares weighted by importance)
        val engagementScore = (metrics.likes * 1.0 + metrics.comments * 2.0 + metrics.shares * 3.0)
        
        // Calculate view-based metrics
        val viewScore = if (metrics.views > 0) {
            metrics.clickThroughRate * 100.0 + metrics.engagementRate * 50.0
        } else 0.0
        
        // Combine engagement and view scores
        val rawScore = engagementScore * POPULARITY_ENGAGEMENT_WEIGHT + viewScore * POPULARITY_VIEW_WEIGHT
        
        // Apply time decay - older content needs higher engagement to maintain score
        val ageHours = contentAge.toHours().toDouble()
        val timeDecayFactor = exp(-ageHours * ln(2.0) / (RECENCY_HALF_LIFE_HOURS * 2)) * POPULARITY_TIME_DECAY_FACTOR + (1 - POPULARITY_TIME_DECAY_FACTOR)
        
        val adjustedScore = rawScore * timeDecayFactor
        
        // Normalize using sigmoid function to 0-1 range
        return 1.0 / (1.0 + exp(-adjustedScore / 10.0))
    }

    override fun calculateRelevanceScore(content: StoredContent, userPreferences: UserPreferences): Double {
        var relevanceScore = 0.0
        
        // 1. Interest matching
        relevanceScore += calculateInterestScore(content, userPreferences.interests)
        
        // 2. Content type preferences
        relevanceScore += calculateContentTypeScore(content, userPreferences.preferredContentTypes)
        
        // 3. Language matching
        relevanceScore += calculateLanguageScore(content, userPreferences.languagePreferences)
        
        // 4. Author following bonus
        if (userPreferences.blockedUsers.contains(content.authorId)) {
            return 0.0 // Blocked users get zero relevance
        }
        
        // 5. Topic blocking
        if (isTopicBlocked(content, userPreferences.blockedTopics)) {
            return 0.0 // Blocked topics get zero relevance
        }
        
        // 6. Historical engagement
        relevanceScore += calculateHistoricalEngagementScore(content, userPreferences.engagementHistory)
        
        // Normalize to 0-1 range
        return minOf(1.0, maxOf(0.0, relevanceScore))
    }

    override fun calculateCompositeScore(
        recencyScore: Double,
        popularityScore: Double,
        relevanceScore: Double,
        weights: ScoringWeights,
        bonusFactors: Map<String, Double>
    ): Double {
        // Base weighted score
        var compositeScore = (recencyScore * weights.recency + 
                             popularityScore * weights.popularity + 
                             relevanceScore * weights.relevance)
        
        // Add following bonus
        compositeScore += bonusFactors["following"] ?: 0.0 * weights.following
        
        // Add engagement bonus
        compositeScore += bonusFactors["engagement"] ?: 0.0 * weights.engagement
        
        // Add custom weights
        weights.customWeights.forEach { (key, weight) ->
            compositeScore += (bonusFactors[key] ?: 0.0) * weight
        }
        
        // Ensure score is in valid range
        return minOf(1.0, maxOf(0.0, compositeScore))
    }

    override suspend fun scoreContent(
        candidate: ContentCandidate,
        userPreferences: UserPreferences,
        weights: ScoringWeights
    ): Double {
        val now = Instant.now()
        val contentAge = Duration.between(candidate.content.createdAt, now)
        
        // Calculate individual scores
        val recencyScore = calculateRecencyScore(candidate.content.createdAt, now)
        val popularityScore = candidate.engagementMetrics?.let { metrics ->
            calculatePopularityScore(metrics, contentAge)
        } ?: candidate.popularityScore
        val relevanceScore = calculateRelevanceScore(candidate.content, userPreferences)
        
        // Bonus factors
        val bonusFactors = mapOf(
            "following" to if (candidate.authorFollowingStatus) 1.0 else 0.0,
            "engagement" to (candidate.engagementMetrics?.engagementRate ?: 0.0),
            "topicRelevance" to candidate.topicRelevance,
            "languageMatch" to if (candidate.languageMatch) 1.0 else 0.0
        )
        
        return calculateCompositeScore(recencyScore, popularityScore, relevanceScore, weights, bonusFactors)
    }

    // Private helper methods

    private fun calculateInterestScore(content: StoredContent, interests: List<String>): Double {
        if (interests.isEmpty()) return 0.5 // Neutral score if no interests specified
        
        var interestScore = 0.0
        val contentText = "${content.textContent ?: ""} ${content.tags.joinToString(" ")}"
        
        interests.forEach { interest ->
            when {
                // Exact match in tags
                content.tags.any { tag -> tag.equals(interest, ignoreCase = true) } -> {
                    interestScore += RELEVANCE_EXACT_MATCH_BONUS
                }
                // Partial match in content or tags
                contentText.contains(interest, ignoreCase = true) -> {
                    interestScore += RELEVANCE_PARTIAL_MATCH_WEIGHT
                }
            }
        }
        
        return minOf(1.0, interestScore / interests.size)
    }

    private fun calculateContentTypeScore(content: StoredContent, preferredTypes: Set<String>): Double {
        if (preferredTypes.isEmpty()) return 0.0
        
        return if (preferredTypes.contains(content.contentType.name.lowercase())) {
            RELEVANCE_CONTENT_TYPE_WEIGHT
        } else 0.0
    }

    private fun calculateLanguageScore(content: StoredContent, languagePreferences: List<String>): Double {
        if (languagePreferences.isEmpty()) return 0.0
        
        return if (content.languageCode != null && languagePreferences.contains(content.languageCode)) {
            RELEVANCE_LANGUAGE_WEIGHT
        } else 0.0
    }

    private fun calculateHistoricalEngagementScore(content: StoredContent, engagementHistory: Map<String, Double>): Double {
        // Look for engagement with similar content from same author
        val authorEngagement = engagementHistory.entries
            .filter { it.key.startsWith(content.authorId) } // Assume format "authorId:contentId"
            .map { it.value }
            .average()
        
        return if (authorEngagement.isNaN()) 0.0 else minOf(0.3, authorEngagement * 0.3)
    }

    private fun isTopicBlocked(content: StoredContent, blockedTopics: Set<String>): Boolean {
        return content.tags.any { tag -> 
            blockedTopics.any { blocked -> tag.contains(blocked, ignoreCase = true) }
        }
    }
}