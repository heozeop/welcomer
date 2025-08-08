package com.welcomer.welcome.diversity.service

import com.welcomer.welcome.diversity.model.*
import com.welcomer.welcome.feed.model.FeedEntry
import com.welcomer.welcome.ingestion.model.StoredContent
import com.welcomer.welcome.user.model.UserEngagement
import com.welcomer.welcome.user.model.UserProfileService
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.*

/**
 * Service for preventing echo chambers by ensuring diverse content exposure
 */
interface EchoChamberPreventionService {
    /**
     * Calculate a user's echo chamber risk based on their engagement patterns
     */
    suspend fun calculateEchoChamberRisk(
        userId: String,
        recentFeedHistory: List<FeedEntry>,
        recentEngagements: List<UserEngagement> = emptyList()
    ): EchoChamberRiskAssessment

    /**
     * Apply echo chamber prevention by adjusting content scores to promote diversity
     */
    suspend fun applyEchoChamberPrevention(
        userId: String,
        candidateItems: List<ScoredContent>,
        recentFeedHistory: List<FeedEntry>,
        preventionConfig: EchoChamberPreventionConfig = EchoChamberPreventionConfig()
    ): List<ScoredContent>

    /**
     * Generate recommendations to break user out of echo chamber
     */
    suspend fun generateBreakoutRecommendations(
        userId: String,
        echoChamberAnalysis: EchoChamberAnalysis
    ): List<EchoChamberBreakoutRecommendation>

    /**
     * Identify underrepresented perspectives for a user
     */
    suspend fun identifyMissingPerspectives(
        userId: String,
        userEngagements: List<UserEngagement>
    ): List<MissingPerspective>
}

/**
 * Content with associated scoring information
 */
data class ScoredContent(
    val content: StoredContent,
    val score: Double,
    val originalScore: Double = score,
    val diversityBoosts: Map<String, Double> = emptyMap(),
    val preventionReasons: List<String> = emptyList()
)

/**
 * Echo chamber risk assessment result
 */
data class EchoChamberRiskAssessment(
    val userId: String,
    val overallRiskScore: Double, // 0-1, higher = more at risk
    val riskLevel: EchoChamberRiskLevel,
    val riskFactors: Map<EchoChamberRiskFactor, Double>,
    val concentrationMetrics: ConcentrationMetrics,
    val assessmentTimestamp: Instant = Instant.now()
)

/**
 * Risk levels for echo chamber formation
 */
enum class EchoChamberRiskLevel {
    LOW,      // 0.0-0.3: Good diversity
    MODERATE, // 0.3-0.6: Some concentration but manageable
    HIGH,     // 0.6-0.8: Significant risk
    CRITICAL  // 0.8-1.0: Severe echo chamber risk
}

/**
 * Factors contributing to echo chamber risk
 */
enum class EchoChamberRiskFactor {
    TOPIC_CONCENTRATION,     // Too focused on few topics
    SOURCE_CONCENTRATION,    // Too few content sources
    PERSPECTIVE_BIAS,        // Skewed toward certain perspectives
    TEMPORAL_CLUSTERING,     // Content consumed at predictable times
    ENGAGEMENT_SELECTIVITY,  // Only engages with certain types
    SOCIAL_HOMOGENEITY      // Follows similar users/sources
}

/**
 * Concentration metrics for echo chamber analysis
 */
data class ConcentrationMetrics(
    val topicHerfindahlIndex: Double,      // Topic concentration using HHI
    val sourceHerfindahlIndex: Double,     // Source concentration using HHI
    val perspectiveEntropy: Double,        // Shannon entropy of perspectives
    val engagementGiniCoefficient: Double, // Inequality in engagement patterns
    val temporalConcentration: Double,     // Time-based clustering
    val diversityDeficit: Double           // How much diversity is lacking
)

/**
 * Configuration for echo chamber prevention
 */
data class EchoChamberPreventionConfig(
    val riskThresholds: Map<EchoChamberRiskLevel, Double> = mapOf(
        EchoChamberRiskLevel.LOW to 0.3,
        EchoChamberRiskLevel.MODERATE to 0.6,
        EchoChamberRiskLevel.HIGH to 0.8,
        EchoChamberRiskLevel.CRITICAL to 1.0
    ),
    val diversityBoostMultipliers: Map<EchoChamberRiskLevel, Double> = mapOf(
        EchoChamberRiskLevel.LOW to 0.1,
        EchoChamberRiskLevel.MODERATE to 0.25,
        EchoChamberRiskLevel.HIGH to 0.5,
        EchoChamberRiskLevel.CRITICAL to 0.8
    ),
    val minimumDiverseContentRatio: Double = 0.2,    // At least 20% diverse content
    val maxSameSourceRatio: Double = 0.3,            // Max 30% from same source
    val maxSameTopicRatio: Double = 0.4,             // Max 40% from same topic
    val perspectiveBalanceWeight: Double = 0.3,       // Weight for perspective balancing
    val enableGradualExposure: Boolean = true,        // Gradually increase diverse content
    val minEngagementHistory: Int = 20                // Minimum engagements for analysis
)

