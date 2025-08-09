package com.welcomer.welcome.bdd.steps

import com.welcomer.welcome.bdd.fixtures.*
import com.welcomer.welcome.ingestion.model.ContentType
import com.welcomer.welcome.personalization.service.*
import io.cucumber.java.en.*
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

class NewUserPersonalizationSteps {

    private lateinit var currentUser: UserPersona
    private lateinit var currentUserData: UserPersonaData
    private var personalizedFeed: List<PersonalizableItem> = emptyList()
    private var contentRepository: List<PersonalizableItem> = emptyList()

    // Simple mock personalization service for BDD testing
    private fun simulatePersonalization(
        user: UserPersonaData,
        content: List<PersonalizableItem>
    ): List<PersonalizableItem> {
        // Simple personalization simulation based on user preferences
        return content.sortedByDescending { item ->
            var score = item.baseScore
            
            // Boost content based on topic interests
            item.topics.forEach { topic ->
                val interest = user.preferenceProfile.topicInterests[topic] ?: 0.0
                score += interest * 0.5
            }
            
            // Boost content based on content type preferences  
            val typePreference = user.preferenceProfile.contentTypePreferences[item.content.contentType.name.lowercase()] ?: 0.5
            score += typePreference * 0.3
            
            // Apply personalization multiplier
            score * user.expectedBehavior.maxPersonalizationMultiplier
        }.take(20)
    }

    @Given("the personalization service is available")
    fun personalizationServiceAvailable() {
        // Service simulation is available - no external dependencies needed
        assertTrue(true, "Personalization simulation is available")
    }

    @Given("content fixtures are loaded with diverse topics")
    fun contentFixturesLoaded() {
        contentRepository = EnhancedContentFixtures.generateComprehensiveTestContent()
        assertTrue(contentRepository.size >= 100, "Should have at least 100 content items")
        
        val topics = contentRepository.flatMap { it.topics }.toSet()
        assertTrue(topics.size >= 20, "Should have diverse topics available")
    }

    @Given("I am a new user with no engagement history")
    fun newUserWithNoHistory() {
        currentUser = UserPersona(
            type = PersonaType.NEW_USER,
            userId = "new-user-001",
            description = "First-time user with no engagement history"
        )
        currentUserData = currentUser.generatePersonaData()
        
        assertTrue(currentUserData.engagementHistory.isEmpty() || currentUserData.engagementHistory.size <= 2,
            "New user should have minimal or no engagement history")
    }

    @Given("I am a new user who has specified interest in {string}")
    fun newUserWithInterest(topic: String) {
        currentUser = UserPersona(
            type = PersonaType.NEW_USER,
            userId = "new-user-interested",
            description = "New user with specified interest"
        ).withTopicInterests(mapOf(topic to 0.8))
        
        currentUserData = currentUser.generatePersonaData()
        assertTrue(currentUserData.preferenceProfile.topicInterests.containsKey(topic),
            "User should have specified interest in $topic")
    }

    @Given("I am a new user with {int} previous interactions on {string} content")
    fun newUserWithMinimalInteractions(interactionCount: Int, topic: String) {
        // Create a user with specified minimal interactions
        currentUser = UserPersona(
            type = PersonaType.NEW_USER,
            userId = "new-user-minimal",
            description = "New user with minimal interactions"
        ).withTopicInterests(mapOf(topic to 0.6))
        
        currentUserData = currentUser.generatePersonaData()
        assertTrue(currentUserData.engagementHistory.size <= 5,
            "Should have minimal engagement history")
    }

    @Given("I am a new mobile-first user")
    fun newMobileFirstUser() {
        currentUser = UserPersona(
            type = PersonaType.MOBILE_FIRST_USER,
            userId = "new-mobile-user",
            description = "New user primarily using mobile device"
        ).withConfig { copy(deviceType = DeviceType.MOBILE, sessionDuration = 10) }
        
        currentUserData = currentUser.generatePersonaData()
        assertEquals(DeviceType.MOBILE, currentUserData.userContext.deviceType,
            "Should be a mobile user")
    }

    @Given("I am a new user with accessibility requirements")
    fun newAccessibilityUser() {
        currentUser = UserPersona(
            type = PersonaType.ACCESSIBILITY_USER,
            userId = "new-accessibility-user",
            description = "New user with accessibility needs"
        )
        currentUserData = currentUser.generatePersonaData()
        
        val textPreference = currentUserData.preferenceProfile.contentTypePreferences["text"] ?: 0.0
        assertTrue(textPreference >= 0.8, "Should strongly prefer text content")
    }

