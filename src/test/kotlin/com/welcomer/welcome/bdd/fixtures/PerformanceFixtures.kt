package com.welcomer.welcome.bdd.fixtures

import com.welcomer.welcome.personalization.service.PersonalizableItem
import com.welcomer.welcome.personalization.service.UserActivity
import com.welcomer.welcome.personalization.service.UserContext
import com.welcomer.welcome.personalization.service.UserLocation
import com.welcomer.welcome.personalization.service.DeviceType
import com.welcomer.welcome.user.model.UserPreferenceProfile
import com.welcomer.welcome.ingestion.model.ContentType
import com.welcomer.welcome.ingestion.model.StoredContent
import com.welcomer.welcome.ingestion.model.ContentStatus
import com.welcomer.welcome.ingestion.model.ContentVisibility
import com.welcomer.welcome.engagement.model.EngagementType
import kotlin.random.Random
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * Fixtures for performance and load testing scenarios.
 * Generates large datasets and diverse user profiles for testing system performance.
 * 
 * This class uses the existing data structures from the BDD fixtures.
 */
class PerformanceFixtures {
    
    // Topic pools for generating diverse content and users
    private val allTopics = listOf(
        "technology", "science", "health", "fitness", "cooking", "travel", "photography",
        "music", "art", "literature", "movies", "gaming", "sports", "business", "finance",
        "education", "politics", "environment", "fashion", "home-improvement", "parenting",
        "relationships", "psychology", "history", "philosophy", "religion", "culture",
        "news", "entertainment", "lifestyle", "automotive", "real-estate", "career",
        "startups", "investing", "cryptocurrency", "ai", "machine-learning", "programming",
        "web-development", "mobile-development", "data-science", "cybersecurity", "networking",
        "cloud-computing", "devops", "agriculture", "biotechnology", "robotics", "space",
        "climate-change", "renewable-energy", "conservation", "wildlife", "marine-biology",
        "astronomy", "physics", "chemistry", "mathematics", "statistics", "economics",
        "sociology", "anthropology", "linguistics", "archaeology", "geography", "geology"
    )
    
    private val contentTypes = listOf(ContentType.TEXT, ContentType.IMAGE, ContentType.VIDEO)
    private val qualityLevels = listOf("high", "medium", "low")
    private val engagementTypes = listOf(EngagementType.LIKE, EngagementType.SHARE, EngagementType.COMMENT, EngagementType.BOOKMARK, EngagementType.CLICK, EngagementType.VIEW)

    /**
     * Creates a user with established preferences and engagement history
     */
    fun createEstablishedUser(interactionCount: Int = 500): UserPersonaData {
        val topics = allTopics.shuffled().take(Random.nextInt(5, 15))
        val engagementHistory = generateEngagementHistory(interactionCount, topics)
        val userContext = generateUserContext()
        
        return UserPersonaData(
            userId = "established_user_${Random.nextInt(1000, 9999)}",
            description = "Established user with $interactionCount interactions",
            preferenceProfile = UserPreferenceProfile(
                userId = "established_user_${Random.nextInt(1000, 9999)}",
                topicInterests = topics.associateWith { Random.nextDouble(0.3, 1.0) },
                contentTypePreferences = mapOf(
                    ContentType.TEXT.name to Random.nextDouble(0.5, 1.0),
                    ContentType.VIDEO.name to Random.nextDouble(0.3, 0.9),
                    ContentType.IMAGE.name to Random.nextDouble(0.2, 0.8)
                ),
                languagePreferences = listOf("en", "es", "fr").shuffled().take(Random.nextInt(1, 3)),
                followedAccounts = setOf(),
                blockedUsers = setOf(),
                blockedTopics = setOf(),
                algorithmPreferences = mapOf("personalization_strength" to "high"),
                lastUpdated = Instant.now(),
                confidence = Random.nextDouble(0.6, 0.9)
            ),
            engagementHistory = engagementHistory,
            userContext = userContext,
            expectedBehavior = ExpectedPersonalizationBehavior(
                shouldHaveDiverseContent = true,
                shouldIncludeTrendingContent = true,
                shouldBeHeavilyPersonalized = true,
                expectedTopicDistribution = topics.associateWith { Random.nextDouble(0.1, 0.4) },
                maxPersonalizationMultiplier = Random.nextDouble(1.5, 2.5)
            )
        )
    }