/**
 * Recommendation to break out of echo chamber
 */
data class EchoChamberBreakoutRecommendation(
    val type: BreakoutRecommendationType,
    val title: String,
    val description: String,
    val priority: RecommendationPriority,
    val suggestedActions: List<String>,
    val expectedImpact: Double, // 0-1, expected reduction in echo chamber risk
    val targetDiversityDimension: DiversityDimension
)

/**
 * Types of breakout recommendations
 */
enum class BreakoutRecommendationType {
    EXPLORE_NEW_TOPICS,      // Suggest new topic areas
    DIVERSIFY_SOURCES,       // Follow different content sources
    SEEK_OPPOSING_VIEWS,     // Expose to different perspectives
    VARY_CONTENT_TYPES,      // Try different media formats
    EXPAND_TIME_PATTERNS,    // Consume content at different times
    FOLLOW_DIVERSE_USERS     // Connect with different user types
}

/**
 * Missing perspective information
 */
data class MissingPerspective(
    val perspective: String,
    val currentExposure: Double,   // 0-1, current exposure level
    val recommendedExposure: Double, // 0-1, recommended exposure level
    val gap: Double,               // Difference between recommended and current
    val sampleTopics: List<String>, // Example topics to explore
    val reasoning: String          // Why this perspective is missing
)

/**
 * Default implementation of EchoChamberPreventionService
 */
