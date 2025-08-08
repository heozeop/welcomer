package com.welcomer.welcome.personalization.service

import com.welcomer.welcome.engagement.model.EngagementType
import com.welcomer.welcome.ingestion.model.ContentStatus
import com.welcomer.welcome.ingestion.model.ContentType
import com.welcomer.welcome.ingestion.model.ContentVisibility
import com.welcomer.welcome.ingestion.model.StoredContent
import com.welcomer.welcome.personalization.model.UserPreferenceProfile
import com.welcomer.welcome.user.service.UserContextService
import com.welcomer.welcome.user.service.UserHistoryService
import com.welcomer.welcome.user.service.UserPreferenceService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Comprehensive tests for enhanced personalized scoring and ranking algorithms
 */
class PersonalizedScoringTest {

    private lateinit var userPreferenceService: UserPreferenceService
    private lateinit var userContextService: UserContextService
    private lateinit var userHistoryService: UserHistoryService
    private lateinit var topicRelevanceService: TopicRelevanceService
    private lateinit var sourceAffinityService: SourceAffinityService
    private lateinit var contextualRelevanceService: ContextualRelevanceService
    private lateinit var feedPersonalizationService: FeedPersonalizationService

    @BeforeEach
    fun setup() {
        userPreferenceService = mock()
        userContextService = mock()
        userHistoryService = mock()
        topicRelevanceService = mock()
        sourceAffinityService = mock()
        contextualRelevanceService = mock()

        feedPersonalizationService = DefaultFeedPersonalizationService(
            userPreferenceService = userPreferenceService,
            userContextService = userContextService,
            userHistoryService = userHistoryService,
            topicRelevanceService = topicRelevanceService,
            sourceAffinityService = sourceAffinityService,
            contextualRelevanceService = contextualRelevanceService
        )
    }

    @Test
    fun `personalized scoring should rank highly relevant items higher`(): Unit = runBlocking {
        // Given
        val userId = "user-123"
        val highRelevanceItem = createPersonalizableItem(
            contentId = "high-relevance",
            topics = listOf("machine-learning", "ai"),
            baseScore = 1.0
        )
        val lowRelevanceItem = createPersonalizableItem(
            contentId = "low-relevance", 
            topics = listOf("cooking", "recipes"),
            baseScore = 1.0
        )
        val feedItems = listOf(highRelevanceItem, lowRelevanceItem)

        // Mock user preferences
        val userPrefs = UserPreferenceProfile(
            userId = userId,
            topicInterests = mapOf("machine-learning" to 0.9, "ai" to 0.8),
            sourcePreferences = emptyMap(),
            contentTypePreferences = emptyMap(),
            timeBasedPreferences = emptyMap(),
            languagePreferences = emptyList(),
            diversityPreference = 0.5,
            freshnessPreference = 0.5,
            engagementHistory = emptyMap(),
            lastUpdated = Instant.now()
        )

        val userContext = UserContext(
            timeOfDay = 10,
            dayOfWeek = 2,
            deviceType = DeviceType.DESKTOP
        )

        val userHistory = listOf(
            createUserActivity("high-relevance-author", EngagementType.LIKE, 0.8),
            createUserActivity("high-relevance-author", EngagementType.SHARE, 0.9)
        )

        // Mock service responses
        whenever(userPreferenceService.getPreferences(eq(userId))).thenReturn(
            createUserModelProfile(userId, mapOf("machine-learning" to 0.9, "ai" to 0.8))
        )
        whenever(userContextService.getUserContext(eq(userId))).thenReturn(userContext)
        whenever(userHistoryService.getUserHistory(eq(userId), any())).thenReturn(userHistory)

        // Enhanced services mocked to return high scores for relevant item
        whenever(topicRelevanceService.calculateEnhancedTopicRelevance(
            eq(highRelevanceItem.topics), any(), any()
        )).thenReturn(createTopicRelevanceResult(0.9))
        
        whenever(topicRelevanceService.calculateEnhancedTopicRelevance(
            eq(lowRelevanceItem.topics), any(), any()
        )).thenReturn(createTopicRelevanceResult(0.2))

        whenever(sourceAffinityService.calculateAdvancedSourceAffinity(any(), any(), any()))
            .thenReturn(createSourceAffinityResult(0.7))

        whenever(contextualRelevanceService.calculateAdvancedContextualRelevance(any(), any(), any()))
            .thenReturn(createContextualRelevanceResult(0.6))

        // When
        val result = feedPersonalizationService.personalizeItems(userId, feedItems)

        // Then
        assertEquals(2, result.personalizedItems.size)
        
        // High relevance item should be ranked first
        assertEquals("high-relevance", result.personalizedItems[0].item.id)
        assertEquals("low-relevance", result.personalizedItems[1].item.id)
        
        // High relevance item should have higher final score
        assertTrue(result.personalizedItems[0].finalScore > result.personalizedItems[1].finalScore)
        
        // Check personalization factors
        val highRelevanceFactors = result.personalizedItems[0].personalizationFactors
        val lowRelevanceFactors = result.personalizedItems[1].personalizationFactors
        
        assertTrue(highRelevanceFactors.topicRelevance > lowRelevanceFactors.topicRelevance)
        assertEquals(1, result.personalizedItems[0].rank)
        assertEquals(2, result.personalizedItems[1].rank)
    }

