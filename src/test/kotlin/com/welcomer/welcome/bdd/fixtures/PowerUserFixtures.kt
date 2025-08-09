package com.welcomer.welcome.bdd.fixtures

import com.welcomer.welcome.engagement.model.EngagementType
import com.welcomer.welcome.ingestion.model.ContentType
import com.welcomer.welcome.personalization.service.*
import com.welcomer.welcome.user.model.UserPreferenceProfile
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Specialized fixtures for power user BDD scenarios
 * 
 * Power users are characterized by:
 * - Extensive engagement history (500+ interactions)
 * - High topic interest scores (0.8+)
 * - Well-established preferences (high confidence)
 * - Strong personalization multipliers (2.0+)
 * - Selective, high-quality engagement patterns
 */
object PowerUserFixtures {

    /**
     * Create a power user with extensive engagement in specific topics
     */
    fun createTopicSpecializedPowerUser(
        userId: String,
        primaryTopic: String,
        secondaryTopic: String,
        interactionCount: Int = 500
    ): UserPersona {
        return UserPersona(
            type = PersonaType.POWER_USER,
            userId = userId,
            description = "Power user specialized in $primaryTopic and $secondaryTopic"
        ).withTopicInterests(mapOf(
            primaryTopic to 0.95,
            secondaryTopic to 0.9,
            "related-${primaryTopic}" to 0.75,
            "advanced-${primaryTopic}" to 0.85
        )).withConfig {
            copy(
                followedAccounts = setOf(
                    "$primaryTopic-expert-1", 
                    "$secondaryTopic-guru-2", 
                    "$primaryTopic-researcher-3",
                    "industry-leader-$primaryTopic"
                ),
                contentTypePreferences = mapOf(
                    "text" to 0.85,
                    "video" to 0.75,
                    "image" to 0.6
                )
            )
        }
    }

    /**
     * Create a power user with high social influence (shares, comments frequently)
     */
    fun createSocialInfluencerPowerUser(
        userId: String,
        followedAuthorsCount: Int = 30
    ): UserPersona {
        val followedAccounts = (1..followedAuthorsCount).map { "influencer-author-$it" }.toSet()
        
        return UserPersona(
            type = PersonaType.POWER_USER,
            userId = userId,
            description = "Power user with high social influence and engagement"
        ).withTopicInterests(mapOf(
            "technology" to 0.9,
            "business" to 0.85,
            "leadership" to 0.8,
            "innovation" to 0.88
        )).withConfig {
            copy(
                followedAccounts = followedAccounts,
                contentTypePreferences = mapOf(
                    "text" to 0.8,
                    "video" to 0.7,
                    "image" to 0.75
                ),
                recentActivity = listOf("shared", "commented", "liked", "bookmarked")
            )
        }
    }

    /**
     * Create a power user with selective, high-quality preferences
     */
    fun createSelectivePowerUser(
        userId: String,
        qualityThreshold: Double = 0.9
    ): UserPersona {
        return UserPersona(
            type = PersonaType.POWER_USER,
            userId = userId,
            description = "Highly selective power user with premium quality standards"
        ).withTopicInterests(mapOf(
            "premium-content" to 0.98,
            "expert-analysis" to 0.95,
            "research" to 0.92,
            "thought-leadership" to 0.88
        )).withConfig {
            copy(
                followedAccounts = setOf(
                    "premium-analyst-1", 
                    "research-director-2", 
                    "industry-expert-3",
                    "thought-leader-4"
                ),
                contentTypePreferences = mapOf(
                    "text" to 0.9,
                    "video" to 0.8
                )
            )
        }
    }

    /**
     * Create a power user with financial/market focus for time-sensitive content
     */
    fun createFinancialPowerUser(
        userId: String,
        marketHours: Boolean = true
    ): UserPersona {
        val timeOfDay = if (marketHours) 11 else 19 // 11 AM or 7 PM
        
        return UserPersona(
            type = PersonaType.POWER_USER,
            userId = userId,
            description = "Power user focused on financial markets and investing"
        ).withTopicInterests(mapOf(
            "finance" to 0.98,
            "investing" to 0.95,
            "market-analysis" to 0.93,
            "trading" to 0.88,
            "economics" to 0.85
        )).withConfig {
            copy(
                timeOfDay = timeOfDay,
                followedAccounts = setOf(
                    "market-analyst-1",
                    "financial-advisor-2", 
                    "investment-strategist-3",
                    "economic-researcher-4"
                ),
                contentTypePreferences = mapOf(
                    "text" to 0.9,
                    "video" to 0.7,
                    "image" to 0.6
                )
            )
        }
    }

