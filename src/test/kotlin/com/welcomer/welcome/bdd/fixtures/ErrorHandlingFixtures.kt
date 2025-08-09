package com.welcomer.welcome.bdd.fixtures

import com.welcomer.welcome.feed.model.*
import com.welcomer.welcome.ingestion.model.*
import com.welcomer.welcome.user.model.*
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Fixtures for error handling and resilience testing scenarios
 * Provides utilities for simulating various failure conditions and error states
 */
@Component
class ErrorHandlingFixtures {

    companion object {
        // System component health tracking
        private val componentHealthStatus = ConcurrentHashMap<String, Boolean>()
        private val errorSimulationActive = AtomicBoolean(false)
        private val databaseHealthMonitoring = AtomicBoolean(false)
        
        // Error simulation parameters
        private val simulatedLatencies = ConcurrentHashMap<String, Long>()
        private val serviceOutages = ConcurrentHashMap<String, Boolean>()
        private val dataCorruption = ConcurrentHashMap<String, Boolean>()
        private val rateLimits = ConcurrentHashMap<String, Int>()
        
        // Fallback content cache
        private val fallbackContentCache = mutableListOf<StoredContent>()
        private val emergencyFeedCache = ConcurrentHashMap<String, GeneratedFeed>()
        
        // System monitoring
        private val errorCounter = AtomicInteger(0)
        private val fallbackActivations = AtomicInteger(0)
        private val circuitBreakerStates = ConcurrentHashMap<String, Boolean>()
    }

    /**
     * Initialize error handling test data and system components
     */
    fun initializeErrorTestData() {
        // Reset all simulation states
        componentHealthStatus.clear()
        simulatedLatencies.clear()
        serviceOutages.clear()
        dataCorruption.clear()
        rateLimits.clear()
        fallbackContentCache.clear()
        emergencyFeedCache.clear()
        circuitBreakerStates.clear()
        
        errorCounter.set(0)
        fallbackActivations.set(0)
        
        // Initialize healthy component states
        componentHealthStatus["database"] = true
        componentHealthStatus["personalization_service"] = true
        componentHealthStatus["content_service"] = true
        componentHealthStatus["recommendation_service"] = true
        componentHealthStatus["cache_service"] = true
        componentHealthStatus["user_preference_service"] = true
        
        // Initialize fallback content
        initializeFallbackContent()
        
        println("Error handling test data initialized with ${componentHealthStatus.size} components")
    }

    /**
     * Configure error simulation capabilities
     */
    fun configureErrorSimulation() {
        errorSimulationActive.set(true)
        databaseHealthMonitoring.set(true)
        
        // Initialize circuit breaker states
        circuitBreakerStates["database"] = false
        circuitBreakerStates["personalization"] = false
        circuitBreakerStates["content_retrieval"] = false
        circuitBreakerStates["recommendation"] = false
        
        println("Error simulation capabilities configured and activated")
    }

    /**
     * Verify that system components are healthy
     */
    fun verifySystemComponentsHealthy(): Boolean {
        return componentHealthStatus.values.all { it }
    }

    /**
     * Create test user for error scenarios with basic preferences
     */
    fun createTestUserForErrorScenarios(userId: String): UserPersonaData {
        return UserPersonaData(
            userId = userId,
            personaType = "error_test_user",
            preferenceProfile = UserPreferenceProfile(
                userId = userId,
                topicInterests = mapOf(
                    "technology" to 0.7,
                    "news" to 0.5,
                    "entertainment" to 0.3
                ),
                contentTypePreferences = mapOf(
                    "text" to 0.8,
                    "image" to 0.6,
                    "video" to 0.4
                ),
                languagePreferences = listOf("en"),
                followedAccounts = setOf("tech_news", "daily_updates"),
                blockedUsers = emptySet(),
                blockedTopics = emptySet(),
                algorithmPreferences = mapOf("feed_type" to "balanced"),
                lastUpdated = Instant.now(),
                confidence = 0.7
            ),
            behaviorHistory = emptyList(),
            expectedBehavior = TestUserBehavior(
                sessionDuration = 15,
                contentInteractionRate = 0.4,
                maxPersonalizationMultiplier = 1.1
            )
        )
    }