    /**
     * Creates a power user with extensive engagement history and complex preferences
     */
    fun createPowerUser(interactionCount: Int = 50000): UserPersonaData {
        val topics = allTopics.shuffled().take(Random.nextInt(20, 35))
        val engagementHistory = generateExtensiveEngagementHistory(interactionCount, topics)
        val userContext = generateUserContext()
        
        return UserPersonaData(
            userId = "power_user_${Random.nextInt(1000, 9999)}",
            description = "Power user with $interactionCount interactions",
            preferenceProfile = UserPreferenceProfile(
                userId = "power_user_${Random.nextInt(1000, 9999)}",
                topicInterests = topics.associateWith { Random.nextDouble(0.1, 1.0) },
                contentTypePreferences = mapOf(
                    ContentType.TEXT.name to Random.nextDouble(0.7, 1.0),
                    ContentType.VIDEO.name to Random.nextDouble(0.6, 1.0),
                    ContentType.IMAGE.name to Random.nextDouble(0.4, 0.8)
                ),
                languagePreferences = listOf("en", "es", "fr", "de").shuffled().take(Random.nextInt(2, 4)),
                followedAccounts = setOf(),
                blockedUsers = setOf(),
                blockedTopics = setOf(),
                algorithmPreferences = mapOf("personalization_strength" to "very_high", "diversity_boost" to "enabled"),
                lastUpdated = Instant.now(),
                confidence = Random.nextDouble(0.8, 0.95)
            ),
            engagementHistory = engagementHistory,
            userContext = userContext,
            expectedBehavior = ExpectedPersonalizationBehavior(
                shouldHaveDiverseContent = true,
                shouldIncludeTrendingContent = true,
                shouldBeHeavilyPersonalized = true,
                expectedTopicDistribution = topics.associateWith { Random.nextDouble(0.05, 0.2) },
                maxPersonalizationMultiplier = Random.nextDouble(2.0, 4.0)
            )
        )
    }

    /**
     * Generates a list of diverse users with varying preference profiles
     */
    fun generateDiverseUsers(count: Int): List<UserPersonaData> {
        return (1..count).map { index ->
            val userType = when (index % 5) {
                0 -> "casual"
                1 -> "enthusiast" 
                2 -> "professional"
                3 -> "explorer"
                else -> "focused"
            }
            
            generateUserByType(userType, index)
        }
    }

    /**
     * Generates new users with minimal or no engagement history
     */
    fun generateNewUsers(count: Int): List<UserPersonaData> {
        return (1..count).map { index ->
            val topics = allTopics.shuffled().take(Random.nextInt(2, 8))
            val engagementHistory = generateMinimalEngagementHistory()
            val userContext = generateUserContext()
            
            UserPersonaData(
                userId = "new_user_${index}",
                description = "New user with minimal engagement history",
                preferenceProfile = UserPreferenceProfile(
                    userId = "new_user_${index}",
                    topicInterests = topics.associateWith { Random.nextDouble(0.1, 0.6) },
                    contentTypePreferences = mapOf(
                        ContentType.TEXT.name to Random.nextDouble(0.2, 0.7),
                        ContentType.VIDEO.name to Random.nextDouble(0.3, 0.8),
                        ContentType.IMAGE.name to Random.nextDouble(0.4, 0.9)
                    ),
                    languagePreferences = listOf("en"),
                    followedAccounts = setOf(),
                    blockedUsers = setOf(),
                    blockedTopics = setOf(),
                    algorithmPreferences = mapOf("personalization_strength" to "medium"),
                    lastUpdated = Instant.now(),
                    confidence = Random.nextDouble(0.3, 0.6)
                ),
                engagementHistory = engagementHistory,
                userContext = userContext,
                expectedBehavior = ExpectedPersonalizationBehavior(
                    shouldHaveDiverseContent = true,
                    shouldIncludeTrendingContent = true,
                    shouldBeHeavilyPersonalized = false,
                    expectedTopicDistribution = topics.associateWith { Random.nextDouble(0.1, 0.3) },
                    maxPersonalizationMultiplier = Random.nextDouble(0.5, 1.2)
                )
            )
        }
    }

