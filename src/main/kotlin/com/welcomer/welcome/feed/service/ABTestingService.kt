package com.welcomer.welcome.feed.service

import com.welcomer.welcome.feed.model.*
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import kotlin.random.Random

/**
 * Service for A/B testing feed algorithm variations
 */
interface ABTestingService {
    /**
     * Get user's experiment assignment for feed generation
     */
    suspend fun getUserExperiment(userId: String, feedType: FeedType): ExperimentConfig?

    /**
     * Apply experiment parameters to base scoring weights
     */
    fun applyExperimentParameters(
        baseWeights: ScoringWeights,
        experimentConfig: ExperimentConfig
    ): ScoringWeights

    /**
     * Log metrics for experiment analysis
     */
    suspend fun logExperimentMetrics(
        userId: String,
        experimentConfig: ExperimentConfig,
        feedMetadata: FeedMetadata,
        userInteractions: Map<String, Any> = emptyMap()
    )

    /**
     * Check if user should be included in experiment
     */
    fun shouldIncludeInExperiment(userId: String, experimentId: String, targetPercentage: Double): Boolean

    /**
     * Get experiment variant based on user assignment
     */
    suspend fun getExperimentVariant(userId: String, experimentId: String): ExperimentVariant?

    /**
     * Track experiment events for analysis
     */
    suspend fun trackExperimentEvent(
        userId: String,
        experimentId: String,
        eventType: ExperimentEventType,
        metadata: Map<String, Any> = emptyMap()
    )
}

/**
 * Experiment variant configuration
 */
data class ExperimentVariant(
    val id: String,
    val experimentId: String,
    val variantName: String,
    val allocationPercentage: Double,
    val parameters: Map<String, Any>,
    val isControl: Boolean = false
)

/**
 * Types of experiment events to track
 */
enum class ExperimentEventType {
    FEED_GENERATED,      // Feed was generated
    CONTENT_VIEWED,      // Content was viewed
    CONTENT_ENGAGED,     // Content was liked/shared/commented
    FEED_REFRESHED,      // User refreshed feed
    SESSION_STARTED,     // User started session
    SESSION_ENDED,       // User ended session
    EXPERIMENT_EXCLUDED  // User was excluded from experiment
}

@Service
class DefaultABTestingService : ABTestingService {

    companion object {
        // Experiment parameters that can be modified
        private val SUPPORTED_PARAMETERS = setOf(
            "recency_weight", "popularity_weight", "relevance_weight",
            "max_same_author", "max_same_topic", "author_spacing",
            "enable_cold_start", "trending_weight", "diversity_enabled"
        )
        
        // Hash salt for consistent user assignment
        private const val ASSIGNMENT_SALT = "feed_experiment_salt_2024"
    }

    // In-memory cache for active experiments (in production, use Redis/database)
    private val activeExperiments = mutableMapOf<String, ExperimentVariant>()
    private val userAssignments = mutableMapOf<String, Map<String, String>>() // userId -> experimentId -> variantId

    override suspend fun getUserExperiment(userId: String, feedType: FeedType): ExperimentConfig? {
        // Get all active experiments for this feed type
        val activeExperiments = getActiveExperimentsForFeedType(feedType)
        
        if (activeExperiments.isEmpty()) return null
        
        // For simplicity, take the first active experiment
        val experiment = activeExperiments.first()
        val variant = getExperimentVariant(userId, experiment.id) ?: return null
        
        return ExperimentConfig(
            experimentId = experiment.id,
            variantId = variant.id,
            parameters = variant.parameters,
            isControl = variant.isControl
        )
    }

    override fun applyExperimentParameters(
        baseWeights: ScoringWeights,
        experimentConfig: ExperimentConfig
    ): ScoringWeights {
        var modifiedWeights = baseWeights
        
        experimentConfig.parameters.forEach { (key, value) ->
            when (key) {
                "recency_weight" -> {
                    val weight = (value as? Number)?.toDouble() ?: baseWeights.recency
                    modifiedWeights = modifiedWeights.copy(recency = weight)
                }
                "popularity_weight" -> {
                    val weight = (value as? Number)?.toDouble() ?: baseWeights.popularity
                    modifiedWeights = modifiedWeights.copy(popularity = weight)
                }
                "relevance_weight" -> {
                    val weight = (value as? Number)?.toDouble() ?: baseWeights.relevance
                    modifiedWeights = modifiedWeights.copy(relevance = weight)
                }
                "following_weight" -> {
                    val weight = (value as? Number)?.toDouble() ?: baseWeights.following
                    modifiedWeights = modifiedWeights.copy(following = weight)
                }
                "engagement_weight" -> {
                    val weight = (value as? Number)?.toDouble() ?: baseWeights.engagement
                    modifiedWeights = modifiedWeights.copy(engagement = weight)
                }
                else -> {
                    // Add to custom weights
                    if (key in SUPPORTED_PARAMETERS && value is Number) {
                        val customWeights = modifiedWeights.customWeights.toMutableMap()
                        customWeights[key] = value.toDouble()
                        modifiedWeights = modifiedWeights.copy(customWeights = customWeights)
                    }
                }
            }
        }
        
        // Normalize weights to ensure they sum to 1.0
        return normalizeWeights(modifiedWeights)
    }

