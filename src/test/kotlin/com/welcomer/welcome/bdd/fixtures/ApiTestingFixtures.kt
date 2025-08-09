package com.welcomer.welcome.bdd.fixtures

import com.welcomer.welcome.feed.model.*
import com.welcomer.welcome.ingestion.model.*
import com.welcomer.welcome.user.model.*
import com.welcomer.welcome.user.controller.*
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random


/**
 * Fixtures for API testing scenarios providing realistic request/response data
 */
@Component
class ApiTestingFixtures {

    companion object {
        // Test user IDs for different scenarios
        const val TEST_USER_ID = "user123"
        const val TEST_USER_2_ID = "user456"
        const val NEW_USER_ID = "new_user_with_no_content"
        const val ADMIN_USER_ID = "admin_testuser"
        
        // Test authentication tokens
        const val VALID_AUTH_TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token"
        const val EXPIRED_AUTH_TOKEN = "Bearer expired.jwt.token"
        const val INVALID_AUTH_TOKEN = "Bearer invalid.malformed.token"
        
        // Test cursors for pagination
        const val VALID_CURSOR = "eyJpZCI6ImNvbnRlbnRfMTAwIiwidGltZXN0YW1wIjoiMjAyNC0wMS0wMVQxMjowMDowMFoifQ=="
        const val INVALID_CURSOR = "invalid_cursor_format"
        const val PREVIOUS_CURSOR_TOKEN = "eyJpZCI6ImNvbnRlbnRfNTAiLCJ0aW1lc3RhbXAiOiIyMDI0LTAxLTAxVDEwOjAwOjAwWiJ9"
        
        // Content type mappings
        val CONTENT_TYPE_MAPPINGS = mapOf(
            "text" to ContentType.TEXT,
            "article" to ContentType.TEXT,
            "image" to ContentType.IMAGE,
            "video" to ContentType.VIDEO,
            "link" to ContentType.LINK,
            "poll" to ContentType.POLL
        )
        
        // Topic categories for testing
        val TEST_TOPICS = listOf(
            "technology", "science", "sports", "entertainment", "news", "health", 
            "business", "politics", "culture", "travel", "food", "education",
            "gaming", "music", "art", "fitness", "fashion", "programming"
        )
    }

    /**
     * Generate mock feed response data for API testing
     */
    fun generateMockFeedResponse(
        userId: String,
        feedType: FeedType = FeedType.HOME,
        itemCount: Int = 25,
        includeNextCursor: Boolean = true,
        applyFilters: Map<String, Any> = emptyMap()
    ): GeneratedFeed {
        val entries = generateFeedEntries(itemCount, applyFilters)
        val metadata = generateFeedMetadata("personalization_v2", itemCount, applyFilters)
        
        return GeneratedFeed(
            userId = userId,
            feedType = feedType,
            entries = entries,
            metadata = metadata,
            nextCursor = if (includeNextCursor && itemCount >= 10) generateNextCursor() else null,
            hasMore = includeNextCursor && itemCount >= 10
        )
    }

    /**
     * Generate feed entries with optional filtering
     */
    private fun generateFeedEntries(
        count: Int, 
        filters: Map<String, Any> = emptyMap()
    ): List<FeedEntry> {
        return (1..count).map { index ->
            val contentType = determineContentType(filters, index)
            val topics = determineTopics(filters, index)
            val authorId = "author_${Random.nextInt(1, 20)}"
            
            val storedContent = StoredContent(
                id = "content_$index",
                authorId = authorId,
                contentType = contentType,
                textContent = generateTextContent(contentType, topics),
                linkUrl = if (contentType == ContentType.LINK) "https://example.com/article_$index" else null,
                mediaAttachments = if (contentType in listOf(ContentType.IMAGE, ContentType.VIDEO)) {
                    listOf(generateStoredMediaAttachment(contentType, index, "content_$index"))
                } else emptyList(),
                tags = topics,
                visibility = ContentVisibility.PUBLIC,
                status = ContentStatus.PUBLISHED,
                createdAt = determineCreationTime(filters, index),
                updatedAt = Instant.now().minus(Random.nextLong(1, 24), ChronoUnit.HOURS)
            )
            
            FeedEntry(
                id = "entry_$index",
                content = storedContent,
                score = Random.nextDouble(0.1, 1.0),
                rank = index,
                reasons = generateFeedReasons(),
                sourceType = FeedSourceType.RECOMMENDATION,
                boosted = Random.nextBoolean() && Random.nextDouble() < 0.1,
                algorithmId = "collaborative_filtering",
                generatedAt = Instant.now()
            )
        }
    }