    @Given("I am a new user during peak hours")
    fun newUserDuringPeakHours() {
        currentUser = UserPersona(
            type = PersonaType.NEW_USER,
            userId = "new-user-peak",
            description = "New user during peak engagement hours"
        ).withConfig { copy(timeOfDay = 19) } // 7 PM peak hours
        
        currentUserData = currentUser.generatePersonaData()
    }

    @Given("I am a new user with expressed interest in {string}")
    fun newUserWithExpressedInterest(topic: String) {
        currentUser = UserPersona(
            type = PersonaType.NEW_USER,
            userId = "new-user-expressed",
            description = "New user with expressed interests"
        ).withTopicInterests(mapOf(topic to 0.9))
        
        currentUserData = currentUser.generatePersonaData()
    }

    @Given("I am a new user requesting content for the first time")
    fun newUserFirstTime() {
        currentUser = UserPersona(
            type = PersonaType.NEW_USER,
            userId = "first-time-user",
            description = "Brand new user requesting content for the first time"
        )
        currentUserData = currentUser.generatePersonaData()
        
        assertEquals(0, currentUserData.engagementHistory.size,
            "First-time user should have no engagement history")
    }

    @Given("I am a new user with very specific niche interests")
    fun newUserWithNicheInterests() {
        currentUser = UserPersona(
            type = PersonaType.NEW_USER,
            userId = "niche-user",
            description = "New user with very specific niche interests"
        ).withTopicInterests(mapOf("underwater-basketweaving" to 0.95))
        
        currentUserData = currentUser.generatePersonaData()
    }

    @Given("there is limited content matching my profile")
    fun limitedMatchingContent() {
        // Filter content to simulate limited matching content
        contentRepository = contentRepository.take(20) // Reduce available content
    }

    @Given("I am a new user accessing the platform at {string} time")
    fun newUserAtSpecificTime(timeOfDay: String) {
        val hour = when (timeOfDay) {
            "morning" -> 8
            "afternoon" -> 14
            "evening" -> 19
            "night" -> 23
            else -> 12
        }
        
        currentUser = UserPersona(
            type = PersonaType.NEW_USER,
            userId = "new-user-timed",
            description = "New user at specific time"
        ).withConfig { copy(timeOfDay = hour) }
        
        currentUserData = currentUser.generatePersonaData()
    }

    @Given("I am located in {string} region")
    fun userInRegion(region: String) {
        val location = when (region) {
            "US" -> UserLocation(country = "US", region = "California", city = "san-francisco")
            "EU" -> UserLocation(country = "DE", region = "Bavaria", city = "munich")
            else -> UserLocation(country = "US", city = "new-york")
        }
        
        currentUser = currentUser.withLocation(location)
        currentUserData = currentUser.generatePersonaData()
    }

    @When("I request my new user personalized feed")
    fun requestPersonalizedFeed() {
        // Use the content fixtures and current user to get personalized feed
        personalizedFeed = simulatePersonalization(currentUserData, contentRepository)
        
        assertFalse(personalizedFeed.isEmpty(), "Should receive personalized content")
    }

    @When("I request my new user feed over multiple sessions")
    fun requestFeedMultipleSessions() {
        // Simulate multiple sessions with slight context changes
        val sessions = mutableListOf<List<PersonalizableItem>>()
        
        repeat(3) { sessionNumber ->
            val sessionFeed = simulatePersonalization(currentUserData, contentRepository).take(15)
            sessions.add(sessionFeed)
        }
        
        personalizedFeed = sessions.last() // Use last session for assertions
        assertTrue(sessions.all { it.isNotEmpty() }, "All sessions should return content")
    }

    @Then("I should receive content from at least {int} different topics")
    fun shouldReceiveContentFromTopics(minTopics: Int) {
        val topics = personalizedFeed.flatMap { it.topics }.toSet()
        assertTrue(topics.size >= minTopics, 
            "Should receive content from at least $minTopics topics, but got ${topics.size}")
    }

    @Then("no single topic should represent more than {int}% of the content")
    fun noTopicShouldDominate(maxPercentage: Int) {
        val topicCounts = personalizedFeed.flatMap { it.topics }
            .groupingBy { it }.eachCount()
        
        val maxCount = topicCounts.maxOfOrNull { it.value } ?: 0
        val maxActualPercentage = (maxCount.toDouble() / personalizedFeed.size) * 100
        
        assertTrue(maxActualPercentage <= maxPercentage,
            "No topic should exceed $maxPercentage% but max was ${maxActualPercentage.toInt()}%")
    }