    override suspend fun logExperimentMetrics(
        userId: String,
        experimentConfig: ExperimentConfig,
        feedMetadata: FeedMetadata,
        userInteractions: Map<String, Any>
    ) {
        val metrics = mapOf(
            "user_id" to userId,
            "experiment_id" to experimentConfig.experimentId,
            "variant_id" to experimentConfig.variantId,
            "is_control" to experimentConfig.isControl,
            "generation_duration" to feedMetadata.generationDuration,
            "content_count" to feedMetadata.contentCount,
            "candidate_count" to feedMetadata.candidateCount,
            "algorithm_id" to feedMetadata.algorithmId,
            "timestamp" to Instant.now().toString()
        ) + userInteractions
        
        // In production, this would write to analytics system
        println("Experiment Metrics: $metrics")
        
        // Track the event
        trackExperimentEvent(userId, experimentConfig.experimentId, ExperimentEventType.FEED_GENERATED, metrics)
    }

    override fun shouldIncludeInExperiment(userId: String, experimentId: String, targetPercentage: Double): Boolean {
        // Use consistent hashing to determine if user should be in experiment
        val hash = "$userId:$experimentId:$ASSIGNMENT_SALT".hashCode()
        val bucket = (Math.abs(hash) % 100).toDouble()
        
        return bucket < targetPercentage
    }

    override suspend fun getExperimentVariant(userId: String, experimentId: String): ExperimentVariant? {
        // Check if user already has an assignment
        val existingAssignment = userAssignments[userId]?.get(experimentId)
        if (existingAssignment != null) {
            return activeExperiments[existingAssignment]
        }
        
        // Get experiment configuration (would typically come from database)
        val experiment = getExperimentConfig(experimentId) ?: return null
        
        // Check if user should be included
        if (!shouldIncludeInExperiment(userId, experimentId, experiment.targetPercentage)) {
            trackExperimentEvent(userId, experimentId, ExperimentEventType.EXPERIMENT_EXCLUDED)
            return null
        }
        
        // Assign user to variant using consistent hashing
        val variant = assignUserToVariant(userId, experimentId, experiment.variants)
        if (variant != null) {
            // Store assignment
            val userExperiments = userAssignments.getOrPut(userId) { mutableMapOf() }.toMutableMap()
            userExperiments[experimentId] = variant.id
            userAssignments[userId] = userExperiments
        }
        
        return variant
    }

    override suspend fun trackExperimentEvent(
        userId: String,
        experimentId: String,
        eventType: ExperimentEventType,
        metadata: Map<String, Any>
    ) {
        val eventData = mapOf(
            "user_id" to userId,
            "experiment_id" to experimentId,
            "event_type" to eventType.name,
            "timestamp" to Instant.now().toString()
        ) + metadata
        
        // In production, this would write to event tracking system (e.g., Kafka, analytics DB)
        println("Experiment Event: $eventData")
    }

    // Private helper methods

    private fun normalizeWeights(weights: ScoringWeights): ScoringWeights {
        val total = weights.recency + weights.popularity + weights.relevance + weights.following + weights.engagement
        
        if (total <= 0) return weights
        
        return weights.copy(
            recency = weights.recency / total,
            popularity = weights.popularity / total,
            relevance = weights.relevance / total,
            following = weights.following / total,
            engagement = weights.engagement / total
        )
    }

    private suspend fun getActiveExperimentsForFeedType(feedType: FeedType): List<Experiment> {
        // In production, this would query database for active experiments
        // For now, return mock experiment
        return listOf(
            Experiment(
                id = "feed_algorithm_test_2024",
                name = "Feed Algorithm Weight Test",
                feedTypes = listOf(feedType),
                targetPercentage = 10.0,
                variants = listOf(
                    ExperimentVariant(
                        id = "control",
                        experimentId = "feed_algorithm_test_2024",
                        variantName = "control",
                        allocationPercentage = 50.0,
                        parameters = emptyMap(),
                        isControl = true
                    ),
                    ExperimentVariant(
                        id = "high_recency",
                        experimentId = "feed_algorithm_test_2024",
                        variantName = "high_recency",
                        allocationPercentage = 50.0,
                        parameters = mapOf(
                            "recency_weight" to 0.7,
                            "popularity_weight" to 0.2,
                            "relevance_weight" to 0.1
                        )
                    )
                )
            )
        )
    }

    private fun getExperimentConfig(experimentId: String): Experiment? {
        // Mock experiment configuration
        return when (experimentId) {
            "feed_algorithm_test_2024" -> Experiment(
                id = experimentId,
                name = "Feed Algorithm Weight Test",
                feedTypes = listOf(FeedType.HOME),
                targetPercentage = 10.0,
                variants = listOf(
                    ExperimentVariant(
                        id = "control",
                        experimentId = experimentId,
                        variantName = "control",
                        allocationPercentage = 50.0,
                        parameters = emptyMap(),
                        isControl = true
                    ),
                    ExperimentVariant(
                        id = "high_recency",
                        experimentId = experimentId,
                        variantName = "high_recency",
                        allocationPercentage = 50.0,
                        parameters = mapOf(
                            "recency_weight" to 0.7,
                            "popularity_weight" to 0.2,
                            "relevance_weight" to 0.1
                        )
                    )
                )
            )
            else -> null
        }
    }

    private fun assignUserToVariant(userId: String, experimentId: String, variants: List<ExperimentVariant>): ExperimentVariant? {
        if (variants.isEmpty()) return null
        
        // Use consistent hashing to assign variant
        val hash = "$userId:$experimentId:variant:$ASSIGNMENT_SALT".hashCode()
        val bucket = Math.abs(hash) % 100.0
        
        var cumulativePercentage = 0.0
        for (variant in variants) {
            cumulativePercentage += variant.allocationPercentage
            if (bucket < cumulativePercentage) {
                return variant
            }
        }
        
        // Fallback to last variant
        return variants.last()
    }
}

/**
 * Experiment configuration (would typically be stored in database)
 */
private data class Experiment(
    val id: String,
    val name: String,
    val feedTypes: List<FeedType>,
    val targetPercentage: Double,
    val variants: List<ExperimentVariant>
)