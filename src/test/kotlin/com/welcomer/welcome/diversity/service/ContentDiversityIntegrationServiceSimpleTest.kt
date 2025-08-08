package com.welcomer.welcome.diversity.service

import com.welcomer.welcome.diversity.model.*
import com.welcomer.welcome.feed.model.FeedEntry
import com.welcomer.welcome.feed.repository.InMemoryFeedHistoryRepository
import com.welcomer.welcome.ingestion.model.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Simplified tests for ContentDiversityIntegrationService that focus on core functionality
 * without complex mocking dependencies
 */
class ContentDiversityIntegrationServiceSimpleTest {

    private lateinit var feedHistoryRepository: InMemoryFeedHistoryRepository

    @BeforeEach
    fun setup() {
        feedHistoryRepository = InMemoryFeedHistoryRepository()
    }

    @Test
    fun `feed history repository should store and retrieve feed entries correctly`(): Unit = runBlocking {
        // Given
        val userId = "testUser"
        val feedEntry = createTestFeedEntry("entry1", "author1", "Test content")
        
        // When
        feedHistoryRepository.saveFeedEntry(userId, feedEntry)
        val retrievedHistory = feedHistoryRepository.getRecentFeedHistory(userId, 10)
        
        // Then
        assertEquals(1, retrievedHistory.size)
        assertEquals(feedEntry.id, retrievedHistory[0].id)
        assertEquals(feedEntry.content.textContent, retrievedHistory[0].content.textContent)
    }

    @Test
    fun `feed history repository should handle multiple entries correctly`(): Unit = runBlocking {
        // Given
        val userId = "testUser"
        val feedEntries = listOf(
            createTestFeedEntry("entry1", "author1", "Content 1"),
            createTestFeedEntry("entry2", "author2", "Content 2"),
            createTestFeedEntry("entry3", "author3", "Content 3")
        )
        
        // When
        feedHistoryRepository.saveFeedEntries(userId, feedEntries)
        val retrievedHistory = feedHistoryRepository.getRecentFeedHistory(userId, 10)
        
        // Then
        assertEquals(3, retrievedHistory.size)
        assertTrue(retrievedHistory.all { entry -> 
            feedEntries.any { it.id == entry.id }
        })
    }

    @Test
    fun `feed history repository should limit history size`(): Unit = runBlocking {
        // Given
        val userId = "testUser"
        val manyEntries = (1..50).map { i ->
            createTestFeedEntry("entry$i", "author$i", "Content $i")
        }
        
        // When
        feedHistoryRepository.saveFeedEntries(userId, manyEntries)
        val retrievedHistory = feedHistoryRepository.getRecentFeedHistory(userId, 20)
        
        // Then
        assertEquals(20, retrievedHistory.size)
        // Should be sorted by generation time (most recent first)
        assertTrue(retrievedHistory[0].generatedAt.isAfter(retrievedHistory.last().generatedAt) ||
                  retrievedHistory[0].generatedAt == retrievedHistory.last().generatedAt)
    }

    @Test
    fun `feed history repository should detect seen content correctly`(): Unit = runBlocking {
        // Given
        val userId = "testUser"
        val feedEntry = createTestFeedEntry("seenContent", "author1", "Seen content")
        
        // When
        feedHistoryRepository.saveFeedEntry(userId, feedEntry)
        val hasSeen = feedHistoryRepository.hasSeenContent(userId, "seenContent", 24)
        val hasNotSeen = feedHistoryRepository.hasSeenContent(userId, "unseenContent", 24)
        
        // Then
        assertTrue(hasSeen)
        assertFalse(hasNotSeen)
    }

    @Test
    fun `feed history repository should handle time-based queries correctly`(): Unit = runBlocking {
        // Given
        val userId = "testUser"
        val now = Instant.now()
        val oldEntry = createTestFeedEntry("old", "author1", "Old content", 
            now.minus(2, ChronoUnit.DAYS))
        val recentEntry = createTestFeedEntry("recent", "author2", "Recent content", 
            now.minus(1, ChronoUnit.HOURS))
        
        feedHistoryRepository.saveFeedEntry(userId, oldEntry)
        feedHistoryRepository.saveFeedEntry(userId, recentEntry)
        
        // When
        val allHistory = feedHistoryRepository.getRecentFeedHistory(userId, 10)
        val recentHistory = feedHistoryRepository.getRecentFeedHistory(
            userId, 10, now.minus(12, ChronoUnit.HOURS)
        )
        
        // Then
        assertEquals(2, allHistory.size)
        assertEquals(1, recentHistory.size)
        assertEquals("recent", recentHistory[0].id)
    }