    /**
     * Create user with established preferences for testing error scenarios
     */
    fun createUserWithEstablishedPreferences(): UserPersonaData {
        return UserPersonaData(
            userId = "established_user_${Random.nextInt(1000)}",
            personaType = "established_user",
            preferenceProfile = UserPreferenceProfile(
                userId = "established_user",
                topicInterests = mapOf(
                    "technology" to 0.9,
                    "programming" to 0.85,
                    "science" to 0.7,
                    "business" to 0.6,
                    "startups" to 0.8
                ),
                contentTypePreferences = mapOf(
                    "text" to 0.9,
                    "link" to 0.8,
                    "video" to 0.6,
                    "image" to 0.4
                ),
                languagePreferences = listOf("en"),
                followedAccounts = setOf(
                    "tech_guru", "startup_insider", "dev_weekly", 
                    "science_daily", "business_news"
                ),
                blockedUsers = setOf("spam_account"),
                blockedTopics = setOf("politics"),
                algorithmPreferences = mapOf(
                    "feed_type" to "personalized",
                    "diversity" to "low",
                    "novelty" to "medium"
                ),
                lastUpdated = Instant.now().minus(7, ChronoUnit.DAYS),
                confidence = 0.95
            ),
            behaviorHistory = emptyList(),
            expectedBehavior = TestUserBehavior(
                sessionDuration = 45,
                contentInteractionRate = 0.8,
                maxPersonalizationMultiplier = 1.5
            )
        )
    }

    /**
     * Create test user with specified ID
     */
    fun createTestUser(userId: String): UserPersonaData {
        return UserPersonaData(
            userId = userId,
            personaType = "test_user",
            preferenceProfile = UserPreferenceProfile(
                userId = userId,
                topicInterests = mapOf("general" to 0.5),
                contentTypePreferences = mapOf("text" to 0.7, "image" to 0.5),
                languagePreferences = listOf("en"),
                followedAccounts = emptySet(),
                blockedUsers = emptySet(),
                blockedTopics = emptySet(),
                algorithmPreferences = mapOf("feed_type" to "basic"),
                lastUpdated = Instant.now(),
                confidence = 0.5
            ),
            behaviorHistory = emptyList(),
            expectedBehavior = TestUserBehavior(
                sessionDuration = 10,
                contentInteractionRate = 0.3,
                maxPersonalizationMultiplier = 1.0
            )
        )
    }

    /**
     * Configure database timeout simulation
     */
    fun configureDatabaseTimeout(timeoutMs: Int) {
        simulatedLatencies["database"] = timeoutMs.toLong()
        componentHealthStatus["database"] = false
        println("Database timeout configured: ${timeoutMs}ms")
    }

    /**
     * Simulate database unavailability
     */
    fun simulateDatabaseUnavailability() {
        serviceOutages["database"] = true
        componentHealthStatus["database"] = false
        println("Database configured as unavailable")
    }

    /**
     * Simulate personalization data corruption
     */
    fun simulatePersonalizationDataCorruption() {
        dataCorruption["personalization"] = true
        println("Personalization service configured to return corrupted data")
    }

    /**
     * Simulate algorithm crash conditions
     */
    fun simulateAlgorithmCrash() {
        serviceOutages["personalization_algorithm"] = true
        println("Personalization algorithm configured to crash")
    }

    /**
     * Simulate content service latency
     */
    fun simulateContentServiceLatency(latencyMs: Long) {
        simulatedLatencies["content_service"] = latencyMs
        println("Content service configured with ${latencyMs}ms latency")
    }

    /**
     * Simulate limited memory conditions
     */
    fun simulateLimitedMemoryConditions() {
        // This would typically configure JVM parameters or mock memory constraints
        println("Limited memory conditions configured for testing")
    }

    /**
     * Simulate external service outage
     */
    fun simulateExternalServiceOutage(serviceName: String) {
        serviceOutages[serviceName] = true
        componentHealthStatus[serviceName] = false
        println("External service '$serviceName' configured as down")
    }

    /**
     * Corrupt feed cache for specific user
     */
    fun corruptFeedCacheForUser(userId: String) {
        dataCorruption["cache_$userId"] = true
        
        // Create corrupted cache entry
        val corruptedFeed = GeneratedFeed(
            userId = userId,
            feedType = FeedType.HOME,
            entries = emptyList(), // Corrupted - empty entries
            metadata = FeedMetadata(
                algorithmId = "corrupted",
                algorithmVersion = "invalid",
                generationDuration = -1, // Invalid duration
                contentCount = -1, // Invalid count
                candidateCount = -1,
                parameters = mapOf("corrupted" to true)
            )
        )
        
        emergencyFeedCache[userId] = corruptedFeed
        println("Feed cache corrupted for user: $userId")
    }

    /**
     * Simulate high traffic conditions
     */
    fun simulateHighTrafficConditions() {
        // Simulate high load by marking services as stressed
        componentHealthStatus.replaceAll { _, _ -> false }
        rateLimits["global"] = 100 // Lower rate limit due to high traffic
        println("High traffic conditions simulated")
    }

