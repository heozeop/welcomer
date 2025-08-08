package com.welcomer.welcome.personalization.service

import com.welcomer.welcome.engagement.model.EngagementType
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.*

/**
 * Enhanced source affinity calculation service with advanced engagement pattern analysis
 */
interface SourceAffinityService {
    /**
     * Calculate comprehensive source affinity based on user interaction patterns
     */
    suspend fun calculateAdvancedSourceAffinity(
        authorId: String,
        userHistory: List<UserActivity>,
        config: SourceAffinityConfig = SourceAffinityConfig()
    ): SourceAffinityResult

    /**
     * Calculate engagement pattern for a specific source
     */
    suspend fun calculateEngagementPattern(
        authorId: String,
        userHistory: List<UserActivity>
    ): EngagementPattern

    /**
     * Calculate source reliability based on user feedback
     */
    fun calculateSourceReliability(
        authorId: String,
        userHistory: List<UserActivity>
    ): Double

    /**
     * Calculate temporal engagement patterns (when user engages with this source)
     */
    fun calculateTemporalPattern(
        authorId: String,
        userHistory: List<UserActivity>
    ): TemporalEngagementPattern
}

/**
 * Configuration for source affinity calculation
 */
data class SourceAffinityConfig(
    val engagementWeights: Map<EngagementType, Double> = defaultEngagementWeights,
    val temporalDecayFactor: Double = 0.1, // How quickly old interactions lose relevance
    val minInteractionsForReliability: Int = 5,
    val negativeEngagementPenalty: Double = 2.0, // Amplify negative feedback
    val consistencyBonus: Double = 0.2, // Bonus for consistent engagement
    val recencyBonus: Double = 0.3, // Bonus for recent interactions
    val diversityBonus: Double = 0.1, // Bonus for engaging with diverse content from source
    val qualityThreshold: Double = 0.4, // Minimum quality score threshold
    val enableTemporalDecay: Boolean = true,
    val enableConsistencyAnalysis: Boolean = true,
    val enableReliabilityScoring: Boolean = true
) {
    companion object {
        val defaultEngagementWeights = mapOf(
            EngagementType.LIKE to 1.0,
            EngagementType.SHARE to 2.0,
            EngagementType.COMMENT to 1.8,
            EngagementType.BOOKMARK to 2.2,
            EngagementType.VIEW to 0.2,
            EngagementType.CLICK to 0.4,
            EngagementType.DWELL_TIME to 1.2,
            EngagementType.EXPAND to 1.0,
            EngagementType.SCROLL to 0.3,
            EngagementType.HIDE to -1.0,
            EngagementType.REPORT to -3.0,
            EngagementType.UNLIKE to -0.8,
            EngagementType.UNBOOKMARK to -0.5
        )
    }
}

/**
 * Comprehensive result of source affinity analysis
 */
data class SourceAffinityResult(
    val authorId: String,
    val overallAffinity: Double,
    val engagementPattern: EngagementPattern,
    val temporalPattern: TemporalEngagementPattern,
    val reliabilityScore: Double,
    val consistencyScore: Double,
    val recencyScore: Double,
    val diversityScore: Double,
    val totalInteractions: Int,
    val explanation: String,
    val confidenceLevel: AffinityConfidence
)

/**
 * Detailed engagement pattern analysis
 */
data class EngagementPattern(
    val positiveEngagements: Int,
    val negativeEngagements: Int,
    val neutralEngagements: Int,
    val averageEngagementScore: Double,
    val engagementTrend: EngagementTrend,
    val dominantEngagementTypes: List<EngagementType>,
    val engagementConsistency: Double,
    val lastEngagementTime: Instant?
)

/**
 * Temporal patterns of when user engages with source
 */
data class TemporalEngagementPattern(
    val peakHours: List<Int>, // Hours when user most engages with this source
    val peakDays: List<Int>, // Days when user most engages (1=Monday)
    val averageSessionGap: Long, // Average time between engagements in hours
    val engagementRegularity: Double, // How regularly user engages (0-1)
    val temporalTrend: TemporalTrend // Whether engagement is increasing/decreasing over time
)

/**
 * Engagement trend over time
 */