    /**
     * Create a power user with high-volume consumption patterns
     */
    fun createHighVolumePowerUser(
        userId: String,
        dailyConsumption: Int = 75
    ): UserPersona {
        return UserPersona(
            type = PersonaType.POWER_USER,
            userId = userId,
            description = "High-volume power user consuming $dailyConsumption+ items daily"
        ).withTopicInterests(mapOf(
            "technology" to 0.9,
            "science" to 0.85,
            "business" to 0.8,
            "innovation" to 0.82,
            "research" to 0.88
        )).withConfig {
            copy(
                sessionDuration = 45, // Long sessions
                followedAccounts = (1..25).map { "high-volume-source-$it" }.toSet(),
                contentTypePreferences = mapOf(
                    "text" to 0.8,
                    "video" to 0.6,
                    "image" to 0.7
                )
            )
        }
    }

    /**
     * Create a power user with multi-topic interests but distinct patterns
     */
    fun createMultiInterestPowerUser(
        userId: String,
        interests: Map<String, Double> = mapOf(
            "technology" to 0.9,
            "fitness" to 0.85, 
            "finance" to 0.82
        )
    ): UserPersona {
        return UserPersona(
            type = PersonaType.POWER_USER,
            userId = userId,
            description = "Power user with multiple distinct interest areas"
        ).withTopicInterests(interests).withConfig {
            copy(
                followedAccounts = interests.keys.flatMap { topic ->
                    listOf("$topic-expert-1", "$topic-specialist-2")
                }.toSet(),
                contentTypePreferences = mapOf(
                    "text" to 0.8,
                    "video" to 0.7,
                    "image" to 0.6
                )
            )
        }
    }

    /**
     * Create a power user with evolving interests (established + emerging)
     */
    fun createEvolvingInterestsPowerUser(
        userId: String,
        establishedTopic: String,
        emergingTopic: String
    ): UserPersona {
        return UserPersona(
            type = PersonaType.POWER_USER,
            userId = userId,
            description = "Power user with evolving interests from $establishedTopic to $emergingTopic"
        ).withTopicInterests(mapOf(
            establishedTopic to 0.95, // Strong established interest
            emergingTopic to 0.65,    // Growing but not yet strong
            "related-${establishedTopic}" to 0.8,
            "bridge-${establishedTopic}-${emergingTopic}" to 0.7
        )).withConfig {
            copy(
                followedAccounts = setOf(
                    "$establishedTopic-veteran-1",
                    "$emergingTopic-pioneer-2",
                    "cross-domain-expert-3"
                )
            )
        }
    }

    /**
     * Generate enhanced engagement history for power users
     */
    fun generatePowerUserEngagementHistory(
        userId: String,
        primaryTopics: List<String>,
        minCount: Int = 500,
        maxCount: Int = 1000
    ): List<UserActivity> {
        val count = Random.nextInt(minCount, maxCount + 1)
        val now = Instant.now()
        
        return (0 until count).map { i ->
            val topic = primaryTopics[i % primaryTopics.size]
            val dayOffset = Random.nextLong(1, 365) // Up to 1 year of history
            
            // Power users have higher engagement scores and more diverse engagement types
            val engagementTypes = listOf(
                EngagementType.LIKE,
                EngagementType.BOOKMARK,
                EngagementType.SHARE,
                EngagementType.COMMENT,
                EngagementType.VIEW
            )
            
            UserActivity(
                contentId = "power-${topic}-content-$i",
                authorId = "$topic-expert-${i % 10}",
                topics = listOf(topic, "advanced-$topic"),
                engagementType = engagementTypes[i % engagementTypes.size],
                engagementScore = Random.nextDouble(0.7, 1.0), // Higher engagement scores
                timestamp = now.minus(dayOffset, ChronoUnit.DAYS),
                sessionContext = mapOf(
                    "session_type" to "focused",
                    "expertise_level" to "advanced",
                    "engagement_quality" to "high"
                )
            )
        }.sortedByDescending { it.timestamp }
    }

    /**
     * Generate power user preference profile with high confidence
     */
    fun generatePowerUserPreferenceProfile(
        userId: String,
        topicInterests: Map<String, Double>,
        followedAccounts: Set<String>
    ): UserPreferenceProfile {
        val now = Instant.now()
        
        return UserPreferenceProfile(
            userId = userId,
            topicInterests = topicInterests,
            contentTypePreferences = mapOf(
                "text" to 0.85,
                "video" to 0.7,
                "image" to 0.6,
                "link" to 0.8
            ),
            languagePreferences = listOf("en"),
            followedAccounts = followedAccounts,
            blockedUsers = setOf("spam-user-1", "low-quality-creator-2"),
            blockedTopics = setOf("celebrity-gossip", "clickbait", "superficial-content"),
            algorithmPreferences = mapOf(
                "personalization_strength" to "0.95",
                "quality_threshold" to "0.8",
                "diversity_factor" to "0.2", // Low diversity, high focus
                "freshness_weight" to "0.7",
                "authority_boost" to "0.9"
            ),
            lastUpdated = now.minus(Random.nextLong(1, 7), ChronoUnit.DAYS),
            confidence = Random.nextDouble(0.9, 0.98) // Very high confidence
        )
    }