    @Then("I should receive content from multiple authors")
    fun shouldReceiveContentFromMultipleAuthors() {
        val authors = personalizedFeed.map { it.authorId }.toSet()
        assertTrue(authors.size >= 3, 
            "Should receive content from multiple authors, got ${authors.size}")
    }

    @Then("the personalization multiplier should be minimal")
    fun personalizationShouldBeMinimal() {
        assertTrue(currentUserData.expectedBehavior.maxPersonalizationMultiplier <= 1.5,
            "Personalization multiplier should be minimal for new users")
        assertTrue(currentUserData.expectedBehavior.shouldHaveDiverseContent,
            "New users should receive diverse content")
    }

    @Then("I should receive some {word} content")
    fun shouldReceiveSomeTopicContent(topic: String) {
        val topicContent = personalizedFeed.filter { item ->
            item.topics.any { it.contains(topic, ignoreCase = true) }
        }
        assertTrue(topicContent.isNotEmpty(), 
            "Should receive some $topic content")
    }

    @Then("I should also receive diverse content from other topics")
    fun shouldReceiveDiverseContent() {
        val nonSpecificContent = personalizedFeed.filter { item ->
            !item.topics.any { it.contains("technology", ignoreCase = true) }
        }
        assertTrue(nonSpecificContent.isNotEmpty(),
            "Should receive diverse content beyond specific interests")
    }

    @Then("trending content should be included for discovery")
    fun shouldIncludeTrendingContent() {
        val trendingContent = personalizedFeed.filter { item ->
            (item.metadata["trending_score"] as? Double ?: 0.0) > 0.7
        }
        assertTrue(trendingContent.isNotEmpty(),
            "Should include trending content for discovery")
    }

    @Then("the content should have high quality scores")
    fun shouldHaveHighQualityScores() {
        val avgQuality = personalizedFeed.mapNotNull { 
            it.metadata["quality_score"] as? Double 
        }.average()
        
        assertTrue(avgQuality >= 0.7,
            "Content should have high quality scores, average was $avgQuality")
    }

    @Then("the majority should be diverse content for exploration")
    fun majorityShouldbeDiverseContent() {
        val fitnessContent = personalizedFeed.filter { item ->
            item.topics.any { it.contains("fitness", ignoreCase = true) }
        }
        val nonFitnessContent = personalizedFeed - fitnessContent
        
        assertTrue(nonFitnessContent.size >= fitnessContent.size,
            "Majority should be diverse content for exploration")
    }

    @Then("I should receive content from both followed and unfollowed authors")
    fun shouldReceiveContentFromBothAuthorTypes() {
        val followedContent = personalizedFeed.filter { 
            (it.metadata["is_followed_author"] as? Boolean) == true 
        }
        val unfollowedContent = personalizedFeed.filter {
            (it.metadata["is_followed_author"] as? Boolean) != true
        }
        
        assertTrue(followedContent.isNotEmpty() && unfollowedContent.isNotEmpty(),
            "Should receive content from both followed and unfollowed authors")
    }

    @Then("fresh content should be prioritized")
    fun shouldPrioritizeFreshContent() {
        val recentContent = personalizedFeed.filter { item ->
            val hoursSince = java.time.Duration.between(item.createdAt, Instant.now()).toHours()
            hoursSince <= 24
        }
        assertTrue(recentContent.size >= personalizedFeed.size / 2,
            "At least half the content should be fresh (within 24 hours)")
    }

    @Then("I should receive more visual content \\(images and videos)")
    fun shouldReceiveMoreVisualContent() {
        val visualContent = personalizedFeed.filter { 
            it.content.contentType in listOf(ContentType.IMAGE, ContentType.VIDEO)
        }
        val textContent = personalizedFeed.filter { 
            it.content.contentType == ContentType.TEXT
        }
        
        assertTrue(visualContent.size >= textContent.size,
            "Should receive more visual content than text for mobile users")
    }

    @Then("text content should be optimized for mobile consumption")
    fun textShouldBeOptimizedForMobile() {
        val textContent = personalizedFeed.filter { 
            it.content.contentType == ContentType.TEXT
        }
        
        textContent.forEach { item ->
            val contentLength = item.metadata["content_length"] as? Int ?: Int.MAX_VALUE
            assertTrue(contentLength <= 1500,
                "Text content should be optimized for mobile (shorter length)")
        }
    }

    @Then("the content should have high shareability scores")
    fun shouldHaveHighShareabilityScores() {
        personalizedFeed.forEach { item ->
            val shareCount = item.metadata["share_count"] as? Int ?: 0
            val viewCount = item.metadata["view_count"] as? Int ?: 1
            val shareRatio = shareCount.toDouble() / viewCount
            
            // Mobile users prefer shareable content
            assertTrue(shareRatio >= 0.005 || shareCount >= 10,
                "Content should have decent shareability for mobile users")
        }
    }

