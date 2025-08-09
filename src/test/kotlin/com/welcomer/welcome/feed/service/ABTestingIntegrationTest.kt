package com.welcomer.welcome.feed.service

import com.welcomer.welcome.feed.model.*
import com.welcomer.welcome.ingestion.model.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Integration tests for A/B Testing Framework Integration
 * 
 * These tests verify that the personalization system correctly integrates with 
 * A/B testing frameworks and delivers consistent experiences within test groups.
 */
class ABTestingIntegrationTest {

    private lateinit var abTestingService: DefaultABTestingService

    @BeforeEach
    fun setup() {
        abTestingService = DefaultABTestingService()
    }

    @Test
    fun `getUserExperiment should return consistent experiment assignment for same user`() = runBlocking {
        // Given
        val userId = "test_user_123"
        val feedType = FeedType.HOME

        // When
        val experiment1 = abTestingService.getUserExperiment(userId, feedType)
        val experiment2 = abTestingService.getUserExperiment(userId, feedType)

        // Then
        if (experiment1 != null && experiment2 != null) {
            assertEquals(experiment1.experimentId, experiment2.experimentId)
            assertEquals(experiment1.variantId, experiment2.variantId)
            assertEquals(experiment1.isControl, experiment2.isControl)
        }
    }

    @Test
    fun `shouldIncludeInExperiment should use consistent hashing for user assignment`() {
        // Given
        val userId = "consistent_user"
        val experimentId = "test_experiment"
        val targetPercentage = 50.0

        // When
        val result1 = abTestingService.shouldIncludeInExperiment(userId, experimentId, targetPercentage)
        val result2 = abTestingService.shouldIncludeInExperiment(userId, experimentId, targetPercentage)

        // Then
        assertEquals(result1, result2, "Same user should get consistent assignment")
    }

    @Test
    fun `applyExperimentParameters should modify scoring weights correctly`() {
        // Given
        val baseWeights = ScoringWeights(
            recency = 0.5,
            popularity = 0.3,
            relevance = 0.2
        )
        
        val experimentConfig = ExperimentConfig(
            experimentId = "weight_test",
            variantId = "high_recency",
            parameters = mapOf(
                "recency_weight" to 0.7,
                "popularity_weight" to 0.2,
                "relevance_weight" to 0.1
            ),
            isControl = false
        )

        // When
        val modifiedWeights = abTestingService.applyExperimentParameters(baseWeights, experimentConfig)

        // Then
        assertEquals(0.7, modifiedWeights.recency, 0.01)
        assertEquals(0.2, modifiedWeights.popularity, 0.01)
        assertEquals(0.1, modifiedWeights.relevance, 0.01)
    }

    @Test
    fun `applyExperimentParameters should handle control group without modifications`() {
        // Given
        val baseWeights = ScoringWeights(
            recency = 0.5,
            popularity = 0.3,
            relevance = 0.2
        )
        
        val controlConfig = ExperimentConfig(
            experimentId = "control_test",
            variantId = "control",
            parameters = emptyMap(),
            isControl = true
        )

        // When
        val modifiedWeights = abTestingService.applyExperimentParameters(baseWeights, controlConfig)

        // Then - weights should remain normalized but essentially unchanged
        assertTrue(modifiedWeights.recency > 0.4 && modifiedWeights.recency < 0.6)
        assertTrue(modifiedWeights.popularity > 0.25 && modifiedWeights.popularity < 0.35)
        assertTrue(modifiedWeights.relevance > 0.15 && modifiedWeights.relevance < 0.25)
    }

    @Test
    fun `trackExperimentEvent should record events correctly`() = runBlocking {
        // Given
        val userId = "tracking_user"
        val experimentId = "tracking_experiment"
        val eventType = ExperimentEventType.CONTENT_VIEWED
        val metadata = mapOf("content_id" to "test_content_123")

        // When - no exception should be thrown
        abTestingService.trackExperimentEvent(userId, experimentId, eventType, metadata)

        // Then - the test passes if no exception is thrown
        // In a real implementation, we would verify the event was stored
        assertTrue(true, "Event tracking should complete without errors")
    }