    /**
     * Generates users with complex preference profiles
     */
    fun generateComplexUsers(count: Int): List<UserPersonaData> {
        return (1..count).map { index ->
            val topics = allTopics.shuffled().take(Random.nextInt(30, allTopics.size))
            val engagementHistory = generateComplexEngagementHistory(topics)
            val userContext = generateUserContext()
            
            UserPersonaData(
                userId = "complex_user_${index}",
                description = "Complex user with detailed preference profile",
                preferenceProfile = UserPreferenceProfile(
                    userId = "complex_user_${index}",
                    topicInterests = topics.associateWith { 
                        // Complex weighting with some very strong and some weak preferences
                        when (Random.nextInt(4)) {
                            0 -> Random.nextDouble(0.9, 1.0)  // Very strong interest
                            1 -> Random.nextDouble(0.7, 0.9)  // Strong interest  
                            2 -> Random.nextDouble(0.3, 0.7)  // Moderate interest
                            else -> Random.nextDouble(0.05, 0.3) // Weak interest
                        }
                    },
                    contentTypePreferences = contentTypes.associateWith { type -> 
                        Random.nextDouble(0.1, 1.0)
                    }.mapKeys { it.key.name },
                    languagePreferences = listOf("en", "es", "fr", "de", "ja", "zh").shuffled().take(Random.nextInt(1, 4)),
                    followedAccounts = (1..Random.nextInt(0, 20)).map { "user_$it" }.toSet(),
                    blockedUsers = setOf(),
                    blockedTopics = allTopics.shuffled().take(Random.nextInt(0, 5)).toSet(),
                    algorithmPreferences = mapOf("personalization_strength" to "adaptive", "diversity_boost" to "enabled"),
                    lastUpdated = Instant.now(),
                    confidence = Random.nextDouble(0.4, 0.8)
                ),
                engagementHistory = engagementHistory,
                userContext = userContext,
                expectedBehavior = ExpectedPersonalizationBehavior(
                    shouldHaveDiverseContent = true,
                    shouldIncludeTrendingContent = true,
                    shouldBeHeavilyPersonalized = true,
                    expectedTopicDistribution = topics.take(10).associateWith { Random.nextDouble(0.05, 0.15) },
                    maxPersonalizationMultiplier = Random.nextDouble(1.5, 3.5)
                )
            )
        }
    }

    /**
     * Generates users with overlapping interest patterns for cache testing
     */
    fun generateOverlappingInterestUsers(count: Int): List<UserPersonaData> {
        // Create clusters of overlapping interests
        val clusters = listOf(
            listOf("technology", "programming", "ai", "machine-learning"),
            listOf("health", "fitness", "nutrition", "psychology"),
            listOf("travel", "photography", "culture", "cooking"),
            listOf("business", "finance", "investing", "startups"),
            listOf("science", "space", "physics", "environment")
        )
        
        return (1..count).map { index ->
            val cluster = clusters[index % clusters.size]
            val additionalTopics = allTopics.minus(cluster.toSet()).shuffled().take(Random.nextInt(2, 8))
            val allUserTopics = cluster + additionalTopics
            val engagementHistory = generateEngagementHistory(Random.nextInt(200, 800), allUserTopics)
            val userContext = generateUserContext()
            
            val topicInterests = allUserTopics.associateWith { topic ->
                if (topic in cluster) {
                    Random.nextDouble(0.7, 1.0) // High interest in cluster topics
                } else {
                    Random.nextDouble(0.1, 0.5) // Lower interest in other topics
                }
            }
            
            UserPersonaData(
                userId = "overlapping_user_${index}",
                description = "User with overlapping interests for cache testing",
                preferenceProfile = UserPreferenceProfile(
                    userId = "overlapping_user_${index}",
                    topicInterests = topicInterests,
                    contentTypePreferences = mapOf(
                        ContentType.TEXT.name to Random.nextDouble(0.5, 0.9),
                        ContentType.VIDEO.name to Random.nextDouble(0.4, 0.8)
                    ),
                    languagePreferences = listOf("en", "es").shuffled().take(1),
                    followedAccounts = setOf(),
                    blockedUsers = setOf(),
                    blockedTopics = setOf(),
                    algorithmPreferences = mapOf("personalization_strength" to "high"),
                    lastUpdated = Instant.now(),
                    confidence = Random.nextDouble(0.6, 0.85)
                ),
                engagementHistory = engagementHistory,
                userContext = userContext,
                expectedBehavior = ExpectedPersonalizationBehavior(
                    shouldHaveDiverseContent = true,
                    shouldIncludeTrendingContent = false,
                    shouldBeHeavilyPersonalized = true,
                    expectedTopicDistribution = cluster.associateWith { Random.nextDouble(0.15, 0.4) },
                    maxPersonalizationMultiplier = Random.nextDouble(1.2, 2.0)
                )
            )
        }
    }