    /**
     * Generate expected behavior for power users
     */
    fun generatePowerUserExpectedBehavior(
        topicDistribution: Map<String, Double> = mapOf(
            "primary" to 0.6,
            "secondary" to 0.25,
            "discovery" to 0.15
        ),
        personalizationMultiplier: Double = 2.5
    ): ExpectedPersonalizationBehavior {
        return ExpectedPersonalizationBehavior(
            shouldHaveDiverseContent = false, // Power users want focused content
            shouldIncludeTrendingContent = false, // Quality over trending
            shouldBeHeavilyPersonalized = true,
            expectedTopicDistribution = topicDistribution,
            maxPersonalizationMultiplier = personalizationMultiplier,
            expectedSourceAffinity = mapOf(
                "followed_authors" to 0.6,
                "high_reputation" to 0.3,
                "new_similar" to 0.1
            ),
            preferredContentTypes = listOf(ContentType.TEXT, ContentType.VIDEO)
        )
    }

    /**
     * Create power user data with full customization
     */
    fun createCustomPowerUserData(
        userId: String,
        description: String,
        topicInterests: Map<String, Double>,
        followedAccounts: Set<String>,
        engagementHistoryCount: Int = 600,
        deviceType: DeviceType = DeviceType.DESKTOP,
        timeOfDay: Int = 14
    ): UserPersonaData {
        val engagementHistory = generatePowerUserEngagementHistory(
            userId, 
            topicInterests.keys.toList(), 
            engagementHistoryCount,
            engagementHistoryCount + 200
        )
        
        val preferenceProfile = generatePowerUserPreferenceProfile(
            userId,
            topicInterests,
            followedAccounts
        )
        
        val expectedBehavior = generatePowerUserExpectedBehavior()
        
        return UserPersonaData(
            userId = userId,
            description = description,
            preferenceProfile = preferenceProfile,
            engagementHistory = engagementHistory,
            userContext = UserContext(
                timeOfDay = timeOfDay,
                dayOfWeek = 3, // Wednesday
                deviceType = deviceType,
                location = UserLocation(country = "US", region = "California", city = "san-francisco"),
                sessionDuration = 30,
                previousActivity = listOf("focused-reading", "bookmarking", "sharing"),
                contextualPreferences = mapOf(
                    "quality_mode" to 1.0,
                    "expertise_level" to 0.9,
                    "personalization_strength" to 0.95
                )
            ),
            expectedBehavior = expectedBehavior
        )
    }

    /**
     * Validate that a persona meets power user criteria
     */
    fun validatePowerUserCriteria(personaData: UserPersonaData): Boolean {
        return personaData.engagementHistory.size >= 50 &&
                personaData.preferenceProfile.confidence >= 0.8 &&
                personaData.expectedBehavior.shouldBeHeavilyPersonalized &&
                personaData.expectedBehavior.maxPersonalizationMultiplier >= 2.0 &&
                personaData.preferenceProfile.topicInterests.values.any { it >= 0.8 }
    }

    /**
     * Get power user test scenarios with pre-configured personas
     */
    fun getPowerUserTestScenarios(): Map<String, UserPersona> {
        return mapOf(
            "tech_specialist" to createTopicSpecializedPowerUser(
                "power-tech-specialist",
                "technology",
                "programming",
                750
            ),
            "social_influencer" to createSocialInfluencerPowerUser(
                "power-social-influencer",
                35
            ),
            "quality_selective" to createSelectivePowerUser(
                "power-quality-selective",
                0.9
            ),
            "financial_trader" to createFinancialPowerUser(
                "power-financial-trader",
                true
            ),
            "high_volume_consumer" to createHighVolumePowerUser(
                "power-high-volume",
                100
            ),
            "multi_interest" to createMultiInterestPowerUser(
                "power-multi-interest",
                mapOf(
                    "technology" to 0.9,
                    "health" to 0.85,
                    "business" to 0.8
                )
            ),
            "evolving_interests" to createEvolvingInterestsPowerUser(
                "power-evolving",
                "web-development",
                "mobile-development"
            )
        )
    }
}