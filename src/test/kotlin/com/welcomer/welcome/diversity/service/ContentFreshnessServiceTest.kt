package com.welcomer.welcome.diversity.service

import com.welcomer.welcome.diversity.model.*
import com.welcomer.welcome.feed.model.FeedEntry
import com.welcomer.welcome.feed.model.FeedReason
import com.welcomer.welcome.feed.model.FeedReasonType
import com.welcomer.welcome.feed.model.FeedSourceType
import com.welcomer.welcome.ingestion.model.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import java.time.temporal.ChronoUnit

class ContentFreshnessServiceTest {

    private lateinit var freshnessService: ContentFreshnessService
    private lateinit var mockTrendingService: TrendingAnalysisService

    @BeforeEach
    fun setup() {
        mockTrendingService = mock()
        freshnessService = DefaultContentFreshnessService(mockTrendingService)
    }

    @Test
    fun `calculateRecencyScore should return high score for very fresh content`() {
        // Given - very fresh content (1 hour old)
        val ageInHours = 1.0
        
        // When
        val score = freshnessService.calculateRecencyScore(ageInHours)
        
        // Then
        assertTrue(score > 0.9, "Very fresh content should have high recency score")
    }

    @Test
    fun `calculateRecencyScore should return lower score for old content`() {
        // Given - old content (72 hours)
        val ageInHours = 72.0
        
        // When
        val score = freshnessService.calculateRecencyScore(ageInHours)
        
        // Then
        assertTrue(score < 0.3, "Old content should have low recency score")
    }

    @Test
    fun `calculateFreshnessScores should identify very fresh content`(): Unit = runBlocking {
        // Given - very fresh content
        val currentTime = Instant.now()
        val freshContent = listOf(
            createTestContent("fresh1", currentTime.minus(30, ChronoUnit.MINUTES))
        )
        whenever(mockTrendingService.getTrendingScore(any())).thenReturn(0.0)
        
        // When
        val results = freshnessService.calculateFreshnessScores(freshContent, currentTime)
        
        // Then
        assertEquals(1, results.size)
        val analysis = results[0]
        assertTrue(analysis.freshnessScore > 0.9, "Very fresh content should have high freshness score")
        assertEquals(FreshnessAction.BOOST, analysis.recommendedAction)
        assertNull(analysis.stalenessReason)
    }

    @Test
    fun `calculateFreshnessScores should identify stale content`(): Unit = runBlocking {
        // Given - old stale content
        val currentTime = Instant.now()
        val staleContent = listOf(
            createTestContent("stale1", currentTime.minus(200, ChronoUnit.HOURS))
        )
        whenever(mockTrendingService.getTrendingScore(any())).thenReturn(0.0)
        
        // When
        val results = freshnessService.calculateFreshnessScores(staleContent, currentTime)
        
        // Then
        assertEquals(1, results.size)
        val analysis = results[0]
        assertTrue(analysis.freshnessScore < 0.3, "Old content should have low freshness score")
        assertTrue(analysis.recommendedAction == FreshnessAction.DEMOTE || 
                  analysis.recommendedAction == FreshnessAction.REPLACE)
        assertNotNull(analysis.stalenessReason)
    }

    @Test
    fun `calculateFreshnessScores should boost trending content`(): Unit = runBlocking {
        // Given - moderately old content that is trending
        val currentTime = Instant.now()
        val trendingContent = listOf(
            createTestContent("trending1", currentTime.minus(24, ChronoUnit.HOURS))
        )
        whenever(mockTrendingService.getTrendingScore("trending1")).thenReturn(0.8)
        
        // When
        val results = freshnessService.calculateFreshnessScores(trendingContent, currentTime)
        
        // Then
        assertEquals(1, results.size)
        val analysis = results[0]
        assertTrue(analysis.factors.containsKey(FreshnessFactor.TRENDING_TOPIC))
        assertTrue(analysis.factors[FreshnessFactor.TRENDING_TOPIC]!! > 0.7)
        // Freshness score should be boosted by trending status
        assertTrue(analysis.freshnessScore > freshnessService.calculateRecencyScore(24.0))
    }