    /**
     * Generates active users for real-time testing
     */
    fun generateActiveUsers(count: Int): List<UserPersonaData> {
        return (1..count).map { index ->
            val topics = allTopics.shuffled().take(Random.nextInt(8, 20))
            val engagementHistory = generateRecentEngagementHistory(topics)
            val userContext = generateUserContext()
            
            UserPersonaData(
                userId = "active_user_${index}",
                description = "Active user for real-time testing",
                preferenceProfile = UserPreferenceProfile(
                    userId = "active_user_${index}",
                    topicInterests = topics.associateWith { Random.nextDouble(0.4, 1.0) },
                    contentTypePreferences = mapOf(
                        ContentType.TEXT.name to Random.nextDouble(0.6, 1.0),
                        ContentType.VIDEO.name to Random.nextDouble(0.5, 0.9),
                        ContentType.IMAGE.name to Random.nextDouble(0.3, 0.8)
                    ),
                    languagePreferences = listOf("en", "es", "fr").shuffled().take(Random.nextInt(1, 3)),
                    followedAccounts = (1..Random.nextInt(5, 30)).map { "user_$it" }.toSet(),
                    blockedUsers = setOf(),
                    blockedTopics = setOf(),
                    algorithmPreferences = mapOf("personalization_strength" to "high", "real_time_boost" to "enabled"),
                    lastUpdated = Instant.now(),
                    confidence = Random.nextDouble(0.7, 0.9)
                ),
                engagementHistory = engagementHistory,
                userContext = userContext,
                expectedBehavior = ExpectedPersonalizationBehavior(
                    shouldHaveDiverseContent = true,
                    shouldIncludeTrendingContent = true,
                    shouldBeHeavilyPersonalized = true,
                    expectedTopicDistribution = topics.take(8).associateWith { Random.nextDouble(0.08, 0.2) },
                    maxPersonalizationMultiplier = Random.nextDouble(1.8, 3.0)
                )
            )
        }
    }

    /**
     * Generates users with varying computational complexity needs
     */
    fun generateComplexityVariedUsers(count: Int): List<UserPersonaData> {
        return (1..count).map { index ->
            val complexityLevel = when (index % 3) {
                0 -> "simple"   // Few topics, simple preferences
                1 -> "moderate" // Medium complexity
                else -> "complex" // High complexity
            }
            
            val (topicCount, interactionCount) = when (complexityLevel) {
                "simple" -> Pair(Random.nextInt(3, 8), Random.nextInt(50, 200))
                "moderate" -> Pair(Random.nextInt(8, 20), Random.nextInt(200, 1000))
                else -> Pair(Random.nextInt(20, 40), Random.nextInt(1000, 5000))
            }
            
            val topics = allTopics.shuffled().take(topicCount)
            val engagementHistory = generateEngagementHistory(interactionCount, topics)
            val userContext = generateUserContext()
            
            UserPersonaData(
                userId = "complexity_${complexityLevel}_user_${index}",
                description = "User with $complexityLevel complexity for performance testing",
                preferenceProfile = UserPreferenceProfile(
                    userId = "complexity_${complexityLevel}_user_${index}",
                    topicInterests = topics.associateWith { Random.nextDouble(0.1, 1.0) },
                    contentTypePreferences = contentTypes.take(Random.nextInt(2, contentTypes.size))
                        .associateWith { Random.nextDouble(0.1, 1.0) }.mapKeys { it.key.name },
                    languagePreferences = listOf("en"),
                    followedAccounts = setOf(),
                    blockedUsers = setOf(),
                    blockedTopics = setOf(),
                    algorithmPreferences = mapOf("personalization_strength" to complexityLevel),
                    lastUpdated = Instant.now(),
                    confidence = Random.nextDouble(0.3, 0.8)
                ),
                engagementHistory = engagementHistory,
                userContext = userContext,
                expectedBehavior = ExpectedPersonalizationBehavior(
                    shouldHaveDiverseContent = complexityLevel != "simple",
                    shouldIncludeTrendingContent = true,
                    shouldBeHeavilyPersonalized = complexityLevel == "complex",
                    expectedTopicDistribution = topics.take(10).associateWith { Random.nextDouble(0.05, 0.3) },
                    maxPersonalizationMultiplier = Random.nextDouble(0.8, 3.5)
                )
            )
        }
    }

