package com.welcomer.welcome.feed.repository

import com.welcomer.welcome.feed.service.FeedAnalyticsEvent
import com.welcomer.welcome.feed.service.FeedAnalyticsEventType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsRepositoryTest {

    private lateinit var repository: InMemoryAnalyticsRepository

    @BeforeEach
    fun setup() {
        repository = InMemoryAnalyticsRepository()
    }

    @Test
    fun `recordEvent should store event successfully`() = runBlocking {
        // Given
        val event = createMockEvent("user1", FeedAnalyticsEventType.FEED_VIEW)

        // When
        repository.recordEvent(event)

        // Then
        val stats = repository.getRepositoryStats()
        assertEquals(1L, stats.totalEvents)
        assertEquals(1L, stats.totalUsers)
    }

    @Test
    fun `recordEventsBatch should store multiple events`() = runBlocking {
        // Given
        val events = listOf(
            createMockEvent("user1", FeedAnalyticsEventType.FEED_VIEW),
            createMockEvent("user2", FeedAnalyticsEventType.ITEM_INTERACTION),
            createMockEvent("user1", FeedAnalyticsEventType.SESSION_METRICS)
        )

        // When
        repository.recordEventsBatch(events)

        // Then
        val stats = repository.getRepositoryStats()
        assertEquals(3L, stats.totalEvents)
        assertEquals(2L, stats.totalUsers) // user1 and user2
    }

    @Test
    fun `getEvents should return events within time range`() = runBlocking {
        // Given
        val now = Instant.now()
        val oneHourAgo = now.minus(1, ChronoUnit.HOURS)
        val twoHoursAgo = now.minus(2, ChronoUnit.HOURS)
        val threeHoursAgo = now.minus(3, ChronoUnit.HOURS)

        val events = listOf(
            createMockEvent("user1", FeedAnalyticsEventType.FEED_VIEW, twoHoursAgo),
            createMockEvent("user2", FeedAnalyticsEventType.ITEM_INTERACTION, oneHourAgo),
            createMockEvent("user3", FeedAnalyticsEventType.SESSION_METRICS, threeHoursAgo)
        )

        repository.recordEventsBatch(events)

        // When
        val result = repository.getEvents(twoHoursAgo.minus(5, ChronoUnit.MINUTES), now)

        // Then
        assertEquals(2, result.size) // Events from 2 hours ago and 1 hour ago
        assertTrue(result.any { it.userId == "user1" })
        assertTrue(result.any { it.userId == "user2" })
        assertTrue(result.none { it.userId == "user3" }) // 3 hours ago is outside range
    }

    @Test
    fun `getUserEvents should return events for specific user`() = runBlocking {
        // Given
        val now = Instant.now()
        val oneHourAgo = now.minus(1, ChronoUnit.HOURS)
        
        val events = listOf(
            createMockEvent("user1", FeedAnalyticsEventType.FEED_VIEW, oneHourAgo),
            createMockEvent("user2", FeedAnalyticsEventType.ITEM_INTERACTION, oneHourAgo),
            createMockEvent("user1", FeedAnalyticsEventType.SESSION_METRICS, oneHourAgo)
        )

        repository.recordEventsBatch(events)

        // When
        val result = repository.getUserEvents("user1", oneHourAgo.minus(5, ChronoUnit.MINUTES), now)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.userId == "user1" })
        assertTrue(result.any { it.eventType == FeedAnalyticsEventType.FEED_VIEW })
        assertTrue(result.any { it.eventType == FeedAnalyticsEventType.SESSION_METRICS })
    }

    @Test
    fun `getAggregatedMetrics should calculate metrics correctly`() = runBlocking {
        // Given
        val now = Instant.now()
        val oneHourAgo = now.minus(1, ChronoUnit.HOURS)
        
        val events = listOf(
            createMockEvent("user1", FeedAnalyticsEventType.FEED_VIEW, oneHourAgo),
            createMockEvent("user2", FeedAnalyticsEventType.FEED_VIEW, oneHourAgo),
            createMockEvent("user1", FeedAnalyticsEventType.ITEM_INTERACTION, oneHourAgo, mapOf("interactionType" to "like")),
            createMockEvent("user2", FeedAnalyticsEventType.ITEM_INTERACTION, oneHourAgo, mapOf("interactionType" to "click")),
            createMockEvent("user3", FeedAnalyticsEventType.SESSION_METRICS, oneHourAgo)
        )

        repository.recordEventsBatch(events)

        // When
        val metrics = repository.getAggregatedMetrics(oneHourAgo.minus(5, ChronoUnit.MINUTES), now)

        // Then
        assertEquals(5L, metrics["total_events"])
        assertEquals(2L, metrics["feed_views"])
        assertEquals(2L, metrics["item_interactions"])
        assertEquals(1L, metrics["sessions"])
        assertEquals(1L, metrics["item_likes"])
        assertEquals(1L, metrics["item_clicks"])
        assertEquals(3L, metrics["unique_users"])
    }

    @Test
    fun `incrementMetric should update daily metrics`() = runBlocking {
        // Given
        val today = Instant.now()
        val metricName = "test_metric"
        
        // When
        repository.incrementMetric(today, metricName, 5)
        repository.incrementMetric(today, metricName, 3)
        
        // Then
        val dailyMetrics = repository.getDailyMetrics(today)
        assertEquals(8L, dailyMetrics[metricName])
    }

    @Test
    fun `getUniqueUsers should return unique user set`() = runBlocking {
        // Given
        val now = Instant.now()
        val oneHourAgo = now.minus(1, ChronoUnit.HOURS)
        
        val events = listOf(
            createMockEvent("user1", FeedAnalyticsEventType.FEED_VIEW, oneHourAgo),
            createMockEvent("user2", FeedAnalyticsEventType.ITEM_INTERACTION, oneHourAgo),
            createMockEvent("user1", FeedAnalyticsEventType.SESSION_METRICS, oneHourAgo), // Duplicate user
            createMockEvent("user3", FeedAnalyticsEventType.FEED_VIEW, oneHourAgo)
        )

        repository.recordEventsBatch(events)

        // When
        val uniqueUsers = repository.getUniqueUsers(oneHourAgo.minus(5, ChronoUnit.MINUTES), now)

        // Then
        assertEquals(3, uniqueUsers.size)
        assertTrue(uniqueUsers.contains("user1"))
        assertTrue(uniqueUsers.contains("user2"))
        assertTrue(uniqueUsers.contains("user3"))
    }

    @Test
    fun `getEventCountByType should group events by type`() = runBlocking {
        // Given
        val now = Instant.now()
        val oneHourAgo = now.minus(1, ChronoUnit.HOURS)
        
        val events = listOf(
            createMockEvent("user1", FeedAnalyticsEventType.FEED_VIEW, oneHourAgo),
            createMockEvent("user2", FeedAnalyticsEventType.FEED_VIEW, oneHourAgo),
            createMockEvent("user1", FeedAnalyticsEventType.ITEM_INTERACTION, oneHourAgo),
            createMockEvent("user2", FeedAnalyticsEventType.SESSION_METRICS, oneHourAgo),
            createMockEvent("user3", FeedAnalyticsEventType.FEED_GENERATION, oneHourAgo)
        )

        repository.recordEventsBatch(events)

        // When
        val eventCounts = repository.getEventCountByType(oneHourAgo.minus(5, ChronoUnit.MINUTES), now)

        // Then
        assertEquals(2L, eventCounts["FEED_VIEW"])
        assertEquals(1L, eventCounts["ITEM_INTERACTION"])
        assertEquals(1L, eventCounts["SESSION_METRICS"])
        assertEquals(1L, eventCounts["FEED_GENERATION"])
    }

    @Test
    fun `deleteEventsBefore should remove old events and clean indices`() = runBlocking {
        // Given
        val now = Instant.now()
        val oneHourAgo = now.minus(1, ChronoUnit.HOURS)
        val twoHoursAgo = now.minus(2, ChronoUnit.HOURS)
        val threeHoursAgo = now.minus(3, ChronoUnit.HOURS)
        
        val events = listOf(
            createMockEvent("user1", FeedAnalyticsEventType.FEED_VIEW, threeHoursAgo),
            createMockEvent("user2", FeedAnalyticsEventType.ITEM_INTERACTION, twoHoursAgo),
            createMockEvent("user1", FeedAnalyticsEventType.SESSION_METRICS, oneHourAgo)
        )

        repository.recordEventsBatch(events)
        
        val initialStats = repository.getRepositoryStats()
        assertEquals(3L, initialStats.totalEvents)

        // When
        val deletedCount = repository.deleteEventsBefore(twoHoursAgo.plus(30, ChronoUnit.MINUTES))

        // Then
        assertEquals(2, deletedCount) // 2 events older than cutoff
        val finalStats = repository.getRepositoryStats()
        assertEquals(1L, finalStats.totalEvents)
        
        // Verify the remaining event
        val remainingEvents = repository.getEvents(oneHourAgo.minus(5, ChronoUnit.MINUTES), now)
        assertEquals(1, remainingEvents.size)
        assertEquals("user1", remainingEvents[0].userId)
        assertEquals(FeedAnalyticsEventType.SESSION_METRICS, remainingEvents[0].eventType)
    }

    @Test
    fun `getItemEvents should return events for specific item`() = runBlocking {
        // Given
        val now = Instant.now()
        val oneHourAgo = now.minus(1, ChronoUnit.HOURS)
        
        val events = listOf(
            createMockEvent("user1", FeedAnalyticsEventType.ITEM_INTERACTION, oneHourAgo, itemId = "item1"),
            createMockEvent("user2", FeedAnalyticsEventType.ITEM_INTERACTION, oneHourAgo, itemId = "item2"),
            createMockEvent("user3", FeedAnalyticsEventType.ITEM_INTERACTION, oneHourAgo, itemId = "item1")
        )

        repository.recordEventsBatch(events)

        // When
        val item1Events = repository.getItemEvents("item1", oneHourAgo.minus(5, ChronoUnit.MINUTES), now)

        // Then
        assertEquals(2, item1Events.size)
        assertTrue(item1Events.all { it.itemId == "item1" })
        assertEquals(setOf("user1", "user3"), item1Events.map { it.userId }.toSet())
    }

    @Test
    fun `clearAll should reset repository to empty state`() = runBlocking {
        // Given
        val events = listOf(
            createMockEvent("user1", FeedAnalyticsEventType.FEED_VIEW),
            createMockEvent("user2", FeedAnalyticsEventType.ITEM_INTERACTION)
        )

        repository.recordEventsBatch(events)
        repository.incrementMetric(Instant.now(), "test_metric", 10)
        
        val initialStats = repository.getRepositoryStats()
        assertTrue(initialStats.totalEvents > 0)

        // When
        repository.clearAll()

        // Then
        val finalStats = repository.getRepositoryStats()
        assertEquals(0L, finalStats.totalEvents)
        assertEquals(0L, finalStats.totalUsers)
        assertEquals(0L, finalStats.timeSlots)
        assertEquals(0L, finalStats.dailyMetricDays)
    }

    @Test
    fun `events should be returned in chronological order`() = runBlocking {
        // Given
        val now = Instant.now()
        val times = listOf(
            now.minus(3, ChronoUnit.HOURS),
            now.minus(1, ChronoUnit.HOURS),
            now.minus(2, ChronoUnit.HOURS)
        )
        
        val events = times.mapIndexed { index, timestamp ->
            createMockEvent("user$index", FeedAnalyticsEventType.FEED_VIEW, timestamp)
        }

        repository.recordEventsBatch(events)

        // When
        val result = repository.getEvents(now.minus(4, ChronoUnit.HOURS), now)

        // Then
        assertEquals(3, result.size)
        // Events should be sorted by timestamp (oldest first)
        assertTrue(result[0].timestamp.isBefore(result[1].timestamp))
        assertTrue(result[1].timestamp.isBefore(result[2].timestamp))
    }

    // Helper method for creating mock events
    private fun createMockEvent(
        userId: String,
        eventType: FeedAnalyticsEventType,
        timestamp: Instant = Instant.now(),
        metadata: Map<String, Any> = emptyMap(),
        itemId: String? = null
    ): FeedAnalyticsEvent {
        return FeedAnalyticsEvent(
            id = java.util.UUID.randomUUID().toString(),
            eventType = eventType,
            userId = userId,
            itemId = itemId,
            timestamp = timestamp,
            metadata = metadata
        )
    }
}