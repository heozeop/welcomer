package com.welcomer.welcome.diversity.service

import com.welcomer.welcome.diversity.model.*
import com.welcomer.welcome.feed.model.FeedEntry
import com.welcomer.welcome.ingestion.model.StoredContent
import com.welcomer.welcome.user.service.UserPreferenceService
import org.springframework.stereotype.Service
import kotlin.math.*

/**
 * Service for applying content quotas and balancing to ensure diverse and engaging feeds
 */
interface ContentBalancingService {
    /**
     * Apply content quotas to ensure balanced mix of content types
     */
    suspend fun applyContentQuotas(
        userId: String,
        rankedItems: List<ScoredContent>,
        targetFeedSize: Int = 20,
        config: ContentBalancingConfig = ContentBalancingConfig()
    ): BalancedFeedResult

    /**
     * Categorize content items for balancing purposes
     */
    suspend fun categorizeItems(
        items: List<ScoredContent>,
        userId: String,
        recentHistory: List<FeedEntry> = emptyList()
    ): CategorizedContent

    /**
     * Ensure minimum source diversity in feed
     */
    suspend fun ensureMinimumSourceDiversity(
        feed: List<ScoredContent>,
        minSources: Int = 3,
        candidatePool: List<ScoredContent> = emptyList()
    ): List<ScoredContent>

    /**
     * Balance content freshness across feed
     */
    suspend fun balanceFreshness(
        items: List<ScoredContent>,
        freshnessScores: Map<String, Double>,
        config: FreshnessBalancingConfig = FreshnessBalancingConfig()
    ): List<ScoredContent>
}

/**
 * Configuration for content balancing
 */
data class ContentBalancingConfig(
    val defaultQuotas: ContentQuotas = ContentQuotas(),
    val enablePersonalization: Boolean = true,
    val minimumSourceDiversity: Int = 3,
    val maximumSameSourceRatio: Double = 0.3,
    val maximumSameTopicRatio: Double = 0.4,
    val enableFreshnessBalancing: Boolean = true,
    val enableDiscoveryBoost: Boolean = true,
    val discoveryBoostMultiplier: Double = 0.2,
    val qualityThreshold: Double = 0.3 // Minimum quality score
)

/**
 * Content quota configuration
 */
data class ContentQuotas(
    val fresh: Double = 0.3,        // 30% fresh/recent content
    val familiar: Double = 0.5,     // 50% familiar/preferred content
    val discovery: Double = 0.2,    // 20% discovery/new content
    val trending: Double = 0.15,    // 15% trending content (can overlap with others)
    val diverse: Double = 0.25      // 25% diverse content (different from user's typical consumption)
) {
    init {
        require(fresh + familiar + discovery <= 1.1) { // Allow slight overlap
            "Content quotas cannot exceed 110% (fresh: $fresh, familiar: $familiar, discovery: $discovery)"
        }
        require(fresh >= 0 && familiar >= 0 && discovery >= 0) {
            "Content quotas must be non-negative"
        }
    }
}

/**
 * Configuration for freshness balancing
 */
data class FreshnessBalancingConfig(
    val freshnessWeight: Double = 0.3,
    val recencyDecayHours: Double = 24.0,
    val minimumFreshRatio: Double = 0.2,
    val maximumStaleRatio: Double = 0.3
)

/**
 * Categorized content for balancing
 */
data class CategorizedContent(
    val fresh: List<ScoredContent>,
    val familiar: List<ScoredContent>,
    val discovery: List<ScoredContent>,
    val trending: List<ScoredContent>,
    val diverse: List<ScoredContent>,
    val lowQuality: List<ScoredContent> = emptyList()
)

/**
 * Result of content balancing operation
 */
data class BalancedFeedResult(
    val balancedFeed: List<ScoredContent>,
    val appliedQuotas: ContentQuotas,
    val actualDistribution: BalancedContentDistribution,
    val qualityMetrics: QualityMetrics,
    val balancingReasons: List<BalancingReason>
)

/**
 * Content distribution metrics for balancing
 */
data class BalancedContentDistribution(
    val freshRatio: Double,
    val familiarRatio: Double,
    val discoveryRatio: Double,
    val trendingRatio: Double,
    val diverseRatio: Double,
    val sourceCount: Int,
    val topicCount: Int,
    val avgQualityScore: Double
)

/**
 * Quality metrics for the balanced feed
 */
data class QualityMetrics(
    val averageScore: Double,
    val scoreVariance: Double,
    val diversityIndex: Double,
    val freshnessIndex: Double,
    val engagementPrediction: Double
)