    /**
     * Determine content type based on filters or random
     */
    private fun determineContentType(filters: Map<String, Any>, index: Int): ContentType {
        filters["content_type"]?.let { filterType ->
            return CONTENT_TYPE_MAPPINGS[filterType.toString().lowercase()] ?: ContentType.TEXT
        }
        
        filters["contentType"]?.let { filterType ->
            return when (filterType.toString().lowercase()) {
                "video" -> ContentType.VIDEO
                "image" -> ContentType.IMAGE
                "article", "text" -> ContentType.TEXT
                "link" -> ContentType.LINK
                else -> ContentType.TEXT
            }
        }
        
        // Random distribution for unfiltered content
        return when (index % 5) {
            0 -> ContentType.VIDEO
            1 -> ContentType.IMAGE
            2 -> ContentType.LINK
            3 -> ContentType.POLL
            else -> ContentType.TEXT
        }
    }

    /**
     * Determine topics based on filters or random
     */
    private fun determineTopics(filters: Map<String, Any>, index: Int): List<String> {
        filters["topic"]?.let { filterTopic ->
            return listOf(filterTopic.toString())
        }
        
        // Return 1-3 random topics
        val topicCount = Random.nextInt(1, 4)
        return TEST_TOPICS.shuffled().take(topicCount)
    }

    /**
     * Generate text content based on type and topics
     */
    private fun generateTextContent(contentType: ContentType, topics: List<String>): String? {
        return when (contentType) {
            ContentType.TEXT -> {
                val primaryTopic = topics.firstOrNull() ?: "general"
                "This is a sample text post about $primaryTopic. It contains relevant information and engaging content for API testing purposes."
            }
            ContentType.POLL -> "What's your opinion on ${topics.firstOrNull() ?: "this topic"}?"
            ContentType.LINK -> "Check out this interesting article about ${topics.joinToString(" and ")}!"
            else -> null
        }
    }

    /**
     * Determine creation time based on filters and sorting
     */
    private fun determineCreationTime(filters: Map<String, Any>, index: Int): Instant {
        filters["since"]?.let { sinceStr ->
            val sinceInstant = Instant.parse(sinceStr.toString())
            // Return timestamps after the since parameter
            return sinceInstant.plus(Random.nextLong(1, 1000), ChronoUnit.MINUTES)
        }
        
        val sort = filters["sort"]?.toString()
        return when (sort) {
            "latest" -> Instant.now().minus(index.toLong() * 10, ChronoUnit.MINUTES)
            "oldest" -> Instant.now().minus((100 - index).toLong(), ChronoUnit.DAYS)
            else -> Instant.now().minus(Random.nextLong(1, 30), ChronoUnit.DAYS)
        }
    }

    /**
     * Generate stored media attachment for visual content
     */
    private fun generateStoredMediaAttachment(contentType: ContentType, index: Int, contentId: String): StoredMediaAttachment {
        return StoredMediaAttachment(
            id = "media_$index",
            contentId = contentId,
            mediaType = when (contentType) {
                ContentType.IMAGE -> MediaType.IMAGE
                ContentType.VIDEO -> MediaType.VIDEO
                else -> MediaType.IMAGE
            },
            originalFilename = "media_$index.${if (contentType == ContentType.VIDEO) "mp4" else "jpg"}",
            fileUrl = "https://cdn.example.com/media_$index.${if (contentType == ContentType.VIDEO) "mp4" else "jpg"}",
            thumbnailUrl = "https://cdn.example.com/thumb_$index.jpg",
            width = Random.nextInt(400, 1200),
            height = Random.nextInt(300, 900),
            processingStatus = MediaProcessingStatus.COMPLETED,
            createdAt = Instant.now()
        )
    }