    @Test
    fun `personalized scoring should handle quality amplification correctly`(): Unit = runBlocking {
        // Given
        val userId = "user-456"
        val highQualityItem = createPersonalizableItem(
            contentId = "high-quality",
            topics = listOf("technology"),
            baseScore = 3.0 // High quality
        )
        val lowQualityItem = createPersonalizableItem(
            contentId = "low-quality",
            topics = listOf("technology"),
            baseScore = 0.5 // Low quality
        )
        val feedItems = listOf(highQualityItem, lowQualityItem)

        // Mock same relevance for both items
        runBlocking { mockBasicServices(userId) }
        whenever(topicRelevanceService.calculateEnhancedTopicRelevance(any(), any(), any()))
            .thenReturn(createTopicRelevanceResult(0.8))

        // When
        val result = feedPersonalizationService.personalizeItems(userId, feedItems)

        // Then
        // High quality item should benefit more from personalization
        val highQualityFinalScore = result.personalizedItems.find { it.item.id == "high-quality" }?.finalScore ?: 0.0
        val lowQualityFinalScore = result.personalizedItems.find { it.item.id == "low-quality" }?.finalScore ?: 0.0
        
        assertTrue(highQualityFinalScore > lowQualityFinalScore)
        
        // Check quality amplification effect
        val highQualityImprovement = highQualityFinalScore / highQualityItem.baseScore
        val lowQualityImprovement = lowQualityFinalScore / lowQualityItem.baseScore
        
        // High quality item should have higher relative improvement
        assertTrue(highQualityImprovement > lowQualityImprovement)
    }

    @Test
    fun `personalized scoring should apply contextual boost for sweet spot scenarios`(): Unit = runBlocking {
        // Given
        val userId = "user-789"
        val sweetSpotItem = createPersonalizableItem(
            contentId = "sweet-spot",
            topics = listOf("ai", "programming"),
            baseScore = 1.5
        )
        val feedItems = listOf(sweetSpotItem)

        runBlocking { mockBasicServices(userId) }
        
        // Mock high scores across all factors (sweet spot scenario)
        whenever(topicRelevanceService.calculateEnhancedTopicRelevance(any(), any(), any()))
            .thenReturn(createTopicRelevanceResult(0.85))
        whenever(sourceAffinityService.calculateAdvancedSourceAffinity(any(), any(), any()))
            .thenReturn(createSourceAffinityResult(0.8))
        whenever(contextualRelevanceService.calculateAdvancedContextualRelevance(any(), any(), any()))
            .thenReturn(createContextualRelevanceResult(0.75))

        // When
        val result = feedPersonalizationService.personalizeItems(userId, feedItems)

        // Then
        val personalizedItem = result.personalizedItems[0]
        
        // Should have high personalization multiplier due to sweet spot bonus
        assertTrue(personalizedItem.personalizationFactors.personalizedMultiplier > 1.5)
        
        // Should have explanations about strong alignment
        assertTrue(personalizedItem.explanations.any { 
            it.contains("Highly personalized") || it.contains("all factors align") 
        })
    }

