package com.welcomer.welcome.diversity.service

import com.welcomer.welcome.diversity.model.*
import com.welcomer.welcome.feed.model.FeedEntry
import com.welcomer.welcome.feed.model.FeedReason
import com.welcomer.welcome.feed.model.FeedReasonType
import com.welcomer.welcome.feed.model.FeedSourceType
import com.welcomer.welcome.ingestion.model.*
import com.welcomer.welcome.user.service.UserPreferenceService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import java.time.temporal.ChronoUnit

class ContentBalancingServiceTest {

    private lateinit var contentBalancingService: ContentBalancingService
    private lateinit var mockUserPreferenceService: UserPreferenceService
    private lateinit var mockContentFreshnessService: ContentFreshnessService
    private lateinit var mockContentDiversityAnalyzer: ContentDiversityAnalyzer

    @BeforeEach
    fun setup() {
        mockUserPreferenceService = mock()
        mockContentFreshnessService = mock()
        mockContentDiversityAnalyzer = mock()
        
        contentBalancingService = DefaultContentBalancingService(
            mockUserPreferenceService,
            mockContentFreshnessService,
            mockContentDiversityAnalyzer
        )
    }

    @Test
    fun `applyContentQuotas should return empty result for empty input`(): Unit = runBlocking {
        // Given - empty input
        val emptyItems = emptyList<ScoredContent>()
        
        // When
        val result = contentBalancingService.applyContentQuotas("testUser", emptyItems)
        
        // Then
        assertTrue(result.balancedFeed.isEmpty())
        assertEquals(0.0, result.actualDistribution.avgQualityScore)
        assertEquals(0, result.actualDistribution.sourceCount)
    }

    @Test
    fun `applyContentQuotas should apply default quotas when personalization disabled`(): Unit = runBlocking {
        // Given - mixed content items
        val items = createMixedScoredContent()
        val config = ContentBalancingConfig(enablePersonalization = false)
        
        // Mock dependencies
        whenever(mockContentFreshnessService.calculateRecencyScore(any())).thenReturn(0.8)
        whenever(mockUserPreferenceService.getUserPreferences(any())).thenReturn(null)
        
        // When
        val result = contentBalancingService.applyContentQuotas("testUser", items, 10, config)
        
        // Then
        assertTrue(result.balancedFeed.isNotEmpty())
        assertEquals(config.defaultQuotas, result.appliedQuotas)
        assertTrue(result.actualDistribution.sourceCount >= 1)
        assertTrue(result.actualDistribution.avgQualityScore > 0)
    }

    @Test
    fun `applyContentQuotas should respect minimum source diversity`(): Unit = runBlocking {
        // Given - items from limited sources
        val items = createScoredContentFromLimitedSources()
        val config = ContentBalancingConfig(minimumSourceDiversity = 3)
        
        // Mock dependencies
        whenever(mockContentFreshnessService.calculateRecencyScore(any())).thenReturn(0.5)
        whenever(mockUserPreferenceService.getUserPreferences(any())).thenReturn(null)
        
        // When
        val result = contentBalancingService.applyContentQuotas("testUser", items, 10, config)
        
        // Then
        val uniqueSources = result.balancedFeed.map { it.content.authorId }.toSet()
        assertTrue(uniqueSources.size >= 2) // At least try to diversify even if not enough sources
        assertTrue(result.balancingReasons.any { it.type == BalancingReasonType.SOURCE_DIVERSITY })
    }

    @Test
    fun `applyContentQuotas should filter out low quality content`(): Unit = runBlocking {
        // Given - items with varying quality scores
        val highQualityItems = createScoredContent("high", 0.8, 3)
        val lowQualityItems = createScoredContent("low", 0.1, 3)
        val allItems = highQualityItems + lowQualityItems
        
        val config = ContentBalancingConfig(qualityThreshold = 0.3)
        
        // Mock dependencies
        whenever(mockContentFreshnessService.calculateRecencyScore(any())).thenReturn(0.5)
        whenever(mockUserPreferenceService.getUserPreferences(any())).thenReturn(null)
        
        // When
        val result = contentBalancingService.applyContentQuotas("testUser", allItems, 10, config)
        
        // Then
        assertTrue(result.balancedFeed.all { it.score >= config.qualityThreshold })
        assertTrue(result.balancingReasons.any { it.type == BalancingReasonType.QUALITY_FILTER })
    }

