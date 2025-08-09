package com.welcomer.welcome.bdd.fixtures

import com.welcomer.welcome.engagement.model.EngagementType
import com.welcomer.welcome.ingestion.model.ContentStatus
import com.welcomer.welcome.ingestion.model.ContentType
import com.welcomer.welcome.ingestion.model.ContentVisibility
import com.welcomer.welcome.ingestion.model.StoredContent
import com.welcomer.welcome.personalization.service.*
import com.welcomer.welcome.user.model.UserPreferenceProfile
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Specialized fixtures for edge case personalization testing
 * Handles complex user scenarios like conflicting preferences, rapid changes, and cold starts
 */
object EdgeCaseFixtures {

    /**
     * Create user personas with conflicting preferences
     */
    fun createConflictingPreferencePersonas(): Map<String, UserPersona> {
        return mapOf(
            "minimalism_maximalism_conflict" to createMinimalismMaximalismConflictUser(),
            "work_personal_conflict" to createWorkPersonalConflictUser(),
            "quality_quantity_conflict" to createQualityQuantityConflictUser(),
            "social_personal_conflict" to createSocialPersonalConflictUser(),
            "stated_behavior_conflict" to createStatedBehaviorConflictUser(),
            "accessibility_visual_conflict" to createAccessibilityVisualConflictUser()
        )
    }

    /**
     * Create user personas with rapidly changing interests
     */
    fun createRapidChangePersonas(): Map<String, UserPersona> {
        return mapOf(
            "photography_to_music" to createPhotographyToMusicTransition(),
            "seasonal_volatility" to createSeasonalVolatilityUser(),
            "interest_fatigue" to createInterestFatigueUser(),
            "temporary_wedding_interest" to createTemporaryWeddingInterestUser(),
            "rapid_shift_user" to createRapidInterestShiftUser()
        )
    }

    /**
     * Create user personas for cold start scenarios
     */
    fun createColdStartPersonas(): Map<String, UserPersona> {
        return mapOf(
            "new_content_type_introduction" to createNewContentTypeUser(),
            "returning_after_absence" to createReturningUserPersona(),
            "privacy_limited" to createPrivacyLimitedUser(),
            "minimal_preferences" to createMinimalPreferencesUser(),
            "semi_cold_start" to createSemiColdStartUser()
        )
    }

    /**
     * Create user personas for complex edge cases
     */
    fun createComplexEdgeCasePersonas(): Map<String, UserPersona> {
        return mapOf(
            "multi_persona_account" to createMultiPersonaAccount(),
            "niche_interest_scarcity" to createNicheInterestUser(),
            "filter_bubble_detected" to createFilterBubbleUser(),
            "algorithm_change_affected" to createAlgorithmChangeUser(),
            "multilingual_conflict" to createMultilingualConflictUser(),
            "safety_guideline_conflict" to createSafetyGuidelineConflictUser()
        )
    }