    @Test
    fun `personalized scoring should penalize disliked sources`(): Unit = runBlocking {
        // Given
        val userId = "user-dislike"
        val dislikedSourceItem = createPersonalizableItem(
            contentId = "disliked-source",
            authorId = "disliked-author",
            topics = listOf("interesting-topic"),
            baseScore = 2.0
        )
        val feedItems = listOf(dislikedSourceItem)

        runBlocking { mockBasicServices(userId) }
        
        // Mock high topic relevance but very low source affinity (disliked source)
        whenever(topicRelevanceService.calculateEnhancedTopicRelevance(any(), any(), any()))
            .thenReturn(createTopicRelevanceResult(0.9))
        whenever(sourceAffinityService.calculateAdvancedSourceAffinity(any(), any(), any()))
            .thenReturn(createSourceAffinityResult(0.1)) // Very low affinity
        whenever(contextualRelevanceService.calculateAdvancedContextualRelevance(any(), any(), any()))
            .thenReturn(createContextualRelevanceResult(0.6))

        // When
        val result = feedPersonalizationService.personalizeItems(userId, feedItems)

        // Then
        val personalizedItem = result.personalizedItems[0]
        
        // Despite high topic relevance, overall score should be penalized
        assertTrue(personalizedItem.finalScore < dislikedSourceItem.baseScore * 1.2)
        
        // Should have explanation about unfamiliar source
        assertTrue(personalizedItem.explanations.any { 
            it.contains("haven't interacted with much") 
        })
    }

    @Test
    fun `personalized scoring should handle diversity controls`(): Unit = runBlocking {
        // Given
        val userId = "user-diversity"
        val similarItems = (1..5).map { i ->
            createPersonalizableItem(
                contentId = "similar-$i",
                authorId = "same-author",
                topics = listOf("same-topic"),
                baseScore = 1.0
            )
        }
        
        val config = PersonalizationConfig(
            enableDiversityControls = true,
            maxSameTopicRatio = 0.4,
            maxSameSourceRatio = 0.3,
            diversityFactor = 0.2
        )

        runBlocking { mockBasicServices(userId) }
        whenever(topicRelevanceService.calculateEnhancedTopicRelevance(any(), any(), any()))
            .thenReturn(createTopicRelevanceResult(0.8))

        // When
        val result = feedPersonalizationService.personalizeItems(userId, similarItems, config)

        // Then
        // Some items should have diversity adjustments applied
        val diversityAdjustments = result.personalizedItems.map { 
            it.personalizationFactors.diversityAdjustment 
        }
        assertTrue(diversityAdjustments.any { it < 0 }) // Some penalties applied
        
        // Should have explanations about diversity
        assertTrue(result.personalizedItems.any { item ->
            item.explanations.any { it.contains("diversity") }
        })
    }

