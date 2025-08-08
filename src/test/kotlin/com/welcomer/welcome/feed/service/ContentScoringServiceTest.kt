package com.welcomer.welcome.feed.service

import com.welcomer.welcome.feed.model.*
import com.welcomer.welcome.ingestion.model.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class ContentScoringServiceTest {

    private lateinit var scoringService: DefaultContentScoringService

    @BeforeEach
    fun setup() {
        scoringService = DefaultContentScoringService()
    }

    @Test
    fun `calculateRecencyScore should return higher scores for newer content`() {
        val now = Instant.now()
        val oneHourAgo = now.minus(Duration.ofHours(1))
        val oneDayAgo = now.minus(Duration.ofDays(1))
        val oneWeekAgo = now.minus(Duration.ofDays(7))

        val recentScore = scoringService.calculateRecencyScore(oneHourAgo, now)
        val dayOldScore = scoringService.calculateRecencyScore(oneDayAgo, now)
        val weekOldScore = scoringService.calculateRecencyScore(oneWeekAgo, now)

        assertTrue(recentScore > dayOldScore, "Recent content should score higher than day-old content")
        assertTrue(dayOldScore > weekOldScore, "Day-old content should score higher than week-old content")
        assertTrue(recentScore <= 1.0, "Recency score should not exceed 1.0")
        assertTrue(weekOldScore >= 0.0, "Recency score should not be negative")
    }

    @Test
    fun `calculatePopularityScore should consider engagement metrics`() {
        val contentAge = Duration.ofHours(6)
        
        val highEngagement = EngagementMetrics(
            likes = 100,
            comments = 50,
            shares = 25,
            views = 1000,
            clickThroughRate = 0.1,
            engagementRate = 0.15
        )
        
        val lowEngagement = EngagementMetrics(
            likes = 5,
            comments = 1,
            shares = 0,
            views = 50,
            clickThroughRate = 0.02,
            engagementRate = 0.02
        )

        val highScore = scoringService.calculatePopularityScore(highEngagement, contentAge)
        val lowScore = scoringService.calculatePopularityScore(lowEngagement, contentAge)

        assertTrue(highScore > lowScore, "High engagement content should score higher")
        assertTrue(highScore <= 1.0, "Popularity score should not exceed 1.0")
        assertTrue(lowScore >= 0.0, "Popularity score should not be negative")
    }

    @Test
    fun `calculateRelevanceScore should consider user interests`() {
        val userPreferences = UserPreferences(
            userId = "user1",
            interests = listOf("technology", "programming", "kotlin"),
            preferredContentTypes = setOf("text", "image"),
            blockedUsers = emptySet(),
            blockedTopics = emptySet(),
            languagePreferences = listOf("en")
        )

        val relevantContent = createTestContent(
            tags = listOf("technology", "programming"),
            contentType = ContentType.TEXT,
            languageCode = "en"
        )

        val irrelevantContent = createTestContent(
            tags = listOf("cooking", "recipes"),
            contentType = ContentType.VIDEO,
            languageCode = "fr"
        )

        val relevantScore = scoringService.calculateRelevanceScore(relevantContent, userPreferences)
        val irrelevantScore = scoringService.calculateRelevanceScore(irrelevantContent, userPreferences)

        assertTrue(relevantScore > irrelevantScore, "Relevant content should score higher")
        assertTrue(relevantScore <= 1.0, "Relevance score should not exceed 1.0")
        assertTrue(irrelevantScore >= 0.0, "Relevance score should not be negative")
    }

    @Test
    fun `calculateRelevanceScore should return zero for blocked users`() {
        val userPreferences = UserPreferences(
            userId = "user1",
            interests = listOf("technology"),
            blockedUsers = setOf("blocked_user")
        )

        val contentFromBlockedUser = createTestContent(
            authorId = "blocked_user",
            tags = listOf("technology") // Even with relevant tags
        )

        val score = scoringService.calculateRelevanceScore(contentFromBlockedUser, userPreferences)

        assertEquals(0.0, score, "Content from blocked users should have zero relevance")
    }

    @Test
    fun `calculateRelevanceScore should return zero for blocked topics`() {
        val userPreferences = UserPreferences(
            userId = "user1",
            interests = listOf("technology"),
            blockedTopics = setOf("politics", "controversy")
        )

        val contentWithBlockedTopic = createTestContent(
            tags = listOf("technology", "politics") // Mix of relevant and blocked
        )

        val score = scoringService.calculateRelevanceScore(contentWithBlockedTopic, userPreferences)

        assertEquals(0.0, score, "Content with blocked topics should have zero relevance")
    }

    @Test
    fun `calculateCompositeScore should combine individual scores with weights`() {
        val weights = ScoringWeights(
            recency = 0.5,
            popularity = 0.3,
            relevance = 0.2
        )

        val recencyScore = 0.8
        val popularityScore = 0.6
        val relevanceScore = 0.9

        val compositeScore = scoringService.calculateCompositeScore(
            recencyScore, popularityScore, relevanceScore, weights
        )

        // Expected: (0.8 * 0.5) + (0.6 * 0.3) + (0.9 * 0.2) = 0.4 + 0.18 + 0.18 = 0.76
        val expected = 0.76
        assertEquals(expected, compositeScore, 0.001, "Composite score should be weighted average")
    }

    @Test
    fun `calculateCompositeScore should apply bonus factors`() {
        val weights = ScoringWeights(
            recency = 0.5,
            popularity = 0.3,
            relevance = 0.2,
            following = 0.1
        )

        val bonusFactors = mapOf("following" to 1.0) // Following bonus

        val compositeScore = scoringService.calculateCompositeScore(
            0.5, 0.5, 0.5, weights, bonusFactors
        )

        val expectedWithoutBonus = 0.5 // Equal weights would give 0.5
        assertTrue(compositeScore > expectedWithoutBonus, "Bonus factors should increase score")
    }

    @Test
    fun `scoreContent should calculate comprehensive score for candidate`() = runBlocking {
        val candidate = ContentCandidate(
            content = createTestContent(
                tags = listOf("technology", "kotlin"),
                contentType = ContentType.TEXT
            ),
            popularityScore = 0.7,
            engagementMetrics = EngagementMetrics(
                likes = 50,
                comments = 10,
                shares = 5,
                views = 500,
                engagementRate = 0.1
            ),
            authorFollowingStatus = true,
            topicRelevance = 0.8,
            languageMatch = true
        )

        val userPreferences = UserPreferences(
            userId = "user1",
            interests = listOf("technology", "kotlin"),
            preferredContentTypes = setOf("text")
        )

        val weights = ScoringWeights(
            recency = 0.4,
            popularity = 0.3,
            relevance = 0.3
        )

        val score = scoringService.scoreContent(candidate, userPreferences, weights)

        assertTrue(score > 0.0, "Score should be positive for good candidate")
        assertTrue(score <= 1.0, "Score should not exceed 1.0")
    }

    // Helper method to create test content
    private fun createTestContent(
        authorId: String = "author1",
        tags: List<String> = emptyList(),
        contentType: ContentType = ContentType.TEXT,
        languageCode: String? = null,
        textContent: String = "Test content"
    ): StoredContent {
        return StoredContent(
            id = "content_${System.nanoTime()}",
            authorId = authorId,
            contentType = contentType,
            textContent = textContent,
            tags = tags,
            languageCode = languageCode,
            visibility = ContentVisibility.PUBLIC,
            status = ContentStatus.PUBLISHED,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            publishedAt = Instant.now()
        )
    }
}