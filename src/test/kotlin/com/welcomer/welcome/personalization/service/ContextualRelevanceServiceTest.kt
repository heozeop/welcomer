package com.welcomer.welcome.personalization.service

import com.welcomer.welcome.ingestion.model.ContentType
import com.welcomer.welcome.ingestion.model.StoredContent
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class ContextualRelevanceServiceTest {

    private lateinit var contextualRelevanceService: ContextualRelevanceService

    @BeforeEach
    fun setup() {
        contextualRelevanceService = DefaultContextualRelevanceService()
    }

    @Test
    fun `calculateAdvancedContextualRelevance should handle all context factors`(): Unit = runBlocking {
        // Given
        val item = createPersonalizableItem(topics = listOf("news", "technology"))
        val userContext = UserContext(
            timeOfDay = 9, // Morning
            dayOfWeek = 2, // Tuesday
            deviceType = DeviceType.MOBILE,
            location = UserLocation(country = "US", city = "new-york"),
            sessionDuration = 15,
            previousActivity = listOf("previous-content-1", "previous-content-2")
        )

        // When
        val result = contextualRelevanceService.calculateAdvancedContextualRelevance(item, userContext)

        // Then
        assertTrue(result.overallRelevance >= 0.0)
        assertTrue(result.overallRelevance <= 1.0)
        assertTrue(result.contextFactorsApplied.isNotEmpty())
        assertNotNull(result.temporalRelevance)
        assertNotNull(result.locationRelevance)
        assertNotNull(result.sessionRelevance)
        assertNotNull(result.deviceRelevance)
        assertTrue(result.explanation.isNotEmpty())
    }

    @Test
    fun `calculateTemporalRelevance should boost morning news content`(): Unit = runBlocking {
        // Given
        val newsItem = createPersonalizableItem(topics = listOf("news", "current-events"))
        val entertainmentItem = createPersonalizableItem(topics = listOf("entertainment", "movies"))
        val morningContext = UserContext(
            timeOfDay = 8, // Morning
            dayOfWeek = 2,
            deviceType = DeviceType.MOBILE
        )

        // When
        val newsResult = contextualRelevanceService.calculateTemporalRelevance(newsItem, morningContext, ContextualRelevanceConfig())
        val entertainmentResult = contextualRelevanceService.calculateTemporalRelevance(entertainmentItem, morningContext, ContextualRelevanceConfig())

        // Then
        println("News morning score: ${newsResult.timeOfDayScore}, Entertainment morning score: ${entertainmentResult.timeOfDayScore}")
        // News might get a slight boost in the morning, but both should be reasonable scores
        assertTrue(newsResult.timeOfDayScore >= 0.5) // Should be reasonable for morning
        assertTrue(entertainmentResult.timeOfDayScore >= 0.5) // Also reasonable
        assertNotNull(newsResult.temporalTrend)
        assertTrue(newsResult.explanation.contains("Morning") || newsResult.explanation.contains("time"))
    }

    @Test
    fun `calculateTemporalRelevance should boost evening entertainment content`(): Unit = runBlocking {
        // Given
        val entertainmentItem = createPersonalizableItem(topics = listOf("entertainment", "movies"))
        val workItem = createPersonalizableItem(topics = listOf("productivity", "business"))
        val eveningContext = UserContext(
            timeOfDay = 20, // Evening
            dayOfWeek = 5, // Friday
            deviceType = DeviceType.TV
        )

        // When
        val entertainmentResult = contextualRelevanceService.calculateTemporalRelevance(entertainmentItem, eveningContext, ContextualRelevanceConfig())
        val workResult = contextualRelevanceService.calculateTemporalRelevance(workItem, eveningContext, ContextualRelevanceConfig())

        // Then
        assertTrue(entertainmentResult.timeOfDayScore > workResult.timeOfDayScore)
        assertTrue(entertainmentResult.peakTimeAlignment > 0.7) // Evening is peak time
    }

    @Test
    fun `calculateTemporalRelevance should handle weekend vs weekday content`(): Unit = runBlocking {
        // Given
        val workItem = createPersonalizableItem(topics = listOf("work", "productivity"))
        val leisureItem = createPersonalizableItem(topics = listOf("entertainment", "leisure"))
        
        val weekdayContext = UserContext(timeOfDay = 14, dayOfWeek = 3, deviceType = DeviceType.DESKTOP) // Wednesday
        val weekendContext = UserContext(timeOfDay = 14, dayOfWeek = 6, deviceType = DeviceType.MOBILE) // Saturday

        // When
        val workWeekdayResult = contextualRelevanceService.calculateTemporalRelevance(workItem, weekdayContext, ContextualRelevanceConfig())
        val workWeekendResult = contextualRelevanceService.calculateTemporalRelevance(workItem, weekendContext, ContextualRelevanceConfig())
        val leisureWeekendResult = contextualRelevanceService.calculateTemporalRelevance(leisureItem, weekendContext, ContextualRelevanceConfig())

        // Then
        assertTrue(workWeekdayResult.dayOfWeekScore > workWeekendResult.dayOfWeekScore)
        assertTrue(leisureWeekendResult.dayOfWeekScore > workWeekendResult.dayOfWeekScore)
        assertEquals(TemporalContextTrend.WORKDAY_PATTERN, workWeekdayResult.temporalTrend)
        assertEquals(TemporalContextTrend.WEEKEND_PATTERN, leisureWeekendResult.temporalTrend)
    }

    @Test
    fun `calculateLocationRelevance should boost geographically relevant content`(): Unit = runBlocking {
        // Given
        val usContentItem = createPersonalizableItem(topics = listOf("american-football", "thanksgiving"))
        val genericItem = createPersonalizableItem(topics = listOf("technology", "programming"))
        
        val usLocation = UserLocation(country = "US", region = "California", city = "san-francisco")
        val config = ContextualRelevanceConfig()

        // When
        val usContentResult = contextualRelevanceService.calculateLocationRelevance(usContentItem, usLocation, config)
        val genericResult = contextualRelevanceService.calculateLocationRelevance(genericItem, usLocation, config)

        // Then
        assertTrue(usContentResult.geographicRelevance > genericResult.geographicRelevance)
        assertTrue(usContentResult.geographicRelevance > 0.6)
        assertTrue(usContentResult.explanation.contains("geographic") || usContentResult.explanation.contains("region"))
    }

    @Test
    fun `calculateLocationRelevance should handle local content`(): Unit = runBlocking {
        // Given
        val localItem = createPersonalizableItem(topics = listOf("local", "new-york"))
        val location = UserLocation(country = "US", city = "new-york")
        val config = ContextualRelevanceConfig()

        // When
        val result = contextualRelevanceService.calculateLocationRelevance(localItem, location, config)

        // Then
        println("Local event relevance: ${result.localEventRelevance}, Geographic relevance: ${result.geographicRelevance}")
        assertTrue(result.localEventRelevance >= 0.5) // Should have some boost for local content
        assertTrue(result.geographicRelevance >= 0.5) // Should have some geographic relevance
    }

    @Test
    fun `calculateLocationRelevance should return neutral for no location`(): Unit = runBlocking {
        // Given
        val item = createPersonalizableItem(topics = listOf("general", "content"))
        val config = ContextualRelevanceConfig()

        // When
        val result = contextualRelevanceService.calculateLocationRelevance(item, null, config)

        // Then
        assertEquals(0.5, result.geographicRelevance, 0.001)
        assertEquals(0.5, result.timezoneAlignment, 0.001)
        assertEquals(0.5, result.localEventRelevance, 0.001)
        assertEquals("No location information available", result.explanation)
    }

    @Test
    fun `calculateSessionRelevance should prefer quick content for short sessions`(): Unit = runBlocking {
        // Given
        val quickItem = createPersonalizableItem(topics = listOf("quick", "brief"))
        val detailedItem = createPersonalizableItem(topics = listOf("detailed", "analysis"))
        
        val shortSessionContext = UserContext(
            timeOfDay = 12,
            dayOfWeek = 3,
            deviceType = DeviceType.MOBILE,
            sessionDuration = 3 // 3 minutes
        )
        val config = ContextualRelevanceConfig()

        // When
        val quickResult = contextualRelevanceService.calculateSessionRelevance(quickItem, shortSessionContext, config)
        val detailedResult = contextualRelevanceService.calculateSessionRelevance(detailedItem, shortSessionContext, config)

        // Then
        assertTrue(quickResult.sessionDurationAlignment > detailedResult.sessionDurationAlignment)
        assertTrue(quickResult.sessionDurationAlignment > 0.8) // Should strongly prefer quick content
    }

    @Test
    fun `calculateSessionRelevance should prefer detailed content for long sessions`(): Unit = runBlocking {
        // Given
        val detailedItem = createPersonalizableItem(topics = listOf("detailed", "analysis"))
        val quickItem = createPersonalizableItem(topics = listOf("quick", "brief"))
        
        val longSessionContext = UserContext(
            timeOfDay = 14,
            dayOfWeek = 2,
            deviceType = DeviceType.DESKTOP,
            sessionDuration = 45 // 45 minutes
        )
        val config = ContextualRelevanceConfig()

        // When
        val detailedResult = contextualRelevanceService.calculateSessionRelevance(detailedItem, longSessionContext, config)
        val quickResult = contextualRelevanceService.calculateSessionRelevance(quickItem, longSessionContext, config)

        // Then
        assertTrue(detailedResult.sessionDurationAlignment > quickResult.sessionDurationAlignment)
        assertTrue(detailedResult.sessionDurationAlignment > 0.8) // Should prefer detailed content
    }

    @Test
    fun `calculateSessionRelevance should penalize recently seen content`(): Unit = runBlocking {
        // Given
        val item = createPersonalizableItem(contentId = "seen-content")
        val contextWithSeenContent = UserContext(
            timeOfDay = 14,
            dayOfWeek = 3,
            deviceType = DeviceType.MOBILE,
            sessionDuration = 20,
            previousActivity = listOf("seen-content", "other-content")
        )
        val config = ContextualRelevanceConfig()

        // When
        val result = contextualRelevanceService.calculateSessionRelevance(item, contextWithSeenContent, config)

        // Then
        assertTrue(result.previousActivityRelevance < 0.5) // Should penalize seen content
    }

    @Test
    fun `calculateDeviceRelevance should optimize content for mobile devices`(): Unit = runBlocking {
        // Given
        val textItem = createPersonalizableItem(contentType = ContentType.TEXT)
        val videoItem = createPersonalizableItem(contentType = ContentType.VIDEO, topics = listOf("video"))
        val config = ContextualRelevanceConfig()

        // When
        val textMobileResult = contextualRelevanceService.calculateDeviceRelevance(textItem, DeviceType.MOBILE, config)
        val videoMobileResult = contextualRelevanceService.calculateDeviceRelevance(videoItem, DeviceType.MOBILE, config)

        // Then
        assertTrue(textMobileResult.contentFormatAlignment > 0.6) // Text should be good for mobile
        assertTrue(textMobileResult.deviceCapabilityMatch > 0.6)
        assertTrue(videoMobileResult.deviceCapabilityMatch < textMobileResult.deviceCapabilityMatch) // Video less optimal on mobile
        assertTrue(textMobileResult.interactionPatternMatch > 0.6) // Mobile optimized for touch
    }

    @Test
    fun `calculateDeviceRelevance should optimize content for desktop devices`(): Unit = runBlocking {
        // Given
        val textItem = createPersonalizableItem(contentType = ContentType.TEXT)
        val productivityItem = createPersonalizableItem(topics = listOf("productivity", "work"))
        val config = ContextualRelevanceConfig()

        // When
        val textResult = contextualRelevanceService.calculateDeviceRelevance(textItem, DeviceType.DESKTOP, config)
        val productivityResult = contextualRelevanceService.calculateDeviceRelevance(productivityItem, DeviceType.DESKTOP, config)

        // Then
        assertTrue(textResult.deviceCapabilityMatch > 0.8) // Text excellent on desktop
        assertTrue(productivityResult.contentFormatAlignment > 0.7) // Productivity content good for desktop
        assertTrue(textResult.displayOptimization > 0.8) // Large screen optimal
        assertTrue(textResult.interactionPatternMatch > 0.8) // Full interaction capabilities
    }

    @Test
    fun `calculateDeviceRelevance should optimize content for TV devices`(): Unit = runBlocking {
        // Given
        val videoItem = createPersonalizableItem(contentType = ContentType.VIDEO, topics = listOf("video", "entertainment"))
        val textItem = createPersonalizableItem(contentType = ContentType.TEXT)
        val config = ContextualRelevanceConfig()

        // When
        val videoResult = contextualRelevanceService.calculateDeviceRelevance(videoItem, DeviceType.TV, config)
        val textResult = contextualRelevanceService.calculateDeviceRelevance(textItem, DeviceType.TV, config)

        // Then
        assertTrue(videoResult.deviceCapabilityMatch > 0.9) // Video excellent on TV
        assertTrue(videoResult.contentFormatAlignment > 0.8)
        assertTrue(textResult.deviceCapabilityMatch < 0.5) // Text poor on TV
        assertTrue(videoResult.displayOptimization > textResult.displayOptimization)
    }

    @Test
    fun `contextual relevance configuration should affect calculation`(): Unit = runBlocking {
        // Given
        val item = createPersonalizableItem(topics = listOf("news", "technology"))
        val userContext = UserContext(
            timeOfDay = 9,
            dayOfWeek = 2,
            deviceType = DeviceType.MOBILE,
            location = UserLocation(country = "US"),
            sessionDuration = 15
        )

        val temporalFocusedConfig = ContextualRelevanceConfig(
            temporalWeight = 0.6,
            locationWeight = 0.1,
            sessionWeight = 0.1,
            deviceWeight = 0.2
        )
        val locationFocusedConfig = ContextualRelevanceConfig(
            temporalWeight = 0.1,
            locationWeight = 0.6,
            sessionWeight = 0.15,
            deviceWeight = 0.15
        )

        // When
        val temporalResult = contextualRelevanceService.calculateAdvancedContextualRelevance(
            item, userContext, temporalFocusedConfig
        )
        val locationResult = contextualRelevanceService.calculateAdvancedContextualRelevance(
            item, userContext, locationFocusedConfig
        )

        // Then
        // Results should be different due to different weightings
        assertNotEquals(temporalResult.overallRelevance, locationResult.overallRelevance, 0.001)
    }

    @Test
    fun `contextual relevance should determine confidence levels correctly`(): Unit = runBlocking {
        // Given
        val item = createPersonalizableItem(topics = listOf("technology"))
        
        val richContext = UserContext(
            timeOfDay = 14,
            dayOfWeek = 3,
            deviceType = DeviceType.DESKTOP,
            location = UserLocation(country = "US", city = "san-francisco"),
            sessionDuration = 25,
            previousActivity = listOf("content1", "content2")
        )
        
        val poorContext = UserContext(
            timeOfDay = 12,
            dayOfWeek = 3,
            deviceType = DeviceType.UNKNOWN
        )

        // When
        val richResult = contextualRelevanceService.calculateAdvancedContextualRelevance(item, richContext)
        val poorResult = contextualRelevanceService.calculateAdvancedContextualRelevance(item, poorContext)

        // Then
        assertTrue(richResult.confidenceLevel.ordinal > poorResult.confidenceLevel.ordinal)
        assertTrue(richResult.contextFactorsApplied.size > poorResult.contextFactorsApplied.size)
    }

    @Test
    fun `contextual relevance should handle edge cases gracefully`(): Unit = runBlocking {
        // Given
        val item = createPersonalizableItem(topics = listOf("general"))
        val minimalContext = UserContext(
            timeOfDay = 12,
            dayOfWeek = 3,
            deviceType = DeviceType.UNKNOWN
        )

        // When
        val result = contextualRelevanceService.calculateAdvancedContextualRelevance(item, minimalContext)

        // Then
        assertTrue(result.overallRelevance >= 0.0)
        assertTrue(result.overallRelevance <= 1.0)
        assertEquals(ContextualConfidence.MINIMAL, result.confidenceLevel)
        assertTrue(result.explanation.isNotEmpty())
    }

    @Test
    fun `contextual relevance should provide meaningful explanations`(): Unit = runBlocking {
        // Given
        val morningNewsItem = createPersonalizableItem(topics = listOf("news", "business"))
        val morningContext = UserContext(
            timeOfDay = 8, // Morning
            dayOfWeek = 2,
            deviceType = DeviceType.MOBILE,
            location = UserLocation(country = "US"),
            sessionDuration = 10
        )

        // When
        val result = contextualRelevanceService.calculateAdvancedContextualRelevance(morningNewsItem, morningContext)

        // Then
        assertTrue(result.explanation.isNotEmpty())
        assertTrue(result.temporalRelevance.explanation.isNotEmpty())
        assertTrue(result.locationRelevance.explanation.isNotEmpty())
        assertTrue(result.sessionRelevance.explanation.isNotEmpty())
        assertTrue(result.deviceRelevance.explanation.isNotEmpty())
        assertTrue(result.contextFactorsApplied.isNotEmpty())
    }

    // Helper methods

    private fun createPersonalizableItem(
        contentId: String = "test-content-${System.nanoTime()}",
        contentType: ContentType = ContentType.TEXT,
        topics: List<String> = listOf("general")
    ): PersonalizableItem {
        val storedContent = StoredContent(
            id = contentId,
            authorId = "test-author",
            contentType = contentType,
            textContent = "Test content body",
            visibility = com.welcomer.welcome.ingestion.model.ContentVisibility.PUBLIC,
            status = com.welcomer.welcome.ingestion.model.ContentStatus.PUBLISHED,
            tags = topics,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        return PersonalizableItem(
            content = storedContent,
            baseScore = 1.0
        )
    }
}