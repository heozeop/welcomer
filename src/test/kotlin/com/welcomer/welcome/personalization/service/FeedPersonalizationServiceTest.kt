package com.welcomer.welcome.personalization.service

import com.welcomer.welcome.engagement.model.EngagementType
import com.welcomer.welcome.engagement.service.EngagementTrackingService
import com.welcomer.welcome.ingestion.model.*
import com.welcomer.welcome.personalization.model.UserPreferenceProfile as PersonalizationUserPreferenceProfile
import com.welcomer.welcome.user.model.UserPreferenceProfile as UserModelPreferenceProfile
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

class FeedPersonalizationServiceTest {

    private lateinit var personalizationService: FeedPersonalizationService
    private lateinit var mockUserPreferenceService: UserPreferenceService
    private lateinit var mockUserContextService: UserContextService
    private lateinit var mockUserHistoryService: UserHistoryService
    private lateinit var mockEngagementService: EngagementTrackingService

    @BeforeEach
    fun setup() {
        mockUserPreferenceService = mock()
        mockUserContextService = mock()
        mockUserHistoryService = mock()
        mockEngagementService = mock()
        
        personalizationService = DefaultFeedPersonalizationService(
            mockUserPreferenceService,
            mockUserContextService,
            mockUserHistoryService,
            mockEngagementService
        )
    }

    @Test
    fun `personalizeItems should return empty result for empty input`(): Unit = runBlocking {
        // Given
        val userId = "testUser"
        val emptyItems = emptyList<PersonalizableItem>()
        
        // When
        val result = personalizationService.personalizeItems(userId, emptyItems)
        
        // Then
        assertTrue(result.personalizedItems.isEmpty())
        assertEquals(0, result.processingStats.itemsProcessed)
        assertTrue(result.processingStats.totalProcessingTimeMs >= 0)
    }

    @Test
    fun `personalizeItems should handle user preferences correctly`(): Unit = runBlocking {
        // Given
        val userId = "testUser"
        val feedItems = createTestFeedItems()
        val userPreferences = createTestUserPreferences()
        val userContext = createTestUserContext()
        val userHistory = createTestUserHistory()
        
        // Mock services
        whenever(mockUserPreferenceService.getPreferences(userId)).thenReturn(convertToUserModelProfile(userPreferences))
        whenever(mockUserContextService.getUserContext(userId)).thenReturn(userContext)
        whenever(mockUserHistoryService.getUserHistory(eq(userId), any())).thenReturn(userHistory)
        
        // When
        val result = personalizationService.personalizeItems(userId, feedItems)
        
        // Then
        assertEquals(feedItems.size, result.personalizedItems.size)
        assertEquals(feedItems.size, result.processingStats.itemsProcessed)
        assertTrue(result.personalizedItems.all { it.finalScore > 0 })
        assertTrue(result.personalizedItems.all { it.personalizationFactors.topicRelevance >= 0 })
        assertTrue(result.personalizedItems.all { it.personalizationFactors.sourceAffinity >= 0 })
        assertTrue(result.personalizedItems.all { it.personalizationFactors.contextualRelevance >= 0 })
        
        // Verify items are ranked by final score
        val sortedItems = result.personalizedItems.sortedByDescending { it.finalScore }
        assertEquals(sortedItems, result.personalizedItems)
    }

    @Test
    fun `personalizeItems should apply diversity controls when enabled`(): Unit = runBlocking {
        // Given
        val userId = "testUser"
        val feedItems = createSimilarTopicItems() // Items with similar topics
        val config = PersonalizationConfig(enableDiversityControls = true)
        
        val userPreferences = createTestUserPreferences()
        val userContext = createTestUserContext()
        val userHistory = createTestUserHistory()
        
        whenever(mockUserPreferenceService.getPreferences(userId)).thenReturn(convertToUserModelProfile(userPreferences))
        whenever(mockUserContextService.getUserContext(userId)).thenReturn(userContext)
        whenever(mockUserHistoryService.getUserHistory(eq(userId), any())).thenReturn(userHistory)
        
        // When
        val result = personalizationService.personalizeItems(userId, feedItems, config)
        
        // Then
        assertTrue(result.personalizedItems.any { 
            it.personalizationFactors.diversityAdjustment != 0.0 
        })
    }

    // Note: Service failure graceful degradation is inherently handled by the null-safe design
    // where each fetch method returns null/defaults on failure rather than throwing exceptions

    @Test
    fun `calculateTopicRelevance should return neutral score for empty preferences`(): Unit = runBlocking {
        // Given
        val item = createTestPersonalizableItem(topics = listOf("technology", "ai"))
        val emptyPreferences = emptyMap<String, Double>()
        
        // When
        val relevance = personalizationService.calculateTopicRelevance(item, emptyPreferences)
        
        // Then
        assertEquals(0.5, relevance, 0.001)
    }

    @Test
    fun `calculateTopicRelevance should return high score for matching topics`(): Unit = runBlocking {
        // Given
        val item = createTestPersonalizableItem(topics = listOf("technology", "ai"))
        val preferences = mapOf(
            "technology" to 0.9,
            "ai" to 0.8,
            "science" to 0.7
        )
        
        // When
        val relevance = personalizationService.calculateTopicRelevance(item, preferences)
        
        // Then
        assertTrue(relevance > 0.7) // Should be high due to exact matches
    }

