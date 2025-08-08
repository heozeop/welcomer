package com.welcomer.welcome.diversity.service

import com.welcomer.welcome.diversity.model.*
import com.welcomer.welcome.feed.model.FeedEntry
import com.welcomer.welcome.feed.repository.FeedHistoryRepository
import com.welcomer.welcome.ingestion.model.StoredContent
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.math.*

/**
 * Main integration service that orchestrates all diversity and freshness components
 * to enhance feed generation with intelligent content diversification
 */
interface ContentDiversityIntegrationService {
    /**
     * Main method to diversify a feed using all available diversity and freshness components
     */
    suspend fun diversifyFeed(
        userId: String,
        candidateItems: List<ScoredContent>,
        feedSize: Int = 20,
        config: DiversityIntegrationConfig = DiversityIntegrationConfig()
    ): DiversifiedFeedResult

    /**
     * Calculate comprehensive diversity and freshness scores for content items
     */
    suspend fun calculateEnhancedScores(
        userId: String,
        candidateItems: List<ScoredContent>,
        recentFeedHistory: List<FeedEntry>
    ): List<EnhancedScoredContent>

    /**
     * Monitor and log diversity metrics for a completed feed
     */
    suspend fun logDiversityMetrics(userId: String, finalFeed: List<EnhancedScoredContent>)
}

/**
 * Configuration for the diversity integration system
 */
data class DiversityIntegrationConfig(
    val enableDiversityBoosts: Boolean = true,
    val enableFreshnessBoosts: Boolean = true,
    val enableEchoChamberPrevention: Boolean = true,
    val enableContentBalancing: Boolean = true,
    val diversityBoostMultiplier: Double = 0.2,  // Up to 20% boost for diverse content
    val freshnessBoostMultiplier: Double = 0.3,  // Up to 30% boost for fresh content
    val maxHistorySize: Int = 100,
    val enableMetricsLogging: Boolean = true,
    val performanceOptimizations: Boolean = true
)

/**
 * Enhanced scored content with diversity and freshness information
 */
