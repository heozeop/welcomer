package com.welcomer.welcome.diversity.service

import com.welcomer.welcome.diversity.model.*
import com.welcomer.welcome.feed.model.FeedEntry
import com.welcomer.welcome.feed.model.FeedReason
import com.welcomer.welcome.feed.model.FeedReasonType
import com.welcomer.welcome.feed.model.FeedSourceType
import com.welcomer.welcome.feed.repository.FeedHistoryRepository
import com.welcomer.welcome.ingestion.model.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import java.time.temporal.ChronoUnit

class ContentDiversityIntegrationServiceTest {

    private lateinit var integrationService: ContentDiversityIntegrationService
    private lateinit var mockContentDiversityAnalyzer: ContentDiversityAnalyzer
    private lateinit var mockContentFreshnessService: ContentFreshnessService
    private lateinit var mockEchoChamberPreventionService: EchoChamberPreventionService
    private lateinit var mockContentBalancingService: ContentBalancingService
    private lateinit var mockFeedHistoryRepository: FeedHistoryRepository

    @BeforeEach
    fun setup() {
        mockContentDiversityAnalyzer = mock()
        mockContentFreshnessService = mock()
        mockEchoChamberPreventionService = mock()
        mockContentBalancingService = mock()
        mockFeedHistoryRepository = mock()
        
        integrationService = DefaultContentDiversityIntegrationService(
            mockContentDiversityAnalyzer,
            mockContentFreshnessService,
            mockEchoChamberPreventionService,
            mockContentBalancingService,
            mockFeedHistoryRepository
        )
    }

    @Test
    fun `diversifyFeed should return empty result for empty input`(): Unit = runBlocking {
        // Given - empty candidate items
        val emptyItems = emptyList<ScoredContent>()
        
        // When
        val result = integrationService.diversifyFeed("testUser", emptyItems)
        
        // Then
        assertTrue(result.diversifiedFeed.isEmpty())
        assertEquals(0, result.processingStats.itemsProcessed)
        assertTrue(result.processingStats.totalProcessingTimeMs >= 0)
    }

    @Test
    fun `diversifyFeed should orchestrate all diversity components correctly`(): Unit = runBlocking {
        // Given - candidate items and mocked dependencies
        val candidateItems = createTestScoredContent()
        val recentHistory = createTestFeedHistory()
        val config = DiversityIntegrationConfig()
        
        // Mock repository
        whenever(mockFeedHistoryRepository.getRecentFeedHistory(anyOrNull(), any())).thenReturn(recentHistory)
        
        // Mock diversity analyzer
        val diversityResults = candidateItems.map { item ->
            DiversityAnalysisResult(
                contentId = item.content.id,
                overallDiversityScore = 0.7,
                dimensionScores = mapOf(DiversityDimension.TOPIC to 0.7),
                recommendations = emptyList()
            )
        }
        whenever(mockContentDiversityAnalyzer.calculateDiversityScores(any(), any(), any()))
            .thenReturn(diversityResults)
        
        // Mock freshness service
        whenever(mockContentFreshnessService.calculateRecencyScore(any())).thenReturn(0.8)
        
        // Mock echo chamber service
        whenever(mockEchoChamberPreventionService.applyEchoChamberPrevention(anyOrNull(), any(), any(), any()))
            .thenReturn(candidateItems)
        
        // Mock balancing service
        val balancingResult = BalancedFeedResult(
            balancedFeed = candidateItems,
            appliedQuotas = ContentQuotas(),
            actualDistribution = BalancedContentDistribution(0.3, 0.5, 0.2, 0.15, 0.25, 3, 5, 0.7),
            qualityMetrics = QualityMetrics(0.7, 0.1, 0.7, 0.8, 0.6),
            balancingReasons = emptyList()
        )
        whenever(mockContentBalancingService.applyContentQuotas(anyOrNull(), any(), any(), any()))
            .thenReturn(balancingResult)
        
        // When
        val result = integrationService.diversifyFeed("testUser", candidateItems, 20, config)
        
        // Then
        assertFalse(result.diversifiedFeed.isEmpty())
        assertTrue(result.diversityMetrics.overallDiversityScore >= 0)
        assertTrue(result.processingStats.totalProcessingTimeMs >= 0)
    }