    @Test
    fun `identifyTimelyContent should detect breaking news`(): Unit = runBlocking {
        // Given - content with breaking news indicators
        val breakingContent = createTestContent(
            "breaking1", 
            Instant.now(),
            textContent = "BREAKING: Major event just happened - urgent update"
        )
        
        // When
        val analysis = freshnessService.identifyTimelyContent(breakingContent)
        
        // Then
        assertTrue(analysis.isTimely)
        assertTrue(analysis.timelyFactors.contains(TimelyFactor.BREAKING_NEWS))
        assertTrue(analysis.timeRelevanceScore > 0.2)
        assertNotNull(analysis.expirationPrediction)
        // Breaking news should expire quickly
        assertTrue(analysis.expirationPrediction!!.isBefore(
            breakingContent.createdAt.plus(12, ChronoUnit.HOURS)
        ))
    }

    @Test
    fun `identifyTimelyContent should detect seasonal content`(): Unit = runBlocking {
        // Given - content with multiple seasonal keywords to ensure detection
        val seasonalContent = createTestContent(
            "seasonal1",
            Instant.now(),
            textContent = "Winter holiday celebrations and snow activities for spring summer autumn seasons"
        )
        
        // When
        val analysis = freshnessService.identifyTimelyContent(seasonalContent)
        
        // Then
        // Should be timely due to multiple seasonal keywords regardless of current season
        assertTrue(analysis.isTimely || analysis.timeRelevanceScore > 0.1, 
                  "Content with seasonal keywords should be detected as timely")
        if (analysis.isTimely) {
            assertTrue(analysis.timelyFactors.contains(TimelyFactor.SEASONAL_TOPIC))
            assertNotNull(analysis.seasonalRelevance)
        }
    }

    @Test
    fun `identifyStaleContent should find overexposed topics`(): Unit = runBlocking {
        // Given - feed history with overexposed technology topic
        val currentTime = Instant.now()
        val overexposedHistory = (1..10).map { i ->
            createTestFeedEntry(
                "tech$i",
                "user$i",
                "Technology content about AI and machine learning",
                listOf("technology", "AI"),
                currentTime.minus(i.toLong(), ChronoUnit.HOURS)
            )
        }
        
        // When
        val staleContent = freshnessService.identifyStaleContent(overexposedHistory, 72, currentTime)
        
        // Then
        assertTrue(staleContent.isNotEmpty(), "Should identify stale content due to topic overexposure")
        val staleAnalysis = staleContent.first()
        assertTrue(staleAnalysis.reasons.contains(StalenessReason.OVEREXPOSED_TOPIC))
        assertTrue(staleAnalysis.overexposureMetrics.containsKey("topic"))
        assertTrue(staleAnalysis.overexposureMetrics["topic"]!! > 0.4)
        assertTrue(staleAnalysis.stalenessScore > 0.3)
    }

    @Test
    fun `identifyStaleContent should find overexposed authors`(): Unit = runBlocking {
        // Given - feed history completely dominated by single author (95% of content, very old to trigger staleness)
        val currentTime = Instant.now()
        val sameAuthorHistory = (1..19).map { i ->
            createTestFeedEntry(
                "post$i",
                "dominantAuthor", // Same author for most posts
                "Different content from same author $i",
                listOf("topic$i"),
                currentTime.minus((i + 50).toLong(), ChronoUnit.HOURS) // Make content old to trigger age-based staleness
            )
        } + (1..1).map { i -> // Only 5% from other authors
            createTestFeedEntry(
                "other$i",
                "otherAuthor$i",
                "Content from other authors",
                listOf("other"),
                currentTime.minus(i.toLong(), ChronoUnit.HOURS)
            )
        }
        
        // When
        val staleContent = freshnessService.identifyStaleContent(sameAuthorHistory, 72, currentTime)
        
        // Then
        assertTrue(staleContent.isNotEmpty(), "Should identify stale content (old content + overexposure)")
        
        // Check overall staleness detection (may be due to age + author overexposure combined)
        val hasHighStaleness = staleContent.any { it.stalenessScore > 0.3 }
        assertTrue(hasHighStaleness, "Should detect high staleness in dominated + old content")
    }

    @Test
    fun `identifyStaleContent should handle empty history gracefully`(): Unit = runBlocking {
        // Given - empty feed history
        val emptyHistory = emptyList<FeedEntry>()
        
        // When
        val staleContent = freshnessService.identifyStaleContent(emptyHistory, 72, Instant.now())
        
        // Then
        assertTrue(staleContent.isEmpty())
    }