data class EnhancedScoredContent(
    val content: StoredContent,
    val originalScore: Double,
    val finalScore: Double,
    val diversityScore: Double = 0.0,
    val freshnessScore: Double = 0.0,
    val diversityBoost: Double = 0.0,
    val freshnessBoost: Double = 0.0,
    val echoChamberAdjustments: Map<String, Double> = emptyMap(),
    val balancingAdjustments: Map<String, Double> = emptyMap(),
    val scoringBreakdown: ScoringBreakdown = ScoringBreakdown(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Detailed breakdown of how the final score was calculated
 */
data class ScoringBreakdown(
    val baseScore: Double = 0.0,
    val diversityContribution: Double = 0.0,
    val freshnessContribution: Double = 0.0,
    val echoChamberContribution: Double = 0.0,
    val balancingContribution: Double = 0.0,
    val qualityAdjustment: Double = 0.0,
    val finalMultiplier: Double = 1.0
)

/**
 * Result of the complete feed diversification process
 */
data class DiversifiedFeedResult(
    val diversifiedFeed: List<EnhancedScoredContent>,
    val diversityMetrics: FeedDiversityMetrics,
    val processingStats: ProcessingStats,
    val recommendations: List<SystemRecommendation> = emptyList()
)

/**
 * Comprehensive metrics about the diversity of the final feed
 */
data class FeedDiversityMetrics(
    val overallDiversityScore: Double,
    val freshnessIndex: Double,
    val sourceDistribution: Map<String, Double>,
    val topicDistribution: Map<String, Double>,
    val contentTypeDistribution: Map<String, Double>,
    val echoChamberRiskLevel: EchoChamberRiskLevel,
    val balanceMetrics: BalancedContentDistribution,
    val qualityMetrics: QualityMetrics
)

/**
 * Performance and processing statistics
 */
data class ProcessingStats(
    val totalProcessingTimeMs: Long,
    val itemsProcessed: Int,
    val itemsFiltered: Int,
    val diversityAnalysisTimeMs: Long,
    val freshnessAnalysisTimeMs: Long,
    val echoChamberAnalysisTimeMs: Long,
    val balancingTimeMs: Long,
    val cacheHits: Int = 0
)

/**
 * System-level recommendation for improving diversity
 */
data class SystemRecommendation(
    val type: SystemRecommendationType,
    val priority: RecommendationPriority,
    val description: String,
    val expectedImpact: Double,
    val actionRequired: String
)

enum class SystemRecommendationType {
    INCREASE_CONTENT_POOL,
    DIVERSIFY_CONTENT_SOURCES,
    ADJUST_FRESHNESS_WEIGHTS,
    IMPROVE_ECHO_CHAMBER_DETECTION,
    OPTIMIZE_BALANCING_QUOTAS,
    ENHANCE_USER_PREFERENCES
}

/**
 * Default implementation that orchestrates all diversity components
 */
@Service
class DefaultContentDiversityIntegrationService(
    private val contentDiversityAnalyzer: ContentDiversityAnalyzer,
    private val contentFreshnessService: ContentFreshnessService,
    private val echoChamberPreventionService: EchoChamberPreventionService,
    private val contentBalancingService: ContentBalancingService,
    private val feedHistoryRepository: FeedHistoryRepository
) : ContentDiversityIntegrationService {

    override suspend fun diversifyFeed(
        userId: String,
        candidateItems: List<ScoredContent>,
        feedSize: Int,
        config: DiversityIntegrationConfig
    ): DiversifiedFeedResult {
        val startTime = System.currentTimeMillis()
        val stats = ProcessingStats(0, candidateItems.size, 0, 0, 0, 0, 0)

        if (candidateItems.isEmpty()) {
            return createEmptyResult(stats)
        }

        try {
            // Step 1: Get user's recent feed history
            val recentFeedHistory = feedHistoryRepository.getRecentFeedHistory(userId, config.maxHistorySize)
            
            // Step 2: Calculate enhanced scores with all diversity components
            val enhancedItems = calculateEnhancedScores(userId, candidateItems, recentFeedHistory)
            
            // Step 3: Apply echo chamber prevention if enabled
            val echoChamberAdjustedItems = if (config.enableEchoChamberPrevention) {
                val echoChamberStartTime = System.currentTimeMillis()
                val adjustedItems = applyEchoChamberPrevention(userId, enhancedItems, recentFeedHistory, config)
                val echoChamberTime = System.currentTimeMillis() - echoChamberStartTime
                stats.copy(echoChamberAnalysisTimeMs = echoChamberTime)
                adjustedItems
            } else {
                enhancedItems
            }
            
            // Step 4: Apply content balancing if enabled
            val balancedItems = if (config.enableContentBalancing) {
                val balancingStartTime = System.currentTimeMillis()
                val balanced = applyContentBalancing(userId, echoChamberAdjustedItems, feedSize, config)
                val balancingTime = System.currentTimeMillis() - balancingStartTime
                stats.copy(balancingTimeMs = balancingTime)
                balanced
            } else {
                echoChamberAdjustedItems.take(feedSize)
            }
            
            // Step 5: Final ranking and selection
            val finalFeed = balancedItems
                .sortedByDescending { it.finalScore }
                .take(feedSize)
            
            // Step 6: Calculate comprehensive metrics
            val diversityMetrics = calculateFeedDiversityMetrics(finalFeed, recentFeedHistory)
            
            // Step 7: Generate system recommendations
            val recommendations = generateSystemRecommendations(diversityMetrics, finalFeed)
            
            // Step 8: Log metrics if enabled
            if (config.enableMetricsLogging) {
                logDiversityMetrics(userId, finalFeed)
            }
            
            val totalTime = System.currentTimeMillis() - startTime
            val finalStats = stats.copy(totalProcessingTimeMs = totalTime)
            
            return DiversifiedFeedResult(
                diversifiedFeed = finalFeed,
                diversityMetrics = diversityMetrics,
                processingStats = finalStats,
                recommendations = recommendations
            )
            
        } catch (e: Exception) {
            // Handle errors gracefully - return original items with minimal processing
            val fallbackItems = candidateItems.take(feedSize).map { item ->
                EnhancedScoredContent(
                    content = item.content,
                    originalScore = item.score,
                    finalScore = item.score,
                    metadata = mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
            
            val errorStats = stats.copy(
                totalProcessingTimeMs = System.currentTimeMillis() - startTime
            )
            
            return DiversifiedFeedResult(
                diversifiedFeed = fallbackItems,
                diversityMetrics = createDefaultMetrics(),
                processingStats = errorStats,
                recommendations = listOf(
                    SystemRecommendation(
                        type = SystemRecommendationType.IMPROVE_ECHO_CHAMBER_DETECTION,
                        priority = RecommendationPriority.HIGH,
                        description = "Error occurred during diversification: ${e.message ?: "Unknown error"}",
                        expectedImpact = 0.0,
                        actionRequired = "Investigation and bug fix required"
                    )
                )
            )
        }
    }

    override suspend fun calculateEnhancedScores(
        userId: String,
        candidateItems: List<ScoredContent>,
        recentFeedHistory: List<FeedEntry>
    ): List<EnhancedScoredContent> {
        val diversityStartTime = System.currentTimeMillis()
        
        // Calculate diversity scores
        val diversityAnalyses = contentDiversityAnalyzer.calculateDiversityScores(
            candidateItems.map { it.content },
            recentFeedHistory
        )
        
        val diversityTime = System.currentTimeMillis() - diversityStartTime
        val freshnessStartTime = System.currentTimeMillis()
        
        // Calculate freshness scores
        val freshnessScores = candidateItems.associate { item ->
            item.content.id to contentFreshnessService.calculateRecencyScore(
                getContentAgeInHours(item.content.createdAt)
            )
        }
        
        val freshnessTime = System.currentTimeMillis() - freshnessStartTime
        
        // Combine all scores into enhanced items
        return candidateItems.mapIndexed { index, item ->
            val diversityAnalysis = diversityAnalyses.getOrNull(index)
            val diversityScore = diversityAnalysis?.overallDiversityScore ?: 0.5
            val freshnessScore = freshnessScores[item.content.id] ?: 0.5
            
            val diversityBoost = calculateDiversityBoost(diversityScore)
            val freshnessBoost = calculateFreshnessBoost(freshnessScore)
            
            val finalScore = item.score * diversityBoost * freshnessBoost
            
            val scoringBreakdown = ScoringBreakdown(
                baseScore = item.score,
                diversityContribution = (diversityBoost - 1.0) * item.score,
                freshnessContribution = (freshnessBoost - 1.0) * item.score,
                finalMultiplier = diversityBoost * freshnessBoost
            )
            
            EnhancedScoredContent(
                content = item.content,
                originalScore = item.score,
                finalScore = finalScore,
                diversityScore = diversityScore,
                freshnessScore = freshnessScore,
                diversityBoost = diversityBoost,
                freshnessBoost = freshnessBoost,
                scoringBreakdown = scoringBreakdown,
                metadata = mapOf(
                    "diversityAnalysisTimeMs" to diversityTime,
                    "freshnessAnalysisTimeMs" to freshnessTime
                )
            )
        }
    }

    override suspend fun logDiversityMetrics(userId: String, finalFeed: List<EnhancedScoredContent>) {
        // In a real implementation, this would log to analytics system
        // For now, we'll just track basic metrics
        val avgDiversityScore = finalFeed.map { it.diversityScore }.average()
        val avgFreshnessScore = finalFeed.map { it.freshnessScore }.average()
        val uniqueSources = finalFeed.map { it.content.authorId }.toSet().size
        val uniqueTopics = finalFeed.flatMap { it.content.tags }.toSet().size
        
        println("Diversity Metrics for user $userId:")
        println("  Average Diversity Score: $avgDiversityScore")
        println("  Average Freshness Score: $avgFreshnessScore") 
        println("  Unique Sources: $uniqueSources")
        println("  Unique Topics: $uniqueTopics")
    }

    // Private helper methods

    private suspend fun applyEchoChamberPrevention(
        userId: String,
        items: List<EnhancedScoredContent>,
        recentHistory: List<FeedEntry>,
        config: DiversityIntegrationConfig
    ): List<EnhancedScoredContent> {
        // Convert EnhancedScoredContent back to ScoredContent for echo chamber service
        val scoredItems = items.map { enhanced ->
            ScoredContent(
                content = enhanced.content,
                score = enhanced.finalScore,
                originalScore = enhanced.originalScore
            )
        }
        
        val adjustedItems = echoChamberPreventionService.applyEchoChamberPrevention(
            userId, scoredItems, recentHistory
        )
        
        // Merge back the adjustments
        return items.mapIndexed { index, original ->
            val adjusted = adjustedItems.getOrNull(index)
            if (adjusted != null && adjusted.score != original.finalScore) {
                val echoChamberAdjustment = adjusted.score / original.finalScore
                original.copy(
                    finalScore = adjusted.score,
                    echoChamberAdjustments = mapOf("score_multiplier" to echoChamberAdjustment),
                    scoringBreakdown = original.scoringBreakdown.copy(
                        echoChamberContribution = adjusted.score - original.finalScore
                    )
                )
            } else {
                original
            }
        }
    }

    private suspend fun applyContentBalancing(
        userId: String,
        items: List<EnhancedScoredContent>,
        targetSize: Int,
        config: DiversityIntegrationConfig
    ): List<EnhancedScoredContent> {
        // Convert to ScoredContent for balancing service
        val scoredItems = items.map { enhanced ->
            ScoredContent(
                content = enhanced.content,
                score = enhanced.finalScore,
                originalScore = enhanced.originalScore
            )
        }
        
        val balancingResult = contentBalancingService.applyContentQuotas(
            userId, scoredItems, targetSize
        )
        
        // Map the balanced results back to enhanced items
        return balancingResult.balancedFeed.map { balanced ->
            val original = items.find { it.content.id == balanced.content.id }
            original?.copy(
                finalScore = balanced.score,
                balancingAdjustments = balanced.diversityBoosts,
                scoringBreakdown = original.scoringBreakdown.copy(
                    balancingContribution = balanced.score - (original.finalScore)
                )
            ) ?: EnhancedScoredContent(
                content = balanced.content,
                originalScore = balanced.originalScore,
                finalScore = balanced.score,
                balancingAdjustments = balanced.diversityBoosts
            )
        }
    }

    private fun calculateDiversityBoost(diversityScore: Double): Double {
        // Apply diminishing returns for diversity boost
        return 1.0 + (diversityScore * 0.2 * (1.0 - exp(-diversityScore * 2)))
    }

    private fun calculateFreshnessBoost(freshnessScore: Double): Double {
        // Apply exponential boost for freshness
        return 1.0 + (freshnessScore * 0.3 * sqrt(freshnessScore))
    }

    private fun calculateFeedDiversityMetrics(
        feed: List<EnhancedScoredContent>,
        recentHistory: List<FeedEntry>
    ): FeedDiversityMetrics {
        val overallDiversityScore = feed.map { it.diversityScore }.average()
        val freshnessIndex = feed.map { it.freshnessScore }.average()
        
        val sources = feed.map { it.content.authorId }
        val sourceDistribution = calculateDistribution(sources)
        
        val topics = feed.flatMap { it.content.tags }
        val topicDistribution = calculateDistribution(topics)
        
        val contentTypes = feed.map { it.content.contentType.name }
        val contentTypeDistribution = calculateDistribution(contentTypes)
        
        return FeedDiversityMetrics(
            overallDiversityScore = overallDiversityScore,
            freshnessIndex = freshnessIndex,
            sourceDistribution = sourceDistribution,
            topicDistribution = topicDistribution,
            contentTypeDistribution = contentTypeDistribution,
            echoChamberRiskLevel = EchoChamberRiskLevel.LOW, // Simplified
            balanceMetrics = BalancedContentDistribution(0.3, 0.5, 0.2, 0.15, 0.25, 
                sourceDistribution.size, topicDistribution.size, overallDiversityScore),
            qualityMetrics = QualityMetrics(
                feed.map { it.finalScore }.average(),
                calculateVariance(feed.map { it.finalScore }),
                overallDiversityScore,
                freshnessIndex,
                overallDiversityScore * 0.8
            )
        )
    }

    private fun generateSystemRecommendations(
        metrics: FeedDiversityMetrics,
        feed: List<EnhancedScoredContent>
    ): List<SystemRecommendation> {
        val recommendations = mutableListOf<SystemRecommendation>()
        
        // Check for low diversity
        if (metrics.overallDiversityScore < 0.4) {
            recommendations.add(
                SystemRecommendation(
                    type = SystemRecommendationType.DIVERSIFY_CONTENT_SOURCES,
                    priority = RecommendationPriority.HIGH,
                    description = "Overall feed diversity is low (${(metrics.overallDiversityScore * 100).toInt()}%)",
                    expectedImpact = 0.6,
                    actionRequired = "Expand content source pool or adjust diversity weights"
                )
            )
        }
        
        // Check for low freshness
        if (metrics.freshnessIndex < 0.3) {
            recommendations.add(
                SystemRecommendation(
                    type = SystemRecommendationType.ADJUST_FRESHNESS_WEIGHTS,
                    priority = RecommendationPriority.MEDIUM,
                    description = "Feed freshness is below optimal level (${(metrics.freshnessIndex * 100).toInt()}%)",
                    expectedImpact = 0.4,
                    actionRequired = "Increase freshness boost multipliers or improve content ingestion"
                )
            )
        }
        
        // Check for source concentration
        if (metrics.sourceDistribution.size < 5 && feed.size >= 10) {
            recommendations.add(
                SystemRecommendation(
                    type = SystemRecommendationType.INCREASE_CONTENT_POOL,
                    priority = RecommendationPriority.MEDIUM,
                    description = "Limited source diversity (${metrics.sourceDistribution.size} unique sources)",
                    expectedImpact = 0.5,
                    actionRequired = "Onboard more content creators or sources"
                )
            )
        }
        
        return recommendations
    }

    private fun <T> calculateDistribution(items: List<T>): Map<String, Double> {
        if (items.isEmpty()) return emptyMap()
        
        val counts = items.groupingBy { it.toString() }.eachCount()
        val total = items.size.toDouble()
        
        return counts.mapValues { it.value / total }
    }

    private fun calculateVariance(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }

    private fun getContentAgeInHours(createdAt: Instant): Double {
        val now = Instant.now()
        return java.time.Duration.between(createdAt, now).toMillis() / (1000.0 * 3600.0)
    }

    private fun createEmptyResult(stats: ProcessingStats): DiversifiedFeedResult {
        return DiversifiedFeedResult(
            diversifiedFeed = emptyList(),
            diversityMetrics = createDefaultMetrics(),
            processingStats = stats,
            recommendations = emptyList()
        )
    }

    private fun createDefaultMetrics(): FeedDiversityMetrics {
        return FeedDiversityMetrics(
            overallDiversityScore = 0.0,
            freshnessIndex = 0.0,
            sourceDistribution = emptyMap(),
            topicDistribution = emptyMap(),
            contentTypeDistribution = emptyMap(),
            echoChamberRiskLevel = EchoChamberRiskLevel.LOW,
            balanceMetrics = BalancedContentDistribution(0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0),
            qualityMetrics = QualityMetrics(0.0, 0.0, 0.0, 0.0, 0.0)
        )
    }
}