@Service
class DefaultEchoChamberPreventionService(
    private val contentDiversityAnalyzer: ContentDiversityAnalyzer,
    private val userProfileService: UserProfileService? = null
) : EchoChamberPreventionService {

    override suspend fun calculateEchoChamberRisk(
        userId: String,
        recentFeedHistory: List<FeedEntry>,
        recentEngagements: List<UserEngagement>
    ): EchoChamberRiskAssessment {
        if (recentFeedHistory.size < 10) {
            // Not enough data for meaningful assessment
            return EchoChamberRiskAssessment(
                userId = userId,
                overallRiskScore = 0.0,
                riskLevel = EchoChamberRiskLevel.LOW,
                riskFactors = emptyMap(),
                concentrationMetrics = createDefaultConcentrationMetrics()
            )
        }

        val features = contentDiversityAnalyzer.extractContentFeatures(
            recentFeedHistory.map { it.content }
        )

        // Calculate concentration metrics
        val concentrationMetrics = calculateConcentrationMetrics(features, recentEngagements)
        
        // Calculate risk factors
        val riskFactors = calculateRiskFactors(features, recentEngagements, concentrationMetrics)
        
        // Calculate overall risk score as weighted average
        val overallRisk = riskFactors.entries.sumOf { (factor, score) ->
            score * getRiskFactorWeight(factor)
        }

        val riskLevel = determineRiskLevel(overallRisk)

        return EchoChamberRiskAssessment(
            userId = userId,
            overallRiskScore = overallRisk,
            riskLevel = riskLevel,
            riskFactors = riskFactors,
            concentrationMetrics = concentrationMetrics
        )
    }

    override suspend fun applyEchoChamberPrevention(
        userId: String,
        candidateItems: List<ScoredContent>,
        recentFeedHistory: List<FeedEntry>,
        preventionConfig: EchoChamberPreventionConfig
    ): List<ScoredContent> {
        // Assess echo chamber risk
        val riskAssessment = calculateEchoChamberRisk(userId, recentFeedHistory)
        
        if (riskAssessment.riskLevel == EchoChamberRiskLevel.LOW) {
            return candidateItems // No prevention needed
        }

        // Get diversity scores for candidate items
        val diversityAnalyses = contentDiversityAnalyzer.calculateDiversityScores(
            candidateItems.map { it.content },
            recentFeedHistory
        )

        // Apply prevention based on risk level
        return applyPreventionStrategy(
            candidateItems, 
            diversityAnalyses, 
            riskAssessment, 
            recentFeedHistory,
            preventionConfig
        )
    }

    override suspend fun generateBreakoutRecommendations(
        userId: String,
        echoChamberAnalysis: EchoChamberAnalysis
    ): List<EchoChamberBreakoutRecommendation> {
        val recommendations = mutableListOf<EchoChamberBreakoutRecommendation>()

        // Topic diversification recommendations
        if (echoChamberAnalysis.topicConcentration > 0.6) {
            recommendations.add(
                EchoChamberBreakoutRecommendation(
                    type = BreakoutRecommendationType.EXPLORE_NEW_TOPICS,
                    title = "Explore New Topic Areas",
                    description = "Your content is heavily concentrated in specific topics. Try exploring related or completely different subject areas.",
                    priority = RecommendationPriority.HIGH,
                    suggestedActions = listOf(
                        "Follow accounts that post about different topics",
                        "Search for content in unfamiliar categories",
                        "Use topic discovery features to find new interests"
                    ),
                    expectedImpact = 0.4,
                    targetDiversityDimension = DiversityDimension.TOPIC
                )
            )
        }

        // Source diversification recommendations
        if (echoChamberAnalysis.sourceConcentration > 0.6) {
            recommendations.add(
                EchoChamberBreakoutRecommendation(
                    type = BreakoutRecommendationType.DIVERSIFY_SOURCES,
                    title = "Diversify Your Content Sources",
                    description = "You're getting most content from a limited set of sources. Branch out to different authors and publishers.",
                    priority = RecommendationPriority.HIGH,
                    suggestedActions = listOf(
                        "Follow new authors in your areas of interest",
                        "Explore content from different regions or cultures",
                        "Subscribe to publications with different editorial perspectives"
                    ),
                    expectedImpact = 0.35,
                    targetDiversityDimension = DiversityDimension.SOURCE
                )
            )
        }

        // Perspective balance recommendations
        if (echoChamberAnalysis.dominantPerspectives.size <= 2) {
            recommendations.add(
                EchoChamberBreakoutRecommendation(
                    type = BreakoutRecommendationType.SEEK_OPPOSING_VIEWS,
                    title = "Seek Different Perspectives",
                    description = "Your feed shows limited viewpoint diversity. Consider exploring content that challenges your assumptions.",
                    priority = RecommendationPriority.HIGH,
                    suggestedActions = listOf(
                        "Read opinion pieces from different political perspectives",
                        "Follow thought leaders with different backgrounds",
                        "Engage with content that presents alternative viewpoints"
                    ),
                    expectedImpact = 0.5,
                    targetDiversityDimension = DiversityDimension.PERSPECTIVE
                )
            )
        }

        return recommendations.sortedByDescending { it.expectedImpact }
    }

    override suspend fun identifyMissingPerspectives(
        userId: String,
        userEngagements: List<UserEngagement>
    ): List<MissingPerspective> {
        val perspectives = listOf("liberal", "conservative", "centrist", "progressive", "libertarian")
        val engagementsByPerspective = userEngagements.groupBy { inferPerspectiveFromEngagement(it) }
        val totalEngagements = userEngagements.size.toDouble()
        
        return perspectives.mapNotNull { perspective ->
            val currentExposure = (engagementsByPerspective[perspective]?.size ?: 0) / totalEngagements
            val recommendedExposure = 0.15 // At least 15% exposure to each major perspective
            val gap = recommendedExposure - currentExposure
            
            if (gap > 0.05) { // Only include if gap is significant
                MissingPerspective(
                    perspective = perspective,
                    currentExposure = currentExposure,
                    recommendedExposure = recommendedExposure,
                    gap = gap,
                    sampleTopics = getSampleTopicsForPerspective(perspective),
                    reasoning = "Currently only ${(currentExposure * 100).toInt()}% exposure, recommended ${(recommendedExposure * 100).toInt()}%"
                )
            } else null
        }.sortedByDescending { it.gap }
    }

    // Private helper methods

    private fun calculateConcentrationMetrics(
        features: List<ContentFeatures>,
        engagements: List<UserEngagement>
    ): ConcentrationMetrics {
        if (features.isEmpty()) {
            return createDefaultConcentrationMetrics()
        }

        // Topic concentration using Herfindahl-Hirschman Index
        val allTopics = features.flatMap { it.topics.takeIf { topics -> topics.isNotEmpty() } ?: listOf("unknown") }
        val topicCounts = allTopics.groupingBy { it }.eachCount()
        val topicHHI = calculateHerfindahlIndex(topicCounts.values)

        // Source concentration
        val sourceCounts = features.groupingBy { it.authorId }.eachCount()
        val sourceHHI = calculateHerfindahlIndex(sourceCounts.values)

        // Perspective entropy (Shannon entropy)
        val perspectiveCounts = features.groupingBy { it.perspective }.eachCount()
        val perspectiveEntropy = calculateShannonEntropy(perspectiveCounts.values)

        // Engagement distribution (Gini coefficient would be complex, using simplified version)
        val engagementGini = if (engagements.isNotEmpty()) {
            calculateEngagementInequality(engagements)
        } else 0.0

        // Temporal concentration
        val temporalConcentration = calculateTemporalConcentration(features)

        // Overall diversity deficit
        val diversityDeficit = (topicHHI + sourceHHI + (1.0 - perspectiveEntropy) + engagementGini + temporalConcentration) / 5.0

        return ConcentrationMetrics(
            topicHerfindahlIndex = topicHHI,
            sourceHerfindahlIndex = sourceHHI,
            perspectiveEntropy = perspectiveEntropy,
            engagementGiniCoefficient = engagementGini,
            temporalConcentration = temporalConcentration,
            diversityDeficit = diversityDeficit
        )
    }

    private fun calculateRiskFactors(
        features: List<ContentFeatures>,
        engagements: List<UserEngagement>,
        metrics: ConcentrationMetrics
    ): Map<EchoChamberRiskFactor, Double> {
        return mapOf(
            EchoChamberRiskFactor.TOPIC_CONCENTRATION to metrics.topicHerfindahlIndex,
            EchoChamberRiskFactor.SOURCE_CONCENTRATION to metrics.sourceHerfindahlIndex,
            EchoChamberRiskFactor.PERSPECTIVE_BIAS to (1.0 - metrics.perspectiveEntropy),
            EchoChamberRiskFactor.TEMPORAL_CLUSTERING to metrics.temporalConcentration,
            EchoChamberRiskFactor.ENGAGEMENT_SELECTIVITY to metrics.engagementGiniCoefficient,
            EchoChamberRiskFactor.SOCIAL_HOMOGENEITY to calculateSocialHomogeneity(features)
        )
    }

    private fun getRiskFactorWeight(factor: EchoChamberRiskFactor): Double {
        return when (factor) {
            EchoChamberRiskFactor.TOPIC_CONCENTRATION -> 0.25
            EchoChamberRiskFactor.SOURCE_CONCENTRATION -> 0.20
            EchoChamberRiskFactor.PERSPECTIVE_BIAS -> 0.25
            EchoChamberRiskFactor.TEMPORAL_CLUSTERING -> 0.10
            EchoChamberRiskFactor.ENGAGEMENT_SELECTIVITY -> 0.10
            EchoChamberRiskFactor.SOCIAL_HOMOGENEITY -> 0.10
        }
    }

    private fun determineRiskLevel(overallRisk: Double): EchoChamberRiskLevel {
        return when {
            overallRisk >= 0.8 -> EchoChamberRiskLevel.CRITICAL
            overallRisk >= 0.6 -> EchoChamberRiskLevel.HIGH
            overallRisk >= 0.3 -> EchoChamberRiskLevel.MODERATE
            else -> EchoChamberRiskLevel.LOW
        }
    }

    private fun applyPreventionStrategy(
        candidateItems: List<ScoredContent>,
        diversityAnalyses: List<DiversityAnalysisResult>,
        riskAssessment: EchoChamberRiskAssessment,
        recentFeedHistory: List<FeedEntry>,
        config: EchoChamberPreventionConfig
    ): List<ScoredContent> {
        val boostMultiplier = config.diversityBoostMultipliers[riskAssessment.riskLevel] ?: 0.0
        
        return candidateItems.mapIndexed { index, item ->
            val diversityAnalysis = diversityAnalyses.getOrNull(index)
            var adjustedScore = item.score
            val boosts = mutableMapOf<String, Double>()
            val reasons = mutableListOf<String>()

            diversityAnalysis?.let { analysis ->
                // Apply diversity boost based on overall diversity score
                val diversityBoost = analysis.overallDiversityScore * boostMultiplier
                adjustedScore *= (1.0 + diversityBoost)
                boosts["diversity"] = diversityBoost

                // Apply specific dimension boosts for high-risk areas
                analysis.dimensionScores.forEach { (dimension, score) ->
                    val riskFactor = mapDimensionToRiskFactor(dimension)
                    val dimensionRisk = riskAssessment.riskFactors[riskFactor] ?: 0.0
                    
                    if (dimensionRisk > 0.6 && score > 0.7) {
                        val dimensionBoost = score * dimensionRisk * 0.3
                        adjustedScore *= (1.0 + dimensionBoost)
                        boosts[dimension.name.lowercase()] = dimensionBoost
                        reasons.add("Boosted for ${dimension.name.lowercase()} diversity")
                    }
                }

                // Additional boost for critical echo chamber risk
                if (riskAssessment.riskLevel == EchoChamberRiskLevel.CRITICAL) {
                    val criticalBoost = 0.2
                    adjustedScore *= (1.0 + criticalBoost)
                    boosts["critical_prevention"] = criticalBoost
                    reasons.add("Critical echo chamber prevention boost")
                }
            }

            item.copy(
                score = adjustedScore,
                diversityBoosts = boosts,
                preventionReasons = reasons
            )
        }
    }

    private fun calculateHerfindahlIndex(counts: Collection<Int>): Double {
        val total = counts.sum().toDouble()
        if (total == 0.0) return 0.0
        
        return counts.sumOf { count ->
            val proportion = count / total
            proportion * proportion
        }
    }

    private fun calculateShannonEntropy(counts: Collection<Int>): Double {
        val total = counts.sum().toDouble()
        if (total == 0.0 || counts.isEmpty()) return 0.0
        
        val entropy = counts.sumOf { count ->
            if (count == 0) 0.0 else {
                val proportion = count / total
                -proportion * ln(proportion)
            }
        }
        
        // Normalize to 0-1 range using max possible entropy for this number of categories
        val maxEntropy = ln(counts.size.toDouble())
        return if (maxEntropy > 0) entropy / maxEntropy else 0.0
    }

    private fun calculateEngagementInequality(engagements: List<UserEngagement>): Double {
        // Simplified engagement inequality calculation
        val engagementCounts = engagements.groupingBy { it.contentId }.eachCount()
        val counts = engagementCounts.values
        if (counts.isEmpty()) return 0.0
        
        val mean = counts.average()
        val variance = counts.sumOf { (it - mean).pow(2) } / counts.size
        val coefficientOfVariation = sqrt(variance) / mean
        
        // Convert to 0-1 scale where higher values indicate more inequality
        return min(coefficientOfVariation / 2.0, 1.0)
    }

    private fun calculateTemporalConcentration(features: List<ContentFeatures>): Double {
        if (features.isEmpty()) return 0.0
        
        val hourCounts = features.groupingBy { 
            it.createdAt.atZone(java.time.ZoneId.systemDefault()).hour 
        }.eachCount()
        
        return calculateHerfindahlIndex(hourCounts.values)
    }

    private fun calculateSocialHomogeneity(features: List<ContentFeatures>): Double {
        // Simplified social homogeneity calculation
        // In a real implementation, this would analyze social network connections
        val uniqueAuthors = features.map { it.authorId }.distinct().size
        val totalContent = features.size
        
        // Higher homogeneity when fewer unique authors relative to content
        return if (totalContent == 0) 0.0 else 1.0 - (uniqueAuthors.toDouble() / totalContent).coerceAtMost(1.0)
    }

    private fun mapDimensionToRiskFactor(dimension: DiversityDimension): EchoChamberRiskFactor {
        return when (dimension) {
            DiversityDimension.TOPIC -> EchoChamberRiskFactor.TOPIC_CONCENTRATION
            DiversityDimension.SOURCE -> EchoChamberRiskFactor.SOURCE_CONCENTRATION
            DiversityDimension.PERSPECTIVE -> EchoChamberRiskFactor.PERSPECTIVE_BIAS
            else -> EchoChamberRiskFactor.ENGAGEMENT_SELECTIVITY
        }
    }

    private fun inferPerspectiveFromEngagement(engagement: UserEngagement): String {
        // Simplified perspective inference from engagement
        // In a real implementation, this would use content analysis
        return "neutral"
    }

    private fun getSampleTopicsForPerspective(perspective: String): List<String> {
        return when (perspective) {
            "liberal" -> listOf("social justice", "climate action", "progressive policies")
            "conservative" -> listOf("traditional values", "free market", "constitutional rights")
            "centrist" -> listOf("bipartisan solutions", "moderate policies", "pragmatic approaches")
            "progressive" -> listOf("systemic reform", "economic equality", "social transformation")
            "libertarian" -> listOf("individual liberty", "limited government", "free markets")
            else -> listOf("balanced perspectives", "diverse viewpoints", "multiple angles")
        }
    }

    private fun createDefaultConcentrationMetrics(): ConcentrationMetrics {
        return ConcentrationMetrics(
            topicHerfindahlIndex = 0.0,
            sourceHerfindahlIndex = 0.0,
            perspectiveEntropy = 1.0,
            engagementGiniCoefficient = 0.0,
            temporalConcentration = 0.0,
            diversityDeficit = 0.0
        )
    }
}