    /**
     * Generates a large content dataset for performance testing
     */
    fun generateLargeContentDataset(size: Int): List<PersonalizableItem> {
        return (1..size).map { index ->
            val topicCount = Random.nextInt(1, 6)
            val topics = allTopics.shuffled().take(topicCount)
            val contentType = contentTypes.random()
            val quality = qualityLevels.random()
            
            val storedContent = StoredContent(
                id = "perf_content_$index",
                authorId = "author_${Random.nextInt(1, 100)}",
                contentType = contentType,
                textContent = "Sample content for performance testing: ${generateContentTitle(topics, contentType)}",
                visibility = ContentVisibility.PUBLIC,
                status = ContentStatus.PUBLISHED,
                createdAt = Instant.now().minus(Random.nextLong(0, 365), ChronoUnit.DAYS),
                updatedAt = Instant.now().minus(Random.nextLong(0, 30), ChronoUnit.DAYS),
                tags = topics
            )
            
            PersonalizableItem(
                content = storedContent,
                baseScore = Random.nextDouble(0.1, 1.0),
                metadata = mapOf(
                    "quality" to quality,
                    "complexity" to Random.nextInt(1, 10),
                    "readingTime" to Random.nextInt(1, 30),
                    "authorReputation" to Random.nextDouble(0.1, 1.0),
                    "engagementScore" to Random.nextDouble(0.0, 1.0),
                    "language" to listOf("en", "es", "fr", "de", "ja").random(),
                    "region" to listOf("US", "EU", "ASIA", "GLOBAL").random()
                )
            )
        }
    }

    /**
     * Generates content with rich metadata for complex matching
     */
    fun generateRichMetadataContent(size: Int): List<PersonalizableItem> {
        return (1..size).map { index ->
            val topics = allTopics.shuffled().take(Random.nextInt(3, 8))
            val contentType = contentTypes.random()
            
            val storedContent = StoredContent(
                id = "rich_content_$index",
                authorId = "author_${Random.nextInt(1, 200)}",
                contentType = contentType,
                textContent = "Rich metadata content for performance testing: ${generateContentTitle(topics, contentType)}",
                visibility = ContentVisibility.PUBLIC,
                status = ContentStatus.PUBLISHED,
                createdAt = Instant.now().minus(Random.nextLong(0, 365), ChronoUnit.DAYS),
                updatedAt = Instant.now().minus(Random.nextLong(0, 30), ChronoUnit.DAYS),
                tags = topics,
                languageCode = listOf("en", "es", "fr", "de", "ja", "zh", "pt", "ru").random()
            )
            
            PersonalizableItem(
                content = storedContent,
                baseScore = Random.nextDouble(0.1, 1.0),
                metadata = mapOf(
                    "quality" to qualityLevels.random(),
                    "complexity" to Random.nextInt(1, 10),
                    "readingTime" to Random.nextInt(1, 60),
                    "authorReputation" to Random.nextDouble(0.1, 1.0),
                    "engagementScore" to Random.nextDouble(0.0, 1.0),
                    "viewCount" to Random.nextInt(100, 100000),
                    "shareCount" to Random.nextInt(0, 10000),
                    "commentCount" to Random.nextInt(0, 5000),
                    "language" to listOf("en", "es", "fr", "de", "ja", "zh", "pt", "ru").random(),
                    "region" to listOf("US", "EU", "ASIA", "LATIN_AMERICA", "AFRICA", "OCEANIA", "GLOBAL").random(),
                    "ageRating" to listOf("G", "PG", "PG13", "R", "ADULT").random(),
                    "technicalLevel" to listOf("beginner", "intermediate", "advanced", "expert").random(),
                    "contentDepth" to listOf("overview", "detailed", "comprehensive", "reference").random(),
                    "interactivity" to listOf("static", "interactive", "multimedia", "vr").random(),
                    "format" to listOf("short", "medium", "long", "series").random(),
                    "trending" to Random.nextBoolean(),
                    "premium" to Random.nextBoolean(),
                    "collaborative" to Random.nextBoolean(),
                    "educational" to Random.nextBoolean(),
                    "seasonal" to Random.nextBoolean()
                )
            )
        }
    }