    /**
     * Configure rate limiting
     */
    fun configureRateLimiting(requestsPerMinute: Int) {
        rateLimits["api_requests"] = requestsPerMinute
        println("Rate limiting configured: $requestsPerMinute requests/minute")
    }

    /**
     * Check if database health monitoring is active
     */
    fun isDatabaseHealthMonitoringActive(): Boolean {
        return databaseHealthMonitoring.get()
    }

    /**
     * Generate fallback feed for user
     */
    fun generateFallbackFeed(userId: String): GeneratedFeed {
        fallbackActivations.incrementAndGet()
        
        val fallbackEntries = generateFallbackContent().take(20).mapIndexed { index, content ->
            FeedEntry(
                id = "fallback_${userId}_$index",
                content = content,
                score = Random.nextDouble(0.3, 0.8), // Lower scores for fallback
                rank = index + 1,
                reasons = listOf(
                    FeedReason(FeedReasonType.POPULARITY, "Popular content fallback", 0.6),
                    FeedReason(FeedReasonType.TRENDING, "Trending fallback content", 0.4)
                ),
                sourceType = FeedSourceType.MANUAL,
                algorithmId = "fallback_algorithm",
                generatedAt = Instant.now()
            )
        }
        
        return GeneratedFeed(
            userId = userId,
            feedType = FeedType.HOME,
            entries = fallbackEntries,
            metadata = FeedMetadata(
                algorithmId = "fallback_algorithm",
                algorithmVersion = "1.0.0",
                generationDuration = Random.nextLong(50, 200),
                contentCount = fallbackEntries.size,
                candidateCount = fallbackEntries.size * 2,
                parameters = mapOf(
                    "service_status" to "degraded",
                    "fallback_activated" to true,
                    "error_recovery" to "active"
                )
            )
        )
    }

    /**
     * Generate corrupted feed data for testing corruption detection
     */
    fun generateCorruptedFeedData(userId: String): GeneratedFeed {
        return GeneratedFeed(
            userId = userId,
            feedType = FeedType.HOME,
            entries = listOf(
                // Create entries with corrupted data
                FeedEntry(
                    id = "", // Invalid empty ID
                    content = StoredContent(
                        id = "corrupted_content",
                        authorId = "", // Invalid empty author ID
                        contentType = ContentType.TEXT,
                        textContent = null, // Invalid null content for TEXT type
                        visibility = ContentVisibility.PUBLIC,
                        status = ContentStatus.PUBLISHED,
                        createdAt = Instant.EPOCH, // Suspicious very old timestamp
                        updatedAt = Instant.EPOCH
                    ),
                    score = -1.0, // Invalid negative score
                    rank = 0, // Invalid zero rank
                    reasons = emptyList(), // Invalid empty reasons
                    sourceType = FeedSourceType.RECOMMENDATION,
                    generatedAt = Instant.now()
                )
            ),
            metadata = FeedMetadata(
                algorithmId = "corrupted_algorithm",
                algorithmVersion = "invalid_version",
                generationDuration = -1, // Invalid negative duration
                contentCount = -5, // Invalid negative count
                candidateCount = -10, // Invalid negative count
                parameters = mapOf("corruption_detected" to true)
            )
        )
    }

    /**
     * Generate feed using internal algorithm (when external services fail)
     */
    fun generateFeedWithInternalAlgorithm(user: UserPersonaData): GeneratedFeed {
        val internalContent = generateBasicPersonalizedContent(user).take(15)
        
        val entries = internalContent.mapIndexed { index, content ->
            FeedEntry(
                id = "internal_${user.userId}_$index",
                content = content,
                score = calculateBasicScore(content, user),
                rank = index + 1,
                reasons = listOf(
                    FeedReason(FeedReasonType.RELEVANCE, "Internal algorithm match", 0.5),
                    FeedReason(FeedReasonType.RECENCY, "Recent content", 0.3)
                ),
                sourceType = FeedSourceType.RECOMMENDATION,
                algorithmId = "internal_algorithm",
                generatedAt = Instant.now()
            )
        }
        
        return GeneratedFeed(
            userId = user.userId,
            feedType = FeedType.HOME,
            entries = entries,
            metadata = FeedMetadata(
                algorithmId = "internal_algorithm",
                algorithmVersion = "1.2.0",
                generationDuration = Random.nextLong(100, 300),
                contentCount = entries.size,
                candidateCount = entries.size * 3,
                parameters = mapOf(
                    "external_service_down" to true,
                    "fallback_to_internal" to true
                )
            )
        )
    }

