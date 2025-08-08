package com.welcomer.welcome.feed.service

import com.welcomer.welcome.feed.model.*
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * Service for handling cold start scenarios for new users with limited data
 */
interface ColdStartService {
    /**
     * Determine if user is in cold start phase
     */
    fun isNewUser(userPreferences: UserPreferences, config: ColdStartConfig): Boolean

    /**
     * Generate feed for cold start users using trending and popular content
     */
    suspend fun generateColdStartFeed(
        userId: String,
        userPreferences: UserPreferences,
        config: ColdStartConfig,
        limit: Int = 50
    ): List<ContentCandidate>

    /**
     * Adjust scoring weights for cold start users
     */
    fun getColdStartWeights(
        baseWeights: ScoringWeights,
        userPreferences: UserPreferences,
        config: ColdStartConfig
    ): ScoringWeights

    /**
     * Generate diverse topic sampling for content discovery
     */
    suspend fun generateTopicDiverseSampling(
        availableContent: List<ContentCandidate>,
        config: ColdStartConfig,
        limit: Int
    ): List<ContentCandidate>

    /**
     * Get popular content fallback when no other content is available
     */
    suspend fun getPopularContentFallback(
        timeWindow: Duration = Duration.ofDays(7),
        limit: Int = 20
    ): List<ContentCandidate>

    /**
     * Progressive personalization - gradually increase personalization as user engages
     */
    fun calculatePersonalizationLevel(
        userPreferences: UserPreferences,
        config: ColdStartConfig
    ): Double
}