    @Test
    fun `calculateTopicRelevance should handle partial matches correctly`(): Unit = runBlocking {
        // Given
        val item = createTestPersonalizableItem(topics = listOf("machine-learning", "data-science"))
        val preferences = mapOf(
            "machine" to 0.8, // Partial match
            "data" to 0.7,    // Partial match
            "politics" to 0.9  // No match
        )
        
        // When
        val relevance = personalizationService.calculateTopicRelevance(item, preferences)
        
        // Then
        assertTrue(relevance > 0.5) // Should get some boost from partial matches
        assertTrue(relevance < 0.8) // But not as high as exact matches
    }

    @Test
    fun `calculateSourceAffinity should return neutral score for new users`(): Unit = runBlocking {
        // Given
        val item = createTestPersonalizableItem(authorId = "author1")
        val emptyHistory = emptyList<UserActivity>()
        
        // When
        val affinity = personalizationService.calculateSourceAffinity(item, emptyHistory)
        
        // Then
        assertEquals(0.5, affinity, 0.001)
    }

    @Test
    fun `calculateSourceAffinity should return low score for unseen sources`(): Unit = runBlocking {
        // Given
        val item = createTestPersonalizableItem(authorId = "unseenAuthor")
        val history = createTestUserHistory() // History with other authors
        
        // When
        val affinity = personalizationService.calculateSourceAffinity(item, history)
        
        // Then
        assertEquals(0.3, affinity, 0.001)
    }

    @Test
    fun `calculateSourceAffinity should calculate weighted engagement scores correctly`(): Unit = runBlocking {
        // Given
        val item = createTestPersonalizableItem(authorId = "author1")
        val history = listOf(
            UserActivity(
                contentId = "content1",
                authorId = "author1",
                topics = listOf("tech"),
                engagementType = EngagementType.LIKE,
                engagementScore = 1.0,
                timestamp = Instant.now()
            ),
            UserActivity(
                contentId = "content2", 
                authorId = "author1",
                topics = listOf("tech"),
                engagementType = EngagementType.SHARE, // Higher weight
                engagementScore = 1.0,
                timestamp = Instant.now()
            )
        )
        
        // When
        val affinity = personalizationService.calculateSourceAffinity(item, history)
        
        // Then
        assertTrue(affinity > 0.7) // Should be high due to positive engagements
    }

    @Test
    fun `calculateContextualRelevance should provide baseline contextual score`(): Unit = runBlocking {
        // Given
        val item = createTestPersonalizableItem()
        val context = createTestUserContext()
        
        // When
        val relevance = personalizationService.calculateContextualRelevance(item, context)
        
        // Then
        assertTrue(relevance in 0.0..1.0)
        assertTrue(relevance > 0.0) // Should not be zero
    }

    @Test
    fun `calculateContextualRelevance should boost morning news content`(): Unit = runBlocking {
        // Given
        val newsItem = createTestPersonalizableItem(topics = listOf("news", "politics"))
        val morningContext = createTestUserContext(timeOfDay = 8) // 8 AM
        
        // When
        val relevance = personalizationService.calculateContextualRelevance(newsItem, morningContext)
        
        // Then
        assertTrue(relevance > 0.5) // Should get some morning news boost (time relevance = 0.8, but gets averaged)
    }

    @Test
    fun `calculateContextualRelevance should penalize recently seen content`(): Unit = runBlocking {
        // Given
        val item = createTestPersonalizableItem(contentId = "recentContent")
        val context = createTestUserContext(previousActivity = listOf("recentContent"))
        
        // When
        val relevance = personalizationService.calculateContextualRelevance(item, context)
        
        // Then
        assertTrue(relevance < 0.5) // Should be penalized for recent viewing
    }

    @Test
    fun `personalization should generate meaningful explanations`(): Unit = runBlocking {
        // Given
        val userId = "testUser"
        val feedItems = createTestFeedItems()
        val userPreferences = createTestUserPreferences()
        val userContext = createTestUserContext()
        val userHistory = createTestUserHistory()
        
        whenever(mockUserPreferenceService.getPreferences(userId)).thenReturn(convertToUserModelProfile(userPreferences))
        whenever(mockUserContextService.getUserContext(userId)).thenReturn(userContext)
        whenever(mockUserHistoryService.getUserHistory(eq(userId), any())).thenReturn(userHistory)
        
        // When
        val result = personalizationService.personalizeItems(userId, feedItems)
        
        // Then
        assertTrue(result.personalizedItems.any { it.explanations.isNotEmpty() })
        assertTrue(result.personalizedItems.flatMap { it.explanations }.any { 
            it.contains("interest") || it.contains("source") || it.contains("context")
        })
    }