    /**
     * Generate normal personalized feed (no errors)
     */
    fun generateNormalPersonalizedFeed(user: UserPersonaData): GeneratedFeed {
        val personalizedContent = generatePersonalizedContent(user).take(25)
        
        val entries = personalizedContent.mapIndexed { index, content ->
            FeedEntry(
                id = "normal_${user.userId}_$index",
                content = content,
                score = calculatePersonalizedScore(content, user),
                rank = index + 1,
                reasons = generatePersonalizedReasons(content, user),
                sourceType = FeedSourceType.RECOMMENDATION,
                algorithmId = "personalization_v2",
                generatedAt = Instant.now()
            )
        }
        
        return GeneratedFeed(
            userId = user.userId,
            feedType = FeedType.HOME,
            entries = entries,
            metadata = FeedMetadata(
                algorithmId = "personalization_v2",
                algorithmVersion = "2.1.0",
                generationDuration = Random.nextLong(200, 500),
                contentCount = entries.size,
                candidateCount = entries.size * 5,
                parameters = mapOf(
                    "personalization_enabled" to true,
                    "service_status" to "healthy"
                )
            )
        )
    }

    /**
     * Initialize fallback content for emergency situations
     */
    private fun initializeFallbackContent() {
        val fallbackTopics = listOf("general", "news", "technology", "science", "entertainment")
        
        repeat(50) { index ->
            val topic = fallbackTopics.random()
            
            fallbackContentCache.add(
                StoredContent(
                    id = "fallback_content_$index",
                    authorId = "system_author_${Random.nextInt(1, 5)}",
                    contentType = ContentType.TEXT,
                    textContent = "This is fallback content #$index about $topic. " +
                               "Safe and appropriate for all audiences during service degradation.",
                    visibility = ContentVisibility.PUBLIC,
                    status = ContentStatus.PUBLISHED,
                    tags = listOf(topic, "fallback", "safe"),
                    createdAt = Instant.now().minus(Random.nextLong(1, 30), ChronoUnit.DAYS),
                    updatedAt = Instant.now().minus(Random.nextLong(1, 24), ChronoUnit.HOURS)
                )
            )
        }
    }

