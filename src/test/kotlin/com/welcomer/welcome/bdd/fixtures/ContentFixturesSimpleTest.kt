package com.welcomer.welcome.bdd.fixtures

import com.welcomer.welcome.ingestion.model.ContentType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Simple test for the content fixtures to verify Task 14.2 completion
 */
class ContentFixturesSimpleTest {

    @Test
    fun `should generate at least 100 content items`() {
        val content = EnhancedContentFixtures.generateComprehensiveTestContent()
        
        assertTrue(content.size >= 100, 
            "Should generate at least 100 content items, but got ${content.size}")
    }

    @Test
    fun `should have diverse content types`() {
        val content = EnhancedContentFixtures.generateComprehensiveTestContent()
        val contentTypes = content.map { it.content.contentType }.toSet()
        
        assertTrue(contentTypes.contains(ContentType.TEXT))
        assertTrue(contentTypes.contains(ContentType.IMAGE))  
        assertTrue(contentTypes.contains(ContentType.VIDEO))
        assertTrue(contentTypes.size >= 3)
    }

    @Test
    fun `should have diverse topics`() {
        val content = EnhancedContentFixtures.generateComprehensiveTestContent()
        val allTopics = content.flatMap { it.topics }.toSet()
        
        assertTrue(allTopics.size >= 15, "Should have at least 15 different topics")
    }

    @Test
    fun `should have unique content IDs`() {
        val content = EnhancedContentFixtures.generateComprehensiveTestContent()
        val ids = content.map { it.content.id }
        val uniqueIds = ids.toSet()
        
        assertEquals(ids.size, uniqueIds.size, "All content should have unique IDs")
    }

    @Test
    fun `should have proper content structure`() {
        val content = EnhancedContentFixtures.generateComprehensiveTestContent().take(5)
        
        content.forEach { item ->
            assertNotNull(item.content.id)
            assertNotNull(item.content.authorId)
            assertNotNull(item.content.textContent)
            assertTrue(item.baseScore > 0.0)
            assertTrue(item.topics.isNotEmpty())
        }
    }

    @Test
    fun `should generate content statistics`() {
        val stats = EnhancedContentFixtures.getContentStatistics()
        
        assertTrue(stats.containsKey("total_items"))
        assertTrue(stats.containsKey("content_types"))
        assertTrue(stats.containsKey("topics"))
        
        val totalItems = stats["total_items"] as Int
        assertTrue(totalItems >= 100)
    }

    @Test
    fun `should filter content by engagement level`() {
        val highEngagement = EnhancedContentFixtures.getContentByEngagementLevel("high")
        val lowEngagement = EnhancedContentFixtures.getContentByEngagementLevel("low")
        
        assertTrue(highEngagement.isNotEmpty())
        assertTrue(lowEngagement.isNotEmpty())
    }

    @Test
    fun `content fixture factory should work`() {
        val newUserContent = ContentFixtureFactory.NewUserScenarios.getDiverseContentMix()
        val trendingContent = ContentFixtureFactory.NewUserScenarios.getTrendingContent()
        
        assertTrue(newUserContent.isNotEmpty())
        assertTrue(trendingContent.isNotEmpty())
    }
}