/**
 * Reason for balancing decision
 */
data class BalancingReason(
    val type: BalancingReasonType,
    val description: String,
    val affectedItems: Int,
    val impact: Double
)

enum class BalancingReasonType {
    QUOTA_ENFORCEMENT,
    SOURCE_DIVERSITY,
    FRESHNESS_BALANCE,
    DISCOVERY_BOOST,
    QUALITY_FILTER
}

/**
 * Default implementation of ContentBalancingService
 */
@Service
class DefaultContentBalancingService(
    private val userPreferenceService: UserPreferenceService,
    private val contentFreshnessService: ContentFreshnessService,
    private val contentDiversityAnalyzer: ContentDiversityAnalyzer
) : ContentBalancingService {

    override suspend fun applyContentQuotas(
        userId: String,
        rankedItems: List<ScoredContent>,
        targetFeedSize: Int,
        config: ContentBalancingConfig
    ): BalancedFeedResult {
        if (rankedItems.isEmpty()) {
            return createEmptyBalancedResult(config.defaultQuotas)
        }

        // Get user preferences for personalized quotas
        val userQuotas = if (config.enablePersonalization) {
            getUserPersonalizedQuotas(userId, config.defaultQuotas)
        } else {
            config.defaultQuotas
        }

        // Filter out low-quality content
        val qualityFiltered = rankedItems.filter { it.score >= config.qualityThreshold }
        if (qualityFiltered.isEmpty()) {
            return createEmptyBalancedResult(userQuotas)
        }

        // Categorize items
        val categorizedItems = categorizeItems(qualityFiltered, userId)

        // Apply quotas
        val balancedItems = applyQuotaBalancing(
            categorizedItems,
            userQuotas,
            targetFeedSize,
            config
        )

        // Ensure source diversity
        val diversifiedItems = ensureMinimumSourceDiversity(
            balancedItems,
            config.minimumSourceDiversity,
            qualityFiltered
        )

        // Calculate metrics and reasons
        val distribution = calculateContentDistribution(diversifiedItems)
        val qualityMetrics = calculateQualityMetrics(diversifiedItems)
        val reasons = generateBalancingReasons(rankedItems, qualityFiltered, diversifiedItems, userQuotas, config.qualityThreshold)

        return BalancedFeedResult(
            balancedFeed = diversifiedItems,
            appliedQuotas = userQuotas,
            actualDistribution = distribution,
            qualityMetrics = qualityMetrics,
            balancingReasons = reasons
        )
    }

    override suspend fun categorizeItems(
        items: List<ScoredContent>,
        userId: String,
        recentHistory: List<FeedEntry>
    ): CategorizedContent {
        // Get freshness scores
        val freshnessScores = items.associate { item ->
            item.content.id to contentFreshnessService.calculateRecencyScore(
                getContentAgeInHours(item.content)
            )
        }

        // Get user engagement patterns for familiarity scoring
        val userPreferences = getUserPreferences(userId)

        val fresh = mutableListOf<ScoredContent>()
        val familiar = mutableListOf<ScoredContent>()
        val discovery = mutableListOf<ScoredContent>()
        val trending = mutableListOf<ScoredContent>()
        val diverse = mutableListOf<ScoredContent>()
        val lowQuality = mutableListOf<ScoredContent>()

        items.forEach { item ->
            val freshnessScore = freshnessScores[item.content.id] ?: 0.0
            val familiarityScore = calculateFamiliarityScore(item, userPreferences, recentHistory)
            val discoveryScore = calculateDiscoveryScore(item, userPreferences, recentHistory)
            val trendingScore = calculateTrendingScore(item)
            val diversityScore = calculateDiversityScore(item, recentHistory)

            // Categorize based on highest scores (items can belong to multiple categories)
            if (freshnessScore > 0.7) fresh.add(item)
            if (familiarityScore > 0.6) familiar.add(item)
            if (discoveryScore > 0.6) discovery.add(item)
            if (trendingScore > 0.7) trending.add(item)
            if (diversityScore > 0.6) diverse.add(item)
            if (item.score < 0.3) lowQuality.add(item)
        }

        return CategorizedContent(
            fresh = fresh,
            familiar = familiar,
            discovery = discovery,
            trending = trending,
            diverse = diverse,
            lowQuality = lowQuality
        )
    }

    override suspend fun ensureMinimumSourceDiversity(
        feed: List<ScoredContent>,
        minSources: Int,
        candidatePool: List<ScoredContent>
    ): List<ScoredContent> {
        val sources = feed.map { it.content.authorId }.toSet()
        
        if (sources.size >= minSources) {
            return feed // Already diverse enough
        }

        val mutableFeed = feed.toMutableList()
        val remainingCandidates = candidatePool.filter { candidate ->
            candidate.content.id !in feed.map { it.content.id }
        }

        // Find candidates from underrepresented sources
        val underrepresentedCandidates = remainingCandidates.filter { candidate ->
            candidate.content.authorId !in sources
        }.sortedByDescending { it.score }

        // Replace lowest scoring items with diverse source items
        val itemsToReplace = minSources - sources.size
        val replacementCandidates = underrepresentedCandidates.take(itemsToReplace)

        if (replacementCandidates.isNotEmpty()) {
            // Sort original feed by score to replace lowest scoring items
            val sortedFeed = mutableFeed.sortedBy { it.score }
            
            replacementCandidates.forEachIndexed { index, replacement ->
                if (index < sortedFeed.size && replacement.score > sortedFeed[index].score * 0.8) {
                    mutableFeed[mutableFeed.indexOf(sortedFeed[index])] = replacement
                }
            }
        }

        return mutableFeed
    }

    override suspend fun balanceFreshness(
        items: List<ScoredContent>,
        freshnessScores: Map<String, Double>,
        config: FreshnessBalancingConfig
    ): List<ScoredContent> {
        val totalItems = items.size
        val targetFreshCount = (totalItems * config.minimumFreshRatio).toInt()
        val maxStaleCount = (totalItems * config.maximumStaleRatio).toInt()

        // Sort items by freshness
        val sortedByFreshness = items.sortedByDescending { 
            freshnessScores[it.content.id] ?: 0.0 
        }

        val fresh = sortedByFreshness.take(targetFreshCount)
        val remaining = sortedByFreshness.drop(targetFreshCount)
        val stale = remaining.takeLast(min(maxStaleCount, remaining.size))
        val middle = remaining.dropLast(stale.size)

        // Combine with freshness-adjusted scores
        return (fresh + middle + stale).map { item ->
            val freshnessScore = freshnessScores[item.content.id] ?: 0.0
            val adjustedScore = item.score * (1.0 + freshnessScore * config.freshnessWeight)
            item.copy(score = adjustedScore)
        }.sortedByDescending { it.score }
    }

    // Private helper methods

    private suspend fun getUserPersonalizedQuotas(
        userId: String, 
        defaultQuotas: ContentQuotas
    ): ContentQuotas {
        return try {
            val preferences = userPreferenceService.getUserPreferences(userId)
            preferences?.contentBalancePreferences ?: defaultQuotas
        } catch (e: Exception) {
            defaultQuotas
        }
    }

    private suspend fun getUserPreferences(userId: String): UserContentPreferences? {
        return try {
            userPreferenceService.getUserPreferences(userId)
        } catch (e: Exception) {
            null
        }
    }

    private fun applyQuotaBalancing(
        categorized: CategorizedContent,
        quotas: ContentQuotas,
        targetSize: Int,
        config: ContentBalancingConfig
    ): List<ScoredContent> {
        val freshCount = (targetSize * quotas.fresh).toInt()
        val familiarCount = (targetSize * quotas.familiar).toInt()
        val discoveryCount = (targetSize * quotas.discovery).toInt()

        // Select top items from each category
        val selectedFresh = selectTopItems(categorized.fresh, freshCount)
        val selectedFamiliar = selectTopItems(categorized.familiar, familiarCount)
        val selectedDiscovery = selectTopItems(categorized.discovery, discoveryCount)

        // Combine and deduplicate
        val combinedItems = (selectedFresh + selectedFamiliar + selectedDiscovery)
            .distinctBy { it.content.id }
            .sortedByDescending { it.score }

        // Fill remaining slots with best available items
        val remainingSlots = targetSize - combinedItems.size
        if (remainingSlots > 0) {
            val allCategorized = (categorized.fresh + categorized.familiar + 
                                categorized.discovery + categorized.trending + categorized.diverse)
                .distinctBy { it.content.id }
                .filter { item -> item.content.id !in combinedItems.map { it.content.id } }
                .sortedByDescending { it.score }
                .take(remainingSlots)
            
            return (combinedItems + allCategorized).sortedByDescending { it.score }
        }

        return combinedItems.take(targetSize)
    }

    private fun selectTopItems(items: List<ScoredContent>, count: Int): List<ScoredContent> {
        return items.sortedByDescending { it.score }.take(count)
    }

    private fun calculateFamiliarityScore(
        item: ScoredContent,
        preferences: UserContentPreferences?,
        recentHistory: List<FeedEntry>
    ): Double {
        preferences ?: return 0.5

        var familiarityScore = 0.0

        // Topic familiarity
        val itemTopics = item.content.tags
        val preferredTopics = preferences.preferredTopics ?: emptyMap()
        val topicFamiliarity = itemTopics.mapNotNull { topic ->
            preferredTopics[topic]
        }.maxOrNull() ?: 0.0
        familiarityScore += topicFamiliarity * 0.4

        // Source familiarity
        val sourcePreference = preferences.preferredSources?.get(item.content.authorId) ?: 0.0
        familiarityScore += sourcePreference * 0.3

        // Historical engagement similarity
        val historicalSimilarity = calculateHistoricalSimilarity(item, recentHistory)
        familiarityScore += historicalSimilarity * 0.3

        return familiarityScore.coerceIn(0.0, 1.0)
    }

    private fun calculateDiscoveryScore(
        item: ScoredContent,
        preferences: UserContentPreferences?,
        recentHistory: List<FeedEntry>
    ): Double {
        // Higher score for content that's different from user's typical consumption
        val familiarityScore = calculateFamiliarityScore(item, preferences, recentHistory)
        val noveltyScore = 1.0 - familiarityScore
        
        // Boost for completely new topics or sources
        val hasNewTopics = item.content.tags.none { topic ->
            preferences?.preferredTopics?.containsKey(topic) == true
        }
        val hasNewSource = preferences?.preferredSources?.containsKey(item.content.authorId) != true
        
        val noveltyBoost = when {
            hasNewTopics && hasNewSource -> 0.3
            hasNewTopics || hasNewSource -> 0.2
            else -> 0.0
        }
        
        return (noveltyScore + noveltyBoost).coerceIn(0.0, 1.0)
    }

    private fun calculateTrendingScore(item: ScoredContent): Double {
        // Simplified trending calculation based on engagement and recency
        val recencyScore = calculateRecencyBonus(item.content.createdAt)
        val engagementScore = item.score // Assuming score includes engagement metrics
        
        return (recencyScore * 0.4 + engagementScore * 0.6).coerceIn(0.0, 1.0)
    }

    private fun calculateDiversityScore(
        item: ScoredContent,
        recentHistory: List<FeedEntry>
    ): Double {
        if (recentHistory.isEmpty()) return 1.0
        
        // Calculate how different this item is from recent history
        val recentTopics = recentHistory.flatMap { it.content.tags }.toSet()
        val recentSources = recentHistory.map { it.content.authorId }.toSet()
        
        val topicDiversity = if (recentTopics.isEmpty()) 1.0 else {
            val commonTopics = item.content.tags.intersect(recentTopics).size
            val totalTopics = item.content.tags.size
            if (totalTopics == 0) 0.5 else 1.0 - (commonTopics.toDouble() / totalTopics)
        }
        
        val sourceDiversity = if (item.content.authorId in recentSources) 0.3 else 1.0
        
        return (topicDiversity * 0.7 + sourceDiversity * 0.3).coerceIn(0.0, 1.0)
    }

    private fun calculateHistoricalSimilarity(
        item: ScoredContent,
        recentHistory: List<FeedEntry>
    ): Double {
        if (recentHistory.isEmpty()) return 0.0
        
        val itemTopics = item.content.tags.toSet()
        val historicalTopics = recentHistory.flatMap { it.content.tags }.toSet()
        
        if (itemTopics.isEmpty() || historicalTopics.isEmpty()) return 0.0
        
        val intersection = itemTopics.intersect(historicalTopics).size
        val union = itemTopics.union(historicalTopics).size
        
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }

    private fun calculateRecencyBonus(createdAt: java.time.Instant): Double {
        val ageInHours = getContentAgeInHours(createdAt)
        return exp(-0.1 * ageInHours) // Exponential decay
    }

    private fun getContentAgeInHours(content: StoredContent): Double {
        return getContentAgeInHours(content.createdAt)
    }

    private fun getContentAgeInHours(createdAt: java.time.Instant): Double {
        val now = java.time.Instant.now()
        return java.time.Duration.between(createdAt, now).toMillis() / (1000.0 * 3600.0)
    }

    private fun calculateContentDistribution(items: List<ScoredContent>): BalancedContentDistribution {
        val totalItems = items.size.toDouble()
        if (totalItems == 0.0) {
            return BalancedContentDistribution(0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0)
        }

        val sources = items.map { it.content.authorId }.toSet()
        val topics = items.flatMap { it.content.tags }.toSet()
        val avgScore = items.map { it.score }.average()

        return BalancedContentDistribution(
            freshRatio = 0.3, // Simplified - would calculate based on actual categorization
            familiarRatio = 0.5,
            discoveryRatio = 0.2,
            trendingRatio = 0.15,
            diverseRatio = 0.25,
            sourceCount = sources.size,
            topicCount = topics.size,
            avgQualityScore = avgScore
        )
    }

    private fun calculateQualityMetrics(items: List<ScoredContent>): QualityMetrics {
        if (items.isEmpty()) {
            return QualityMetrics(0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val scores = items.map { it.score }
        val avgScore = scores.average()
        val variance = scores.map { (it - avgScore).pow(2) }.average()
        
        return QualityMetrics(
            averageScore = avgScore,
            scoreVariance = variance,
            diversityIndex = calculateDiversityIndex(items),
            freshnessIndex = calculateFreshnessIndex(items),
            engagementPrediction = avgScore * 0.8 // Simplified prediction
        )
    }

    private fun calculateDiversityIndex(items: List<ScoredContent>): Double {
        val sources = items.map { it.content.authorId }
        val topics = items.flatMap { it.content.tags }
        
        val sourceEntropy = calculateEntropy(sources)
        val topicEntropy = calculateEntropy(topics)
        
        return (sourceEntropy + topicEntropy) / 2.0
    }

    private fun calculateFreshnessIndex(items: List<ScoredContent>): Double {
        val now = java.time.Instant.now()
        val avgAgeHours = items.map { 
            java.time.Duration.between(it.content.createdAt, now).toHours() 
        }.average()
        
        // Convert age to freshness score (0-1, where 1 is most fresh)
        return exp(-avgAgeHours / 24.0).coerceIn(0.0, 1.0)
    }

    private fun <T> calculateEntropy(items: List<T>): Double {
        if (items.isEmpty()) return 0.0
        
        val counts = items.groupingBy { it }.eachCount()
        val total = items.size.toDouble()
        
        return -counts.values.sumOf { count ->
            val p = count / total
            if (p > 0) p * ln(p) else 0.0
        } / ln(counts.size.toDouble())
    }

    private fun generateBalancingReasons(
        originalItems: List<ScoredContent>,
        filteredItems: List<ScoredContent>,
        finalItems: List<ScoredContent>,
        quotas: ContentQuotas,
        qualityThreshold: Double
    ): List<BalancingReason> {
        val reasons = mutableListOf<BalancingReason>()
        
        // Quality filtering reason
        val filteredOutCount = originalItems.size - filteredItems.size
        if (filteredOutCount > 0) {
            reasons.add(BalancingReason(
                type = BalancingReasonType.QUALITY_FILTER,
                description = "Filtered out $filteredOutCount items below quality threshold ($qualityThreshold)",
                affectedItems = filteredOutCount,
                impact = 0.7
            ))
        }
        
        reasons.add(BalancingReason(
            type = BalancingReasonType.QUOTA_ENFORCEMENT,
            description = "Applied content quotas: ${(quotas.fresh * 100).toInt()}% fresh, ${(quotas.familiar * 100).toInt()}% familiar, ${(quotas.discovery * 100).toInt()}% discovery",
            affectedItems = finalItems.size,
            impact = 0.8
        ))
        
        val uniqueSources = finalItems.map { it.content.authorId }.toSet().size
        if (uniqueSources >= 3) {
            reasons.add(BalancingReason(
                type = BalancingReasonType.SOURCE_DIVERSITY,
                description = "Ensured content from $uniqueSources different sources",
                affectedItems = uniqueSources,
                impact = 0.6
            ))
        }
        
        return reasons
    }

    private fun createEmptyBalancedResult(quotas: ContentQuotas): BalancedFeedResult {
        return BalancedFeedResult(
            balancedFeed = emptyList(),
            appliedQuotas = quotas,
            actualDistribution = BalancedContentDistribution(0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0),
            qualityMetrics = QualityMetrics(0.0, 0.0, 0.0, 0.0, 0.0),
            balancingReasons = emptyList()
        )
    }
}

// Extension data classes for user preferences (would be defined in user preference service)
data class UserContentPreferences(
    val preferredTopics: Map<String, Double>?,
    val preferredSources: Map<String, Double>?,
    val contentBalancePreferences: ContentQuotas?,
    val qualityThreshold: Double = 0.3
)