package com.welcomer.welcome.personalization.service

import com.welcomer.welcome.engagement.model.EngagementType
import com.welcomer.welcome.engagement.service.EngagementTrackingService
import com.welcomer.welcome.ingestion.model.StoredContent
import com.welcomer.welcome.personalization.model.*
import com.welcomer.welcome.user.service.UserContextService
import com.welcomer.welcome.user.service.UserHistoryService
import com.welcomer.welcome.user.service.UserPreferenceService
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.*

/**
 * Service for personalizing feed content based on user preferences, behavior, and context
 */
interface FeedPersonalizationService {
    /**
     * Personalize a list of feed items for a specific user
     */
    suspend fun personalizeItems(
        userId: String,
        feedItems: List<PersonalizableItem>,
        config: PersonalizationConfig = PersonalizationConfig()
    ): PersonalizationResult

    /**
     * Calculate topic relevance between content and user preferences
     */
    suspend fun calculateTopicRelevance(
        item: PersonalizableItem,
        userTopicPreferences: Map<String, Double>
    ): Double

    /**
     * Calculate source affinity based on user history
     */
    suspend fun calculateSourceAffinity(
        item: PersonalizableItem,
        userHistory: List<UserActivity>
    ): Double

    /**
     * Calculate contextual relevance based on user context
     */
    suspend fun calculateContextualRelevance(
        item: PersonalizableItem,
        userContext: UserContext
    ): Double
}

/**
 * Configuration for personalization behavior
 */
data class PersonalizationConfig(
    val topicWeight: Double = 0.5,           // Weight for topic relevance
    val sourceWeight: Double = 0.3,          // Weight for source affinity
    val contextWeight: Double = 0.2,         // Weight for contextual relevance
    val diversityFactor: Double = 0.1,       // Factor to maintain diversity
    val recencyDecayHours: Double = 168.0,   // 1 week decay for content age
    val minPersonalizationScore: Double = 0.1, // Minimum personalization multiplier
    val maxPersonalizationScore: Double = 3.0, // Maximum personalization multiplier
    val enableDiversityControls: Boolean = true,
    val enableRecencyBoost: Boolean = true,
    val maxSameTopicRatio: Double = 0.4,     // Max 40% from same topic
    val maxSameSourceRatio: Double = 0.3,    // Max 30% from same source
    val historyLookbackDays: Int = 30,       // Days of history to consider
    val contextualBoostEnabled: Boolean = true
)

/**
 * Item that can be personalized
 */
data class PersonalizableItem(
    val content: StoredContent,
    val baseScore: Double = 1.0,
    val metadata: Map<String, Any> = emptyMap()
) {
    val id: String get() = content.id
    val authorId: String get() = content.authorId
    val topics: List<String> get() = content.tags
    val createdAt: Instant get() = content.createdAt
}

/**
 * Result of personalization process
 */
data class PersonalizationResult(
    val personalizedItems: List<PersonalizedItem>,
    val personalizationMetrics: PersonalizationMetrics,
    val appliedConfig: PersonalizationConfig,
    val processingStats: PersonalizationStats
)

/**
 * Personalized item with scoring breakdown
 */
data class PersonalizedItem(
    val item: PersonalizableItem,
    val finalScore: Double,
    val personalizationFactors: PersonalizationFactors,
    val rank: Int = 0,
    val explanations: List<String> = emptyList()
)

/**
 * Breakdown of personalization factors
 */
data class PersonalizationFactors(
    val topicRelevance: Double,
    val sourceAffinity: Double,
    val contextualRelevance: Double,
    val diversityAdjustment: Double = 0.0,
    val recencyBoost: Double = 0.0,
    val personalizedMultiplier: Double = 1.0
)

/**
 * Metrics about the personalization process
 */
data class PersonalizationMetrics(
    val averagePersonalizationScore: Double,
    val topicCoverage: Int,            // Number of unique topics
    val sourceDiversity: Int,          // Number of unique sources
    val temporalSpread: Double,        // Spread of content across time
    val personalizedVsBaseScoreImprovement: Double
)

/**
 * Statistics about personalization processing
 */