    @Test
    fun `personalization metrics should be calculated correctly`(): Unit = runBlocking {
        // Given
        val userId = "testUser"
        val feedItems = createTestFeedItems()
        val userPreferences = createTestUserPreferences()
        val userContext = createTestUserContext()
        val userHistory = createTestUserHistory()
        
        whenever(mockUserPreferenceService.getPreferences(userId)).thenReturn(convertToUserModelProfile(userPreferences))
        whenever(mockUserContextService.getUserContext(userId)).thenReturn(userContext)
        whenever(mockUserHistoryService.getUserHistory(eq(userId), any())).thenReturn(userHistory)
        
        // When
        val result = personalizationService.personalizeItems(userId, feedItems)
        
        // Then
        assertTrue(result.personalizationMetrics.averagePersonalizationScore > 0)
        assertTrue(result.personalizationMetrics.topicCoverage >= 0)
        assertTrue(result.personalizationMetrics.sourceDiversity >= 0)
        assertTrue(result.personalizationMetrics.temporalSpread >= 0.0)
    }

    // Helper methods for creating test data

    private fun createTestFeedItems(): List<PersonalizableItem> {
        return listOf(
            createTestPersonalizableItem(
                contentId = "item1",
                authorId = "author1", 
                topics = listOf("technology", "ai"),
                baseScore = 0.8
            ),
            createTestPersonalizableItem(
                contentId = "item2",
                authorId = "author2",
                topics = listOf("science", "research"),
                baseScore = 0.7
            ),
            createTestPersonalizableItem(
                contentId = "item3",
                authorId = "author3",
                topics = listOf("entertainment", "movies"),
                baseScore = 0.6
            ),
            createTestPersonalizableItem(
                contentId = "item4", 
                authorId = "author1",
                topics = listOf("technology", "programming"),
                baseScore = 0.9
            )
        )
    }

    private fun createSimilarTopicItems(): List<PersonalizableItem> {
        return listOf(
            createTestPersonalizableItem(contentId = "item1", topics = listOf("technology"), baseScore = 0.8),
            createTestPersonalizableItem(contentId = "item2", topics = listOf("technology"), baseScore = 0.7),
            createTestPersonalizableItem(contentId = "item3", topics = listOf("technology"), baseScore = 0.6),
            createTestPersonalizableItem(contentId = "item4", topics = listOf("technology"), baseScore = 0.9)
        )
    }

    private fun createTestPersonalizableItem(
        contentId: String = "testContent",
        authorId: String = "testAuthor",
        topics: List<String> = listOf("general"),
        baseScore: Double = 0.5,
        createdAt: Instant = Instant.now().minus(6, ChronoUnit.HOURS)
    ): PersonalizableItem {
        val content = StoredContent(
            id = contentId,
            authorId = authorId,
            contentType = ContentType.TEXT,
            textContent = "Test content for $contentId",
            visibility = ContentVisibility.PUBLIC,
            status = ContentStatus.PUBLISHED,
            createdAt = createdAt,
            updatedAt = createdAt,
            tags = topics
        )
        
        return PersonalizableItem(content = content, baseScore = baseScore)
    }

    private fun createTestUserPreferences(): PersonalizationUserPreferenceProfile {
        return PersonalizationUserPreferenceProfile(
            userId = "testUser",
            topicInterests = mapOf(
                "technology" to 0.9,
                "science" to 0.7,
                "entertainment" to 0.6,
                "ai" to 0.8,
                "programming" to 0.7
            ),
            sourcePreferences = mapOf(
                "author1" to 0.8,
                "author2" to 0.6
            ),
            contentTypePreferences = mapOf(
                "TEXT" to 0.7,
                "VIDEO" to 0.8
            )
        )
    }

    private fun convertToUserModelProfile(profile: PersonalizationUserPreferenceProfile): UserModelPreferenceProfile {
        return UserModelPreferenceProfile(
            userId = profile.userId,
            topicInterests = profile.topicInterests,
            contentTypePreferences = profile.contentTypePreferences,
            languagePreferences = profile.languagePreferences,
            followedAccounts = profile.sourcePreferences.keys.toSet(),
            blockedUsers = emptySet(),
            blockedTopics = emptySet(),
            algorithmPreferences = emptyMap(),
            lastUpdated = profile.lastUpdated,
            confidence = 1.0
        )
    }

    private fun createTestUserContext(
        timeOfDay: Int = 14,
        previousActivity: List<String> = emptyList()
    ): UserContext {
        return UserContext(
            timeOfDay = timeOfDay,
            dayOfWeek = 2, // Tuesday
            deviceType = DeviceType.DESKTOP,
            location = null,
            sessionDuration = 45,
            previousActivity = previousActivity,
            contextualPreferences = emptyMap()
        )
    }

    private fun createTestUserHistory(): List<UserActivity> {
        val now = Instant.now()
        return listOf(
            UserActivity(
                contentId = "hist1",
                authorId = "author1",
                topics = listOf("technology"),
                engagementType = EngagementType.LIKE,
                engagementScore = 0.8,
                timestamp = now.minus(2, ChronoUnit.HOURS)
            ),
            UserActivity(
                contentId = "hist2",
                authorId = "author2", 
                topics = listOf("science"),
                engagementType = EngagementType.SHARE,
                engagementScore = 0.9,
                timestamp = now.minus(4, ChronoUnit.HOURS)
            )
        )
    }
}