    @Test
    fun `personalized scoring should provide meaningful metrics`(): Unit = runBlocking {
        // Given
        val userId = "user-metrics"
        val diverseFeedItems = listOf(
            createPersonalizableItem(contentId = "tech1", topics = listOf("programming"), authorId = "author1"),
            createPersonalizableItem(contentId = "sci1", topics = listOf("science"), authorId = "author2"),
            createPersonalizableItem(contentId = "art1", topics = listOf("art"), authorId = "author1"),
            createPersonalizableItem(contentId = "tech2", topics = listOf("ai"), authorId = "author3")
        )

        runBlocking { mockBasicServices(userId) }
        whenever(topicRelevanceService.calculateEnhancedTopicRelevance(any(), any(), any()))
            .thenReturn(createTopicRelevanceResult(0.7))

        // When
        val result = feedPersonalizationService.personalizeItems(userId, diverseFeedItems)

        // Then
        val metrics = result.personalizationMetrics
        
        // Should track topic and source diversity
        assertTrue(metrics.topicCoverage >= 3) // At least 3 different topics
        assertTrue(metrics.sourceDiversity >= 2) // At least 2 different authors
        
        // Should have reasonable average personalization score
        assertTrue(metrics.averagePersonalizationScore > 0.5)
        assertTrue(metrics.averagePersonalizationScore < 3.0)
        
        // Should show improvement over base scores
        assertTrue(metrics.personalizedVsBaseScoreImprovement >= 0.0)
        
        // Processing stats should be meaningful
        val stats = result.processingStats
        assertTrue(stats.totalProcessingTimeMs > 0)
        assertEquals(diverseFeedItems.size, stats.itemsProcessed)
    }

    @Test
    fun `personalized scoring should handle graceful degradation`(): Unit = runBlocking {
        // Given
        val userId = "user-error"
        val feedItems = listOf(
            createPersonalizableItem(contentId = "test1", baseScore = 1.5),
            createPersonalizableItem(contentId = "test2", baseScore = 2.0)
        )

        // Mock services to throw exceptions
        whenever(userPreferenceService.getPreferences(eq(userId))).thenThrow(RuntimeException("Service error"))

        // When
        val result = feedPersonalizationService.personalizeItems(userId, feedItems)

        // Then
        // Should return fallback results without crashing
        assertEquals(2, result.personalizedItems.size)
        
        // Should use base scores as final scores
        result.personalizedItems.forEach { item ->
            assertEquals(item.item.baseScore, item.finalScore, 0.001)
            assertTrue(item.explanations.any { it.contains("Personalization unavailable") })
        }
        
        // Metrics should still be calculated
        assertNotNull(result.personalizationMetrics)
        assertTrue(result.processingStats.totalProcessingTimeMs > 0)
    }

    // Helper methods