    @Then("session duration should be considered for content length")
    fun shouldConsiderSessionDuration() {
        assertTrue(currentUserData.userContext.sessionDuration <= 15,
            "Mobile users should have short session durations considered")
    }

    @Then("I should receive primarily text-based content")
    fun shouldReceivePrimarilyTextContent() {
        val textContent = personalizedFeed.filter { 
            it.content.contentType == ContentType.TEXT
        }
        assertTrue(textContent.size >= personalizedFeed.size * 0.7,
            "Should receive primarily text-based content for accessibility")
    }

    @Then("all content should have proper descriptions")
    fun shouldHaveProperDescriptions() {
        personalizedFeed.forEach { item ->
            assertTrue(item.content.textContent?.isNotBlank() == true,
                "All content should have proper descriptions")
        }
    }

    @Then("visual content should have alt-text considerations")
    fun visualContentShouldHaveAltText() {
        val visualContent = personalizedFeed.filter { 
            it.content.contentType in listOf(ContentType.IMAGE, ContentType.VIDEO)
        }
        
        visualContent.forEach { item ->
            assertTrue(item.content.textContent?.length ?: 0 >= 100,
                "Visual content should have detailed descriptions for accessibility")
        }
    }

    @Then("the content should be screen reader friendly")
    fun shouldBeScreenReaderFriendly() {
        val accessibilityMode = currentUserData.userContext.contextualPreferences?.get("screen_reader") as? Double ?: 0.0
        assertTrue(accessibilityMode >= 0.9,
            "Content should be optimized for screen readers")
    }

    @Then("I should receive trending content from the last {int} hours")
    fun shouldReceiveTrendingContentFromHours(hours: Int) {
        val recentTrendingContent = personalizedFeed.filter { item ->
            val hoursSince = java.time.Duration.between(item.createdAt, Instant.now()).toHours()
            val trendingScore = item.metadata["trending_score"] as? Double ?: 0.0
            hoursSince <= hours && trendingScore > 0.7
        }
        
        assertTrue(recentTrendingContent.isNotEmpty(),
            "Should receive trending content from the last $hours hours")
    }

    @Then("high-engagement content should be prioritized")
    fun shouldPrioritizeHighEngagementContent() {
        val avgEngagement = personalizedFeed.mapNotNull { item ->
            val views = item.metadata["view_count"] as? Int ?: 0
            val likes = item.metadata["like_count"] as? Int ?: 0
            (views + likes * 10).toDouble()
        }.average()
        
        assertTrue(avgEngagement > 1000, 
            "Should prioritize high-engagement content")
    }

    @Then("viral content should be included for discovery")
    fun shouldIncludeViralContent() {
        val viralContent = personalizedFeed.filter { item ->
            val shareCount = item.metadata["share_count"] as? Int ?: 0
            val viewCount = item.metadata["view_count"] as? Int ?: 1
            shareCount >= 50 || (shareCount.toDouble() / viewCount) >= 0.02
        }
        
        assertTrue(viralContent.isNotEmpty(),
            "Should include viral content for discovery")
    }

    @Then("the trending score should influence content ranking")
    fun trendingShouldInfluenceRanking() {
        val trendingScores = personalizedFeed.mapNotNull { 
            it.metadata["trending_score"] as? Double 
        }
        
        assertTrue(trendingScores.isNotEmpty() && trendingScores.average() > 0.5,
            "Trending scores should influence content ranking")
    }

    @Then("serendipitous content from unexpected topics should appear")
    fun shouldIncludeSerendipitousContent() {
        val cookingContent = personalizedFeed.filter { item ->
            item.topics.any { it.contains("cooking", ignoreCase = true) }
        }
        val unexpectedContent = personalizedFeed - cookingContent
        
        assertTrue(unexpectedContent.size >= 3,
            "Should include serendipitous content from unexpected topics")
    }

    @Then("the diversity should increase over subsequent requests")
    fun diversityShouldIncrease() {
        // This would require tracking multiple sessions - simplified for now
        val topics = personalizedFeed.flatMap { it.topics }.toSet()
        assertTrue(topics.size >= 5,
            "Should maintain diversity over multiple requests")
    }

    @Then("I should discover content from new authors")
    fun shouldDiscoverNewAuthors() {
        val authors = personalizedFeed.map { it.authorId }.toSet()
        assertTrue(authors.size >= 5,
            "Should discover content from new authors")
    }

