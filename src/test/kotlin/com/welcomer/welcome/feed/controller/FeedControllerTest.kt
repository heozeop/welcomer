package com.welcomer.welcome.feed.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.welcomer.welcome.feed.model.*
import com.welcomer.welcome.feed.service.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import java.time.Instant

class FeedControllerTest {

    private lateinit var feedGenerationService: FeedGenerationService
    private lateinit var cacheService: FeedCacheService
    private lateinit var performanceService: FeedPerformanceService
    private lateinit var feedController: FeedController
    private lateinit var mockRequest: MockHttpServletRequest

    @BeforeEach
    fun setup() {
        feedGenerationService = mock()
        cacheService = mock()
        performanceService = mock()
        feedController = FeedController(feedGenerationService, cacheService, performanceService)
        mockRequest = MockHttpServletRequest()
    }

    @Test
    fun `getFeed should return 401 when user ID is not in request attributes`() = runBlocking {
        // Don't set userId attribute to simulate unauthenticated request
        
        val response = feedController.getFeed(
            feedType = "home",
            request = mockRequest
        )
        
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertFalse(body!!.success)
        assertEquals("User authentication required", body.error)
    }

    @Test
    fun `getFeed should return 400 for invalid feed type`() = runBlocking {
        mockRequest.setAttribute("userId", "user123")
        
        val response = feedController.getFeed(
            feedType = "invalid_type",
            request = mockRequest
        )
        
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertFalse(body!!.success)
        assertTrue(body.error!!.contains("Invalid feed type"))
    }

