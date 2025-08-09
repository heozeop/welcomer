package com.welcomer.welcome.bdd

import com.welcomer.welcome.bdd.fixtures.*
import com.welcomer.welcome.ingestion.model.ContentType
import com.welcomer.welcome.personalization.service.DeviceType
import com.welcomer.welcome.personalization.service.PersonalizableItem
import com.welcomer.welcome.personalization.service.UserLocation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName

/**
 * Simplified BDD-style tests for new user personalization scenarios
 * 
 * These tests verify the core functionality of our BDD scenarios
 * without requiring a full Cucumber setup or Spring context.
 */
class SimpleBddTest {

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
            score *= (1.0 + typePreference) // Multiplicative boost for stronger preference effect
            
            // Apply personalization multiplier
            score * user.expectedBehavior.maxPersonalizationMultiplier
        }.take(20)
    }

    @Test
    @DisplayName("New user should receive diverse content from multiple topics")
    fun newUserReceivesDiverseContent() {
        // Given: I am a new user with no engagement history
        val newUser = UserPersona(
            type = PersonaType.NEW_USER,
            userId = "test-new-user",
            description = "New user for BDD testing"
        ).generatePersonaData()

        // And: Content fixtures are loaded with diverse topics
        val contentRepository = EnhancedContentFixtures.generateComprehensiveTestContent()
        
        // When: I request my personalized feed
        val personalizedFeed = simulatePersonalization(newUser, contentRepository)
        
        // Then: I should receive content from at least 5 different topics
        val topics = personalizedFeed.flatMap { it.topics }.toSet()
        assertTrue(topics.size >= 5, 
            "Should receive content from at least 5 topics, got ${topics.size}")
        
        // And: No single topic should represent more than 30% of the content
        val topicCounts = personalizedFeed.flatMap { it.topics }.groupingBy { it }.eachCount()
        val maxCount = topicCounts.maxOfOrNull { it.value } ?: 0
        val maxPercentage = (maxCount.toDouble() / personalizedFeed.size) * 100
        assertTrue(maxPercentage <= 30, 
            "No topic should exceed 30% but max was ${maxPercentage.toInt()}%")
        
        // And: I should receive content from multiple authors
        val authors = personalizedFeed.map { it.authorId }.toSet()
        assertTrue(authors.size >= 3, 
            "Should receive content from multiple authors, got ${authors.size}")
    }

    @Test
    @DisplayName("New user with interests should get balanced exploration content")  
    fun newUserWithInterestsGetsBalancedContent() {
        // Given: I am a new user who has specified interest in "technology"
        val techInterestedUser = UserPersona(
            type = PersonaType.NEW_USER,
            userId = "tech-interested-user",
            description = "New user interested in technology"
        ).withTopicInterests(mapOf("technology" to 0.8))
        .generatePersonaData()

        val contentRepository = EnhancedContentFixtures.generateComprehensiveTestContent()
        
        // When: I request my personalized feed
        val personalizedFeed = simulatePersonalization(techInterestedUser, contentRepository)
        
        // Then: I should receive some technology content
        val techContent = personalizedFeed.filter { item ->
            item.topics.any { it.contains("technology", ignoreCase = true) }
        }
        assertTrue(techContent.isNotEmpty(), "Should receive some technology content")
        
        // But: I should also receive diverse content from other topics
        val nonTechContent = personalizedFeed.filter { item ->
            !item.topics.any { it.contains("technology", ignoreCase = true) }
        }
        assertTrue(nonTechContent.isNotEmpty(), 
            "Should also receive diverse content from other topics")
        
        // And: The content should have high quality scores
        val avgQuality = personalizedFeed.mapNotNull { 
            it.metadata["quality_score"] as? Double 
        }.average()
        assertTrue(avgQuality >= 0.6, 
            "Content should have good quality scores, average was $avgQuality")
    }

    @Test
    @DisplayName("Mobile-first new user should receive optimized visual content")
    fun mobileFirstUserReceivesOptimizedContent() {
        // Given: I am a new mobile-first user
        val mobileUser = UserPersona(
            type = PersonaType.MOBILE_FIRST_USER,
            userId = "mobile-new-user",
            description = "New mobile-first user"
        ).generatePersonaData()

        val contentRepository = EnhancedContentFixtures.generateComprehensiveTestContent()
        
        // When: I request my personalized feed
        val personalizedFeed = simulatePersonalization(mobileUser, contentRepository)
        
        // Then: I should receive more visual content (images and videos)
        val visualContent = personalizedFeed.filter { 
            it.content.contentType in listOf(ContentType.IMAGE, ContentType.VIDEO)
        }
        val textContent = personalizedFeed.filter { 
            it.content.contentType == ContentType.TEXT
        }
        
        assertTrue(visualContent.size >= textContent.size * 0.5, 
            "Should receive significant visual content for mobile users")
        
        // And: Session duration should be considered for content length
        assertEquals(DeviceType.MOBILE, mobileUser.userContext.deviceType)
        assertTrue(mobileUser.userContext.sessionDuration <= 15, 
            "Mobile users should have shorter session durations")
    }

    @Test
    @DisplayName("Accessibility user should receive text-optimized content")
    fun accessibilityUserReceivesTextOptimizedContent() {
        // Given: I am a new user with accessibility requirements
        val accessibilityUser = UserPersona(
            type = PersonaType.ACCESSIBILITY_USER,
            userId = "accessibility-new-user",
            description = "New user with accessibility needs"
        ).generatePersonaData()

        val contentRepository = EnhancedContentFixtures.generateComprehensiveTestContent()
        
        // When: I request my personalized feed
        val personalizedFeed = simulatePersonalization(accessibilityUser, contentRepository)
        
        // Then: I should receive primarily text-based content
        val textContent = personalizedFeed.filter { 
            it.content.contentType == ContentType.TEXT
        }
        assertTrue(textContent.size >= personalizedFeed.size * 0.6, 
            "Should receive primarily text-based content for accessibility")
        
        // And: All content should have proper descriptions
        personalizedFeed.forEach { item ->
            assertTrue(item.content.textContent?.isNotBlank() == true,
                "All content should have proper descriptions")
        }
        
        // And: The content should be screen reader friendly
        val screenReaderSupport = accessibilityUser.userContext.contextualPreferences?.get("screen_reader") as? Double ?: 0.0
        assertTrue(screenReaderSupport >= 0.9, 
            "Content should be optimized for screen readers")
    }

    @Test
    @DisplayName("New user should receive quality-assured content for good first impression")
    fun newUserReceivesQualityContent() {
        // Given: I am a new user requesting content for the first time
        val firstTimeUser = UserPersona(
            type = PersonaType.NEW_USER,
            userId = "first-time-user",
            description = "Brand new user requesting content for the first time"
        ).generatePersonaData()

        val contentRepository = EnhancedContentFixtures.generateComprehensiveTestContent()
        
        // When: I request my personalized feed
        val personalizedFeed = simulatePersonalization(firstTimeUser, contentRepository)
        
        // Then: All content should have quality scores above 0.7
        personalizedFeed.forEach { item ->
            val quality = item.metadata["quality_score"] as? Double ?: 0.0
            assertTrue(quality >= 0.7, 
                "All content should have good quality for first impression")
        }
        
        // And: Content from high-reputation sources should be prioritized
        val highReputationContent = personalizedFeed.filter { 
            (it.metadata["is_followed_author"] as? Boolean) == true ||
            it.baseScore >= 2.0
        }
        assertTrue(highReputationContent.size >= personalizedFeed.size / 3,
            "Should prioritize content from high-reputation sources")
        
        // And: The content should be recent
        personalizedFeed.forEach { item ->
            val daysSince = java.time.Duration.between(item.createdAt, java.time.Instant.now()).toDays()
            assertTrue(daysSince <= 7, 
                "Content should be recent for good first impression")
        }
    }

    @Test
    @DisplayName("New user personalization should avoid filter bubbles")
    fun newUserPersonalizationAvoidsFilterBubbles() {
        // Given: I am a new user with expressed interest in "cooking"
        val cookingUser = UserPersona(
            type = PersonaType.NEW_USER,
            userId = "cooking-interested-user",
            description = "New user interested in cooking"
        ).withTopicInterests(mapOf("cooking" to 0.9))
        .generatePersonaData()

        val contentRepository = EnhancedContentFixtures.generateComprehensiveTestContent()
        
        // When: I request my personalized feed
        val personalizedFeed = simulatePersonalization(cookingUser, contentRepository)
        
        // Then: I should receive cooking content but not exclusively
        val cookingContent = personalizedFeed.filter { item ->
            item.topics.any { it.contains("cooking", ignoreCase = true) }
        }
        val nonCookingContent = personalizedFeed - cookingContent
        
        assertTrue(cookingContent.isNotEmpty(), "Should receive some cooking content")
        assertTrue(nonCookingContent.isNotEmpty(), "Should not receive cooking content exclusively")
        
        // And: Serendipitous content from unexpected topics should appear
        assertTrue(nonCookingContent.size >= 3, 
            "Should include serendipitous content from unexpected topics")
        
        // And: I should discover content from new authors
        val authors = personalizedFeed.map { it.authorId }.toSet()
        assertTrue(authors.size >= 5, 
            "Should discover content from multiple new authors")
    }

    @Test
    @DisplayName("Content statistics should show proper diversity for new users")
    fun contentStatisticsShowProperDiversity() {
        // Given: Content fixtures are available
        val stats = EnhancedContentFixtures.getContentStatistics()
        
        // Then: Should have comprehensive content for testing
        val totalItems = stats["total_items"] as Int
        assertTrue(totalItems >= 100, "Should have at least 100 content items")
        
        @Suppress("UNCHECKED_CAST")
        val topicCount = (stats["topics"] as Map<String, Int>).size
        assertTrue(topicCount >= 15, "Should have diverse topics available")
        
        @Suppress("UNCHECKED_CAST") 
        val authorCount = (stats["authors"] as Map<String, Int>).size
        assertTrue(authorCount >= 15, "Should have multiple authors available")
        
        // And: Should have balanced content types
        @Suppress("UNCHECKED_CAST")
        val contentTypes = stats["content_types"] as Map<ContentType, Int>
        assertTrue(contentTypes.size >= 3, "Should have multiple content types")
        
        // And: Should have variety in followed vs unfollowed authors
        val followedAuthors = stats["followed_authors"] as Int
        assertTrue(followedAuthors > 0, "Should have some content from followed authors")
        assertTrue(followedAuthors < totalItems, "Should not have all content from followed authors")
    }

    @Test  
    @DisplayName("Persona fixture factory should provide scenario-specific content")
    fun personaFixtureFactoryProvidesScenarioContent() {
        // Given: Content fixture factory scenarios
        val newUserContent = ContentFixtureFactory.NewUserScenarios.getDiverseContentMix()
        val trendingContent = ContentFixtureFactory.NewUserScenarios.getTrendingContent()
        val qualityContent = ContentFixtureFactory.NewUserScenarios.getHighQualityContent()
        
        // Then: Should provide appropriate content for new users
        assertTrue(newUserContent.isNotEmpty(), "Should provide diverse content mix")
        assertTrue(trendingContent.isNotEmpty(), "Should provide trending content")
        assertTrue(qualityContent.isNotEmpty(), "Should provide high quality content")
        
        // And: Content should have proper diversity
        val topics = newUserContent.flatMap { it.topics }.toSet()
        assertTrue(topics.size >= 4, "Diverse content should cover multiple topics")
        
        // And: Trending content should have high trending scores
        trendingContent.forEach { item ->
            val trendingScore = item.metadata["trending_score"] as? Double ?: 0.0
            assertTrue(trendingScore >= 0.7, "Trending content should have high trending scores")
        }
        
        // And: Quality content should have high quality scores
        qualityContent.forEach { item ->
            val qualityScore = item.metadata["quality_score"] as? Double ?: 0.0
            assertTrue(qualityScore >= 0.8, "Quality content should have high quality scores")
        }
    }
}