    @Test
    fun `feed history repository should delete old history correctly`(): Unit = runBlocking {
        // Given
        val userId = "testUser"
        val now = Instant.now()
        val oldEntry = createTestFeedEntry("old", "author1", "Old content", 
            now.minus(10, ChronoUnit.DAYS))
        val recentEntry = createTestFeedEntry("recent", "author2", "Recent content", now)
        
        feedHistoryRepository.saveFeedEntry(userId, oldEntry)
        feedHistoryRepository.saveFeedEntry(userId, recentEntry)
        
        // When
        val deleted = feedHistoryRepository.deleteOldHistory(userId, now.minus(5, ChronoUnit.DAYS))
        val remainingHistory = feedHistoryRepository.getRecentFeedHistory(userId, 10)
        
        // Then
        assertTrue(deleted)
        assertEquals(1, remainingHistory.size)
        assertEquals("recent", remainingHistory[0].id)
    }

    @Test
    fun `enhanced scored content should maintain score relationships correctly`() {
        // Given
        val content = createTestContent("test1", "author1", "Test content")
        val originalScore = 0.7
        val diversityScore = 0.8
        val freshnessScore = 0.6
        
        // When
        val enhanced = EnhancedScoredContent(
            content = content,
            originalScore = originalScore,
            finalScore = originalScore * 1.2,
            diversityScore = diversityScore,
            freshnessScore = freshnessScore,
            diversityBoost = 1.1,
            freshnessBoost = 1.05
        )
        
        // Then
        assertEquals(originalScore, enhanced.originalScore)
        assertTrue(enhanced.finalScore > enhanced.originalScore)
        assertEquals(diversityScore, enhanced.diversityScore)
        assertEquals(freshnessScore, enhanced.freshnessScore)
        assertTrue(enhanced.diversityBoost > 1.0)
        assertTrue(enhanced.freshnessBoost > 1.0)
    }

    @Test
    fun `diversity integration config should have sensible defaults`() {
        // Given & When
        val config = DiversityIntegrationConfig()
        
        // Then
        assertTrue(config.enableDiversityBoosts)
        assertTrue(config.enableFreshnessBoosts)
        assertTrue(config.enableEchoChamberPrevention)
        assertTrue(config.enableContentBalancing)
        assertTrue(config.diversityBoostMultiplier > 0)
        assertTrue(config.freshnessBoostMultiplier > 0)
        assertTrue(config.maxHistorySize > 0)
        assertTrue(config.enableMetricsLogging)
    }

    @Test
    fun `system recommendations should have proper priorities and types`() {
        // Given
        val highPriorityRec = SystemRecommendation(
            type = SystemRecommendationType.DIVERSIFY_CONTENT_SOURCES,
            priority = RecommendationPriority.HIGH,
            description = "High priority recommendation",
            expectedImpact = 0.8,
            actionRequired = "Take immediate action"
        )
        
        val mediumPriorityRec = SystemRecommendation(
            type = SystemRecommendationType.ADJUST_FRESHNESS_WEIGHTS,
            priority = RecommendationPriority.MEDIUM,
            description = "Medium priority recommendation",
            expectedImpact = 0.5,
            actionRequired = "Schedule for next iteration"
        )
        
        // When & Then
        assertEquals(SystemRecommendationType.DIVERSIFY_CONTENT_SOURCES, highPriorityRec.type)
        assertEquals(RecommendationPriority.HIGH, highPriorityRec.priority)
        assertTrue(highPriorityRec.expectedImpact > mediumPriorityRec.expectedImpact)
        assertNotNull(highPriorityRec.description)
        assertNotNull(highPriorityRec.actionRequired)
    }

    // Helper methods for creating test data

    private fun createTestContent(
        id: String,
        authorId: String,
        textContent: String
    ): StoredContent {
        return StoredContent(
            id = id,
            authorId = authorId,
            contentType = ContentType.TEXT,
            textContent = textContent,
            visibility = ContentVisibility.PUBLIC,
            status = ContentStatus.PUBLISHED,
            createdAt = Instant.now().minus((1..24).random().toLong(), ChronoUnit.HOURS),
            updatedAt = Instant.now(),
            tags = emptyList()
        )
    }

    private fun createTestFeedEntry(
        id: String,
        authorId: String,
        textContent: String,
        generatedAt: Instant = Instant.now().minus((1..24).random().toLong(), ChronoUnit.HOURS)
    ): FeedEntry {
        val content = createTestContent(id, authorId, textContent)
        
        return FeedEntry(
            id = id,
            content = content,
            score = 0.7,
            rank = 1,
            reasons = emptyList(),
            sourceType = com.welcomer.welcome.feed.model.FeedSourceType.RECOMMENDATION,
            generatedAt = generatedAt
        )
    }
}