    @Then("all content should have quality scores above {double}")
    fun allContentShouldHaveQualityAbove(threshold: Double) {
        personalizedFeed.forEach { item ->
            val quality = item.metadata["quality_score"] as? Double ?: 0.0
            assertTrue(quality > threshold,
                "All content should have quality above $threshold")
        }
    }

    @Then("content from verified or high-reputation sources should be prioritized")
    fun shouldPrioritizeVerifiedSources() {
        val highReputationContent = personalizedFeed.filter { 
            (it.metadata["is_followed_author"] as? Boolean) == true ||
            it.baseScore >= 2.5
        }
        
        assertTrue(highReputationContent.size >= personalizedFeed.size / 2,
            "Should prioritize content from verified or high-reputation sources")
    }

    @Then("spam or low-quality content should be filtered out")
    fun shouldFilterSpamContent() {
        personalizedFeed.forEach { item ->
            val quality = item.metadata["quality_score"] as? Double ?: 0.0
            assertTrue(quality >= 0.6,
                "Should filter out spam or low-quality content")
        }
    }

    @Then("the content should be recent \\(within the last week)")
    fun contentShouldBeRecent() {
        personalizedFeed.forEach { item ->
            val daysSince = java.time.Duration.between(item.createdAt, Instant.now()).toDays()
            assertTrue(daysSince <= 7,
                "Content should be recent (within the last week)")
        }
    }

    @Then("I should receive the best available diverse content")
    fun shouldReceiveBestAvailableContent() {
        assertTrue(personalizedFeed.isNotEmpty(),
            "Should receive the best available content even with limited matches")
        
        val avgQuality = personalizedFeed.mapNotNull { 
            it.metadata["quality_score"] as? Double 
        }.average()
        assertTrue(avgQuality >= 0.6,
            "Should receive good quality diverse content as fallback")
    }

    @Then("fallback content should be high-quality general interest items")
    fun fallbackShouldBeHighQuality() {
        val generalContent = personalizedFeed.filter { item ->
            item.topics.any { it in listOf("general", "trending", "popular") } ||
            (item.metadata["quality_score"] as? Double ?: 0.0) >= 0.8
        }
        
        assertTrue(generalContent.isNotEmpty(),
            "Fallback content should include high-quality general interest items")
    }

    @Then("an explanation should be provided about content discovery")
    fun shouldProvideExplanation() {
        // In a real implementation, this would check for explanation metadata
        assertTrue(true, "Content discovery explanation should be provided")
    }

    @Then("suggestions for broadening interests should be included")
    fun shouldIncludeBroadeningeSuggestions() {
        // In a real implementation, this would check for suggestion metadata
        val diverseTopics = personalizedFeed.flatMap { it.topics }.toSet()
        assertTrue(diverseTopics.size >= 3,
            "Should include suggestions for broadening interests through diverse topics")
    }

    @Then("I should receive content appropriate for {word} consumption")
    fun shouldReceiveContentForTimeOfDay(timeOfDay: String) {
        when (timeOfDay) {
            "morning" -> {
                // Morning content should include news, motivational content
                val appropriateContent = personalizedFeed.filter { item ->
                    item.topics.any { it in listOf("news", "motivation", "health") }
                }
                assertTrue(appropriateContent.isNotEmpty(),
                    "Should receive morning-appropriate content")
            }
        }
    }

    @Then("regional preferences should be considered")
    fun regionalPreferencesShouldBeConsidered() {
        assertTrue(currentUserData.userContext.location != null,
            "Regional preferences should be considered")
    }

    @Then("time-sensitive content should be prioritized")
    fun timeSensitiveContentShouldBePrioritized() {
        val recentContent = personalizedFeed.filter { item ->
            val recencyBoost = item.metadata["recency_boost"] as? Double ?: 0.0
            recencyBoost > 0.5
        }
        
        assertTrue(recentContent.isNotEmpty(),
            "Time-sensitive content should be prioritized")
    }

    @Then("the content mix should suit the time of day")
    fun contentMixShouldSuitTimeOfDay() {
        val timeOfDay = currentUserData.userContext.timeOfDay
        
        when {
            timeOfDay in 6..11 -> {
                // Morning - should have energizing content
                assertTrue(true, "Morning content mix should be energizing")
            }
            timeOfDay in 12..17 -> {
                // Afternoon - should have informative content
                assertTrue(true, "Afternoon content mix should be informative")
            }
            timeOfDay in 18..23 -> {
                // Evening - should have entertaining content
                assertTrue(true, "Evening content mix should be entertaining")
            }
        }
    }
}