data class PersonalizationStats(
    val totalProcessingTimeMs: Long,
    val itemsProcessed: Int,
    val preferencesUsed: Int,
    val contextFactorsApplied: Int,
    val diversityAdjustmentsMade: Int
)

/**
 * User activity record for affinity calculation
 */
data class UserActivity(
    val contentId: String,
    val authorId: String,
    val topics: List<String>,
    val engagementType: EngagementType,
    val engagementScore: Double,
    val timestamp: Instant,
    val sessionContext: Map<String, Any> = emptyMap()
)

/**
 * User context for contextual relevance
 */
data class UserContext(
    val timeOfDay: Int,              // Hour of day (0-23)
    val dayOfWeek: Int,              // Day of week (1-7, Monday=1)
    val deviceType: DeviceType,
    val location: UserLocation? = null,
    val sessionDuration: Long = 0,   // Current session duration in minutes
    val previousActivity: List<String> = emptyList(), // Recent content IDs
    val contextualPreferences: Map<String, Double> = emptyMap()
)

enum class DeviceType {
    MOBILE, TABLET, DESKTOP, TV, UNKNOWN
}

data class UserLocation(
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val timezone: String? = null
)

/**
 * Default implementation of FeedPersonalizationService
 */
@Service
class DefaultFeedPersonalizationService(
    private val userPreferenceService: UserPreferenceService,
    private val userContextService: UserContextService? = null,
    private val userHistoryService: UserHistoryService? = null,
    private val engagementService: EngagementTrackingService? = null,
    private val topicRelevanceService: TopicRelevanceService? = null,
    private val sourceAffinityService: SourceAffinityService? = null,
    private val contextualRelevanceService: ContextualRelevanceService? = null
) : FeedPersonalizationService {

    override suspend fun personalizeItems(
        userId: String,
        feedItems: List<PersonalizableItem>,
        config: PersonalizationConfig
    ): PersonalizationResult {
        val startTime = System.currentTimeMillis()
        
        if (feedItems.isEmpty()) {
            return createEmptyResult(config, System.currentTimeMillis() - startTime)
        }

        try {
            // Step 1: Gather user data
            val userPreferences = fetchUserPreferences(userId)
            val userContext = fetchUserContext(userId)
            val userHistory = fetchUserHistory(userId, config.historyLookbackDays)
            
            // Step 2: Calculate personalization factors for each item
            val personalizedItems = feedItems.map { item ->
                personalizeItem(item, userPreferences, userContext, userHistory, config)
            }
            
            // Step 3: Apply diversity controls if enabled
            val finalItems = if (config.enableDiversityControls) {
                applyDiversityControls(personalizedItems, config)
            } else {
                personalizedItems
            }
            
            // Step 4: Rank and add explanations
            val rankedItems = finalItems
                .sortedByDescending { it.finalScore }
                .mapIndexed { index, item ->
                    item.copy(
                        rank = index + 1,
                        explanations = generatePersonalizationExplanations(item, userPreferences)
                    )
                }
            
            // Step 5: Calculate metrics
            val metrics = calculatePersonalizationMetrics(rankedItems, feedItems)
            val processingTime = System.currentTimeMillis() - startTime
            val stats = PersonalizationStats(
                totalProcessingTimeMs = processingTime,
                itemsProcessed = feedItems.size,
                preferencesUsed = userPreferences?.topicInterests?.size ?: 0,
                contextFactorsApplied = countContextFactors(userContext),
                diversityAdjustmentsMade = rankedItems.count { it.personalizationFactors.diversityAdjustment != 0.0 }
            )
            
            return PersonalizationResult(
                personalizedItems = rankedItems,
                personalizationMetrics = metrics,
                appliedConfig = config,
                processingStats = stats
            )
            
        } catch (e: Exception) {
            // Graceful degradation - return items with original scores
            val fallbackItems = feedItems.mapIndexed { index, item ->
                PersonalizedItem(
                    item = item,
                    finalScore = item.baseScore,
                    personalizationFactors = PersonalizationFactors(0.5, 0.5, 0.5),
                    rank = index + 1,
                    explanations = listOf("Personalization unavailable, using base score")
                )
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            val stats = PersonalizationStats(processingTime, feedItems.size, 0, 0, 0)
            
            return PersonalizationResult(
                personalizedItems = fallbackItems,
                personalizationMetrics = calculatePersonalizationMetrics(fallbackItems, feedItems),
                appliedConfig = config,
                processingStats = stats
            )
        }
    }

    override suspend fun calculateTopicRelevance(
        item: PersonalizableItem,
        userTopicPreferences: Map<String, Double>
    ): Double {
        // Use enhanced topic relevance service if available
        return if (topicRelevanceService != null) {
            val result = topicRelevanceService.calculateEnhancedTopicRelevance(
                itemTopics = item.topics,
                userPreferences = userTopicPreferences
            )
            result.overallRelevance
        } else {
            // Fallback to basic implementation
            calculateBasicTopicRelevance(item, userTopicPreferences)
        }
    }

    /**
     * Basic topic relevance calculation (fallback)
     */
    private fun calculateBasicTopicRelevance(
        item: PersonalizableItem,
        userTopicPreferences: Map<String, Double>
    ): Double {
        if (userTopicPreferences.isEmpty() || item.topics.isEmpty()) {
            return 0.5 // Neutral relevance
        }
        
        val itemTopics = item.topics.toSet()
        
        // Calculate relevance scores for each item topic
        val topicRelevanceScores = itemTopics.map { itemTopic ->
            // Exact match gets full score
            val exactMatch = userTopicPreferences[itemTopic] ?: 0.0
            
            // Check for related topics (simple prefix/substring matching)
            val relatedMatches = userTopicPreferences.entries.filter { (userTopic, _) ->
                itemTopic.contains(userTopic, ignoreCase = true) || 
                userTopic.contains(itemTopic, ignoreCase = true)
            }
            
            val relatedScore = relatedMatches.maxOfOrNull { it.value * 0.7 } ?: 0.0
            
            max(exactMatch, relatedScore)
        }
        
        // Return the average of topic relevance scores
        return topicRelevanceScores.average().coerceIn(0.0, 1.0)
    }

    override suspend fun calculateSourceAffinity(
        item: PersonalizableItem,
        userHistory: List<UserActivity>
    ): Double {
        // Use enhanced source affinity service if available
        return if (sourceAffinityService != null) {
            val result = sourceAffinityService.calculateAdvancedSourceAffinity(
                authorId = item.authorId,
                userHistory = userHistory
            )
            result.overallAffinity
        } else {
            // Fallback to basic implementation
            calculateBasicSourceAffinity(item, userHistory)
        }
    }

    /**
     * Basic source affinity calculation (fallback)
     */
    private fun calculateBasicSourceAffinity(
        item: PersonalizableItem,
        userHistory: List<UserActivity>
    ): Double {
        if (userHistory.isEmpty()) {
            return 0.5 // Neutral affinity for new users
        }
        
        // Calculate source interaction patterns
        val sourceInteractions = userHistory
            .filter { it.authorId == item.authorId }
            .groupBy { it.engagementType }
        
        if (sourceInteractions.isEmpty()) {
            return 0.3 // Low affinity for unseen sources
        }
        
        // Weight different engagement types
        val engagementWeights = mapOf(
            EngagementType.LIKE to 1.0,
            EngagementType.SHARE to 1.5,
            EngagementType.COMMENT to 1.2,
            EngagementType.BOOKMARK to 1.3,
            EngagementType.VIEW to 0.3,
            EngagementType.CLICK to 0.5,
            EngagementType.HIDE to -0.5,
            EngagementType.REPORT to -1.0
        )
        
        val affinityScore = sourceInteractions.entries.sumOf { (engagementType, interactions) ->
            val weight = engagementWeights[engagementType] ?: 0.1
            val count = interactions.size
            val avgEngagementScore = interactions.map { it.engagementScore }.average()
            weight * count * avgEngagementScore
        }
        
        // Normalize based on total interactions with this source
        val totalInteractions = sourceInteractions.values.sumOf { it.size }
        val normalizedScore = affinityScore / max(totalInteractions, 1)
        
        // Apply sigmoid function to keep score in reasonable range
        return (1.0 / (1.0 + exp(-normalizedScore))).coerceIn(0.0, 1.0)
    }

    override suspend fun calculateContextualRelevance(
        item: PersonalizableItem,
        userContext: UserContext
    ): Double {
        // Use enhanced contextual relevance service if available
        return if (contextualRelevanceService != null) {
            val result = contextualRelevanceService.calculateAdvancedContextualRelevance(
                item = item,
                userContext = userContext
            )
            result.overallRelevance
        } else {
            // Fallback to basic implementation
            calculateBasicContextualRelevance(item, userContext)
        }
    }

    /**
     * Basic contextual relevance calculation (fallback)
     */
    private suspend fun calculateBasicContextualRelevance(
        item: PersonalizableItem,
        userContext: UserContext
    ): Double {
        var contextualScore = 0.5 // Base contextual score
        
        // Time of day relevance
        val timeRelevance = calculateTimeRelevance(item, userContext.timeOfDay)
        contextualScore = (contextualScore + timeRelevance) / 2
        
        // Device type relevance
        val deviceRelevance = calculateDeviceRelevance(item, userContext.deviceType)
        contextualScore = (contextualScore + deviceRelevance) / 2
        
        // Session context relevance
        val sessionRelevance = calculateSessionRelevance(item, userContext)
        contextualScore = (contextualScore + sessionRelevance) / 2
        
        return contextualScore.coerceIn(0.0, 1.0)
    }

    // Private helper methods

    private suspend fun fetchUserPreferences(userId: String): UserPreferenceProfile? {
        return try {
            val userModelProfile = userPreferenceService.getPreferences(userId)
            userModelProfile?.let { profile ->
                // Convert from user.model.UserPreferenceProfile to personalization.model.UserPreferenceProfile
                UserPreferenceProfile(
                    userId = profile.userId,
                    topicInterests = profile.topicInterests,
                    sourcePreferences = profile.followedAccounts.associateWith { 0.8 },
                    contentTypePreferences = profile.contentTypePreferences,
                    timeBasedPreferences = emptyMap(),
                    languagePreferences = profile.languagePreferences,
                    diversityPreference = 0.5,
                    freshnessPreference = 0.5,
                    engagementHistory = emptyMap(),
                    lastUpdated = profile.lastUpdated
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchUserContext(userId: String): UserContext {
        return try {
            userContextService?.getUserContext(userId) ?: createDefaultContext()
        } catch (e: Exception) {
            createDefaultContext()
        }
    }

    private suspend fun fetchUserHistory(userId: String, lookbackDays: Int): List<UserActivity> {
        return try {
            userHistoryService?.getUserHistory(userId, lookbackDays) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun personalizeItem(
        item: PersonalizableItem,
        userPreferences: UserPreferenceProfile?,
        userContext: UserContext,
        userHistory: List<UserActivity>,
        config: PersonalizationConfig
    ): PersonalizedItem {
        // Calculate individual personalization factors using enhanced services
        val topicRelevance = if (userPreferences != null) {
            calculateTopicRelevance(item, userPreferences.topicInterests)
        } else {
            0.5
        }
        
        val sourceAffinity = calculateSourceAffinity(item, userHistory)
        val contextualRelevance = calculateContextualRelevance(item, userContext)
        
        // Calculate additional factors
        val recencyBoost = if (config.enableRecencyBoost) {
            calculateRecencyBoost(item.createdAt, config.recencyDecayHours)
        } else {
            0.0
        }
        
        // Enhanced scoring algorithm with non-linear combination
        val basePersonalizationScore = calculateWeightedPersonalizationScore(
            topicRelevance = topicRelevance,
            sourceAffinity = sourceAffinity,
            contextualRelevance = contextualRelevance,
            config = config
        )
        
        // Apply quality amplification - higher quality content gets more benefit from personalization
        val qualityAmplification = calculateQualityAmplification(item.baseScore, basePersonalizationScore)
        
        // Apply contextual boost based on enhanced services
        val contextualBoost = if (config.contextualBoostEnabled) {
            calculateAdvancedContextualBoost(topicRelevance, sourceAffinity, contextualRelevance)
        } else {
            0.0
        }
        
        // Calculate final personalization multiplier
        val personalizationMultiplier = (basePersonalizationScore * qualityAmplification + contextualBoost)
            .coerceIn(config.minPersonalizationScore, config.maxPersonalizationScore)
        
        // Apply recency boost multiplicatively for more natural effect
        val finalMultiplier = personalizationMultiplier * (1.0 + recencyBoost)
        val finalScore = item.baseScore * finalMultiplier
        
        val factors = PersonalizationFactors(
            topicRelevance = topicRelevance,
            sourceAffinity = sourceAffinity,
            contextualRelevance = contextualRelevance,
            recencyBoost = recencyBoost,
            personalizedMultiplier = finalMultiplier
        )
        
        return PersonalizedItem(
            item = item,
            finalScore = finalScore,
            personalizationFactors = factors
        )
    }

    private fun applyDiversityControls(
        items: List<PersonalizedItem>,
        config: PersonalizationConfig
    ): List<PersonalizedItem> {
        // Sort by current score
        val sortedItems = items.sortedByDescending { it.finalScore }
        val totalItems = sortedItems.size
        
        // Track diversity metrics
        val topicCounts = mutableMapOf<String, Int>()
        val sourceCounts = mutableMapOf<String, Int>()
        
        return sortedItems.map { item ->
            // Calculate current ratios
            val topicRatios = item.item.topics.associateWith { topic ->
                topicCounts[topic]?.toDouble()?.div(totalItems) ?: 0.0
            }
            val sourceRatio = sourceCounts[item.item.authorId]?.toDouble()?.div(totalItems) ?: 0.0
            
            // Apply diversity penalty if needed
            var diversityAdjustment = 0.0
            
            // Topic diversity check
            val maxTopicRatio = topicRatios.maxOfOrNull { it.value } ?: 0.0
            if (maxTopicRatio > config.maxSameTopicRatio) {
                diversityAdjustment -= config.diversityFactor
            }
            
            // Source diversity check
            if (sourceRatio > config.maxSameSourceRatio) {
                diversityAdjustment -= config.diversityFactor
            }
            
            // Update counts
            item.item.topics.forEach { topic ->
                topicCounts[topic] = (topicCounts[topic] ?: 0) + 1
            }
            sourceCounts[item.item.authorId] = (sourceCounts[item.item.authorId] ?: 0) + 1
            
            // Apply diversity adjustment
            val adjustedScore = item.finalScore * (1.0 + diversityAdjustment)
            
            item.copy(
                finalScore = adjustedScore,
                personalizationFactors = item.personalizationFactors.copy(
                    diversityAdjustment = diversityAdjustment
                )
            )
        }
    }

    private fun calculateRecencyBoost(createdAt: Instant, decayHours: Double): Double {
        val now = Instant.now()
        val ageInHours = ChronoUnit.HOURS.between(createdAt, now).toDouble()
        
        // Exponential decay with configurable half-life
        return exp(-ageInHours / decayHours) * 0.2 // Max 20% boost for very recent content
    }

    private fun calculateTimeRelevance(item: PersonalizableItem, timeOfDay: Int): Double {
        // Simple time-based relevance (could be enhanced with ML models)
        return when (timeOfDay) {
            in 6..9 -> if (item.topics.any { it.contains("news", true) }) 0.8 else 0.5  // Morning news boost
            in 12..14 -> if (item.topics.any { it.contains("lifestyle", true) }) 0.7 else 0.5  // Lunch break content
            in 18..22 -> if (item.topics.any { it.contains("entertainment", true) }) 0.8 else 0.5  // Evening entertainment
            else -> 0.5  // Neutral for other times
        }
    }

    private fun calculateDeviceRelevance(item: PersonalizableItem, deviceType: DeviceType): Double {
        // Content type relevance based on device
        return when (deviceType) {
            DeviceType.MOBILE -> if (item.content.contentType.name == "TEXT") 0.7 else 0.5
            DeviceType.TABLET -> 0.6  // Neutral for tablets
            DeviceType.DESKTOP -> if (item.topics.any { it.contains("productivity", true) }) 0.8 else 0.5
            DeviceType.TV -> if (item.topics.any { it.contains("video", true) }) 0.9 else 0.3
            else -> 0.5
        }
    }

    private fun calculateSessionRelevance(item: PersonalizableItem, userContext: UserContext): Double {
        // Session-based relevance
        var relevance = 0.5
        
        // If user has been active for a while, prefer engaging content
        if (userContext.sessionDuration > 30) {
            relevance += 0.1
        }
        
        // Avoid showing recently seen content
        if (item.content.id in userContext.previousActivity) {
            relevance -= 0.3
        }
        
        return relevance.coerceIn(0.0, 1.0)
    }

    private fun generatePersonalizationExplanations(
        item: PersonalizedItem,
        userPreferences: UserPreferenceProfile?
    ): List<String> {
        val explanations = mutableListOf<String>()
        val factors = item.personalizationFactors
        
        // Topic relevance explanations
        when {
            factors.topicRelevance > 0.8 -> 
                explanations.add("Strongly matches your interests in ${item.item.topics.take(2).joinToString(", ")}")
            factors.topicRelevance > 0.6 -> 
                explanations.add("Aligns with your interests in ${item.item.topics.take(2).joinToString(", ")}")
            factors.topicRelevance < 0.3 -> 
                explanations.add("Exploring new topics outside your usual interests")
        }
        
        // Source affinity explanations
        when {
            factors.sourceAffinity > 0.8 -> 
                explanations.add("From ${item.item.authorId}, a highly trusted source for you")
            factors.sourceAffinity > 0.6 -> 
                explanations.add("From ${item.item.authorId}, a source you engage with positively")
            factors.sourceAffinity < 0.3 -> 
                explanations.add("From a source you haven't interacted with much")
        }
        
        // Contextual relevance explanations
        when {
            factors.contextualRelevance > 0.7 -> 
                explanations.add("Perfect timing for your current context and device")
            factors.contextualRelevance > 0.5 -> 
                explanations.add("Good match for your current time and device")
        }
        
        // Special scoring explanations
        if (factors.recencyBoost > 0.15) {
            explanations.add("Fresh content published recently")
        }
        
        if (factors.personalizedMultiplier > 2.0) {
            explanations.add("Highly personalized recommendation - all factors align well")
        } else if (factors.personalizedMultiplier > 1.5) {
            explanations.add("Well-tailored to your preferences")
        }
        
        // Diversity explanations
        when {
            factors.diversityAdjustment > 0.05 -> 
                explanations.add("Boosted to add variety to your feed")
            factors.diversityAdjustment < -0.05 -> 
                explanations.add("Score balanced to maintain feed diversity")
        }
        
        // Quality amplification explanation
        if (item.item.baseScore > 2.0 && factors.personalizedMultiplier > 1.3) {
            explanations.add("High-quality content that matches your interests well")
        }
        
        return explanations.ifEmpty { 
            listOf("Standard personalization applied based on available data") 
        }
    }

    private fun calculatePersonalizationMetrics(
        personalizedItems: List<PersonalizedItem>,
        originalItems: List<PersonalizableItem>
    ): PersonalizationMetrics {
        if (personalizedItems.isEmpty()) {
            return PersonalizationMetrics(0.0, 0, 0, 0.0, 0.0)
        }
        
        // Enhanced personalization scoring metrics
        val personalizationMultipliers = personalizedItems.map { it.personalizationFactors.personalizedMultiplier }
        val avgPersonalizationScore = personalizationMultipliers.average()
        
        // Content diversity metrics
        val topicCoverage = personalizedItems.flatMap { it.item.topics }.toSet().size
        val sourceDiversity = personalizedItems.map { it.item.authorId }.toSet().size
        
        // Temporal distribution analysis
        val timestamps = personalizedItems.map { it.item.createdAt }
        val temporalSpread = if (timestamps.isNotEmpty()) {
            val earliest = timestamps.minOrNull()!!
            val latest = timestamps.maxOrNull()!!
            ChronoUnit.HOURS.between(earliest, latest).toDouble()
        } else 0.0
        
        // Score improvement calculation with quality factors
        val personalizedAvgScore = personalizedItems.map { it.finalScore }.average()
        val baseAvgScore = originalItems.map { it.baseScore }.average()
        val improvement = if (baseAvgScore > 0) {
            (personalizedAvgScore - baseAvgScore) / baseAvgScore
        } else 0.0
        
        return PersonalizationMetrics(
            averagePersonalizationScore = avgPersonalizationScore,
            topicCoverage = topicCoverage,
            sourceDiversity = sourceDiversity,
            temporalSpread = temporalSpread,
            personalizedVsBaseScoreImprovement = improvement
        )
    }

    private fun countContextFactors(userContext: UserContext): Int {
        var count = 0
        if (userContext.timeOfDay in 0..23) count++
        if (userContext.deviceType != DeviceType.UNKNOWN) count++
        if (userContext.location != null) count++
        if (userContext.sessionDuration > 0) count++
        if (userContext.previousActivity.isNotEmpty()) count++
        return count
    }

    private fun createDefaultContext(): UserContext {
        val now = Instant.now()
        return UserContext(
            timeOfDay = now.atZone(java.time.ZoneId.systemDefault()).hour,
            dayOfWeek = now.atZone(java.time.ZoneId.systemDefault()).dayOfWeek.value,
            deviceType = DeviceType.UNKNOWN
        )
    }

    private fun calculateWeightedPersonalizationScore(
        topicRelevance: Double,
        sourceAffinity: Double,
        contextualRelevance: Double,
        config: PersonalizationConfig
    ): Double {
        // Enhanced weighting with non-linear combination
        val weightedSum = (topicRelevance * config.topicWeight) +
                         (sourceAffinity * config.sourceWeight) +
                         (contextualRelevance * config.contextWeight)
        
        // Apply harmonic mean for more balanced scoring when factors vary significantly
        val harmonicMean = 3.0 / (1.0/maxOf(topicRelevance, 0.01) + 
                                  1.0/maxOf(sourceAffinity, 0.01) + 
                                  1.0/maxOf(contextualRelevance, 0.01))
        
        // Combine weighted sum with harmonic mean for balanced results
        return (weightedSum * 0.7 + harmonicMean * 0.3).coerceIn(0.0, 1.0)
    }
    
    private fun calculateQualityAmplification(baseScore: Double, personalizationScore: Double): Double {
        // Higher quality content benefits more from personalization
        // Lower quality content gets less amplification even with high personalization
        val qualityFactor = (baseScore / 3.0).coerceIn(0.3, 1.2) // Normalize assuming max base score of 3
        val personalizationFactor = (personalizationScore + 0.5) // Ensure minimum benefit
        
        return qualityFactor * personalizationFactor
    }
    
    private fun calculateAdvancedContextualBoost(
        topicRelevance: Double,
        sourceAffinity: Double,
        contextualRelevance: Double
    ): Double {
        // Give extra boost when all factors are high (user is in their "sweet spot")
        val allFactorsHigh = topicRelevance > 0.7 && sourceAffinity > 0.7 && contextualRelevance > 0.6
        val sweetSpotBonus = if (allFactorsHigh) 0.2 else 0.0
        
        // Give boost when contextual relevance is very high regardless of other factors
        val contextualBonus = if (contextualRelevance > 0.8) 0.1 else 0.0
        
        // Penalize when source affinity is very low (user dislikes this source)
        val sourcePenalty = if (sourceAffinity < 0.3) -0.15 else 0.0
        
        return sweetSpotBonus + contextualBonus + sourcePenalty
    }

    private fun createEmptyResult(config: PersonalizationConfig, processingTime: Long): PersonalizationResult {
        return PersonalizationResult(
            personalizedItems = emptyList(),
            personalizationMetrics = PersonalizationMetrics(0.0, 0, 0, 0.0, 0.0),
            appliedConfig = config,
            processingStats = PersonalizationStats(processingTime, 0, 0, 0, 0)
        )
    }
}