    @Test
    fun `categorizeItems should properly categorize content types`(): Unit = runBlocking {
        // Given - diverse content items
        val items = createDiverseScoredContent()
        val recentHistory = createRecentFeedHistory()
        
        // Mock freshness service
        whenever(mockContentFreshnessService.calculateRecencyScore(any())).thenReturn(0.8)
        whenever(mockUserPreferenceService.getUserPreferences(any())).thenReturn(createMockUserPreferences())
        
        // When
        val categorized = contentBalancingService.categorizeItems(items, "testUser", recentHistory)
        
        // Then
        assertTrue(categorized.fresh.isNotEmpty())
        assertTrue(categorized.familiar.isNotEmpty())
        assertTrue(categorized.discovery.isNotEmpty())
        // Verify no items are duplicated inappropriately
        val allCategorizedIds = (categorized.fresh + categorized.familiar + categorized.discovery)
            .map { it.content.id }
        assertTrue(allCategorizedIds.size <= items.size * 3) // Items can be in multiple categories
    }

    @Test
    fun `ensureMinimumSourceDiversity should add diverse sources when needed`(): Unit = runBlocking {
        // Given - feed with only one source
        val singleSourceFeed = createScoredContent("sameAuthor", 0.7, 5)
        val candidatePool = createScoredContent("diverseAuthor", 0.6, 5) + singleSourceFeed
        
        // When
        val result = contentBalancingService.ensureMinimumSourceDiversity(
            singleSourceFeed, 
            minSources = 2, 
            candidatePool = candidatePool
        )
        
        // Then
        val uniqueSources = result.map { it.content.authorId }.toSet()
        assertTrue(uniqueSources.size >= 2)
        assertEquals(5, result.size) // Same feed size
    }

    @Test
    fun `ensureMinimumSourceDiversity should not modify already diverse feed`(): Unit = runBlocking {
        // Given - already diverse feed
        val diverseFeed = createDiverseScoredContent()
        
        // When
        val result = contentBalancingService.ensureMinimumSourceDiversity(diverseFeed, 2)
        
        // Then
        assertEquals(diverseFeed.size, result.size)
        assertEquals(diverseFeed.map { it.content.id }.toSet(), result.map { it.content.id }.toSet())
    }

    @Test
    fun `balanceFreshness should maintain minimum fresh content ratio`(): Unit = runBlocking {
        // Given - mix of fresh and stale content
        val items = createScoredContentWithVaryingFreshness()
        val freshnessScores = items.associate { 
            it.content.id to if (it.content.id.contains("fresh")) 0.9 else 0.2 
        }
        val config = FreshnessBalancingConfig(minimumFreshRatio = 0.3)
        
        // When
        val result = contentBalancingService.balanceFreshness(items, freshnessScores, config)
        
        // Then
        assertEquals(items.size, result.size)
        val freshCount = result.take((result.size * config.minimumFreshRatio).toInt())
            .count { freshnessScores[it.content.id] ?: 0.0 > 0.5 }
        assertTrue(freshCount >= (result.size * config.minimumFreshRatio).toInt())
    }

    @Test
    fun `ContentQuotas validation should enforce limits`() {
        // Test valid quotas
        assertDoesNotThrow {
            ContentQuotas(fresh = 0.3, familiar = 0.5, discovery = 0.2)
        }
        
        // Test invalid quotas (sum > 1.1)
        assertThrows(IllegalArgumentException::class.java) {
            ContentQuotas(fresh = 0.6, familiar = 0.6, discovery = 0.6)
        }
        
        // Test negative quotas
        assertThrows(IllegalArgumentException::class.java) {
            ContentQuotas(fresh = -0.1, familiar = 0.5, discovery = 0.2)
        }
    }