    @Test
    fun `calculateEnhancedScores should combine diversity and freshness scores correctly`(): Unit = runBlocking {
        // Given - test items and history
        val candidateItems = createTestScoredContent()
        val recentHistory = createTestFeedHistory()
        
        // Mock diversity analyzer
        val diversityResults = candidateItems.map { item ->
            DiversityAnalysisResult(
                contentId = item.content.id,
                overallDiversityScore = 0.6,
                dimensionScores = mapOf(DiversityDimension.TOPIC to 0.6),
                recommendations = emptyList()
            )
        }
        whenever(mockContentDiversityAnalyzer.calculateDiversityScores(any(), any(), any()))
            .thenReturn(diversityResults)
        
        // Mock freshness service
        whenever(mockContentFreshnessService.calculateRecencyScore(any())).thenReturn(0.7)
        
        // When
        val enhancedItems = integrationService.calculateEnhancedScores("testUser", candidateItems, recentHistory)
        
        // Then
        assertEquals(candidateItems.size, enhancedItems.size)
        enhancedItems.forEach { enhanced ->
            assertEquals(0.6, enhanced.diversityScore, 0.001)
            assertEquals(0.7, enhanced.freshnessScore, 0.001)
            assertTrue(enhanced.diversityBoost > 1.0) // Should have diversity boost
            assertTrue(enhanced.freshnessBoost > 1.0) // Should have freshness boost
            assertTrue(enhanced.finalScore > enhanced.originalScore) // Score should be enhanced
            assertNotNull(enhanced.scoringBreakdown)
        }
    }

    @Test
    fun `diversifyFeed should handle exceptions gracefully and return fallback results`(): Unit = runBlocking {
        // Given - candidate items but dependencies that throw exceptions
        val candidateItems = createTestScoredContent()
        
        // Mock repository to throw exception
        whenever(mockFeedHistoryRepository.getRecentFeedHistory(anyOrNull(), any()))
            .thenThrow(RuntimeException("Database connection failed"))
        
        // When
        val result = integrationService.diversifyFeed("testUser", candidateItems)
        
        // Then
        assertEquals(candidateItems.size, result.diversifiedFeed.size)
        assertEquals(candidateItems.size, result.processingStats.itemsProcessed)
        assertTrue(result.processingStats.totalProcessingTimeMs >= 0)
        assertTrue(result.recommendations.isNotEmpty())
        assertTrue(result.recommendations.any { it.priority == RecommendationPriority.HIGH })
        
        // Verify fallback items have original scores
        result.diversifiedFeed.forEach { item ->
            assertEquals(item.originalScore, item.finalScore)
            assertTrue(item.metadata.containsKey("error"))
        }
    }

    @Test
    fun `diversifyFeed should skip disabled components correctly`(): Unit = runBlocking {
        // Given - config with some components disabled
        val candidateItems = createTestScoredContent()
        val recentHistory = createTestFeedHistory()
        val config = DiversityIntegrationConfig(
            enableEchoChamberPrevention = false,
            enableContentBalancing = false,
            enableDiversityBoosts = true,
            enableFreshnessBoosts = true
        )
        
        // Mock repository
        whenever(mockFeedHistoryRepository.getRecentFeedHistory(anyOrNull(), any())).thenReturn(recentHistory)
        
        // Mock diversity analyzer
        val diversityResults = candidateItems.map { item ->
            DiversityAnalysisResult(
                contentId = item.content.id,
                overallDiversityScore = 0.5,
                dimensionScores = mapOf(DiversityDimension.TOPIC to 0.5),
                recommendations = emptyList()
            )
        }
        whenever(mockContentDiversityAnalyzer.calculateDiversityScores(any(), any(), any()))
            .thenReturn(diversityResults)
        
        // Mock freshness service
        whenever(mockContentFreshnessService.calculateRecencyScore(any())).thenReturn(0.6)
        
        // When
        val result = integrationService.diversifyFeed("testUser", candidateItems, 20, config)
        
        // Then
        assertFalse(result.diversifiedFeed.isEmpty())
        
        // Verify disabled services were not called
        verify(mockEchoChamberPreventionService, never()).applyEchoChamberPrevention(any(), any(), any(), any())
        verify(mockContentBalancingService, never()).applyContentQuotas(any(), any(), any(), any())
        
        // Verify enabled services were called
        verify(mockContentDiversityAnalyzer).calculateDiversityScores(any(), any(), any())
        verify(mockContentFreshnessService, atLeast(candidateItems.size)).calculateRecencyScore(any())
    }