    // Private helper methods

    private fun generateUserByType(userType: String, index: Int): UserPersonaData {
        val (topicCount, interactionCount, sessionTime, itemsPerSession) = when (userType) {
            "casual" -> arrayOf(Random.nextInt(3, 8), Random.nextInt(10, 100), Random.nextDouble(5.0, 15.0), Random.nextInt(3, 10))
            "enthusiast" -> arrayOf(Random.nextInt(8, 15), Random.nextInt(100, 500), Random.nextDouble(15.0, 30.0), Random.nextInt(10, 20))
            "professional" -> arrayOf(Random.nextInt(5, 12), Random.nextInt(200, 800), Random.nextDouble(20.0, 45.0), Random.nextInt(15, 30))
            "explorer" -> arrayOf(Random.nextInt(15, 25), Random.nextInt(150, 400), Random.nextDouble(10.0, 25.0), Random.nextInt(8, 25))
            else -> arrayOf(Random.nextInt(2, 6), Random.nextInt(50, 200), Random.nextDouble(8.0, 20.0), Random.nextInt(5, 15)) // focused
        }
        
        val topics = allTopics.shuffled().take(topicCount as Int)
        val topicInterests = topics.associateWith { 
            when (userType) {
                "enthusiast" -> Random.nextDouble(0.6, 1.0)
                "professional" -> Random.nextDouble(0.5, 0.9)  
                "focused" -> Random.nextDouble(0.7, 1.0)
                else -> Random.nextDouble(0.2, 0.8)
            }
        }
        
        val engagementHistory = generateEngagementHistory(interactionCount as Int, topics)
        val userContext = generateUserContext()
        
        return UserPersonaData(
            userId = "${userType}_user_${index}",
            description = "$userType user for performance testing",
            preferenceProfile = UserPreferenceProfile(
                userId = "${userType}_user_${index}",
                topicInterests = topicInterests,
                contentTypePreferences = mapOf(
                    ContentType.TEXT.name to Random.nextDouble(0.3, 1.0),
                    ContentType.VIDEO.name to Random.nextDouble(0.2, 0.9),
                    ContentType.IMAGE.name to Random.nextDouble(0.1, 0.8)
                ),
                languagePreferences = listOf("en"),
                followedAccounts = when (userType) {
                    "professional" -> (1..Random.nextInt(10, 50)).map { "expert_$it" }.toSet()
                    "enthusiast" -> (1..Random.nextInt(20, 100)).map { "creator_$it" }.toSet()
                    else -> setOf()
                },
                blockedUsers = setOf(),
                blockedTopics = setOf(),
                algorithmPreferences = mapOf(
                    "personalization_strength" to when (userType) {
                        "professional" -> "high"
                        "enthusiast" -> "very_high"
                        "focused" -> "maximum"
                        else -> "medium"
                    }
                ),
                lastUpdated = Instant.now(),
                confidence = when (userType) {
                    "professional" -> Random.nextDouble(0.8, 0.95)
                    "enthusiast" -> Random.nextDouble(0.7, 0.9)
                    else -> Random.nextDouble(0.5, 0.8)
                }
            ),
            engagementHistory = engagementHistory,
            userContext = userContext,
            expectedBehavior = ExpectedPersonalizationBehavior(
                shouldHaveDiverseContent = userType == "explorer",
                shouldIncludeTrendingContent = userType != "focused",
                shouldBeHeavilyPersonalized = userType in listOf("enthusiast", "professional"),
                expectedTopicDistribution = topics.take(8).associateWith { Random.nextDouble(0.05, 0.25) },
                maxPersonalizationMultiplier = Random.nextDouble(1.0, 2.5)
            )
        )
    }

