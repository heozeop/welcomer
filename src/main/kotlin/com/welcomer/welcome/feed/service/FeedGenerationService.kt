package com.welcomer.welcome.feed.service

import com.welcomer.welcome.feed.model.*
import com.welcomer.welcome.ingestion.repository.ContentRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import kotlin.system.measureTimeMillis

/**
 * Main service for generating personalized feeds
 */
interface FeedGenerationService {
    /**
     * Generate feed for a user based on their preferences and algorithm configuration
     */
    suspend fun generateFeed(request: FeedGenerationRequest): GeneratedFeed

    /**
     * Get candidate content for feed generation
     */
    suspend fun getCandidateContent(
        userId: String,
        feedType: FeedType,
        limit: Int,
        filters: Map<String, Any> = emptyMap()
    ): List<ContentCandidate>

    /**
     * Score and rank content candidates
     */
    suspend fun scoreAndRankContent(
        candidates: List<ContentCandidate>,
        userPreferences: UserPreferences,
        weights: ScoringWeights,
        diversityConfig: DiversityConfig
    ): List<Pair<ContentCandidate, Double>>

    /**
     * Get user preferences for feed generation
     */
    suspend fun getUserPreferences(userId: String): UserPreferences

    /**
     * Get default algorithm configuration for feed type
     */
    suspend fun getDefaultAlgorithmConfig(feedType: FeedType): AlgorithmConfig
}

/**
 * Algorithm configuration
 */
data class AlgorithmConfig(
    val id: String,
    val name: String,
    val weights: ScoringWeights,
    val diversityConfig: DiversityConfig,
    val coldStartConfig: ColdStartConfig,
    val parameters: Map<String, Any> = emptyMap()
)

