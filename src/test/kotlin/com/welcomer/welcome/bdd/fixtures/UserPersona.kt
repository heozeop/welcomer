package com.welcomer.welcome.bdd.fixtures

import com.welcomer.welcome.engagement.model.EngagementType
import com.welcomer.welcome.ingestion.model.ContentType
import com.welcomer.welcome.personalization.service.DeviceType
import com.welcomer.welcome.personalization.service.UserActivity
import com.welcomer.welcome.personalization.service.UserContext
import com.welcomer.welcome.personalization.service.UserLocation
import com.welcomer.welcome.user.model.UserPreferenceProfile
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Enumeration of different user persona types for BDD testing
 */
enum class PersonaType {
    NEW_USER,
    POWER_USER,
    TOPIC_FOCUSED_USER,
    DIVERSE_CONSUMPTION_USER,
    CASUAL_USER,
    LURKER_USER,
    MOBILE_FIRST_USER,
    ACCESSIBILITY_USER
}

/**
 * Configurable user persona class for BDD testing scenarios
 */
data class UserPersona(
    val type: PersonaType,
    val userId: String,
    val description: String,
    val config: PersonaConfig = PersonaConfig()
) {
    
    fun generatePersonaData(): UserPersonaData {
        return when (type) {
            PersonaType.NEW_USER -> generateNewUserData()
            PersonaType.POWER_USER -> generatePowerUserData()
            PersonaType.TOPIC_FOCUSED_USER -> generateTopicFocusedUserData()
            PersonaType.DIVERSE_CONSUMPTION_USER -> generateDiverseConsumptionUserData()
            PersonaType.CASUAL_USER -> generateCasualUserData()
            PersonaType.LURKER_USER -> generateLurkerUserData()
            PersonaType.MOBILE_FIRST_USER -> generateMobileFirstUserData()
            PersonaType.ACCESSIBILITY_USER -> generateAccessibilityUserData()
        }
    }

    fun withConfig(configModifier: PersonaConfig.() -> PersonaConfig): UserPersona {
        return this.copy(config = configModifier(config))
    }

    fun withUserId(newUserId: String): UserPersona {
        return this.copy(userId = newUserId)
    }

    fun withTopicInterests(interests: Map<String, Double>): UserPersona {
        return withConfig { copy(topicInterests = interests) }
    }

    fun withDeviceType(deviceType: DeviceType): UserPersona {
        return withConfig { copy(deviceType = deviceType) }
    }

    fun withLocation(location: UserLocation): UserPersona {
        return withConfig { copy(location = location) }
    }

    private fun generateNewUserData(): UserPersonaData {
        val now = Instant.now()
        return UserPersonaData(
            userId = userId,
            description = description,
            preferenceProfile = UserPreferenceProfile(
                userId = userId,
                topicInterests = config.topicInterests,
                contentTypePreferences = config.contentTypePreferences,
                languagePreferences = config.languagePreferences,
                followedAccounts = config.followedAccounts,
                blockedUsers = emptySet(),
                blockedTopics = emptySet(),
                algorithmPreferences = emptyMap(),
                lastUpdated = now,
                confidence = 0.1
            ),
            engagementHistory = generateEngagementHistory(0, 2),
            userContext = UserContext(
                timeOfDay = config.timeOfDay,
                dayOfWeek = config.dayOfWeek,
                deviceType = config.deviceType,
                location = config.location,
                sessionDuration = config.sessionDuration,
                previousActivity = emptyList()
            ),
            expectedBehavior = ExpectedPersonalizationBehavior(
                shouldHaveDiverseContent = true,
                shouldIncludeTrendingContent = true,
                shouldBeHeavilyPersonalized = false,
                expectedTopicDistribution = emptyMap(),
                maxPersonalizationMultiplier = 1.2,
                preferredContentTypes = config.preferredContentTypes
            )
        )
    }

    private fun generatePowerUserData(): UserPersonaData {
        val now = Instant.now()
        return UserPersonaData(
            userId = userId,
            description = description,
            preferenceProfile = UserPreferenceProfile(
                userId = userId,
                topicInterests = config.topicInterests.ifEmpty { 
                    mapOf(
                        "technology" to 0.95,
                        "programming" to 0.9,
                        "ai" to 0.85,
                        "startup" to 0.7
                    )
                },
                contentTypePreferences = config.contentTypePreferences.ifEmpty {
                    mapOf(
                        "text" to 0.8,
                        "video" to 0.6
                    )
                },
                languagePreferences = config.languagePreferences,
                followedAccounts = config.followedAccounts.ifEmpty { 
                    setOf("tech-blogger-1", "ai-researcher-2", "startup-founder-3")
                },
                blockedUsers = emptySet(),
                blockedTopics = setOf("sports", "celebrity-gossip"),
                algorithmPreferences = mapOf("personalization_strength" to "0.9"),
                lastUpdated = now.minus(1, ChronoUnit.DAYS),
                confidence = 0.95
            ),
            engagementHistory = generateEngagementHistory(50, 100),
            userContext = UserContext(
                timeOfDay = config.timeOfDay,
                dayOfWeek = config.dayOfWeek,
                deviceType = config.deviceType,
                location = config.location,
                sessionDuration = config.sessionDuration,
                previousActivity = config.recentActivity
            ),
            expectedBehavior = ExpectedPersonalizationBehavior(
                shouldHaveDiverseContent = false,
                shouldIncludeTrendingContent = false,
                shouldBeHeavilyPersonalized = true,
                expectedTopicDistribution = mapOf(
                    "technology" to 0.4,
                    "programming" to 0.3,
                    "ai" to 0.2,
                    "other" to 0.1
                ),
                maxPersonalizationMultiplier = 2.5,
                preferredContentTypes = config.preferredContentTypes
            )
        )
    }

    private fun generateTopicFocusedUserData(): UserPersonaData {
        val now = Instant.now()
        val focusTopic = config.focusTopic ?: "fitness"
        
        return UserPersonaData(
            userId = userId,
            description = description,
            preferenceProfile = UserPreferenceProfile(
                userId = userId,
                topicInterests = config.topicInterests.ifEmpty {
                    when (focusTopic) {
                        "fitness" -> mapOf(
                            "fitness" to 0.9,
                            "health" to 0.85,
                            "nutrition" to 0.8,
                            "workout" to 0.85
                        )
                        "technology" -> mapOf(
                            "technology" to 0.9,
                            "programming" to 0.85,
                            "software" to 0.8
                        )
                        else -> mapOf(focusTopic to 0.9)
                    }
                },
                contentTypePreferences = config.contentTypePreferences.ifEmpty {
                    if (focusTopic == "fitness") mapOf("video" to 0.9, "image" to 0.7)
                    else mapOf("text" to 0.8, "image" to 0.6)
                },
                languagePreferences = config.languagePreferences,
                followedAccounts = config.followedAccounts.ifEmpty {
                    setOf("${focusTopic}-expert-1", "${focusTopic}-creator-2")
                },
                blockedUsers = emptySet(),
                blockedTopics = emptySet(),
                algorithmPreferences = mapOf("${focusTopic}_preference" to "0.9"),
                lastUpdated = now.minus(1, ChronoUnit.HOURS),
                confidence = 0.8
            ),
            engagementHistory = generateTopicFocusedEngagementHistory(focusTopic, 20, 40),
            userContext = UserContext(
                timeOfDay = config.timeOfDay,
                dayOfWeek = config.dayOfWeek,
                deviceType = config.deviceType,
                location = config.location,
                sessionDuration = config.sessionDuration
            ),
            expectedBehavior = ExpectedPersonalizationBehavior(
                shouldHaveDiverseContent = false,
                shouldIncludeTrendingContent = false,
                shouldBeHeavilyPersonalized = true,
                expectedTopicDistribution = mapOf(
                    focusTopic to 0.6,
                    "related" to 0.3,
                    "other" to 0.1
                ),
                maxPersonalizationMultiplier = 2.0,
                preferredContentTypes = config.preferredContentTypes
            )
        )
    }

    private fun generateDiverseConsumptionUserData(): UserPersonaData {
        val now = Instant.now()
        return UserPersonaData(
            userId = userId,
            description = description,
            preferenceProfile = UserPreferenceProfile(
                userId = userId,
                topicInterests = config.topicInterests.ifEmpty {
                    mapOf(
                        "technology" to 0.6,
                        "travel" to 0.65,
                        "cooking" to 0.55,
                        "photography" to 0.7,
                        "books" to 0.5,
                        "movies" to 0.6
                    )
                },
                contentTypePreferences = config.contentTypePreferences.ifEmpty {
                    mapOf(
                        "image" to 0.8,
                        "text" to 0.6,
                        "video" to 0.5
                    )
                },
                languagePreferences = config.languagePreferences,
                followedAccounts = config.followedAccounts.ifEmpty {
                    setOf("photographer-1", "travel-blogger-2", "chef-3", "tech-writer-4")
                },
                blockedUsers = emptySet(),
                blockedTopics = setOf("politics"),
                algorithmPreferences = mapOf("diversity_preference" to "0.8"),
                lastUpdated = now.minus(2, ChronoUnit.HOURS),
                confidence = 0.7
            ),
            engagementHistory = generateMixedEngagementHistory(25, 50),
            userContext = UserContext(
                timeOfDay = config.timeOfDay,
                dayOfWeek = config.dayOfWeek,
                deviceType = config.deviceType,
                location = config.location,
                sessionDuration = config.sessionDuration
            ),
            expectedBehavior = ExpectedPersonalizationBehavior(
                shouldHaveDiverseContent = true,
                shouldIncludeTrendingContent = true,
                shouldBeHeavilyPersonalized = false,
                expectedTopicDistribution = mapOf(
                    "photography" to 0.2,
                    "travel" to 0.2,
                    "cooking" to 0.15,
                    "technology" to 0.15,
                    "other" to 0.3
                ),
                maxPersonalizationMultiplier = 1.5,
                preferredContentTypes = config.preferredContentTypes
            )
        )
    }

    private fun generateCasualUserData(): UserPersonaData {
        val now = Instant.now()
        return UserPersonaData(
            userId = userId,
            description = description,
            preferenceProfile = UserPreferenceProfile(
                userId = userId,
                topicInterests = config.topicInterests.ifEmpty {
                    mapOf(
                        "entertainment" to 0.6,
                        "lifestyle" to 0.5,
                        "news" to 0.4
                    )
                },
                contentTypePreferences = config.contentTypePreferences.ifEmpty {
                    mapOf(
                        "image" to 0.7,
                        "video" to 0.6,
                        "text" to 0.4
                    )
                },
                languagePreferences = config.languagePreferences,
                followedAccounts = config.followedAccounts,
                blockedUsers = emptySet(),
                blockedTopics = emptySet(),
                algorithmPreferences = emptyMap(),
                lastUpdated = now.minus(1, ChronoUnit.DAYS),
                confidence = 0.5
            ),
            engagementHistory = generateEngagementHistory(5, 20),
            userContext = UserContext(
                timeOfDay = config.timeOfDay,
                dayOfWeek = config.dayOfWeek,
                deviceType = config.deviceType,
                location = config.location,
                sessionDuration = config.sessionDuration
            ),
            expectedBehavior = ExpectedPersonalizationBehavior(
                shouldHaveDiverseContent = true,
                shouldIncludeTrendingContent = true,
                shouldBeHeavilyPersonalized = false,
                expectedTopicDistribution = emptyMap(),
                maxPersonalizationMultiplier = 1.3,
                preferredContentTypes = config.preferredContentTypes
            )
        )
    }

    private fun generateLurkerUserData(): UserPersonaData {
        val now = Instant.now()
        return UserPersonaData(
            userId = userId,
            description = description,
            preferenceProfile = UserPreferenceProfile(
                userId = userId,
                topicInterests = config.topicInterests,
                contentTypePreferences = config.contentTypePreferences,
                languagePreferences = config.languagePreferences,
                followedAccounts = config.followedAccounts,
                blockedUsers = emptySet(),
                blockedTopics = emptySet(),
                algorithmPreferences = emptyMap(),
                lastUpdated = now.minus(7, ChronoUnit.DAYS),
                confidence = 0.2
            ),
            engagementHistory = generateViewOnlyEngagementHistory(10, 30),
            userContext = UserContext(
                timeOfDay = config.timeOfDay,
                dayOfWeek = config.dayOfWeek,
                deviceType = config.deviceType,
                location = config.location,
                sessionDuration = config.sessionDuration
            ),
            expectedBehavior = ExpectedPersonalizationBehavior(
                shouldHaveDiverseContent = true,
                shouldIncludeTrendingContent = true,
                shouldBeHeavilyPersonalized = false,
                expectedTopicDistribution = emptyMap(),
                maxPersonalizationMultiplier = 1.1,
                preferredContentTypes = config.preferredContentTypes
            )
        )
    }

    private fun generateMobileFirstUserData(): UserPersonaData {
        val now = Instant.now()
        return UserPersonaData(
            userId = userId,
            description = description,
            preferenceProfile = UserPreferenceProfile(
                userId = userId,
                topicInterests = config.topicInterests.ifEmpty {
                    mapOf(
                        "mobile" to 0.8,
                        "quick-reads" to 0.7,
                        "visual-content" to 0.8
                    )
                },
                contentTypePreferences = config.contentTypePreferences.ifEmpty {
                    mapOf(
                        "image" to 0.9,
                        "video" to 0.8,
                        "text" to 0.3
                    )
                },
                languagePreferences = config.languagePreferences,
                followedAccounts = config.followedAccounts,
                blockedUsers = emptySet(),
                blockedTopics = emptySet(),
                algorithmPreferences = mapOf("mobile_optimized" to "0.9"),
                lastUpdated = now.minus(2, ChronoUnit.HOURS),
                confidence = 0.7
            ),
            engagementHistory = generateMobileEngagementHistory(15, 35),
            userContext = UserContext(
                timeOfDay = config.timeOfDay,
                dayOfWeek = config.dayOfWeek,
                deviceType = DeviceType.MOBILE,
                location = config.location,
                sessionDuration = 10
            ),
            expectedBehavior = ExpectedPersonalizationBehavior(
                shouldHaveDiverseContent = true,
                shouldIncludeTrendingContent = true,
                shouldBeHeavilyPersonalized = false,
                expectedTopicDistribution = emptyMap(),
                maxPersonalizationMultiplier = 1.4,
                preferredContentTypes = listOf(ContentType.IMAGE, ContentType.VIDEO)
            )
        )
    }

    private fun generateAccessibilityUserData(): UserPersonaData {
        val now = Instant.now()
        return UserPersonaData(
            userId = userId,
            description = description,
            preferenceProfile = UserPreferenceProfile(
                userId = userId,
                topicInterests = config.topicInterests,
                contentTypePreferences = config.contentTypePreferences.ifEmpty {
                    mapOf(
                        "text" to 0.9,
                        "image" to 0.3,
                        "video" to 0.4
                    )
                },
                languagePreferences = config.languagePreferences,
                followedAccounts = config.followedAccounts,
                blockedUsers = emptySet(),
                blockedTopics = emptySet(),
                algorithmPreferences = mapOf(
                    "accessibility_mode" to "1.0",
                    "text_preferred" to "0.9",
                    "alt_text_required" to "1.0"
                ),
                lastUpdated = now.minus(3, ChronoUnit.HOURS),
                confidence = 0.8
            ),
            engagementHistory = generateTextFocusedEngagementHistory(20, 40),
            userContext = UserContext(
                timeOfDay = config.timeOfDay,
                dayOfWeek = config.dayOfWeek,
                deviceType = config.deviceType,
                location = config.location,
                sessionDuration = config.sessionDuration,
                contextualPreferences = mapOf(
                    "screen_reader" to 1.0,
                    "high_contrast" to 1.0
                )
            ),
            expectedBehavior = ExpectedPersonalizationBehavior(
                shouldHaveDiverseContent = true,
                shouldIncludeTrendingContent = true,
                shouldBeHeavilyPersonalized = false,
                expectedTopicDistribution = emptyMap(),
                maxPersonalizationMultiplier = 1.3,
                preferredContentTypes = listOf(ContentType.TEXT)
            )
        )
    }

    // Helper methods for generating engagement histories
    private fun generateEngagementHistory(minCount: Int, maxCount: Int): List<UserActivity> {
        val count = Random.nextInt(minCount, maxCount + 1)
        val now = Instant.now()
        
        return (0 until count).map { i ->
            UserActivity(
                contentId = "content-$i-${Random.nextInt(1000)}",
                authorId = "author-${Random.nextInt(10)}",
                topics = listOf("topic-${Random.nextInt(5)}"),
                engagementType = listOf(
                    EngagementType.LIKE, 
                    EngagementType.VIEW, 
                    EngagementType.SHARE,
                    EngagementType.BOOKMARK
                ).random(),
                engagementScore = Random.nextDouble(0.5, 1.0),
                timestamp = now.minus(Random.nextLong(1, 30), ChronoUnit.DAYS)
            )
        }
    }

    private fun generateTopicFocusedEngagementHistory(topic: String, minCount: Int, maxCount: Int): List<UserActivity> {
        val count = Random.nextInt(minCount, maxCount + 1)
        val now = Instant.now()
        
        return (0 until count).map { i ->
            UserActivity(
                contentId = "$topic-content-$i",
                authorId = "$topic-author-${i % 3}",
                topics = listOf(topic, "related-${Random.nextInt(3)}"),
                engagementType = listOf(
                    EngagementType.LIKE,
                    EngagementType.BOOKMARK,
                    EngagementType.SHARE
                ).random(),
                engagementScore = Random.nextDouble(0.7, 1.0),
                timestamp = now.minus(Random.nextLong(1, 14), ChronoUnit.DAYS)
            )
        }
    }

    private fun generateMixedEngagementHistory(minCount: Int, maxCount: Int): List<UserActivity> {
        val topics = listOf("photography", "travel", "cooking", "technology", "books")
        val count = Random.nextInt(minCount, maxCount + 1)
        val now = Instant.now()
        
        return (0 until count).map { i ->
            val topic = topics[i % topics.size]
            UserActivity(
                contentId = "$topic-mixed-$i",
                authorId = "$topic-author-${i % 2}",
                topics = listOf(topic),
                engagementType = listOf(
                    EngagementType.LIKE,
                    EngagementType.VIEW,
                    EngagementType.BOOKMARK
                ).random(),
                engagementScore = Random.nextDouble(0.5, 0.9),
                timestamp = now.minus(Random.nextLong(1, 21), ChronoUnit.DAYS)
            )
        }
    }

    private fun generateViewOnlyEngagementHistory(minCount: Int, maxCount: Int): List<UserActivity> {
        val count = Random.nextInt(minCount, maxCount + 1)
        val now = Instant.now()
        
        return (0 until count).map { i ->
            UserActivity(
                contentId = "viewed-content-$i",
                authorId = "author-${Random.nextInt(20)}",
                topics = listOf("general-${Random.nextInt(10)}"),
                engagementType = EngagementType.VIEW,
                engagementScore = Random.nextDouble(0.2, 0.5),
                timestamp = now.minus(Random.nextLong(1, 60), ChronoUnit.DAYS)
            )
        }
    }

    private fun generateMobileEngagementHistory(minCount: Int, maxCount: Int): List<UserActivity> {
        val count = Random.nextInt(minCount, maxCount + 1)
        val now = Instant.now()
        
        return (0 until count).map { i ->
            UserActivity(
                contentId = "mobile-content-$i",
                authorId = "mobile-creator-${Random.nextInt(5)}",
                topics = listOf("mobile-friendly", "quick-content"),
                engagementType = listOf(
                    EngagementType.LIKE,
                    EngagementType.VIEW,
                    EngagementType.SCROLL
                ).random(),
                engagementScore = Random.nextDouble(0.4, 0.8),
                timestamp = now.minus(Random.nextLong(1, 7), ChronoUnit.DAYS),
                sessionContext = mapOf("device" to "mobile", "session_short" to true)
            )
        }
    }

    private fun generateTextFocusedEngagementHistory(minCount: Int, maxCount: Int): List<UserActivity> {
        val count = Random.nextInt(minCount, maxCount + 1)
        val now = Instant.now()
        
        return (0 until count).map { i ->
            UserActivity(
                contentId = "text-content-$i",
                authorId = "text-author-${Random.nextInt(8)}",
                topics = listOf("accessible-content", "text-based"),
                engagementType = listOf(
                    EngagementType.VIEW,
                    EngagementType.BOOKMARK,
                    EngagementType.SHARE
                ).random(),
                engagementScore = Random.nextDouble(0.6, 1.0),
                timestamp = now.minus(Random.nextLong(1, 30), ChronoUnit.DAYS),
                sessionContext = mapOf("accessibility_mode" to true)
            )
        }
    }
}