    @Test
    fun `diversifyFeed should generate appropriate system recommendations`(): Unit = runBlocking {
        // Given - candidate items that will result in low diversity metrics
        val candidateItems = createLimitedDiversityContent()
        val recentHistory = createTestFeedHistory()
        
        // Mock repository
        whenever(mockFeedHistoryRepository.getRecentFeedHistory(anyOrNull(), any())).thenReturn(recentHistory)
        
        // Mock diversity analyzer with low scores
        val diversityResults = candidateItems.map { item ->
            DiversityAnalysisResult(
                contentId = item.content.id,
                overallDiversityScore = 0.2, // Low diversity
                dimensionScores = mapOf(DiversityDimension.TOPIC to 0.2),
                recommendations = emptyList()
            )
        }
        whenever(mockContentDiversityAnalyzer.calculateDiversityScores(any(), any(), any()))
            .thenReturn(diversityResults)
        
        // Mock freshness service with low scores
        whenever(mockContentFreshnessService.calculateRecencyScore(any())).thenReturn(0.1) // Low freshness
        
        // Mock other services
        whenever(mockEchoChamberPreventionService.applyEchoChamberPrevention(anyOrNull(), any(), any(), any()))
            .thenReturn(candidateItems)
        
        val balancingResult = BalancedFeedResult(
            balancedFeed = candidateItems,
            appliedQuotas = ContentQuotas(),
            actualDistribution = BalancedContentDistribution(0.1, 0.8, 0.1, 0.05, 0.1, 2, 2, 0.3),
            qualityMetrics = QualityMetrics(0.3, 0.1, 0.2, 0.1, 0.2),
            balancingReasons = emptyList()
        )
        whenever(mockContentBalancingService.applyContentQuotas(anyOrNull(), any(), any(), any()))
            .thenReturn(balancingResult)
        
        // When
        val result = integrationService.diversifyFeed("testUser", candidateItems)
        
        // Then
        assertTrue(result.recommendations.isNotEmpty())
        assertTrue(result.recommendations.any { it.type == SystemRecommendationType.DIVERSIFY_CONTENT_SOURCES })
        assertTrue(result.recommendations.any { it.type == SystemRecommendationType.ADJUST_FRESHNESS_WEIGHTS })
        assertTrue(result.recommendations.any { it.type == SystemRecommendationType.INCREASE_CONTENT_POOL })
    }

    @Test
    fun `logDiversityMetrics should not throw exceptions`(): Unit = runBlocking {
        // Given - enhanced feed items
        val enhancedItems = createTestScoredContent().map { item ->
            EnhancedScoredContent(
                content = item.content,
                originalScore = item.score,
                finalScore = item.score * 1.2,
                diversityScore = 0.7,
                freshnessScore = 0.8
            )
        }
        
        // When & Then - should not throw
        assertDoesNotThrow {
            runBlocking {
                integrationService.logDiversityMetrics("testUser", enhancedItems)
            }
        }
    }

    // Helper methods for creating test data

    private fun createTestScoredContent(): List<ScoredContent> {
        return listOf(
            createScoredContentItem("item1", "author1", 0.8, "technology"),
            createScoredContentItem("item2", "author2", 0.7, "sports"),
            createScoredContentItem("item3", "author3", 0.6, "politics"),
            createScoredContentItem("item4", "author4", 0.5, "entertainment"),
            createScoredContentItem("item5", "author5", 0.4, "science")
        )
    }

    private fun createLimitedDiversityContent(): List<ScoredContent> {
        return listOf(
            createScoredContentItem("limited1", "author1", 0.8, "technology"),
            createScoredContentItem("limited2", "author1", 0.7, "technology"),
            createScoredContentItem("limited3", "author2", 0.6, "technology"),
            createScoredContentItem("limited4", "author2", 0.5, "technology")
        )
    }

    private fun createScoredContentItem(
        id: String,
        authorId: String,
        score: Double,
        tag: String,
        createdAt: Instant = Instant.now().minus((1..24).random().toLong(), ChronoUnit.HOURS)
    ): ScoredContent {
        val content = StoredContent(
            id = id,
            authorId = authorId,
            contentType = ContentType.TEXT,
            textContent = "Test content for $id",
            visibility = ContentVisibility.PUBLIC,
            status = ContentStatus.PUBLISHED,
            createdAt = createdAt,
            updatedAt = createdAt,
            tags = listOf(tag)
        )
        
        return ScoredContent(
            content = content,
            score = score
        )
    }

    private fun createTestFeedHistory(): List<FeedEntry> {
        return listOf(
            createTestFeedEntry("history1", "author1", "Historical content 1", listOf("technology")),
            createTestFeedEntry("history2", "author2", "Historical content 2", listOf("sports")),
            createTestFeedEntry("history3", "author3", "Historical content 3", listOf("politics"))
        )
    }

    private fun createTestFeedEntry(
        id: String,
        authorId: String,
        textContent: String,
        tags: List<String>,
        generatedAt: Instant = Instant.now().minus((1..48).random().toLong(), ChronoUnit.HOURS)
    ): FeedEntry {
        val content = StoredContent(
            id = id,
            authorId = authorId,
            contentType = ContentType.TEXT,
            textContent = textContent,
            visibility = ContentVisibility.PUBLIC,
            status = ContentStatus.PUBLISHED,
            createdAt = generatedAt.minus((1..24).random().toLong(), ChronoUnit.HOURS),
            updatedAt = generatedAt.minus((1..24).random().toLong(), ChronoUnit.HOURS),
            tags = tags
        )
        
        return FeedEntry(
            id = id,
            content = content,
            score = 0.7,
            rank = 1,
            reasons = listOf(
                FeedReason(
                    type = FeedReasonType.RELEVANCE,
                    description = "Test reason",
                    weight = 1.0
                )
            ),
            sourceType = FeedSourceType.RECOMMENDATION,
            generatedAt = generatedAt
        )
    }
}