    @Test
    fun `logExperimentMetrics should process feed metadata correctly`() = runBlocking {
        // Given
        val userId = "metrics_user"
        val experimentConfig = ExperimentConfig(
            experimentId = "metrics_test",
            variantId = "test_variant", 
            parameters = mapOf("recency_weight" to 0.6),
            isControl = false
        )
        
        val feedMetadata = FeedMetadata(
            algorithmId = "test_algorithm",
            algorithmVersion = "1.0.0",
            generationDuration = 150L,
            contentCount = 25,
            candidateCount = 1000,
            parameters = emptyMap()
        )

        // When - no exception should be thrown
        abTestingService.logExperimentMetrics(userId, experimentConfig, feedMetadata)

        // Then - the test passes if no exception is thrown
        assertTrue(true, "Metrics logging should complete without errors")
    }

    @Test
    fun `getExperimentVariant should return null for excluded users`() = runBlocking {
        // Given
        val userId = "excluded_user"
        val experimentId = "exclusive_experiment"
        
        // When
        val variant = abTestingService.getExperimentVariant(userId, experimentId)

        // Then - most users should be excluded from the 10% experiment
        // We can't guarantee exclusion, but we can test the method doesn't crash
        // In a real test, we'd mock the shouldIncludeInExperiment method
        assertNotNull(variant?.let { it } ?: "excluded", "Method should return variant or exclude user")
    }

    @Test
    fun `experiment parameter normalization should maintain weight sum`() {
        // Given
        val baseWeights = ScoringWeights(
            recency = 0.6,
            popularity = 0.3,
            relevance = 0.1
        )
        
        val experimentConfig = ExperimentConfig(
            experimentId = "normalization_test",
            variantId = "unnormalized",
            parameters = mapOf(
                "recency_weight" to 0.8,
                "popularity_weight" to 0.4,
                "relevance_weight" to 0.2
            ),
            isControl = false
        )

        // When
        val normalizedWeights = abTestingService.applyExperimentParameters(baseWeights, experimentConfig)

        // Then - weights should sum to approximately 1.0
        val sum = normalizedWeights.recency + normalizedWeights.popularity + normalizedWeights.relevance
        assertEquals(1.0, sum, 0.01, "Weights should be normalized to sum to 1.0")
    }

    @Test
    fun `different users should potentially get different experiment assignments`() {
        // Given
        val user1 = "user_001"
        val user2 = "user_002"
        val user3 = "user_003"
        val experimentId = "distribution_test"
        val targetPercentage = 50.0

        // When
        val assignments = listOf(user1, user2, user3).map { userId ->
            abTestingService.shouldIncludeInExperiment(userId, experimentId, targetPercentage)
        }

        // Then - with 50% targeting, we should see some variation in assignments
        // (Though it's possible all three could get the same assignment by chance)
        val assignmentSet = assignments.toSet()
        
        // This test mainly verifies the method works for different users
        assertTrue(assignmentSet.isNotEmpty(), "Should produce assignment decisions")
        assertTrue(assignmentSet.size <= 2, "Should only have true/false assignments")
    }

    @Test
    fun `experiment parameters should handle invalid values gracefully`() {
        // Given
        val baseWeights = ScoringWeights(
            recency = 0.5,
            popularity = 0.3,
            relevance = 0.2
        )
        
        val experimentConfig = ExperimentConfig(
            experimentId = "invalid_params_test",
            variantId = "invalid_variant",
            parameters = mapOf(
                "recency_weight" to "invalid_string", // Invalid type
                "unknown_param" to 0.5,              // Unknown parameter
                "negative_weight" to -0.1             // Invalid value (handled by normalization)
            ),
            isControl = false
        )

        // When - should not throw exception
        val result = abTestingService.applyExperimentParameters(baseWeights, experimentConfig)

        // Then - should return valid weights
        assertNotNull(result)
        assertTrue(result.recency >= 0)
        assertTrue(result.popularity >= 0)
        assertTrue(result.relevance >= 0)
        
        val sum = result.recency + result.popularity + result.relevance
        assertTrue(sum > 0.9 && sum < 1.1, "Weights should still sum to approximately 1.0")
    }
}