@Service
class DefaultFeedGenerationService(
    private val contentRepository: ContentRepository,
    private val scoringService: ContentScoringService,
    private val diversityService: FeedDiversityService,
    private val coldStartService: ColdStartService,
    private val abTestingService: ABTestingService,
    private val feedIntegrationService: com.welcomer.welcome.user.integration.FeedIntegrationService
) : FeedGenerationService {

    companion object {
        // Default configurations
        private val DEFAULT_SCORING_WEIGHTS = ScoringWeights(
            recency = 0.5,
            popularity = 0.3,
            relevance = 0.2,
            following = 0.0,
            engagement = 0.0
        )
        
        private val DEFAULT_DIVERSITY_CONFIG = DiversityConfig(
            maxSameAuthor = 3,
            maxSameTopic = 5,
            maxSameContentType = 10,
            authorSpacing = 3,
            topicSpacing = 2,
            enforceContentTypeBalance = true
        )
        
        private val DEFAULT_COLD_START_CONFIG = ColdStartConfig(
            newUserThresholdDays = 7,
            minEngagementActions = 10,
            trendingContentWeight = 0.7,
            diverseTopicSampling = true,
            popularContentFallback = true
        )
    }

    override suspend fun generateFeed(request: FeedGenerationRequest): GeneratedFeed {
        val startTime = Instant.now()
        var candidatesEvaluated = 0
        var scoringTime: Long
        var diversityTime: Long

        try {
            // 1. Get user preferences
            val userPreferences = getUserPreferences(request.userId)

            // 2. Get algorithm configuration
            val algorithmConfig = getAlgorithmConfig(request)

            // 3. Check for A/B testing
            val experimentConfig = abTestingService.getUserExperiment(request.userId, request.feedType)
            val effectiveWeights = experimentConfig?.let { config ->
                abTestingService.applyExperimentParameters(algorithmConfig.weights, config)
            } ?: algorithmConfig.weights

            // 4. Handle cold start users
            val (candidates, isColdStart) = if (coldStartService.isNewUser(userPreferences, algorithmConfig.coldStartConfig)) {
                val coldStartCandidates = coldStartService.generateColdStartFeed(
                    request.userId,
                    userPreferences,
                    algorithmConfig.coldStartConfig,
                    request.limit * 3 // Get more candidates to have selection
                )
                coldStartCandidates to true
            } else {
                val regularCandidates = getCandidateContent(request.userId, request.feedType, request.limit * 3)
                regularCandidates to false
            }

            candidatesEvaluated = candidates.size

            // 5. Score and rank content
            val scoredContent = measureTimeMillis {
                scoreAndRankContent(candidates, userPreferences, effectiveWeights, algorithmConfig.diversityConfig)
            }.let { time ->
                scoringTime = time
                scoreAndRankContent(candidates, userPreferences, effectiveWeights, algorithmConfig.diversityConfig)
            }

            // 6. Apply diversity rules
            val diversifiedContent = measureTimeMillis {
                diversityService.applyDiversityRules(scoredContent, algorithmConfig.diversityConfig)
            }.let { time ->
                diversityTime = time
                diversityService.applyDiversityRules(scoredContent, algorithmConfig.diversityConfig)
            }

            // 7. Create feed entries
            val feedEntries = diversifiedContent.take(request.limit).mapIndexed { index, (candidate, score) ->
                createFeedEntry(candidate, score, index, algorithmConfig.id, isColdStart)
            }

            // 8. Create feed metadata
            val generationDuration = Duration.between(startTime, Instant.now()).toMillis()
            val feedMetadata = FeedMetadata(
                algorithmId = algorithmConfig.id,
                algorithmVersion = "1.0.0",
                generationDuration = generationDuration,
                contentCount = feedEntries.size,
                candidateCount = candidatesEvaluated,
                parameters = buildMap {
                    putAll(algorithmConfig.parameters)
                    put("is_cold_start", isColdStart)
                    put("scoring_time_ms", scoringTime)
                    put("diversity_time_ms", diversityTime)
                    experimentConfig?.let { put("experiment_config", it) }
                }
            )

            // 9. Log experiment metrics if applicable
            experimentConfig?.let { config ->
                abTestingService.logExperimentMetrics(request.userId, config, feedMetadata)
            }

            return GeneratedFeed(
                userId = request.userId,
                feedType = request.feedType,
                entries = feedEntries,
                metadata = feedMetadata,
                nextCursor = if (feedEntries.size == request.limit) feedEntries.last().id else null,
                hasMore = diversifiedContent.size > request.limit
            )

        } catch (exception: Exception) {
            // Log error and return empty feed with error metadata
            val generationDuration = Duration.between(startTime, Instant.now()).toMillis()
            val errorMetadata = FeedMetadata(
                algorithmId = "error",
                algorithmVersion = "1.0.0",
                generationDuration = generationDuration,
                contentCount = 0,
                candidateCount = candidatesEvaluated,
                parameters = mapOf(
                    "error" to (exception.message ?: "Unknown error"),
                    "error_type" to exception.javaClass.simpleName
                )
            )

            return GeneratedFeed(
                userId = request.userId,
                feedType = request.feedType,
                entries = emptyList(),
                metadata = errorMetadata
            )
        }
    }

    override suspend fun getCandidateContent(
        userId: String,
        feedType: FeedType,
        limit: Int,
        filters: Map<String, Any>
    ): List<ContentCandidate> {
        // This would implement complex querying logic based on feed type
        // For now, return empty list as placeholder
        
        return when (feedType) {
            FeedType.HOME -> {
                // Get content from followed users + recommendations + trending
                getHomeContentCandidates(userId, limit, filters)
            }
            FeedType.FOLLOWING -> {
                // Get content only from followed users
                getFollowingContentCandidates(userId, limit, filters)
            }
            FeedType.EXPLORE -> {
                // Get discovery content
                getExploreContentCandidates(userId, limit, filters)
            }
            FeedType.TRENDING -> {
                // Get trending content
                getTrendingContentCandidates(limit, filters)
            }
            FeedType.PERSONALIZED -> {
                // Get ML-based personalized recommendations
                getPersonalizedContentCandidates(userId, limit, filters)
            }
        }
    }

    override suspend fun scoreAndRankContent(
        candidates: List<ContentCandidate>,
        userPreferences: UserPreferences,
        weights: ScoringWeights,
        diversityConfig: DiversityConfig
    ): List<Pair<ContentCandidate, Double>> {
        // Score all candidates
        val scoredCandidates = candidates.map { candidate ->
            val score = scoringService.scoreContent(candidate, userPreferences, weights)
            candidate to score
        }

        // Sort by score (highest first)
        return scoredCandidates.sortedByDescending { it.second }
    }

    override suspend fun getUserPreferences(userId: String): UserPreferences {
        // Use the feed integration service to get preferences
        return try {
            feedIntegrationService.getFeedUserPreferences(userId)
        } catch (e: Exception) {
            // Fallback to default preferences if there's an error
            UserPreferences(
                userId = userId,
                interests = emptyList(),
                preferredContentTypes = emptySet(),
                blockedUsers = emptySet(),
                blockedTopics = emptySet(),
                languagePreferences = listOf("en"),
                engagementHistory = emptyMap(),
                lastActiveAt = Instant.now(),
                accountAge = 1 // Default to 1 day old
            )
        }
    }

    override suspend fun getDefaultAlgorithmConfig(feedType: FeedType): AlgorithmConfig {
        return AlgorithmConfig(
            id = "default_${feedType.name.lowercase()}",
            name = "Default ${feedType.name} Algorithm",
            weights = DEFAULT_SCORING_WEIGHTS,
            diversityConfig = DEFAULT_DIVERSITY_CONFIG,
            coldStartConfig = DEFAULT_COLD_START_CONFIG,
            parameters = mapOf("feed_type" to feedType.name)
        )
    }

    // Private helper methods

    private suspend fun getAlgorithmConfig(request: FeedGenerationRequest): AlgorithmConfig {
        // If specific algorithm requested, get its configuration
        return if (request.algorithmId != null) {
            getAlgorithmConfigById(request.algorithmId) ?: getDefaultAlgorithmConfig(request.feedType)
        } else {
            getDefaultAlgorithmConfig(request.feedType)
        }
    }

    private suspend fun getAlgorithmConfigById(algorithmId: String): AlgorithmConfig? {
        // This would query algorithm configuration from database
        // For now, return null (fall back to default)
        return null
    }

    private fun createFeedEntry(
        candidate: ContentCandidate,
        score: Double,
        rank: Int,
        algorithmId: String,
        isColdStart: Boolean
    ): FeedEntry {
        val reasons = mutableListOf<FeedReason>()
        
        // Add reasons based on scoring factors
        if (score > 0.7) {
            reasons.add(FeedReason(FeedReasonType.RELEVANCE, "High relevance score", 0.3))
        }
        if (candidate.popularityScore > 0.6) {
            reasons.add(FeedReason(FeedReasonType.POPULARITY, "Popular content", 0.2))
        }
        if (candidate.authorFollowingStatus) {
            reasons.add(FeedReason(FeedReasonType.FOLLOWING, "From followed user", 0.2))
        }
        if (isColdStart) {
            reasons.add(FeedReason(FeedReasonType.COLD_START, "New user recommendation", 0.1))
        }

        val sourceType = when {
            candidate.authorFollowingStatus -> FeedSourceType.FOLLOWING
            candidate.popularityScore > 0.8 -> FeedSourceType.TRENDING
            isColdStart -> FeedSourceType.RECOMMENDATION
            else -> FeedSourceType.RECOMMENDATION
        }

        return FeedEntry(
            id = "${candidate.content.id}_${rank}",
            content = candidate.content,
            score = score,
            rank = rank + 1, // 1-based ranking
            reasons = reasons,
            sourceType = sourceType,
            boosted = score > 0.8,
            algorithmId = algorithmId
        )
    }

    // Content candidate retrieval methods (would be implemented with actual database queries)

    private suspend fun getHomeContentCandidates(userId: String, limit: Int, filters: Map<String, Any>): List<ContentCandidate> {
        // Implementation would query:
        // 1. Content from followed users
        // 2. Recommended content
        // 3. Trending content
        // 4. Mix based on algorithm weights
        return emptyList() // Placeholder
    }

    private suspend fun getFollowingContentCandidates(userId: String, limit: Int, filters: Map<String, Any>): List<ContentCandidate> {
        // Implementation would query only content from users the current user follows
        return emptyList() // Placeholder
    }

    private suspend fun getExploreContentCandidates(userId: String, limit: Int, filters: Map<String, Any>): List<ContentCandidate> {
        // Implementation would query discovery content
        // 1. Content from popular but unfollowed users
        // 2. Content from similar topics but new sources
        // 3. Trending content from different communities
        return emptyList() // Placeholder
    }

    private suspend fun getTrendingContentCandidates(limit: Int, filters: Map<String, Any>): List<ContentCandidate> {
        // Implementation would query trending content based on engagement metrics
        return emptyList() // Placeholder
    }

    private suspend fun getPersonalizedContentCandidates(userId: String, limit: Int, filters: Map<String, Any>): List<ContentCandidate> {
        // Implementation would use ML models for personalized recommendations
        return emptyList() // Placeholder
    }
}