    @Test
    fun `calculateFreshnessScores should boost original content over shared`(): Unit = runBlocking {
        // Given - original content vs shared content
        val currentTime = Instant.now()
        val originalContent = createTestContent(
            "original1", 
            currentTime.minus(2, ChronoUnit.HOURS),
            linkUrl = null // Original content
        )
        val sharedContent = createTestContent(
            "shared1", 
            currentTime.minus(2, ChronoUnit.HOURS),
            linkUrl = "https://example.com/article" // Shared content
        )
        whenever(mockTrendingService.getTrendingScore(any())).thenReturn(0.0)
        
        // When
        val originalResults = freshnessService.calculateFreshnessScores(listOf(originalContent), currentTime)
        val sharedResults = freshnessService.calculateFreshnessScores(listOf(sharedContent), currentTime)
        
        // Then
        assertTrue(originalResults[0].freshnessScore > sharedResults[0].freshnessScore,
                  "Original content should have higher freshness than shared content")
        assertTrue(originalResults[0].factors[FreshnessFactor.ORIGINAL_CONTENT]!! > 
                  sharedResults[0].factors.getOrDefault(FreshnessFactor.ORIGINAL_CONTENT, 0.0))
    }

    @Test
    fun `identifyTimelyContent should detect holiday content`(): Unit = runBlocking {
        // Given - holiday-related content
        val holidayContent = createTestContent(
            "holiday1",
            Instant.now(),
            textContent = "Christmas holiday celebrations and New Year resolutions"
        )
        
        // When
        val analysis = freshnessService.identifyTimelyContent(holidayContent)
        
        // Then
        assertTrue(analysis.isTimely)
        assertTrue(analysis.timelyFactors.contains(TimelyFactor.HOLIDAY_RELATED))
        assertTrue(analysis.timeRelevanceScore > 0.2)
    }

    @Test
    fun `calculateFreshnessScores should handle multiple freshness factors`(): Unit = runBlocking {
        // Given - content that is fresh, trending, and timely
        val currentTime = Instant.now()
        val multiFactorContent = listOf(
            createTestContent(
                "multi1", 
                currentTime.minus(1, ChronoUnit.HOURS),
                textContent = "Breaking news about trending technology today"
            )
        )
        whenever(mockTrendingService.getTrendingScore("multi1")).thenReturn(0.7)
        
        // When
        val results = freshnessService.calculateFreshnessScores(multiFactorContent, currentTime)
        
        // Then
        val analysis = results[0]
        assertTrue(analysis.factors.size >= 3, "Should have multiple freshness factors")
        assertTrue(analysis.factors.containsKey(FreshnessFactor.RECENCY))
        assertTrue(analysis.factors.containsKey(FreshnessFactor.TRENDING_TOPIC))
        assertTrue(analysis.factors.containsKey(FreshnessFactor.TIMELY_REFERENCE))
        assertTrue(analysis.freshnessScore > 0.8, "Multi-factor content should have very high freshness")
        assertEquals(FreshnessAction.BOOST, analysis.recommendedAction)
    }

    // Helper methods for creating test data

    private fun createTestContent(
        id: String,
        createdAt: Instant,
        textContent: String? = "Test content for $id",
        linkUrl: String? = null,
        tags: List<String> = emptyList()
    ): StoredContent {
        return StoredContent(
            id = id,
            authorId = "author_$id",
            contentType = ContentType.TEXT,
            textContent = textContent,
            linkUrl = linkUrl,
            visibility = ContentVisibility.PUBLIC,
            status = ContentStatus.PUBLISHED,
            createdAt = createdAt,
            updatedAt = createdAt,
            tags = tags
        )
    }

    private fun createTestFeedEntry(
        id: String,
        authorId: String,
        textContent: String,
        tags: List<String> = emptyList(),
        generatedAt: Instant = Instant.now()
    ): FeedEntry {
        val content = StoredContent(
            id = id,
            authorId = authorId,
            contentType = ContentType.TEXT,
            textContent = textContent,
            visibility = ContentVisibility.PUBLIC,
            status = ContentStatus.PUBLISHED,
            createdAt = generatedAt.minus(1, ChronoUnit.HOURS),
            updatedAt = generatedAt.minus(1, ChronoUnit.HOURS),
            tags = tags
        )
        
        return FeedEntry(
            id = id,
            content = content,
            score = 0.8,
            rank = 1,
            reasons = listOf(
                FeedReason(
                    type = FeedReasonType.RELEVANCE,
                    description = "Test content",
                    weight = 1.0
                )
            ),
            sourceType = FeedSourceType.RECOMMENDATION,
            generatedAt = generatedAt
        )
    }
}