    private fun generateUserContext(): UserContext {
        return UserContext(
            timeOfDay = Random.nextInt(0, 24),
            dayOfWeek = Random.nextInt(1, 8),
            deviceType = listOf(DeviceType.MOBILE, DeviceType.DESKTOP, DeviceType.TABLET).random(),
            location = UserLocation(
                country = listOf("US", "CA", "UK", "DE", "FR", "AU").random(),
                region = listOf("North America", "Europe", "Asia Pacific").random()
            ),
            sessionDuration = Random.nextLong(5, 120),
            previousActivity = emptyList(),
            contextualPreferences = emptyMap()
        )
    }

    private fun generateEngagementHistory(interactionCount: Int, topics: List<String>): List<UserActivity> {
        return (1..interactionCount).map { index ->
            val topic = topics.random()
            val contentType = contentTypes.random()
            val engagementType = engagementTypes.random()
            val timestamp = Instant.now().minus(Random.nextLong(0, 365), ChronoUnit.DAYS)
            
            UserActivity(
                contentId = "content_${Random.nextInt(1, 10000)}",
                authorId = "author_${Random.nextInt(1, 100)}",
                topics = listOf(topic),
                engagementType = engagementType,
                engagementScore = Random.nextDouble(0.1, 1.0),
                timestamp = timestamp,
                sessionContext = mapOf(
                    "contentType" to contentType.name,
                    "quality" to qualityLevels.random()
                )
            )
        }
    }

    private fun generateExtensiveEngagementHistory(interactionCount: Int, topics: List<String>): List<UserActivity> {
        return (1..interactionCount).map { index ->
            val topic = topics.random()
            val contentType = contentTypes.random()
            val engagementType = when (Random.nextInt(10)) {
                in 0..4 -> EngagementType.VIEW     // 50% views
                in 5..7 -> EngagementType.LIKE     // 30% likes  
                8 -> EngagementType.SHARE          // 10% shares
                else -> EngagementType.COMMENT     // 10% comments
            }
            val timestamp = Instant.now().minus(Random.nextLong(0, 730), ChronoUnit.DAYS) // 2 years of history
            
            UserActivity(
                contentId = "content_${Random.nextInt(1, 50000)}",
                authorId = "author_${Random.nextInt(1, 200)}",
                topics = listOf(topic),
                engagementType = engagementType,
                engagementScore = Random.nextDouble(0.1, 1.0),
                timestamp = timestamp,
                sessionContext = mapOf(
                    "contentType" to contentType.name,
                    "quality" to qualityLevels.random(),
                    "complexity" to Random.nextInt(1, 10).toString(),
                    "authorReputation" to Random.nextDouble(0.1, 1.0).toString(),
                    "duration" to when (contentType) {
                        ContentType.VIDEO -> Random.nextDouble(30.0, 1800.0)    // 30s to 30min
                        ContentType.TEXT -> Random.nextDouble(60.0, 900.0)   // 1min to 15min  
                        else -> Random.nextDouble(5.0, 60.0)          // 5s to 1min
                    }.toString()
                )
            )
        }
    }