    // Conflicting Preference Personas
    private fun createMinimalismMaximalismConflictUser(): UserPersona {
        return UserPersona(
            type = PersonaType.NEW_USER,
            userId = "minimalism-maximalism-conflict",
            description = "User with contradictory minimalism and maximalism interests"
        ).withTopicInterests(mapOf(
            "minimalism" to 0.9,
            "maximalism" to 0.85,
            "design" to 0.8,
            "lifestyle" to 0.7,
            "philosophy" to 0.6
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.8, "image" to 0.7),
                followedAccounts = setOf("minimalist-guru", "maximalist-designer", "philosophy-writer")
            )
        }
    }

    private fun createWorkPersonalConflictUser(): UserPersona {
        return UserPersona(
            type = PersonaType.POWER_USER,
            userId = "work-personal-conflict",
            description = "User with conflicting work and personal interests"
        ).withTopicInterests(mapOf(
            "corporate-finance" to 0.9,
            "anti-corporate-activism" to 0.85,
            "finance" to 0.8,
            "social-justice" to 0.7,
            "economics" to 0.75
        )).withConfig {
            copy(
                timeOfDay = 14, // Business hours
                contentTypePreferences = mapOf("text" to 0.9, "video" to 0.6),
                followedAccounts = setOf("finance-expert", "activist-leader", "economic-analyst")
            )
        }
    }

    private fun createQualityQuantityConflictUser(): UserPersona {
        return UserPersona(
            type = PersonaType.DIVERSE_CONSUMPTION_USER,
            userId = "quality-quantity-conflict",
            description = "User who consumes both high-quality and quick content"
        ).withTopicInterests(mapOf(
            "in-depth-analysis" to 0.9,
            "quick-entertainment" to 0.8,
            "news" to 0.7,
            "analysis" to 0.85,
            "casual-content" to 0.6
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.8, "image" to 0.7, "video" to 0.6),
                deviceType = DeviceType.MOBILE,
                sessionDuration = 25
            )
        }
    }

    private fun createSocialPersonalConflictUser(): UserPersona {
        return UserPersona(
            type = PersonaType.NEW_USER,
            userId = "social-personal-conflict",
            description = "User with conflicts between personal preferences and social influence"
        ).withTopicInterests(mapOf(
            "quiet-contemplative" to 0.95,
            "viral-trending" to 0.6,
            "meditation" to 0.9,
            "mindfulness" to 0.85,
            "trending" to 0.5
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.9, "image" to 0.6),
                followedAccounts = setOf("meditation-teacher", "mindfulness-coach")
            )
        }
    }

    private fun createStatedBehaviorConflictUser(): UserPersona {
        return UserPersona(
            type = PersonaType.NEW_USER,
            userId = "stated-behavior-conflict",
            description = "User whose stated preferences conflict with actual behavior"
        ).withTopicInterests(mapOf(
            "sports" to -0.5, // Stated dislike
            "anti-sports" to 0.3,
            "books" to 0.8,
            "reading" to 0.7
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.9),
                // Blocked topics include sports, but engagement history would show otherwise
                followedAccounts = setOf("book-reviewer", "literature-critic")
            )
        }
    }

    private fun createAccessibilityVisualConflictUser(): UserPersona {
        return UserPersona(
            type = PersonaType.ACCESSIBILITY_USER,
            userId = "accessibility-visual-conflict",
            description = "User with accessibility needs but interests in visual content"
        ).withTopicInterests(mapOf(
            "visual-art" to 0.9,
            "photography" to 0.85,
            "design" to 0.8,
            "accessibility" to 0.9,
            "inclusive-design" to 0.8
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.9, "image" to 0.7), // Needs text but likes visual
                followedAccounts = setOf("accessibility-advocate", "inclusive-designer", "art-critic")
            )
        }
    }

    // Rapid Change Personas
    private fun createPhotographyToMusicTransition(): UserPersona {
        return UserPersona(
            type = PersonaType.NEW_USER,
            userId = "photography-to-music-transition",
            description = "User rapidly transitioning from photography to music production"
        ).withTopicInterests(mapOf(
            "photography" to 0.4, // Declining from previous 0.9
            "music-production" to 0.95, // New intense interest
            "audio" to 0.8,
            "creative" to 0.9,
            "visual-audio-bridge" to 0.7
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.8, "video" to 0.9), // Learning through video
                followedAccounts = setOf("music-producer", "audio-engineer", "former-photographer")
            )
        }
    }

    private fun createSeasonalVolatilityUser(): UserPersona {
        return UserPersona(
            type = PersonaType.NEW_USER,
            userId = "seasonal-volatility",
            description = "User with dramatically changing seasonal interests"
        ).withTopicInterests(mapOf(
            "winter-sports" to 0.2, // Very low - out of season
            "summer-travel" to 0.9, // High current seasonal interest
            "seasonal-activities" to 0.8,
            "outdoor" to 0.85,
            "adventure" to 0.8
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("image" to 0.8, "video" to 0.7, "text" to 0.6),
                followedAccounts = setOf("travel-blogger", "adventure-photographer", "seasonal-guide")
            )
        }
    }

    private fun createInterestFatigueUser(): UserPersona {
        return UserPersona(
            type = PersonaType.POWER_USER,
            userId = "interest-fatigue",
            description = "User experiencing fatigue with long-term interest"
        ).withTopicInterests(mapOf(
            "technology" to 0.95, // Still high but showing fatigue signals
            "innovation" to 0.7, // Adjacent topics becoming more interesting
            "science" to 0.75,
            "creativity" to 0.8,
            "burnout-recovery" to 0.6 // Subconscious interest in variety
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.8, "video" to 0.6),
                followedAccounts = setOf("tech-veteran", "innovation-thinker", "science-communicator")
            )
        }
    }

    private fun createTemporaryWeddingInterestUser(): UserPersona {
        return UserPersona(
            type = PersonaType.TOPIC_FOCUSED_USER,
            userId = "temporary-wedding-interest",
            description = "User with temporary wedding planning interest spike"
        ).withTopicInterests(mapOf(
            "science" to 0.9, // Stable baseline interest
            "research" to 0.85,
            "wedding" to 0.95, // Temporary spike
            "event-planning" to 0.8,
            "temporary-event" to 0.7
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.8, "image" to 0.9), // Visual for wedding planning
                followedAccounts = setOf("scientist", "wedding-planner", "event-coordinator")
            )
        }
    }

    private fun createRapidInterestShiftUser(): UserPersona {
        return UserPersona(
            type = PersonaType.NEW_USER,
            userId = "rapid-interest-shift",
            description = "User showing rapid interest shift patterns"
        ).withTopicInterests(mapOf(
            "previous-interest" to 0.3, // Rapidly declining
            "current-passion" to 0.95, // New intense focus
            "emerging-interest" to 0.6, // Starting to develop
            "transition" to 0.8,
            "adaptability" to 0.7
        ))
    }

    // Cold Start Personas
    private fun createNewContentTypeUser(): UserPersona {
        return UserPersona(
            type = PersonaType.POWER_USER, // Established user encountering new content type
            userId = "new-content-type-introduction",
            description = "Established user being introduced to podcasts"
        ).withTopicInterests(mapOf(
            "technology" to 0.9,
            "programming" to 0.85,
            "learning" to 0.8,
            "audio-learning" to 0.0 // No experience with audio content yet
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.9, "image" to 0.7), // No audio preference yet
                followedAccounts = setOf("tech-blogger", "programming-instructor")
            )
        }
    }

    private fun createReturningUserPersona(): UserPersona {
        return UserPersona(
            type = PersonaType.NEW_USER, // Treated as new due to long absence
            userId = "returning-after-absence",
            description = "User returning after 1 year absence with potentially stale preferences"
        ).withTopicInterests(mapOf(
            "travel" to 0.6, // Reduced confidence due to staleness
            "photography" to 0.5, // May have changed
            "stale-preferences" to 0.3,
            "needs-update" to 0.8
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("image" to 0.7, "text" to 0.6),
                // Reduced confidence in all preferences
                followedAccounts = setOf("travel-blogger-old", "photographer-vintage")
            )
        }
    }

    private fun createPrivacyLimitedUser(): UserPersona {
        return UserPersona(
            type = PersonaType.NEW_USER,
            userId = "privacy-limited",
            description = "User who wants personalization but has limited behavioral tracking"
        ).withTopicInterests(mapOf(
            "general" to 0.5, // Very basic preferences only
            "privacy" to 0.8,
            "quality-content" to 0.7
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.6), // Minimal preferences
                followedAccounts = emptySet() // No social connections tracked
            )
        }
    }

    private fun createMinimalPreferencesUser(): UserPersona {
        return UserPersona(
            type = PersonaType.NEW_USER,
            userId = "minimal-preferences",
            description = "User with minimal explicit preferences set"
        ).withTopicInterests(mapOf(
            "general" to 0.4,
            "popular" to 0.5
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.5), // Default preferences
                followedAccounts = emptySet()
            )
        }
    }

    private fun createSemiColdStartUser(): UserPersona {
        return UserPersona(
            type = PersonaType.NEW_USER,
            userId = "semi-cold-start",
            description = "User in semi-cold start situation with some stale data"
        ).withTopicInterests(mapOf(
            "outdated-interest" to 0.4,
            "uncertain" to 0.5,
            "needs-refresh" to 0.7
        ))
    }

    // Complex Edge Case Personas
    private fun createMultiPersonaAccount(): UserPersona {
        return UserPersona(
            type = PersonaType.DIVERSE_CONSUMPTION_USER,
            userId = "multi-persona-account",
            description = "Account showing usage patterns of multiple people"
        ).withTopicInterests(mapOf(
            "children-content" to 0.7, // Family member
            "investment-advice" to 0.8, // Adult member
            "cooking" to 0.7, // Shared interest
            "gaming" to 0.6, // Another member
            "family-shared" to 0.8
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.7, "image" to 0.8, "video" to 0.7),
                followedAccounts = setOf("family-blogger", "investment-advisor", "cooking-channel", "gamer")
            )
        }
    }

    private fun createNicheInterestUser(): UserPersona {
        return UserPersona(
            type = PersonaType.TOPIC_FOCUSED_USER,
            userId = "niche-interest-scarcity",
            description = "User with extreme niche interests and limited content availability"
        ).withTopicInterests(mapOf(
            "18th-century-clockmaking" to 0.98, // Extremely niche
            "craftsmanship" to 0.8, // Related broader topic
            "history" to 0.7,
            "mechanical-arts" to 0.85,
            "precision-crafts" to 0.8
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.9, "image" to 0.8),
                followedAccounts = setOf("clockmaker-master", "craft-historian", "mechanical-expert")
            )
        }
    }

    private fun createFilterBubbleUser(): UserPersona {
        return UserPersona(
            type = PersonaType.TOPIC_FOCUSED_USER,
            userId = "filter-bubble-detected",
            description = "User potentially stuck in a filter bubble"
        ).withTopicInterests(mapOf(
            "narrow-tech-topic" to 0.95, // Very focused interest
            "specific-framework" to 0.9,
            "niche-programming" to 0.88,
            "very-specialized" to 0.92
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.9),
                followedAccounts = setOf("niche-expert-1", "specialized-blogger", "narrow-focus-author")
            )
        }
    }

    private fun createAlgorithmChangeUser(): UserPersona {
        return UserPersona(
            type = PersonaType.POWER_USER,
            userId = "algorithm-change-affected",
            description = "User with well-established preferences experiencing algorithm changes"
        ).withTopicInterests(mapOf(
            "established-interest" to 0.95,
            "consistent-topic" to 0.9,
            "stable-preference" to 0.88,
            "long-term-focus" to 0.85
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.9, "video" to 0.6),
                followedAccounts = setOf("trusted-source", "reliable-author", "consistent-creator")
            )
        }
    }

    private fun createMultilingualConflictUser(): UserPersona {
        return UserPersona(
            type = PersonaType.NEW_USER,
            userId = "multilingual-conflict",
            description = "User consuming content in multiple languages with availability conflicts"
        ).withTopicInterests(mapOf(
            "technology" to 0.9,
            "culture" to 0.8,
            "language-learning" to 0.7,
            "international" to 0.85
        )).withConfig {
            copy(
                languagePreferences = listOf("english", "spanish"),
                contentTypePreferences = mapOf("text" to 0.8, "video" to 0.7),
                followedAccounts = setOf("multilingual-tech", "spanish-culture", "english-news")
            )
        }
    }

    private fun createSafetyGuidelineConflictUser(): UserPersona {
        return UserPersona(
            type = PersonaType.NEW_USER,
            userId = "safety-guideline-conflict",
            description = "User interested in topics that may conflict with platform safety"
        ).withTopicInterests(mapOf(
            "controversial-politics" to 0.9,
            "debate" to 0.8,
            "political-analysis" to 0.85,
            "civil-discourse" to 0.7
        )).withConfig {
            copy(
                contentTypePreferences = mapOf("text" to 0.9),
                followedAccounts = setOf("political-analyst", "debate-moderator", "policy-expert")
            )
        }
    }

    /**
     * Generate specialized engagement histories for edge cases
     */
    fun generateEdgeCaseEngagementHistory(
        userId: String,
        edgeCaseType: String,
        minCount: Int = 20,
        maxCount: Int = 100
    ): List<UserActivity> {
        val count = Random.nextInt(minCount, maxCount + 1)
        val now = Instant.now()
        
        return when (edgeCaseType) {
            "conflicting_preferences" -> generateConflictingEngagementHistory(count, now)
            "rapid_change" -> generateRapidChangeEngagementHistory(count, now)
            "interest_fatigue" -> generateInterestFatigueEngagementHistory(count, now)
            "multi_persona" -> generateMultiPersonaEngagementHistory(count, now)
            "stated_vs_behavior" -> generateStatedVsBehaviorEngagementHistory(count, now)
            else -> generateStandardEngagementHistory(count, now)
        }
    }

    private fun generateConflictingEngagementHistory(count: Int, now: Instant): List<UserActivity> {
        return (0 until count).map { i ->
            val isConflicting = i % 2 == 0 // Alternate between conflicting topics
            val topic = if (isConflicting) "minimalism" else "maximalism"
            
            UserActivity(
                contentId = "conflicting-content-$i",
                authorId = if (isConflicting) "minimalist-author" else "maximalist-author",
                topics = listOf(topic, "design", "lifestyle"),
                engagementType = listOf(EngagementType.LIKE, EngagementType.VIEW, EngagementType.BOOKMARK).random(),
                engagementScore = Random.nextDouble(0.7, 1.0), // High engagement with both
                timestamp = now.minus(Random.nextLong(1, 90), ChronoUnit.DAYS),
                sessionContext = mapOf("conflict_type" to "topic_preference")
            )
        }
    }

    private fun generateRapidChangeEngagementHistory(count: Int, now: Instant): List<UserActivity> {
        return (0 until count).map { i ->
            // Simulate transition from photography to music over time
            val daysAgo = Random.nextLong(1, 120)
            val isOldInterest = daysAgo > 30 // Older than 30 days = photography era
            val topic = if (isOldInterest) "photography" else "music-production"
            
            UserActivity(
                contentId = "transition-content-$i",
                authorId = if (isOldInterest) "photographer-$i" else "music-producer-$i",
                topics = listOf(topic, if (isOldInterest) "visual" else "audio"),
                engagementType = if (isOldInterest) EngagementType.VIEW else EngagementType.BOOKMARK,
                engagementScore = if (isOldInterest) Random.nextDouble(0.4, 0.7) else Random.nextDouble(0.8, 1.0),
                timestamp = now.minus(daysAgo, ChronoUnit.DAYS),
                sessionContext = mapOf("transition_phase" to if (isOldInterest) "old" else "new")
            )
        }
    }

    private fun generateInterestFatigueEngagementHistory(count: Int, now: Instant): List<UserActivity> {
        return (0 until count).map { i ->
            val daysAgo = Random.nextLong(1, 730) // Up to 2 years of history
            // Simulate declining engagement with main topic over time
            val engagementScore = if (daysAgo > 365) {
                Random.nextDouble(0.8, 1.0) // High engagement in the past
            } else if (daysAgo > 180) {
                Random.nextDouble(0.6, 0.8) // Declining engagement
            } else {
                Random.nextDouble(0.4, 0.6) // Recent fatigue
            }
            
            UserActivity(
                contentId = "fatigue-content-$i",
                authorId = "tech-author-${i % 5}",
                topics = listOf("technology", "programming", "innovation"),
                engagementType = if (engagementScore > 0.7) EngagementType.SHARE else EngagementType.VIEW,
                engagementScore = engagementScore,
                timestamp = now.minus(daysAgo, ChronoUnit.DAYS),
                sessionContext = mapOf("fatigue_level" to if (engagementScore < 0.6) "high" else "low")
            )
        }
    }

    private fun generateMultiPersonaEngagementHistory(count: Int, now: Instant): List<UserActivity> {
        return (0 until count).map { i ->
            val hour = Random.nextInt(6, 23) // Different times suggest different users
            val persona = when (hour) {
                in 6..8 -> "adult" // Morning adult usage
                in 15..17 -> "child" // After school child usage
                in 18..20 -> "family" // Family time
                else -> "adult" // Evening adult usage
            }
            
            val topic = when (persona) {
                "child" -> "games"
                "family" -> "cooking"
                else -> "investment"
            }
            
            UserActivity(
                contentId = "multi-persona-content-$i",
                authorId = "$topic-author-${i % 3}",
                topics = listOf(topic, "${persona}-content"),
                engagementType = listOf(EngagementType.LIKE, EngagementType.VIEW).random(),
                engagementScore = Random.nextDouble(0.5, 0.9),
                timestamp = now.minus(Random.nextLong(1, 30), ChronoUnit.DAYS).plus(hour.toLong(), ChronoUnit.HOURS),
                sessionContext = mapOf("persona_type" to persona, "time_of_day" to hour)
            )
        }
    }

    private fun generateStatedVsBehaviorEngagementHistory(count: Int, now: Instant): List<UserActivity> {
        return (0 until count).map { i ->
            // User stated they dislike sports but actually engage with it frequently
            val topic = if (i % 3 == 0) "sports" else "books" // 33% sports despite stated dislike
            val engagementScore = if (topic == "sports") {
                Random.nextDouble(0.6, 0.9) // Actually high engagement with "disliked" sports
            } else {
                Random.nextDouble(0.5, 0.8) // Moderate engagement with stated preference
            }
            
            UserActivity(
                contentId = "behavior-conflict-content-$i",
                authorId = "$topic-author-${i % 4}",
                topics = listOf(topic),
                engagementType = if (topic == "sports") EngagementType.VIEW else EngagementType.BOOKMARK,
                engagementScore = engagementScore,
                timestamp = now.minus(Random.nextLong(1, 60), ChronoUnit.DAYS),
                sessionContext = mapOf("conflict_detected" to (topic == "sports"))
            )
        }
    }

    private fun generateStandardEngagementHistory(count: Int, now: Instant): List<UserActivity> {
        return (0 until count).map { i ->
            UserActivity(
                contentId = "edge-case-content-$i",
                authorId = "edge-case-author-${i % 10}",
                topics = listOf("general", "mixed"),
                engagementType = listOf(EngagementType.VIEW, EngagementType.LIKE).random(),
                engagementScore = Random.nextDouble(0.4, 0.8),
                timestamp = now.minus(Random.nextLong(1, 60), ChronoUnit.DAYS)
            )
        }
    }

    /**
     * Generate content specifically for edge case testing
     */
    fun generateEdgeCaseContent(): List<PersonalizableItem> {
        val baseTime = Instant.now()
        return listOf(
            // Bridging content for conflicting preferences
            generateBridgingContent(baseTime),
            // Transitional content for rapid changes
            generateTransitionalContent(baseTime),
            // Diverse content for filter bubble breaking
            generateFilterBubbleBreakingContent(baseTime),
            // Accessible visual content
            generateAccessibleVisualContent(baseTime),
            // Multi-persona appropriate content
            generateMultiPersonaContent(baseTime),
            // New content type introductions
            generateNewContentTypeIntroductions(baseTime)
        ).flatten()
    }

    private fun generateBridgingContent(baseTime: Instant): List<PersonalizableItem> {
        return listOf(
            createEdgeCaseContent(
                "bridge-minimalism-maximalism",
                "Finding Balance: When Less Meets More in Design",
                listOf("minimalism", "maximalism", "bridge", "balance", "design"),
                mapOf(
                    "bridging_content" to true,
                    "quality_score" to 0.9,
                    "conflict_resolution" to true
                )
            ),
            createEdgeCaseContent(
                "work-life-integration",
                "Corporate Finance for Social Good: Ethical Investment Strategies",
                listOf("corporate-finance", "social-justice", "ethics", "bridge"),
                mapOf(
                    "bridging_content" to true,
                    "quality_score" to 0.85,
                    "workplace_appropriate" to true
                )
            )
        )
    }

    private fun generateTransitionalContent(baseTime: Instant): List<PersonalizableItem> {
        return listOf(
            createEdgeCaseContent(
                "visual-audio-bridge",
                "From Visual to Audio: Photography Techniques in Music Production",
                listOf("photography", "music-production", "transition", "creative", "bridge"),
                mapOf(
                    "transitional" to true,
                    "quality_score" to 0.88,
                    "skill_transfer" to true
                )
            ),
            createEdgeCaseContent(
                "seasonal-bridge",
                "Year-Round Adventure: Transitioning from Winter to Summer Activities",
                listOf("winter-sports", "summer-travel", "seasonal", "transition"),
                mapOf(
                    "transitional" to true,
                    "seasonal_bridge" to true,
                    "quality_score" to 0.8
                )
            )
        )
    }

    private fun generateFilterBubbleBreakingContent(baseTime: Instant): List<PersonalizableItem> {
        return listOf(
            createEdgeCaseContent(
                "diverse-perspective",
                "Breaking Out: Why Diverse Perspectives Matter in Tech",
                listOf("technology", "diversity", "perspective", "growth"),
                mapOf(
                    "bubble_breaking" to true,
                    "quality_score" to 0.9,
                    "diversity_content" to true
                )
            ),
            createEdgeCaseContent(
                "adjacent-exploration",
                "Beyond Your Comfort Zone: Exploring Adjacent Fields",
                listOf("exploration", "growth", "learning", "adjacent"),
                mapOf(
                    "bubble_breaking" to true,
                    "quality_score" to 0.85,
                    "exploration_content" to true
                )
            )
        )
    }

    private fun generateAccessibleVisualContent(baseTime: Instant): List<PersonalizableItem> {
        return listOf(
            createEdgeCaseContent(
                "accessible-art-description",
                "Describing Visual Art: A Guide to Accessible Art Appreciation",
                listOf("visual-art", "accessibility", "description", "inclusive"),
                mapOf(
                    "detailed_description" to true,
                    "accessibility_optimized" to true,
                    "quality_score" to 0.9
                )
            ),
            createEdgeCaseContent(
                "tactile-photography",
                "Photography Beyond Sight: Tactile and Audio Approaches to Visual Art",
                listOf("photography", "accessibility", "tactile", "alternative"),
                mapOf(
                    "accessibility_alternative" to true,
                    "quality_score" to 0.85,
                    "detailed_description" to true
                )
            )
        )
    }

    private fun generateMultiPersonaContent(baseTime: Instant): List<PersonalizableItem> {
        return listOf(
            createEdgeCaseContent(
                "family-investment",
                "Teaching Kids About Money: Family-Friendly Investment Basics",
                listOf("investment", "family", "education", "children"),
                mapOf(
                    "family_appropriate" to true,
                    "multi_persona" to true,
                    "quality_score" to 0.8
                )
            ),
            createEdgeCaseContent(
                "cooking-games",
                "Gamification in the Kitchen: Making Cooking Fun for Everyone",
                listOf("cooking", "games", "family", "fun"),
                mapOf(
                    "multi_generational" to true,
                    "family_activity" to true,
                    "quality_score" to 0.75
                )
            )
        )
    }

    private fun generateNewContentTypeIntroductions(baseTime: Instant): List<PersonalizableItem> {
        return listOf(
            createEdgeCaseContent(
                "podcast-introduction",
                "Your First Podcast: A Beginner's Guide to Audio Learning [PODCAST]",
                listOf("podcast", "introduction", "learning", "audio", "beginner"),
                mapOf(
                    "cold_start_friendly" to true,
                    "content_type_introduction" to true,
                    "quality_score" to 0.85
                ),
                ContentType.VIDEO // Using VIDEO as proxy for podcast
            ),
            createEdgeCaseContent(
                "audio-tech-learning",
                "Tech Tutorials: Learning Programming Through Audio [PODCAST]",
                listOf("technology", "programming", "audio", "tutorial"),
                mapOf(
                    "cold_start_friendly" to true,
                    "familiar_topic_new_format" to true,
                    "quality_score" to 0.8
                ),
                ContentType.VIDEO // Using VIDEO as proxy for podcast
            )
        )
    }

    private fun createEdgeCaseContent(
        id: String,
        title: String,
        topics: List<String>,
        metadata: Map<String, Any>,
        contentType: ContentType = ContentType.TEXT
    ): PersonalizableItem {
        val enhancedMetadata = metadata.toMutableMap()
        
        // Add standard metadata
        enhancedMetadata["view_count"] = Random.nextInt(500, 5000)
        enhancedMetadata["like_count"] = Random.nextInt(50, enhancedMetadata["view_count"] as Int / 20)
        enhancedMetadata["share_count"] = Random.nextInt(5, enhancedMetadata["like_count"] as Int / 10)
        enhancedMetadata["comment_count"] = Random.nextInt(2, enhancedMetadata["like_count"] as Int / 15)
        enhancedMetadata["trending_score"] = Random.nextDouble(0.4, 0.9)
        
        if (!enhancedMetadata.containsKey("content_length")) {
            enhancedMetadata["content_length"] = when (contentType) {
                ContentType.VIDEO -> Random.nextInt(900, 3600) // 15-60 minutes for podcasts
                else -> Random.nextInt(1200, 5000) // Longer, more detailed content
            }
        }
        
        val baseScore = Random.nextDouble(2.5, 4.5) // Higher base scores for specialized content
        
        return PersonalizableItem(
            content = StoredContent(
                id = id,
                authorId = "edge-case-specialist-${Random.nextInt(1, 15)}",
                contentType = contentType,
                textContent = "$title\n\nSpecialized edge case content addressing ${topics.joinToString(", ")}. This content is designed to handle complex personalization scenarios and user conflicts.",
                visibility = ContentVisibility.PUBLIC,
                status = ContentStatus.PUBLISHED,
                tags = topics,
                createdAt = Instant.now().minus(Random.nextLong(1, 72), ChronoUnit.HOURS),
                updatedAt = Instant.now().minus(Random.nextLong(0, 24), ChronoUnit.HOURS)
            ),
            baseScore = baseScore,
            metadata = enhancedMetadata
        )
    }

    /**
     * Get all edge case personas combined
     */
    fun getAllEdgeCasePersonas(): Map<String, UserPersona> {
        return createConflictingPreferencePersonas() + 
               createRapidChangePersonas() + 
               createColdStartPersonas() + 
               createComplexEdgeCasePersonas()
    }

    /**
     * Generate custom edge case user data
     */
    fun createCustomEdgeCaseUserData(
        userId: String,
        edgeCaseType: String,
        customInterests: Map<String, Double> = emptyMap(),
        customConfig: PersonaConfig = PersonaConfig()
    ): UserPersonaData {
        val basePersona = when (edgeCaseType) {
            "conflicting_preferences" -> createMinimalismMaximalismConflictUser()
            "rapid_change" -> createPhotographyToMusicTransition()
            "cold_start" -> createNewContentTypeUser()
            "multi_persona" -> createMultiPersonaAccount()
            "niche_interest" -> createNicheInterestUser()
            else -> UserPersona(PersonaType.NEW_USER, userId, "Custom edge case user")
        }.copy(userId = userId)
        
        val finalPersona = if (customInterests.isNotEmpty()) {
            basePersona.withTopicInterests(customInterests)
        } else basePersona
        
        val userData = finalPersona.generatePersonaData()
        
        // Add custom engagement history
        val customEngagementHistory = generateEdgeCaseEngagementHistory(
            userId, edgeCaseType, 30, 150
        )
        
        return userData.copy(engagementHistory = customEngagementHistory)
    }

    /**
     * Get edge case content statistics
     */
    fun getEdgeCaseContentStats(): Map<String, Any> {
        val edgeCaseContent = generateEdgeCaseContent()
        
        return mapOf(
            "total_edge_case_items" to edgeCaseContent.size,
            "bridging_content_count" to edgeCaseContent.count { 
                it.metadata["bridging_content"] == true 
            },
            "transitional_content_count" to edgeCaseContent.count { 
                it.metadata["transitional"] == true 
            },
            "accessibility_optimized_count" to edgeCaseContent.count { 
                it.metadata["accessibility_optimized"] == true 
            },
            "multi_persona_count" to edgeCaseContent.count { 
                it.metadata["multi_persona"] == true 
            },
            "cold_start_friendly_count" to edgeCaseContent.count { 
                it.metadata["cold_start_friendly"] == true 
            },
            "avg_edge_case_quality" to edgeCaseContent.mapNotNull { 
                it.metadata["quality_score"] as? Double 
            }.average(),
            "persona_types" to getAllEdgeCasePersonas().keys.toList()
        )
    }
}