    /**
     * Generate fallback content for emergency feeds
     */
    private fun generateFallbackContent(): List<StoredContent> {
        return if (fallbackContentCache.isNotEmpty()) {
            fallbackContentCache.shuffled()
        } else {
            // Generate emergency content if cache is empty
            listOf(
                StoredContent(
                    id = "emergency_content_1",
                    authorId = "system",
                    contentType = ContentType.TEXT,
                    textContent = "Service temporarily unavailable. Please try again later.",
                    visibility = ContentVisibility.PUBLIC,
                    status = ContentStatus.PUBLISHED,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        }
    }

    /**
     * Generate basic personalized content using simple rules
     */
    private fun generateBasicPersonalizedContent(user: UserPersonaData): List<StoredContent> {
        val userInterests = user.preferenceProfile.topicInterests.keys.toList()
        val content = mutableListOf<StoredContent>()
        
        repeat(20) { index ->
            val topic = if (userInterests.isNotEmpty()) {
                userInterests.random()
            } else {
                listOf("general", "news", "technology").random()
            }
            
            content.add(
                StoredContent(
                    id = "basic_content_$index",
                    authorId = "author_${Random.nextInt(1, 10)}",
                    contentType = ContentType.TEXT,
                    textContent = "Basic personalized content about $topic for ${user.userId}. " +
                               "Generated using internal algorithm when external services are unavailable.",
                    tags = listOf(topic, "personalized", "internal"),
                    visibility = ContentVisibility.PUBLIC,
                    status = ContentStatus.PUBLISHED,
                    createdAt = Instant.now().minus(Random.nextLong(1, 7), ChronoUnit.DAYS),
                    updatedAt = Instant.now()
                )
            )
        }
        
        return content
    }

    /**
     * Generate fully personalized content
     */
    private fun generatePersonalizedContent(user: UserPersonaData): List<StoredContent> {
        val content = mutableListOf<StoredContent>()
        val userInterests = user.preferenceProfile.topicInterests
        
        userInterests.forEach { (topic, interest) ->
            val itemsForTopic = (interest * 10).toInt().coerceAtMost(8)
            
            repeat(itemsForTopic) { index ->
                content.add(
                    StoredContent(
                        id = "personalized_${topic}_$index",
                        authorId = if (user.preferenceProfile.followedAccounts.isNotEmpty()) {
                            user.preferenceProfile.followedAccounts.random()
                        } else {
                            "author_${Random.nextInt(1, 20)}"
                        },
                        contentType = user.preferenceProfile.contentTypePreferences.maxByOrNull { it.value }?.let {
                            when (it.key) {
                                "text" -> ContentType.TEXT
                                "image" -> ContentType.IMAGE
                                "video" -> ContentType.VIDEO
                                "link" -> ContentType.LINK
                                else -> ContentType.TEXT
                            }
                        } ?: ContentType.TEXT,
                        textContent = "Highly personalized content about $topic. " +
                                   "This content matches your interests with ${(interest * 100).toInt()}% relevance.",
                        tags = listOf(topic, "personalized", "high_relevance"),
                        visibility = ContentVisibility.PUBLIC,
                        status = ContentStatus.PUBLISHED,
                        createdAt = Instant.now().minus(Random.nextLong(1, 3), ChronoUnit.DAYS),
                        updatedAt = Instant.now()
                    )
                )
            }
        }
        
        return content.shuffled()
    }

    /**
     * Calculate basic score for content
     */
    private fun calculateBasicScore(content: StoredContent, user: UserPersonaData): Double {
        var score = 0.5 // Base score
        
        // Simple topic matching
        content.tags.forEach { tag ->
            if (user.preferenceProfile.topicInterests.containsKey(tag)) {
                score += 0.3
            }
        }
        
        // Recency bonus
        val daysSinceCreation = ChronoUnit.DAYS.between(content.createdAt, Instant.now())
        if (daysSinceCreation <= 1) score += 0.2
        else if (daysSinceCreation <= 7) score += 0.1
        
        return score.coerceAtMost(1.0)
    }

    /**
     * Calculate personalized score for content
     */
    private fun calculatePersonalizedScore(content: StoredContent, user: UserPersonaData): Double {
        var score = 0.3 // Base score
        
        // Topic interest matching
        content.tags.forEach { tag ->
            val interest = user.preferenceProfile.topicInterests[tag] ?: 0.0
            score += interest * 0.4
        }
        
        // Content type preference
        val contentTypePref = when (content.contentType) {
            ContentType.TEXT -> user.preferenceProfile.contentTypePreferences["text"] ?: 0.5
            ContentType.IMAGE -> user.preferenceProfile.contentTypePreferences["image"] ?: 0.5
            ContentType.VIDEO -> user.preferenceProfile.contentTypePreferences["video"] ?: 0.5
            ContentType.LINK -> user.preferenceProfile.contentTypePreferences["link"] ?: 0.5
            ContentType.POLL -> user.preferenceProfile.contentTypePreferences["poll"] ?: 0.3
        }
        score += contentTypePref * 0.3
        
        // Author preference (followed accounts)
        if (user.preferenceProfile.followedAccounts.contains(content.authorId)) {
            score += 0.2
        }
        
        // Recency with personalized weighting
        val daysSinceCreation = ChronoUnit.DAYS.between(content.createdAt, Instant.now())
        when {
            daysSinceCreation <= 1 -> score += 0.15
            daysSinceCreation <= 3 -> score += 0.1
            daysSinceCreation <= 7 -> score += 0.05
        }
        
        return score.coerceAtMost(1.0)
    }

    /**
     * Generate personalized reasons for content inclusion
     */
    private fun generatePersonalizedReasons(content: StoredContent, user: UserPersonaData): List<FeedReason> {
        val reasons = mutableListOf<FeedReason>()
        
        // Topic relevance
        content.tags.forEach { tag ->
            val interest = user.preferenceProfile.topicInterests[tag]
            if (interest != null && interest > 0.5) {
                reasons.add(FeedReason(
                    type = FeedReasonType.TOPIC_INTEREST,
                    description = "Matches your interest in $tag",
                    weight = interest
                ))
            }
        }
        
        // Following relationship
        if (user.preferenceProfile.followedAccounts.contains(content.authorId)) {
            reasons.add(FeedReason(
                type = FeedReasonType.FOLLOWING,
                description = "From ${content.authorId} whom you follow",
                weight = 0.8
            ))
        }
        
        // Relevance
        reasons.add(FeedReason(
            type = FeedReasonType.RELEVANCE,
            description = "Highly relevant to your preferences",
            weight = 0.7
        ))
        
        // Add recency if content is recent
        val daysSinceCreation = ChronoUnit.DAYS.between(content.createdAt, Instant.now())
        if (daysSinceCreation <= 2) {
            reasons.add(FeedReason(
                type = FeedReasonType.RECENCY,
                description = "Recently published content",
                weight = 0.5
            ))
        }
        
        return reasons.take(3) // Limit to top 3 reasons
    }
}