    /**
     * Generate feed reasons for content inclusion
     */
    private fun generateFeedReasons(): List<FeedReason> {
        val reasons = listOf(
            FeedReason(FeedReasonType.RELEVANCE, "Matches your interests", 0.8),
            FeedReason(FeedReasonType.POPULARITY, "Popular content", 0.6),
            FeedReason(FeedReasonType.RECENCY, "Recently published", 0.4),
            FeedReason(FeedReasonType.TOPIC_INTEREST, "Related to your topics", 0.7)
        )
        
        // Return 1-3 random reasons
        return reasons.shuffled().take(Random.nextInt(1, 4))
    }

    /**
     * Generate feed metadata
     */
    private fun generateFeedMetadata(
        algorithmId: String, 
        itemCount: Int, 
        parameters: Map<String, Any>
    ): FeedMetadata {
        return FeedMetadata(
            algorithmId = algorithmId,
            algorithmVersion = "2.1.0",
            generationDuration = Random.nextLong(50, 500),
            contentCount = itemCount,
            candidateCount = itemCount * Random.nextInt(3, 8),
            parameters = parameters,
            generatedAt = Instant.now(),
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS)
        )
    }

    /**
     * Generate next cursor for pagination
     */
    private fun generateNextCursor(): String {
        return "eyJpZCI6ImNvbnRlbnRfJHtSYW5kb20ubmV4dEludCgxMDAsIDIwMCl9IiwidGltZXN0YW1wIjoiJHtJbnN0YW50Lm5vdygpfSJ9"
    }

    /**
     * Generate user preference profile for API responses
     */
    fun generateUserPreferenceProfile(
        userId: String,
        preferenceType: String? = null
    ): UserPreferenceProfile {
        val baseProfile = UserPreferenceProfile(
            userId = userId,
            topicInterests = mapOf(
                "technology" to 0.9,
                "science" to 0.7,
                "programming" to 0.8,
                "business" to 0.5
            ),
            contentTypePreferences = mapOf(
                "text" to 0.8,
                "video" to 0.6,
                "image" to 0.7,
                "link" to 0.5
            ),
            languagePreferences = listOf("en", "es"),
            followedAccounts = setOf("tech_guru", "science_daily", "startup_news"),
            blockedUsers = setOf("spam_account", "blocked_user"),
            blockedTopics = setOf("politics", "controversial_topic"),
            algorithmPreferences = mapOf(
                "feed_type" to "balanced",
                "diversity" to "high",
                "novelty" to "medium"
            ),
            lastUpdated = Instant.now().minus(2, ChronoUnit.DAYS),
            confidence = 0.85
        )
        
        return when (preferenceType?.lowercase()) {
            "explicit" -> baseProfile.copy(
                topicInterests = baseProfile.topicInterests.filterValues { it > 0.6 },
                confidence = 0.95
            )
            "implicit" -> baseProfile.copy(
                topicInterests = mapOf(
                    "technology" to 0.7,
                    "programming" to 0.6
                ),
                confidence = 0.65
            )
            else -> baseProfile
        }
    }

    /**
     * Generate preference statistics
     */
    fun generatePreferenceStats(userId: String): PreferenceStats {
        return PreferenceStats(
            userId = userId,
            totalPreferences = 47,
            explicitPreferences = 12,
            implicitPreferences = 35,
            preferencesByType = mapOf(
                PreferenceType.TOPIC_INTEREST to 20,
                PreferenceType.CONTENT_TYPE to 8,
                PreferenceType.LANGUAGE to 2,
                PreferenceType.FOLLOWED_ACCOUNT to 12,
                PreferenceType.BLOCKED_USER to 3,
                PreferenceType.BLOCKED_TOPIC to 2
            ),
            averageConfidence = 0.78,
            lastActivityAt = Instant.now().minus(1, ChronoUnit.HOURS)
        )
    }

    /**
     * Generate feed types information
     */
    fun generateFeedTypes(): List<com.welcomer.welcome.feed.controller.FeedTypeInfo> {
        return FeedType.values().map { type ->
            com.welcomer.welcome.feed.controller.FeedTypeInfo(
                type = type.name.lowercase(),
                displayName = type.name.lowercase().replaceFirstChar { it.uppercase() },
                description = when (type) {
                    FeedType.HOME -> "Personalized feed with content from followed users and recommendations"
                    FeedType.FOLLOWING -> "Content from users you follow"
                    FeedType.EXPLORE -> "Discover new content and creators"
                    FeedType.TRENDING -> "Currently trending and popular content"
                    FeedType.PERSONALIZED -> "AI-powered personalized recommendations"
                }
            )
        }
    }

    /**
     * Generate cache statistics for performance testing
     */
    fun generateCacheStats(): CacheStats {
        return CacheStats(
            totalEntries = Random.nextInt(1000, 5000),
            hitRate = Random.nextDouble(0.70, 0.95),
            missRate = Random.nextDouble(0.05, 0.30),
            evictionCount = Random.nextLong(10, 100),
            averageLoadTime = Random.nextLong(50, 200),
            memoryUsage = Random.nextLong(100_000_000, 500_000_000)
        )
    }

    /**
     * Generate performance metrics for API responses
     */
    fun generatePerformanceMetrics(): Map<String, Any> {
        return mapOf(
            "averageResponseTime" to Random.nextLong(50, 300),
            "p95ResponseTime" to Random.nextLong(200, 500),
            "requestsPerSecond" to Random.nextDouble(100.0, 1000.0),
            "errorRate" to Random.nextDouble(0.001, 0.01),
            "databaseConnections" to Random.nextInt(5, 25),
            "memoryUsage" to Random.nextLong(200_000_000, 800_000_000)
        )
    }

    /**
     * Generate error response for various error scenarios
     */
    fun generateErrorResponse(
        errorType: String,
        statusCode: Int,
        customMessage: String? = null
    ): Map<String, Any> {
        val baseError = mapOf(
            "error" to true,
            "status" to statusCode,
            "timestamp" to Instant.now().toString()
        )
        
        return when (errorType.lowercase()) {
            "authentication" -> baseError + mapOf(
                "message" to (customMessage ?: "User authentication required"),
                "code" to "AUTH_REQUIRED"
            )
            "authorization" -> baseError + mapOf(
                "message" to (customMessage ?: "Access denied"),
                "code" to "ACCESS_DENIED"
            )
            "validation" -> baseError + mapOf(
                "message" to (customMessage ?: "Invalid request parameters"),
                "code" to "VALIDATION_ERROR"
            )
            "rate_limit" -> baseError + mapOf(
                "message" to (customMessage ?: "Rate limit exceeded"),
                "code" to "RATE_LIMITED",
                "retryAfter" to 60
            )
            "server_error" -> baseError + mapOf(
                "message" to (customMessage ?: "Internal server error"),
                "code" to "INTERNAL_ERROR"
            )
            else -> baseError + mapOf(
                "message" to (customMessage ?: "Unknown error"),
                "code" to "UNKNOWN_ERROR"
            )
        }
    }

    /**
     * Generate concurrent request results for load testing
     */
    fun generateConcurrentRequestResults(requestCount: Int, failureRate: Double = 0.0): List<ApiRequestResult> {
        return (1..requestCount).map { index ->
            val shouldFail = Random.nextDouble() < failureRate
            ApiRequestResult(
                requestId = "req_$index",
                statusCode = if (shouldFail) listOf(500, 503, 429).random() else 200,
                responseTimeMs = Random.nextLong(50, if (shouldFail) 5000 else 800),
                success = !shouldFail,
                error = if (shouldFail) "Request failed" else null,
                timestamp = Instant.now()
            )
        }
    }

    /**
     * Generate valid HTTP headers for API responses
     */
    fun generateResponseHeaders(includeRateLimit: Boolean = true): Map<String, String> {
        val headers = mutableMapOf(
            "Content-Type" to "application/json",
            "Cache-Control" to "private, max-age=300"
        )
        
        if (includeRateLimit) {
            headers["X-RateLimit-Limit"] = "1000"
            headers["X-RateLimit-Remaining"] = Random.nextInt(800, 1000).toString()
            headers["X-RateLimit-Reset"] = (Instant.now().epochSecond + 3600).toString()
        }
        
        return headers
    }

    /**
     * Validate JSON structure for API responses
     */
    fun validateFeedResponseStructure(responseBody: String): ResponseValidation {
        val errors = mutableListOf<String>()
        
        try {
            // This would normally use Jackson or similar JSON parser
            // For testing purposes, we'll simulate validation
            
            // Check for required top-level fields
            val requiredFields = listOf("items", "meta", "pagination")
            requiredFields.forEach { field ->
                if (!responseBody.contains("\"$field\"")) {
                    errors.add("Missing required field: $field")
                }
            }
            
            // Check for proper timestamp format (simplified check)
            if (responseBody.contains("createdAt") && !responseBody.contains("T") && !responseBody.contains("Z")) {
                errors.add("Invalid timestamp format - should be ISO format")
            }
            
        } catch (e: Exception) {
            errors.add("Invalid JSON structure: ${e.message}")
        }
        
        return ResponseValidation(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Generate algorithm confidence scores for personalized feeds
     */
    fun generateAlgorithmScores(): Map<String, Double> {
        return mapOf(
            "collaborative_filtering" to Random.nextDouble(0.7, 0.95),
            "content_based" to Random.nextDouble(0.6, 0.85),
            "hybrid_recommendation" to Random.nextDouble(0.75, 0.92),
            "trending_analysis" to Random.nextDouble(0.5, 0.8)
        )
    }

    /**
     * Initialize test data (placeholder for API testing setup)
     */
    fun initializeTestData() {
        // This would initialize test database or mock data
        println("Test data initialization completed")
    }

    /**
     * Create test user for API testing
     */
    fun createTestUser(userId: String): UserPersonaData {
        return UserPersonaData(
            userId = userId,
            personaType = "test_user",
            preferenceProfile = generateUserPreferenceProfile(userId),
            behaviorHistory = emptyList(),
            expectedBehavior = TestUserBehavior(
                sessionDuration = 30,
                contentInteractionRate = 0.5,
                maxPersonalizationMultiplier = 1.2
            )
        )
    }

    /**
     * Create timestamped content for API testing
     */
    fun createTimeStampedContent() {
        // This would create content with specific timestamps
        println("Timestamped content created for API testing")
    }
}

/**
 * Data classes for API testing support
 */
data class ApiRequestResult(
    val requestId: String,
    val statusCode: Int,
    val responseTimeMs: Long,
    val success: Boolean,
    val error: String? = null,
    val timestamp: Instant
)

data class ResponseValidation(
    val isValid: Boolean,
    val errors: List<String>
)

data class CacheStats(
    val totalEntries: Int,
    val hitRate: Double,
    val missRate: Double,
    val evictionCount: Long,
    val averageLoadTime: Long,
    val memoryUsage: Long
)

/**
 * Test user behavior configuration
 */
data class TestUserBehavior(
    val sessionDuration: Int,
    val contentInteractionRate: Double,
    val maxPersonalizationMultiplier: Double
)

/**
 * User persona data for API testing
 */
data class UserPersonaData(
    val userId: String,
    val personaType: String,
    val preferenceProfile: UserPreferenceProfile,
    val behaviorHistory: List<Any>, // Simplified for API testing
    val expectedBehavior: TestUserBehavior
)

