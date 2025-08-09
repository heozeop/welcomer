package com.welcomer.welcome.feed.service

import com.welcomer.welcome.engagement.repository.EngagementRepository
import com.welcomer.welcome.feed.model.*
import com.welcomer.welcome.feed.repository.AnalyticsRepository
import com.welcomer.welcome.ingestion.model.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FeedAnalyticsServiceTest {

    @Mock
    private lateinit var analyticsRepository: AnalyticsRepository

    @Mock
    private lateinit var engagementRepository: EngagementRepository

    private lateinit var feedAnalyticsService: FeedAnalyticsService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        feedAnalyticsService = FeedAnalyticsService(analyticsRepository, engagementRepository)
    }

    @Test
    fun `recordFeedView should record feed view event with correct metadata`() = runBlocking {
        // Given
        val userId = "user123"
        val feedItems = listOf(
            createMockFeedEntry("item1", "algorithm1"),
            createMockFeedEntry("item2", "algorithm1")
        )
        val metadata = mapOf("source" to "home_feed")

        // When
        feedAnalyticsService.recordFeedView(userId, feedItems, metadata)

        // Then
        verify(analyticsRepository).recordEvent(argThat { event ->
            event.eventType == FeedAnalyticsEventType.FEED_VIEW &&
            event.userId == userId &&
            event.metadata["feedSize"] == 2 &&
            event.metadata["source"] == "home_feed"
        })
        
        verify(analyticsRepository).incrementMetric(any(), eq("feed_views"), eq(1))
    }

    @Test
    fun `recordItemInteraction should record interaction event and update metrics`() = runBlocking {
        // Given
        val userId = "user123"
        val itemId = "item456"
        val interactionType = "like"
        val metadata = mapOf("position" to 5)

        // When
        feedAnalyticsService.recordItemInteraction(userId, itemId, interactionType, metadata)

        // Then
        verify(analyticsRepository).recordEvent(argThat { event ->
            event.eventType == FeedAnalyticsEventType.ITEM_INTERACTION &&
            event.userId == userId &&
            event.itemId == itemId &&
            event.metadata["interactionType"] == "like" &&
            event.metadata["position"] == 5
        })
        
        verify(analyticsRepository).incrementMetric(any(), eq("item_likes"), eq(1))
    }

    @Test
    fun `recordSessionMetrics should record session data correctly`() = runBlocking {
        // Given
        val userId = "user123"
        val sessionDuration = 300000L // 5 minutes
        val itemsViewed = 10
        val interactionsCount = 3

        // When
        feedAnalyticsService.recordSessionMetrics(userId, sessionDuration, itemsViewed, interactionsCount)

        // Then
        verify(analyticsRepository).recordEvent(argThat { event ->
            event.eventType == FeedAnalyticsEventType.SESSION_METRICS &&
            event.userId == userId &&
            event.metadata["sessionDuration"] == sessionDuration &&
            event.metadata["itemsViewed"] == itemsViewed &&
            event.metadata["interactionsCount"] == interactionsCount
        })
        
        verify(analyticsRepository).incrementMetric(any(), eq("user_sessions"), eq(1))
    }

    @Test
    fun `recordFeedGeneration should record generation performance metrics`() = runBlocking {
        // Given
        val userId = "user123"
        val algorithmId = "ml_v2"
        val generationDuration = 150L
        val candidateCount = 1000
        val finalCount = 50

        // When
        feedAnalyticsService.recordFeedGeneration(userId, algorithmId, generationDuration, candidateCount, finalCount)

        // Then
        verify(analyticsRepository).recordEvent(argThat { event ->
            event.eventType == FeedAnalyticsEventType.FEED_GENERATION &&
            event.userId == userId &&
            event.metadata["algorithmId"] == algorithmId &&
            event.metadata["generationDuration"] == generationDuration &&
            event.metadata["candidateCount"] == candidateCount &&
            event.metadata["finalCount"] == finalCount
        })
        
        verify(analyticsRepository).incrementMetric(any(), eq("feed_generations"), eq(1))
    }

    @Test
    fun `generateDailyReport should create comprehensive report`() = runBlocking {
        // Given
        val today = Instant.now().epochSecond / 86400 * 86400 // Start of today
        val metrics = mapOf(
            "feed_views" to 1000L,
            "item_clicks" to 150L,
            "item_likes" to 80L,
            "item_shares" to 20L,
            "item_comments" to 30L
        )
        
        whenever(analyticsRepository.getAggregatedMetrics(any(), any())).thenReturn(metrics)
        whenever(engagementRepository.getTopEngagedContent(any(), any(), eq(10)))
            .thenReturn(listOf("content1" to 100L, "content2" to 80L))
        whenever(analyticsRepository.getUniqueUsers(any(), any())).thenReturn(setOf("user1", "user2", "user3"))

        // When
        val report = feedAnalyticsService.generateDailyReport()

        // Then
        assertNotNull(report)
        assertEquals(1000L, report.totalViews)
        assertTrue(report.averageEngagementRate > 0)
        assertEquals(2, report.topPerformingContent.size)
        verify(analyticsRepository).getAggregatedMetrics(any(), any())
    }

    @Test
    fun `getAnalytics should return comprehensive analytics data`() = runBlocking {
        // Given
        val startTime = Instant.now().minusSeconds(86400)
        val endTime = Instant.now()
        val mockEvents = listOf(
            createMockAnalyticsEvent("user1", FeedAnalyticsEventType.FEED_VIEW),
            createMockAnalyticsEvent("user1", FeedAnalyticsEventType.ITEM_INTERACTION),
            createMockAnalyticsEvent("user2", FeedAnalyticsEventType.FEED_VIEW)
        )
        val mockEngagement = listOf("content1" to 50L, "content2" to 30L)
        
        whenever(analyticsRepository.getEvents(startTime, endTime)).thenReturn(mockEvents)
        whenever(engagementRepository.getTopEngagedContent(startTime, endTime, 50)).thenReturn(mockEngagement)

        // When
        val analytics = feedAnalyticsService.getAnalytics(startTime, endTime)

        // Then
        assertEquals(startTime, analytics.periodStart)
        assertEquals(endTime, analytics.periodEnd)
        assertEquals(2L, analytics.totalFeedViews)
        assertEquals(1L, analytics.totalInteractions)
        assertEquals(2L, analytics.uniqueActiveUsers)
        assertEquals(0.5, analytics.averageEngagementRate)
        assertEquals(2, analytics.topContent.size)
    }

    @Test
    fun `getDashboardData should return real-time dashboard metrics`() = runBlocking {
        // Given
        val mockHourlyEvents = listOf(
            createMockAnalyticsEvent("user1", FeedAnalyticsEventType.FEED_VIEW),
            createMockAnalyticsEvent("user2", FeedAnalyticsEventType.ITEM_INTERACTION)
        )
        val mockDailyEvents = listOf(
            createMockAnalyticsEvent("user1", FeedAnalyticsEventType.FEED_VIEW),
            createMockAnalyticsEvent("user2", FeedAnalyticsEventType.SESSION_METRICS),
            createMockAnalyticsEvent("user3", FeedAnalyticsEventType.ITEM_INTERACTION)
        )
        val mockActiveUsers = setOf("user1", "user2")
        val mockTrendingContent = listOf("trending1" to 100L, "trending2" to 80L)
        
        whenever(analyticsRepository.getEvents(any(), any()))
            .thenReturn(mockHourlyEvents)
            .thenReturn(mockDailyEvents)
        whenever(analyticsRepository.getUniqueUsers(any(), any())).thenReturn(mockActiveUsers)
        whenever(engagementRepository.getTopEngagedContent(any(), any(), eq(20))).thenReturn(mockTrendingContent)

        // When
        val dashboard = feedAnalyticsService.getDashboardData()

        // Then
        assertNotNull(dashboard)
        assertEquals(2L, dashboard.currentActiveUsers)
        assertEquals(1L, dashboard.hourlyMetrics.feedViews)
        assertEquals(1L, dashboard.hourlyMetrics.interactions)
        assertEquals(1L, dashboard.dailyMetrics.feedViews)
        assertEquals(1L, dashboard.dailyMetrics.sessions)
        assertEquals(2, dashboard.trendingContent.size)
        assertEquals(SystemHealthStatus.HEALTHY, dashboard.systemHealth)
    }

    @Test
    fun `recordItemInteraction should handle different interaction types correctly`() = runBlocking {
        // Test different interaction types
        val interactionTypes = listOf("click", "like", "share", "comment", "bookmark")
        val expectedMetrics = listOf("item_clicks", "item_likes", "item_shares", "item_comments", "item_bookmarks")
        
        interactionTypes.forEachIndexed { index, interactionType ->
            // When
            feedAnalyticsService.recordItemInteraction("user1", "item1", interactionType)
            
            // Then
            verify(analyticsRepository).incrementMetric(any(), eq(expectedMetrics[index]), eq(1))
        }
    }

    @Test
    fun `engagement rate calculation should handle zero views`() = runBlocking {
        // Given
        val metrics = mapOf(
            "feed_views" to 0L,
            "item_clicks" to 5L,
            "item_likes" to 3L
        )
        
        whenever(analyticsRepository.getAggregatedMetrics(any(), any())).thenReturn(metrics)
        whenever(engagementRepository.getTopEngagedContent(any(), any(), any())).thenReturn(emptyList())
        whenever(analyticsRepository.getUniqueUsers(any(), any())).thenReturn(setOf("user1", "user2"))

        // When
        val report = feedAnalyticsService.generateDailyReport()

        // Then
        assertEquals(0.0, report.averageEngagementRate)
    }

    // Helper methods for creating mock objects

    private fun createMockFeedEntry(itemId: String, algorithmId: String): FeedEntry {
        return FeedEntry(
            id = itemId,
            content = createMockStoredContent(itemId),
            score = 0.8,
            rank = 1,
            reasons = emptyList(),
            sourceType = FeedSourceType.RECOMMENDATION,
            algorithmId = algorithmId
        )
    }

    private fun createMockStoredContent(contentId: String): StoredContent {
        return StoredContent(
            id = contentId,
            authorId = "author_$contentId",
            contentType = ContentType.TEXT,
            textContent = "This is test content for $contentId",
            visibility = ContentVisibility.PUBLIC,
            status = ContentStatus.PUBLISHED,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            publishedAt = Instant.now()
        )
    }

    private fun createMockAnalyticsEvent(userId: String, eventType: FeedAnalyticsEventType): FeedAnalyticsEvent {
        return FeedAnalyticsEvent(
            eventType = eventType,
            userId = userId,
            timestamp = Instant.now(),
            metadata = emptyMap()
        )
    }
}