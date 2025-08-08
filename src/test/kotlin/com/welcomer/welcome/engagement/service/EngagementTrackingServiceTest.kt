package com.welcomer.welcome.engagement.service

import com.welcomer.welcome.engagement.event.EngagementEventPublisher
import com.welcomer.welcome.engagement.model.*
import com.welcomer.welcome.engagement.repository.EngagementRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

class EngagementTrackingServiceTest {

    private lateinit var engagementRepository: EngagementRepository
    private lateinit var metricsAggregator: MetricsAggregationService
    private lateinit var eventPublisher: EngagementEventPublisher
    private lateinit var trackingService: EngagementTrackingService

    @BeforeEach
    fun setup() {
        engagementRepository = mock()
        metricsAggregator = mock()
        eventPublisher = mock()
        trackingService = DefaultEngagementTrackingService(
            engagementRepository,
            metricsAggregator,
            eventPublisher
        )
    }

    @Test
    fun `trackEngagement should successfully track valid engagement`() = runBlocking {
        // Given
        val userId = "user123"
        val request = TrackEngagementRequest(
            contentId = "content456",
            engagementType = EngagementType.LIKE,
            metadata = emptyMap()
        )
        
        whenever(engagementRepository.record(any<EngagementEvent>())).thenReturn("engagement123")
        whenever(engagementRepository.existsSimilar(any(), any(), any(), any())).thenReturn(false)

        // When
        val response = trackingService.trackEngagement(userId, request)

        // Then
        assertTrue(response.success)
        assertEquals("engagement123", response.engagementId)
        verify(engagementRepository).record(any<EngagementEvent>())
        verify(metricsAggregator).updateMetrics(request.contentId, request.engagementType)
        verify(eventPublisher).publishEngagementEvent(any<EngagementEvent>())
    }

    @Test
    fun `validateEngagementData should reject invalid data`() {
        // When
        val result = trackingService.validateEngagementData("", "content123", EngagementType.LIKE, emptyMap())

        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("User ID is required"))
    }

    @Test
    fun `validateEngagementData should require duration for dwell time events`() {
        // When
        val result = trackingService.validateEngagementData(
            "user123", 
            "content456", 
            EngagementType.DWELL_TIME, 
            emptyMap()
        )

        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Duration is required for dwell time events"))
    }

    @Test
    fun `getUserEngagementHistory should return user events`() = runBlocking {
        // Given
        val userId = "user123"
        val events = listOf(
            createTestEvent(userId, "content1", EngagementType.VIEW),
            createTestEvent(userId, "content2", EngagementType.LIKE)
        )
        whenever(engagementRepository.findByUserId(eq(userId), isNull(), isNull(), eq(100)))
            .thenReturn(events)

        // When
        val history = trackingService.getUserEngagementHistory(userId)

        // Then
        assertEquals(2, history.size)
        assertEquals(events, history)
    }

    private fun createTestEvent(userId: String, contentId: String, type: EngagementType): EngagementEvent {
        return EngagementEvent(
            id = "event_${System.nanoTime()}",
            userId = userId,
            contentId = contentId,
            type = type,
            timestamp = Instant.now()
        )
    }
}