/**
 * Configuration class for customizing persona characteristics
 */
data class PersonaConfig(
    val topicInterests: Map<String, Double> = emptyMap(),
    val contentTypePreferences: Map<String, Double> = emptyMap(),
    val languagePreferences: List<String> = listOf("en"),
    val followedAccounts: Set<String> = emptySet(),
    val deviceType: DeviceType = DeviceType.DESKTOP,
    val location: UserLocation? = UserLocation(country = "US", city = "san-francisco"),
    val timeOfDay: Int = 14,
    val dayOfWeek: Int = 3,
    val sessionDuration: Long = 20,
    val recentActivity: List<String> = emptyList(),
    val preferredContentTypes: List<ContentType> = emptyList(),
    val focusTopic: String? = null
)

/**
 * Data class representing a complete user persona for testing
 */
data class UserPersonaData(
    val userId: String,
    val description: String,
    val preferenceProfile: UserPreferenceProfile,
    val engagementHistory: List<UserActivity>,
    val userContext: UserContext,
    val expectedBehavior: ExpectedPersonalizationBehavior
)

/**
 * Expected personalization behavior for assertions
 */
data class ExpectedPersonalizationBehavior(
    val shouldHaveDiverseContent: Boolean,
    val shouldIncludeTrendingContent: Boolean,
    val shouldBeHeavilyPersonalized: Boolean,
    val expectedTopicDistribution: Map<String, Double>,
    val maxPersonalizationMultiplier: Double,
    val expectedSourceAffinity: Map<String, Double> = emptyMap(),
    val preferredContentTypes: List<ContentType> = emptyList()
)