@Service
class DefaultColdStartService(
    private val contentRepository: com.welcomer.welcome.ingestion.repository.ContentRepository
) : ColdStartService {

    companion object {
        // Cold start thresholds
        private const val MIN_ENGAGEMENT_FOR_PERSONALIZATION = 5
        private const val MAX_PERSONALIZATION_DAYS = 30
        private const val TRENDING_SCORE_THRESHOLD = 0.7
        
        // Topic diversity parameters
        private const val MAX_TOPICS_TO_SAMPLE = 15
        private const val MIN_CONTENT_PER_TOPIC = 2
        private const val MAX_CONTENT_PER_TOPIC = 5
        
        // Popular content parameters
        private const val POPULAR_CONTENT_MIN_ENGAGEMENT = 10
        private const val POPULAR_CONTENT_MIN_SCORE = 0.6
    }

    override fun isNewUser(userPreferences: UserPreferences, config: ColdStartConfig): Boolean {
        // Check account age
        val accountAge = userPreferences.accountAge ?: return true
        if (accountAge <= config.newUserThresholdDays) return true
        
        // Check engagement history
        val totalEngagements = userPreferences.engagementHistory.values.sum()
        if (totalEngagements < config.minEngagementActions) return true
        
        // Check last activity
        val lastActive = userPreferences.lastActiveAt ?: return true
        val daysSinceActive = Duration.between(lastActive, Instant.now()).toDays()
        
        return daysSinceActive > config.newUserThresholdDays
    }

    override suspend fun generateColdStartFeed(
        userId: String,
        userPreferences: UserPreferences,
        config: ColdStartConfig,
        limit: Int
    ): List<ContentCandidate> {
        val candidates = mutableListOf<ContentCandidate>()
        
        // 1. Get trending content (high weight)
        if (config.trendingContentWeight > 0) {
            val trendingCount = (limit * config.trendingContentWeight).toInt()
            val trendingContent = getTrendingContent(trendingCount)
            candidates.addAll(trendingContent)
        }
        
        // 2. Get diverse topic sampling
        if (config.diverseTopicSampling && candidates.size < limit) {
            val remainingLimit = limit - candidates.size
            val diverseContent = generateTopicDiverseSampling(
                getRecentQualityContent(remainingLimit * 2), // Get more to have selection
                config,
                remainingLimit
            )
            candidates.addAll(diverseContent)
        }
        
        // 3. Popular content fallback
        if (config.popularContentFallback && candidates.size < limit) {
            val remainingLimit = limit - candidates.size
            val popularContent = getPopularContentFallback(Duration.ofDays(7), remainingLimit)
            candidates.addAll(popularContent)
        }
        
        // 4. Apply basic user preferences if available
        val filteredCandidates = applyBasicUserFilters(candidates, userPreferences)
        
        // 5. Remove duplicates and limit
        return filteredCandidates
            .distinctBy { it.content.id }
            .take(limit)
    }

    override fun getColdStartWeights(
        baseWeights: ScoringWeights,
        userPreferences: UserPreferences,
        config: ColdStartConfig
    ): ScoringWeights {
        val personalizationLevel = calculatePersonalizationLevel(userPreferences, config)
        val trendingWeight = 1.0 - personalizationLevel
        
        return baseWeights.copy(
            // Emphasize recency and popularity for new users
            recency = baseWeights.recency * (0.7 + personalizationLevel * 0.3),
            popularity = baseWeights.popularity * (0.8 + personalizationLevel * 0.2),
            relevance = baseWeights.relevance * personalizationLevel, // Lower relevance for new users
            customWeights = baseWeights.customWeights + mapOf(
                "trending" to config.trendingContentWeight * trendingWeight,
                "diversity" to if (config.diverseTopicSampling) 0.3 else 0.0
            )
        )
    }

    override suspend fun generateTopicDiverseSampling(
        availableContent: List<ContentCandidate>,
        config: ColdStartConfig,
        limit: Int
    ): List<ContentCandidate> {
        if (availableContent.isEmpty()) return emptyList()
        
        // Group content by topics (using tags as proxy for topics)
        val contentByTopic = mutableMapOf<String, MutableList<ContentCandidate>>()
        
        availableContent.forEach { candidate ->
            candidate.content.tags.forEach { topic ->
                contentByTopic.getOrPut(topic) { mutableListOf() }.add(candidate)
            }
        }
        
        // Remove topics with insufficient content
        val viableTopics = contentByTopic.filter { it.value.size >= MIN_CONTENT_PER_TOPIC }
        
        if (viableTopics.isEmpty()) {
            // Fallback to top scored content
            return availableContent.sortedByDescending { it.popularityScore }.take(limit)
        }
        
        // Sample diverse topics
        val selectedTopics = viableTopics.keys
            .shuffled()
            .take(minOf(MAX_TOPICS_TO_SAMPLE, viableTopics.size))
        
        val diverseContent = mutableListOf<ContentCandidate>()
        val contentPerTopic = limit / selectedTopics.size
        
        selectedTopics.forEach { topic ->
            val topicContent = viableTopics[topic] ?: emptyList()
            val sampleSize = minOf(
                maxOf(MIN_CONTENT_PER_TOPIC, contentPerTopic),
                maxOf(MAX_CONTENT_PER_TOPIC, topicContent.size)
            )
            
            // Select best content from this topic
            val selectedContent = topicContent
                .sortedByDescending { it.popularityScore }
                .take(sampleSize)
            
            diverseContent.addAll(selectedContent)
        }
        
        return diverseContent
            .distinctBy { it.content.id }
            .sortedByDescending { it.popularityScore }
            .take(limit)
    }

    override suspend fun getPopularContentFallback(timeWindow: Duration, limit: Int): List<ContentCandidate> {
        // This would typically query trending/popular content from database
        // For now, return empty list as placeholder
        // In real implementation, would use ContentRepository to fetch popular content
        
        return emptyList() // Placeholder - would fetch from database
    }

    override fun calculatePersonalizationLevel(
        userPreferences: UserPreferences,
        config: ColdStartConfig
    ): Double {
        var personalizationLevel = 0.0
        
        // Account age factor (0 to 1)
        val accountAge = userPreferences.accountAge ?: 0
        val ageFactor = minOf(1.0, accountAge.toDouble() / MAX_PERSONALIZATION_DAYS)
        personalizationLevel += ageFactor * 0.4
        
        // Engagement history factor (0 to 1)
        val totalEngagements = userPreferences.engagementHistory.size
        val engagementFactor = minOf(1.0, totalEngagements.toDouble() / (config.minEngagementActions * 3))
        personalizationLevel += engagementFactor * 0.4
        
        // Interest specification factor (0 to 1)
        val interestFactor = minOf(1.0, userPreferences.interests.size.toDouble() / 5.0)
        personalizationLevel += interestFactor * 0.2
        
        return minOf(1.0, personalizationLevel)
    }

    // Private helper methods

    private suspend fun getTrendingContent(limit: Int): List<ContentCandidate> {
        // This would typically query trending content from database or cache
        // Based on engagement metrics, recent activity, etc.
        
        // Placeholder implementation
        return emptyList()
    }

    private suspend fun getRecentQualityContent(limit: Int): List<ContentCandidate> {
        // This would query recent content with minimum quality thresholds
        // Based on engagement metrics, author reputation, etc.
        
        // Placeholder implementation
        return emptyList()
    }

    private fun applyBasicUserFilters(
        candidates: List<ContentCandidate>,
        userPreferences: UserPreferences
    ): List<ContentCandidate> {
        return candidates.filter { candidate ->
            val content = candidate.content
            
            // Filter blocked users
            if (userPreferences.blockedUsers.contains(content.authorId)) {
                return@filter false
            }
            
            // Filter blocked topics
            val hasBlockedTopic = content.tags.any { tag ->
                userPreferences.blockedTopics.any { blocked ->
                    tag.contains(blocked, ignoreCase = true)
                }
            }
            if (hasBlockedTopic) return@filter false
            
            // Language filtering (if specified)
            if (userPreferences.languagePreferences.isNotEmpty() && 
                content.languageCode != null &&
                !userPreferences.languagePreferences.contains(content.languageCode)) {
                return@filter false
            }
            
            // Content type filtering (if specified)
            if (userPreferences.preferredContentTypes.isNotEmpty() &&
                !userPreferences.preferredContentTypes.contains(content.contentType.name.lowercase())) {
                return@filter false
            }
            
            true
        }
    }
}