    @Test
    fun `getFeed should return cached feed when available`(): Unit = runBlocking {
        val userId = "user123"
        val feedType = FeedType.HOME
        mockRequest.setAttribute("userId", userId)
        
        val cachedFeed = createMockGeneratedFeed(userId, feedType)
        whenever(cacheService.getCachedFeed(userId, feedType)).thenReturn(cachedFeed)
        
        val response = feedController.getFeed(
            feedType = "home",
            request = mockRequest
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertTrue(body!!.success)
        assertTrue(body.fromCache)
        
        verify(cacheService).getCachedFeed(userId, feedType)
        verify(feedGenerationService, never()).generateFeed(any())
    }

    @Test
    fun `getFeed should generate new feed when not cached`() = runBlocking {
        val userId = "user123"
        val feedType = FeedType.HOME
        mockRequest.setAttribute("userId", userId)
        
        whenever(cacheService.getCachedFeed(userId, feedType)).thenReturn(null)
        
        val generatedFeed = createMockGeneratedFeed(userId, feedType)
        whenever(feedGenerationService.generateFeed(any())).thenReturn(generatedFeed)
        whenever(performanceService.withPerformanceMonitoring<GeneratedFeed>(eq("generate_feed"), any())).doAnswer { invocation ->
            val block = invocation.getArgument<suspend () -> GeneratedFeed>(1)
            runBlocking { block() }
        }
        
        val response = feedController.getFeed(
            feedType = "home",
            request = mockRequest
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertTrue(body!!.success)
        assertFalse(body.fromCache)
        
        verify(cacheService).getCachedFeed(userId, feedType)
        verify(feedGenerationService).generateFeed(any())
        verify(cacheService).cacheFeed(generatedFeed)
    }

    @Test
    fun `getFeed should respect limit parameter and cap at 100`(): Unit = runBlocking {
        val userId = "user123"
        mockRequest.setAttribute("userId", userId)
        
        whenever(cacheService.getCachedFeed(any(), any())).thenReturn(null)
        
        val generatedFeed = createMockGeneratedFeed(userId, FeedType.HOME)
        whenever(feedGenerationService.generateFeed(any())).thenReturn(generatedFeed)
        whenever(performanceService.withPerformanceMonitoring<GeneratedFeed>(any(), any())).doAnswer { invocation ->
            val block = invocation.getArgument<suspend () -> GeneratedFeed>(1)
            runBlocking { block() }
        }
        
        val response = feedController.getFeed(
            feedType = "home",
            limit = 150, // Should be capped at 100
            request = mockRequest
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        
        verify(feedGenerationService).generateFeed(argThat { request ->
            request.limit == 100
        })
    }

    @Test
    fun `getFeed should include filters in feed request`(): Unit = runBlocking {
        val userId = "user123"
        mockRequest.setAttribute("userId", userId)
        
        whenever(cacheService.getCachedFeed(any(), any())).thenReturn(null)
        
        val generatedFeed = createMockGeneratedFeed(userId, FeedType.HOME)
        whenever(feedGenerationService.generateFeed(any())).thenReturn(generatedFeed)
        whenever(performanceService.withPerformanceMonitoring<GeneratedFeed>(any(), any())).doAnswer { invocation ->
            val block = invocation.getArgument<suspend () -> GeneratedFeed>(1)
            runBlocking { block() }
        }
        
        val response = feedController.getFeed(
            feedType = "home",
            contentType = "image",
            topic = "technology",
            request = mockRequest
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        
        verify(feedGenerationService).generateFeed(argThat { request ->
            request.parameters["content_type"] == "image" &&
            request.parameters["topic"] == "technology"
        })
    }

    @Test
    fun `getRefresh should validate since parameter`() = runBlocking {
        mockRequest.setAttribute("userId", "user123")
        
        val response = feedController.getRefresh(
            feedType = "home",
            since = "invalid-timestamp",
            request = mockRequest
        )
        
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertFalse(body!!.success)
        assertTrue(body.error!!.contains("Invalid since parameter"))
    }

    @Test
    fun `getRefresh should generate refresh feed with valid timestamp`(): Unit = runBlocking {
        val userId = "user123"
        mockRequest.setAttribute("userId", userId)
        
        val generatedFeed = createMockGeneratedFeed(userId, FeedType.HOME)
        whenever(feedGenerationService.generateFeed(any())).thenReturn(generatedFeed)
        whenever(performanceService.withPerformanceMonitoring<GeneratedFeed>(eq("refresh_feed"), any())).doAnswer { invocation ->
            val block = invocation.getArgument<suspend () -> GeneratedFeed>(1)
            runBlocking { block() }
        }
        
        val response = feedController.getRefresh(
            feedType = "home",
            since = "2024-01-01T12:00:00Z",
            request = mockRequest
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertTrue(body!!.success)
        
        verify(feedGenerationService).generateFeed(argThat { request ->
            request.refreshForced && request.parameters.containsKey("since")
        })
    }

    @Test
    fun `refreshFeed should invalidate cache`() = runBlocking {
        val userId = "user123"
        val feedType = FeedType.HOME
        mockRequest.setAttribute("userId", userId)
        
        val response = feedController.refreshFeed(
            feedType = "home",
            request = mockRequest
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertTrue(body!!.success)
        assertTrue(body.message.contains("cache invalidated"))
        
        verify(cacheService).invalidateFeed(userId, feedType)
    }

    @Test
    fun `getFeedTypes should return all available feed types`() {
        val response = feedController.getFeedTypes()
        
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals(5, body!!.feedTypes.size) // All FeedType enum values
        
        val homeType = body.feedTypes.find { it.type == "home" }
        assertNotNull(homeType)
        assertEquals("Home", homeType!!.displayName)
        assertTrue(homeType.description.contains("Personalized"))
    }

    @Test
    fun `getFeedStats should return cache and performance statistics`() {
        val cacheStats = CacheStats(
            feedCacheSize = 100,
            userPreferencesCacheSize = 50,
            popularityScoresCacheSize = 200,
            feedHitRate = 0.85,
            preferencesHitRate = 0.90,
            popularityHitRate = 0.75,
            totalMemoryUsed = 1048576L
        )
        
        val performanceMetrics = mapOf(
            "generate_feed_avg_ms" to 150.0,
            "generate_feed_count" to 1000L
        )
        
        whenever(cacheService.getCacheStats()).thenReturn(cacheStats)
        whenever(performanceService.getPerformanceMetrics()).thenReturn(performanceMetrics)
        
        val response = feedController.getFeedStats()
        
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals(cacheStats, body!!.cacheStats)
        assertEquals(performanceMetrics, body.performanceMetrics)
    }

    @Test
    fun `updateFeedPreferences should invalidate all feed caches`() = runBlocking {
        val userId = "user123"
        mockRequest.setAttribute("userId", userId)
        
        val preferences = UpdatePreferencesRequest(
            interests = listOf("technology", "kotlin"),
            preferredContentTypes = setOf("text", "image")
        )
        
        val response = feedController.updateFeedPreferences(preferences, mockRequest)
        
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertTrue(body!!.success)
        
        // Verify cache invalidation for all feed types
        FeedType.values().forEach { feedType ->
            verify(cacheService).invalidateFeed(userId, feedType)
        }
    }

    // Helper method to create mock GeneratedFeed
    private fun createMockGeneratedFeed(userId: String, feedType: FeedType): GeneratedFeed {
        return GeneratedFeed(
            userId = userId,
            feedType = feedType,
            entries = emptyList(),
            metadata = FeedMetadata(
                algorithmId = "test_algorithm",
                algorithmVersion = "1.0.0",
                generationDuration = 100L,
                contentCount = 0,
                candidateCount = 0,
                parameters = emptyMap()
            ),
            nextCursor = null,
            hasMore = false
        )
    }
}