enum class EngagementTrend {
    INCREASING, // User is engaging more with this source over time
    DECREASING, // User is engaging less with this source over time
    STABLE,     // Engagement is consistent
    VOLATILE,   // Engagement varies significantly
    INSUFFICIENT_DATA
}

/**
 * Temporal trend analysis
 */
enum class TemporalTrend {
    MORE_RECENT_ACTIVITY,
    LESS_RECENT_ACTIVITY,
    CONSISTENT_ACTIVITY,
    INSUFFICIENT_DATA
}

/**
 * Confidence level in affinity calculation
 */
enum class AffinityConfidence {
    HIGH,    // 20+ interactions
    MEDIUM,  // 5-19 interactions
    LOW,     // 2-4 interactions
    MINIMAL  // 1 interaction
}

/**
 * Default implementation with advanced source affinity analysis
 */
@Service
class DefaultSourceAffinityService : SourceAffinityService {

    override suspend fun calculateAdvancedSourceAffinity(
        authorId: String,
        userHistory: List<UserActivity>,
        config: SourceAffinityConfig
    ): SourceAffinityResult {
        val sourceHistory = userHistory.filter { it.authorId == authorId }
        
        if (sourceHistory.isEmpty()) {
            return createNoInteractionResult(authorId)
        }

        val engagementPattern = calculateEngagementPattern(authorId, sourceHistory)
        val temporalPattern = calculateTemporalPattern(authorId, sourceHistory)
        val reliabilityScore = if (config.enableReliabilityScoring) {
            calculateSourceReliability(authorId, sourceHistory)
        } else 0.5

        // Calculate base affinity from engagement weights
        val baseAffinity = calculateBaseAffinity(sourceHistory, config)
        
        // Calculate enhancement factors
        val consistencyScore = if (config.enableConsistencyAnalysis) {
            engagementPattern.engagementConsistency
        } else 0.5
        
        val recencyScore = calculateRecencyScore(sourceHistory, config)
        val diversityScore = calculateContentDiversityScore(sourceHistory)
        
        // Apply enhancements
        var finalAffinity = baseAffinity
        
        // Consistency bonus
        finalAffinity += consistencyScore * config.consistencyBonus
        
        // Recency bonus
        finalAffinity += recencyScore * config.recencyBonus
        
        // Diversity bonus
        finalAffinity += diversityScore * config.diversityBonus
        
        // Reliability factor
        finalAffinity *= (0.5 + reliabilityScore * 0.5)
        
        // Apply temporal decay if enabled
        if (config.enableTemporalDecay) {
            val temporalDecay = calculateTemporalDecay(sourceHistory, config)
            finalAffinity *= temporalDecay
        }
        
        finalAffinity = finalAffinity.coerceIn(0.0, 1.0)
        
        val confidenceLevel = determineConfidenceLevel(sourceHistory.size)
        val explanation = generateAffinityExplanation(
            engagementPattern, temporalPattern, reliabilityScore, consistencyScore
        )
        
        return SourceAffinityResult(
            authorId = authorId,
            overallAffinity = finalAffinity,
            engagementPattern = engagementPattern,
            temporalPattern = temporalPattern,
            reliabilityScore = reliabilityScore,
            consistencyScore = consistencyScore,
            recencyScore = recencyScore,
            diversityScore = diversityScore,
            totalInteractions = sourceHistory.size,
            explanation = explanation,
            confidenceLevel = confidenceLevel
        )
    }