    private fun createPersonalizableItem(
        contentId: String,
        authorId: String = "default-author",
        topics: List<String> = listOf("general"),
        baseScore: Double = 1.0
    ): PersonalizableItem {
        val storedContent = StoredContent(
            id = contentId,
            authorId = authorId,
            contentType = ContentType.TEXT,
            textContent = "Test content",
            visibility = ContentVisibility.PUBLIC,
            status = ContentStatus.PUBLISHED,
            tags = topics,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        return PersonalizableItem(
            content = storedContent,
            baseScore = baseScore
        )
    }

    private fun createUserActivity(
        authorId: String,
        engagementType: EngagementType,
        engagementScore: Double,
        timestamp: Instant = Instant.now()
    ): UserActivity {
        return UserActivity(
            contentId = "content-${System.nanoTime()}",
            authorId = authorId,
            topics = listOf("topic"),
            engagementType = engagementType,
            engagementScore = engagementScore,
            timestamp = timestamp
        )
    }

    private fun createUserModelProfile(
        userId: String,
        topicInterests: Map<String, Double>
    ): com.welcomer.welcome.user.model.UserPreferenceProfile {
        return com.welcomer.welcome.user.model.UserPreferenceProfile(
            userId = userId,
            topicInterests = topicInterests,
            contentTypePreferences = emptyMap(),
            languagePreferences = emptyList(),
            followedAccounts = emptySet(),
            blockedUsers = emptySet(),
            blockedTopics = emptySet(),
            algorithmPreferences = emptyMap(),
            lastUpdated = Instant.now(),
            confidence = 0.8
        )
    }

    private suspend fun mockBasicServices(userId: String) {
        whenever(userPreferenceService.getPreferences(eq(userId))).thenReturn(
            createUserModelProfile(userId, mapOf("technology" to 0.8))
        )
        whenever(userContextService.getUserContext(eq(userId))).thenReturn(
            UserContext(
                timeOfDay = 14,
                dayOfWeek = 3,
                deviceType = DeviceType.DESKTOP
            )
        )
        whenever(userHistoryService.getUserHistory(eq(userId), any())).thenReturn(
            listOf(createUserActivity("some-author", EngagementType.LIKE, 0.7))
        )
        whenever(sourceAffinityService.calculateAdvancedSourceAffinity(any(), any(), any()))
            .thenReturn(createSourceAffinityResult(0.6))
        whenever(contextualRelevanceService.calculateAdvancedContextualRelevance(any(), any(), any()))
            .thenReturn(createContextualRelevanceResult(0.6))
    }

    private fun createTopicRelevanceResult(overallRelevance: Double): TopicRelevanceResult {
        return TopicRelevanceResult(
            overallRelevance = overallRelevance,
            topicBreakdown = emptyList(),
            dominantCategories = emptyList(),
            specificityScore = 0.5,
            diversityScore = 0.5,
            explanations = emptyList()
        )
    }

    private fun createSourceAffinityResult(overallAffinity: Double): SourceAffinityResult {
        return SourceAffinityResult(
            authorId = "test-author",
            overallAffinity = overallAffinity,
            engagementPattern = EngagementPattern(
                positiveEngagements = 5,
                negativeEngagements = 1,
                neutralEngagements = 2,
                averageEngagementScore = 0.7,
                engagementTrend = EngagementTrend.STABLE,
                dominantEngagementTypes = listOf(EngagementType.LIKE),
                engagementConsistency = 0.8,
                lastEngagementTime = Instant.now()
            ),
            temporalPattern = TemporalEngagementPattern(
                peakHours = listOf(9, 14),
                peakDays = listOf(2, 3),
                averageSessionGap = 24,
                engagementRegularity = 0.7,
                temporalTrend = TemporalTrend.CONSISTENT_ACTIVITY
            ),
            reliabilityScore = 0.8,
            consistencyScore = 0.8,
            recencyScore = 0.6,
            diversityScore = 0.7,
            totalInteractions = 8,
            explanation = "Test explanation",
            confidenceLevel = AffinityConfidence.MEDIUM
        )
    }

    private fun createContextualRelevanceResult(overallRelevance: Double): ContextualRelevanceResult {
        return ContextualRelevanceResult(
            overallRelevance = overallRelevance,
            temporalRelevance = TemporalRelevanceResult(
                timeOfDayScore = 0.7,
                dayOfWeekScore = 0.6,
                recencyScore = 0.8,
                peakTimeAlignment = 0.7,
                temporalTrend = TemporalContextTrend.WORKDAY_PATTERN,
                explanation = "Good timing"
            ),
            locationRelevance = LocationRelevanceResult(
                geographicRelevance = 0.5,
                timezoneAlignment = 0.5,
                localEventRelevance = 0.5,
                culturalRelevance = 0.5,
                explanation = "Standard location relevance"
            ),
            sessionRelevance = SessionRelevanceResult(
                sessionDurationAlignment = 0.7,
                previousActivityRelevance = 0.6,
                engagementPatternMatch = 0.6,
                contextualContinuity = 0.5,
                attentionSpanMatch = 0.7,
                explanation = "Good session alignment"
            ),
            deviceRelevance = DeviceRelevanceResult(
                contentFormatAlignment = 0.8,
                deviceCapabilityMatch = 0.8,
                interactionPatternMatch = 0.7,
                displayOptimization = 0.9,
                explanation = "Optimized for desktop"
            ),
            confidenceLevel = ContextualConfidence.MEDIUM,
            explanation = "Good contextual match",
            contextFactorsApplied = listOf("temporal", "device")
        )
    }
}