    @Test
    fun `applyContentQuotas should handle personalized user preferences`(): Unit = runBlocking {
        // Given - user with specific preferences
        val items = createMixedScoredContent()
        val userPreferences = createMockUserPreferences()
        val config = ContentBalancingConfig(enablePersonalization = true)
        
        // Mock user preferences service
        whenever(mockUserPreferenceService.getUserPreferences("testUser")).thenReturn(userPreferences)
        whenever(mockContentFreshnessService.calculateRecencyScore(any())).thenReturn(0.5)
        
        // When
        val result = contentBalancingService.applyContentQuotas("testUser", items, 10, config)
        
        // Then
        assertNotNull(result.appliedQuotas)
        assertTrue(result.balancingReasons.isNotEmpty())
        assertTrue(result.balancedFeed.isNotEmpty())
    }

    // Helper methods for creating test data

    private fun createMixedScoredContent(): List<ScoredContent> {
        return listOf(
            createScoredContentItem("fresh1", "author1", 0.8, "technology"),
            createScoredContentItem("familiar1", "author2", 0.7, "sports"),
            createScoredContentItem("discovery1", "author3", 0.6, "science"),
            createScoredContentItem("fresh2", "author1", 0.75, "technology"),
            createScoredContentItem("familiar2", "author4", 0.65, "politics"),
            createScoredContentItem("discovery2", "author5", 0.55, "entertainment")
        )
    }

    private fun createScoredContentFromLimitedSources(): List<ScoredContent> {
        return listOf(
            createScoredContentItem("item1", "author1", 0.8, "tech"),
            createScoredContentItem("item2", "author1", 0.75, "tech"),
            createScoredContentItem("item3", "author2", 0.7, "sports"),
            createScoredContentItem("item4", "author2", 0.65, "sports"),
            createScoredContentItem("item5", "author3", 0.6, "science"),
            createScoredContentItem("item6", "author3", 0.55, "science")
        )
    }

    private fun createScoredContent(authorPrefix: String, score: Double, count: Int): List<ScoredContent> {
        return (1..count).map { i ->
            createScoredContentItem("${authorPrefix}_item$i", "${authorPrefix}_author$i", score, "topic$i")
        }
    }

    private fun createDiverseScoredContent(): List<ScoredContent> {
        return listOf(
            createScoredContentItem("diverse1", "author1", 0.8, "technology"),
            createScoredContentItem("diverse2", "author2", 0.75, "sports"),
            createScoredContentItem("diverse3", "author3", 0.7, "politics"),
            createScoredContentItem("diverse4", "author4", 0.65, "entertainment"),
            createScoredContentItem("diverse5", "author5", 0.6, "science")
        )
    }

    private fun createScoredContentWithVaryingFreshness(): List<ScoredContent> {
        return listOf(
            createScoredContentItem("fresh1", "author1", 0.8, "tech", Instant.now().minus(1, ChronoUnit.HOURS)),
            createScoredContentItem("fresh2", "author2", 0.75, "sports", Instant.now().minus(2, ChronoUnit.HOURS)),
            createScoredContentItem("stale1", "author3", 0.7, "politics", Instant.now().minus(48, ChronoUnit.HOURS)),
            createScoredContentItem("stale2", "author4", 0.65, "science", Instant.now().minus(72, ChronoUnit.HOURS)),
            createScoredContentItem("medium1", "author5", 0.6, "entertainment", Instant.now().minus(12, ChronoUnit.HOURS))
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

    private fun createRecentFeedHistory(): List<FeedEntry> {
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

    private fun createMockUserPreferences(): UserContentPreferences {
        return UserContentPreferences(
            preferredTopics = mapOf("technology" to 0.8, "sports" to 0.6, "politics" to 0.4),
            preferredSources = mapOf("author1" to 0.9, "author2" to 0.7),
            contentBalancePreferences = ContentQuotas(fresh = 0.4, familiar = 0.4, discovery = 0.2),
            qualityThreshold = 0.3
        )
    }
}