    override suspend fun calculateEngagementPattern(
        authorId: String,
        userHistory: List<UserActivity>
    ): EngagementPattern {
        val sourceHistory = userHistory.filter { it.authorId == authorId }
        
        if (sourceHistory.isEmpty()) {
            return EngagementPattern(
                positiveEngagements = 0,
                negativeEngagements = 0,
                neutralEngagements = 0,
                averageEngagementScore = 0.0,
                engagementTrend = EngagementTrend.INSUFFICIENT_DATA,
                dominantEngagementTypes = emptyList(),
                engagementConsistency = 0.0,
                lastEngagementTime = null
            )
        }

        val positiveTypes = setOf(
            EngagementType.LIKE, EngagementType.SHARE,
            EngagementType.COMMENT, EngagementType.BOOKMARK, EngagementType.EXPAND
        )
        val negativeTypes = setOf(
            EngagementType.HIDE, EngagementType.REPORT, 
            EngagementType.UNLIKE, EngagementType.UNBOOKMARK
        )

        val positiveEngagements = sourceHistory.count { it.engagementType in positiveTypes }
        val negativeEngagements = sourceHistory.count { it.engagementType in negativeTypes }
        val neutralEngagements = sourceHistory.size - positiveEngagements - negativeEngagements

        val averageEngagementScore = sourceHistory.map { it.engagementScore }.average()
        
        val engagementTrend = calculateEngagementTrend(sourceHistory)
        val dominantEngagementTypes = calculateDominantEngagementTypes(sourceHistory)
        val engagementConsistency = calculateEngagementConsistency(sourceHistory)
        val lastEngagementTime = sourceHistory.maxOfOrNull { it.timestamp }

        return EngagementPattern(
            positiveEngagements = positiveEngagements,
            negativeEngagements = negativeEngagements,
            neutralEngagements = neutralEngagements,
            averageEngagementScore = averageEngagementScore,
            engagementTrend = engagementTrend,
            dominantEngagementTypes = dominantEngagementTypes,
            engagementConsistency = engagementConsistency,
            lastEngagementTime = lastEngagementTime
        )
    }

    override fun calculateSourceReliability(
        authorId: String,
        userHistory: List<UserActivity>
    ): Double {
        val sourceHistory = userHistory.filter { it.authorId == authorId }
        
        if (sourceHistory.size < 3) {
            return 0.5 // Neutral reliability for insufficient data
        }

        val positiveActions = sourceHistory.count { activity ->
            activity.engagementType in setOf(
                EngagementType.LIKE, EngagementType.SHARE, EngagementType.BOOKMARK,
                EngagementType.COMMENT, EngagementType.EXPAND
            )
        }
        
        val negativeActions = sourceHistory.count { activity ->
            activity.engagementType in setOf(
                EngagementType.HIDE, EngagementType.REPORT, EngagementType.UNLIKE
            )
        }

        val neutralActions = sourceHistory.size - positiveActions - negativeActions
        val totalActions = sourceHistory.size.toDouble()

        // Calculate reliability as positive ratio minus negative impact
        val positiveRatio = positiveActions / totalActions
        val negativeRatio = negativeActions / totalActions
        val reliability = positiveRatio - (negativeRatio * 2.0) + (neutralActions / totalActions * 0.5)

        return reliability.coerceIn(0.0, 1.0)
    }

    override fun calculateTemporalPattern(
        authorId: String,
        userHistory: List<UserActivity>
    ): TemporalEngagementPattern {
        val sourceHistory = userHistory.filter { it.authorId == authorId }
        
        if (sourceHistory.size < 2) {
            return TemporalEngagementPattern(
                peakHours = emptyList(),
                peakDays = emptyList(),
                averageSessionGap = 0,
                engagementRegularity = 0.0,
                temporalTrend = TemporalTrend.INSUFFICIENT_DATA
            )
        }

        // Calculate peak hours (hours of day when user engages most)
        val hourCounts = sourceHistory.groupingBy { 
            it.timestamp.atZone(java.time.ZoneId.systemDefault()).hour 
        }.eachCount()
        val peakHours = hourCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        // Calculate peak days (days of week when user engages most)
        val dayCounts = sourceHistory.groupingBy {
            it.timestamp.atZone(java.time.ZoneId.systemDefault()).dayOfWeek.value
        }.eachCount()
        val peakDays = dayCounts.entries
            .sortedByDescending { it.value }
            .take(2)
            .map { it.key }

        // Calculate average time gap between engagements
        val sortedHistory = sourceHistory.sortedBy { it.timestamp }
        val gaps = sortedHistory.zipWithNext { a, b ->
            ChronoUnit.HOURS.between(a.timestamp, b.timestamp)
        }
        val averageSessionGap = if (gaps.isNotEmpty()) gaps.average().toLong() else 0L

        // Calculate engagement regularity (how evenly spaced engagements are)
        val engagementRegularity = if (gaps.isNotEmpty()) {
            val avgGap = gaps.average()
            val variance = gaps.map { (it - avgGap).pow(2) }.average()
            val stdDev = sqrt(variance)
            // Higher regularity for lower variance
            max(0.0, 1.0 - (stdDev / avgGap))
        } else 0.0

        // Calculate temporal trend
        val temporalTrend = calculateTemporalTrend(sourceHistory)

        return TemporalEngagementPattern(
            peakHours = peakHours,
            peakDays = peakDays,
            averageSessionGap = averageSessionGap,
            engagementRegularity = engagementRegularity,
            temporalTrend = temporalTrend
        )
    }