    private fun generateMinimalEngagementHistory(): List<UserActivity> {
        val interactionCount = Random.nextInt(0, 10)
        return if (interactionCount == 0) {
            emptyList()
        } else {
            (1..interactionCount).map { index ->
                val topic = allTopics.random()
                val contentType = contentTypes.random()
                val timestamp = Instant.now().minus(Random.nextLong(0, 30), ChronoUnit.DAYS)
                
                UserActivity(
                    contentId = "content_${Random.nextInt(1, 1000)}",
                    authorId = "author_${Random.nextInt(1, 50)}",
                    topics = listOf(topic),
                    engagementType = listOf(EngagementType.VIEW, EngagementType.LIKE).random(),
                    engagementScore = Random.nextDouble(0.1, 0.8),
                    timestamp = timestamp,
                    sessionContext = mapOf(
                        "contentType" to contentType.name,
                        "duration" to Random.nextDouble(5.0, 120.0).toString()
                    )
                )
            }
        }
    }

    private fun generateComplexEngagementHistory(topics: List<String>): List<UserActivity> {
        val interactionCount = Random.nextInt(1000, 5000)
        return (1..interactionCount).map { index ->
            val topic = topics.random()
            val contentType = contentTypes.random()
            val engagementType = engagementTypes.random()
            val timestamp = Instant.now().minus(Random.nextLong(0, 1095), ChronoUnit.DAYS) // 3 years
            
            UserActivity(
                contentId = "content_${Random.nextInt(1, 100000)}",
                authorId = "author_${Random.nextInt(1, 500)}",
                topics = listOf(topic),
                engagementType = engagementType,
                engagementScore = Random.nextDouble(0.1, 1.0),
                timestamp = timestamp,
                sessionContext = mapOf(
                    "contentType" to contentType.name,
                    "quality" to qualityLevels.random(),
                    "complexity" to Random.nextInt(1, 10).toString(),
                    "language" to listOf("en", "es", "fr", "de").random(),
                    "region" to listOf("US", "EU", "ASIA", "GLOBAL").random(),
                    "trending" to Random.nextBoolean().toString(),
                    "seasonal" to Random.nextBoolean().toString(),
                    "duration" to when (engagementType) {
                        EngagementType.VIEW -> Random.nextDouble(1.0, 3600.0).toString()
                        else -> "0"
                    }
                )
            )
        }
    }

    private fun generateRecentEngagementHistory(topics: List<String>): List<UserActivity> {
        val interactionCount = Random.nextInt(200, 800)
        return (1..interactionCount).map { index ->
            val topic = topics.random()
            val contentType = contentTypes.random()
            val engagementType = engagementTypes.random()
            val timestamp = Instant.now().minus(Random.nextLong(0, 90), ChronoUnit.DAYS) // Last 3 months
            
            UserActivity(
                contentId = "content_${Random.nextInt(1, 5000)}",
                authorId = "author_${Random.nextInt(1, 100)}",
                topics = listOf(topic),
                engagementType = engagementType,
                engagementScore = Random.nextDouble(0.2, 1.0),
                timestamp = timestamp,
                sessionContext = mapOf(
                    "contentType" to contentType.name,
                    "quality" to qualityLevels.random(),
                    "recency" to "recent",
                    "duration" to Random.nextDouble(10.0, 600.0).toString()
                )
            )
        }
    }

    private fun generateContentTitle(topics: List<String>, contentType: ContentType): String {
        val titleTemplates = mapOf(
            ContentType.TEXT to listOf(
                "Understanding {topic}: A Complete Guide",
                "The Future of {topic} in 2024", 
                "Top 10 {topic} Trends You Need to Know",
                "How {topic} is Transforming Industries",
                "The Ultimate {topic} Resource Collection"
            ),
            ContentType.VIDEO to listOf(
                "{topic} Explained in 10 Minutes",
                "Mastering {topic}: Video Tutorial",
                "The {topic} Revolution: Documentary",
                "{topic} Tips and Tricks",
                "Behind the Scenes: {topic} Industry"
            ),
            ContentType.IMAGE to listOf(
                "Stunning {topic} Photography Collection",
                "Visual Guide to {topic}",
                "{topic} Infographic: Key Stats",
                "Beautiful {topic} Artwork Gallery",
                "{topic} Before and After Comparison"
            )
        )
        
        val templates = titleTemplates[contentType] ?: titleTemplates[ContentType.TEXT]!!
        val template = templates.random()
        val topic = topics.firstOrNull() ?: "general"
        
        return template.replace("{topic}", topic.replaceFirstChar { it.titlecase() })
    }
}