    // Private helper methods

    private fun calculateBaseAffinity(
        sourceHistory: List<UserActivity>,
        config: SourceAffinityConfig
    ): Double {
        val affinityScore = sourceHistory.sumOf { activity ->
            val weight = config.engagementWeights[activity.engagementType] ?: 0.1
            val score = activity.engagementScore
            weight * score
        }
        
        val totalInteractions = sourceHistory.size
        val normalizedScore = affinityScore / max(totalInteractions, 1)
        
        // Apply sigmoid function to keep score in reasonable range
        return (1.0 / (1.0 + exp(-normalizedScore))).coerceIn(0.0, 1.0)
    }

    private fun calculateRecencyScore(
        sourceHistory: List<UserActivity>,
        config: SourceAffinityConfig
    ): Double {
        if (sourceHistory.isEmpty()) return 0.0
        
        val now = Instant.now()
        val recentInteractions = sourceHistory.filter { activity ->
            ChronoUnit.DAYS.between(activity.timestamp, now) <= 30
        }
        
        val recencyRatio = recentInteractions.size.toDouble() / sourceHistory.size
        val avgRecency = sourceHistory.map { activity ->
            val daysAgo = ChronoUnit.DAYS.between(activity.timestamp, now)
            exp(-daysAgo / 30.0) // Exponential decay over 30 days
        }.average()
        
        return (recencyRatio * 0.6 + avgRecency * 0.4).coerceIn(0.0, 1.0)
    }

    private fun calculateContentDiversityScore(sourceHistory: List<UserActivity>): Double {
        if (sourceHistory.isEmpty()) return 0.0
        
        val topicSets = sourceHistory.map { it.topics.toSet() }
        val allTopics = topicSets.flatten().toSet()
        
        // Calculate topic diversity using Shannon entropy
        val topicCounts = sourceHistory.flatMap { it.topics }.groupingBy { it }.eachCount()
        val total = sourceHistory.flatMap { it.topics }.size.toDouble()
        
        if (total == 0.0) return 0.0
        
        val entropy = -topicCounts.values.sumOf { count ->
            val p = count / total
            if (p > 0) p * ln(p) else 0.0
        }
        
        // Normalize entropy (max entropy for uniform distribution)
        val maxEntropy = ln(allTopics.size.toDouble())
        return if (maxEntropy > 0) (entropy / maxEntropy).coerceIn(0.0, 1.0) else 0.0
    }

    private fun calculateTemporalDecay(
        sourceHistory: List<UserActivity>,
        config: SourceAffinityConfig
    ): Double {
        if (sourceHistory.isEmpty()) return 1.0
        
        val now = Instant.now()
        val weightedScore = sourceHistory.sumOf { activity ->
            val daysAgo = ChronoUnit.DAYS.between(activity.timestamp, now)
            exp(-daysAgo * config.temporalDecayFactor)
        }
        
        val maxPossibleScore = sourceHistory.size.toDouble()
        return (weightedScore / maxPossibleScore).coerceIn(0.0, 1.0)
    }

    private fun calculateEngagementTrend(sourceHistory: List<UserActivity>): EngagementTrend {
        if (sourceHistory.size < 4) return EngagementTrend.INSUFFICIENT_DATA
        
        val sortedHistory = sourceHistory.sortedBy { it.timestamp }
        val midpoint = sortedHistory.size / 2
        
        val firstHalf = sortedHistory.take(midpoint)
        val secondHalf = sortedHistory.drop(midpoint)
        
        val firstHalfAvg = firstHalf.map { it.engagementScore }.average()
        val secondHalfAvg = secondHalf.map { it.engagementScore }.average()
        
        val difference = secondHalfAvg - firstHalfAvg
        val threshold = 0.1
        
        return when {
            difference > threshold -> EngagementTrend.INCREASING
            difference < -threshold -> EngagementTrend.DECREASING
            abs(difference) <= threshold -> {
                val variance = sortedHistory.map { it.engagementScore }.let { scores ->
                    val avg = scores.average()
                    scores.map { (it - avg).pow(2) }.average()
                }
                if (variance > 0.2) EngagementTrend.VOLATILE else EngagementTrend.STABLE
            }
            else -> EngagementTrend.STABLE
        }
    }

    private fun calculateDominantEngagementTypes(sourceHistory: List<UserActivity>): List<EngagementType> {
        return sourceHistory
            .groupingBy { it.engagementType }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
    }

    private fun calculateEngagementConsistency(sourceHistory: List<UserActivity>): Double {
        if (sourceHistory.size < 2) return 0.0
        
        val scores = sourceHistory.map { it.engagementScore }
        val average = scores.average()
        val variance = scores.map { (it - average).pow(2) }.average()
        val stdDev = sqrt(variance)
        
        // Lower standard deviation = higher consistency
        return max(0.0, 1.0 - min(stdDev, 1.0))
    }

    private fun calculateTemporalTrend(sourceHistory: List<UserActivity>): TemporalTrend {
        if (sourceHistory.size < 3) return TemporalTrend.INSUFFICIENT_DATA
        
        val now = Instant.now()
        val recent = sourceHistory.count { ChronoUnit.DAYS.between(it.timestamp, now) <= 7 }
        val older = sourceHistory.count { ChronoUnit.DAYS.between(it.timestamp, now) > 7 }
        
        val recentRatio = recent.toDouble() / sourceHistory.size
        
        return when {
            recentRatio > 0.6 -> TemporalTrend.MORE_RECENT_ACTIVITY
            recentRatio < 0.3 -> TemporalTrend.LESS_RECENT_ACTIVITY
            else -> TemporalTrend.CONSISTENT_ACTIVITY
        }
    }

    private fun determineConfidenceLevel(interactionCount: Int): AffinityConfidence {
        return when {
            interactionCount >= 20 -> AffinityConfidence.HIGH
            interactionCount >= 5 -> AffinityConfidence.MEDIUM
            interactionCount >= 2 -> AffinityConfidence.LOW
            else -> AffinityConfidence.MINIMAL
        }
    }

    private fun generateAffinityExplanation(
        engagementPattern: EngagementPattern,
        temporalPattern: TemporalEngagementPattern,
        reliabilityScore: Double,
        consistencyScore: Double
    ): String {
        val explanations = mutableListOf<String>()
        
        if (engagementPattern.positiveEngagements > engagementPattern.negativeEngagements * 2) {
            explanations.add("You frequently engage positively with this source")
        }
        
        if (reliabilityScore > 0.7) {
            explanations.add("This source has proven reliable for your interests")
        }
        
        if (consistencyScore > 0.6) {
            explanations.add("You consistently engage with content from this source")
        }
        
        if (temporalPattern.engagementRegularity > 0.5) {
            explanations.add("You regularly check content from this source")
        }
        
        if (explanations.isEmpty()) {
            explanations.add("Limited interaction history with this source")
        }
        
        return explanations.joinToString("; ")
    }

    private fun createNoInteractionResult(authorId: String): SourceAffinityResult {
        return SourceAffinityResult(
            authorId = authorId,
            overallAffinity = 0.3, // Low but not zero for new sources
            engagementPattern = EngagementPattern(
                positiveEngagements = 0,
                negativeEngagements = 0,
                neutralEngagements = 0,
                averageEngagementScore = 0.0,
                engagementTrend = EngagementTrend.INSUFFICIENT_DATA,
                dominantEngagementTypes = emptyList(),
                engagementConsistency = 0.0,
                lastEngagementTime = null
            ),
            temporalPattern = TemporalEngagementPattern(
                peakHours = emptyList(),
                peakDays = emptyList(),
                averageSessionGap = 0,
                engagementRegularity = 0.0,
                temporalTrend = TemporalTrend.INSUFFICIENT_DATA
            ),
            reliabilityScore = 0.5,
            consistencyScore = 0.0,
            recencyScore = 0.0,
            diversityScore = 0.0,
            totalInteractions = 0,
            explanation = "No previous interactions with this source",
            confidenceLevel = AffinityConfidence.MINIMAL
        )
    }
}