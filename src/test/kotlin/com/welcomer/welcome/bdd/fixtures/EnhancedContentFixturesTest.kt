package com.welcomer.welcome.bdd.fixtures

import com.welcomer.welcome.ingestion.model.ContentType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

/**
 * Tests for EnhancedContentFixtures to ensure comprehensive content generation
 */
class EnhancedContentFixturesTest {

    @Nested
    @DisplayName("Content Generation")
    inner class ContentGeneration {

        @Test
        fun `should generate at least 100 content items`() {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            
            assertTrue(content.size >= 100, 
                "Should generate at least 100 content items, but got ${content.size}")
        }

        @Test
        fun `should generate diverse content types`() {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            val contentTypes = content.map { it.content.contentType }.toSet()
            
            assertTrue(contentTypes.contains(ContentType.TEXT), "Should include text content")
            assertTrue(contentTypes.contains(ContentType.IMAGE), "Should include image content")
            assertTrue(contentTypes.contains(ContentType.VIDEO), "Should include video content")
            
            // Verify reasonable distribution
            val textCount = content.count { it.content.contentType == ContentType.TEXT }
            val imageCount = content.count { it.content.contentType == ContentType.IMAGE }
            val videoCount = content.count { it.content.contentType == ContentType.VIDEO }
            
            assertTrue(textCount > 10, "Should have significant text content")
            assertTrue(imageCount > 5, "Should have some image content")
            assertTrue(videoCount > 5, "Should have some video content")
        }

        @Test
        fun `should generate content with diverse topics`() {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            val allTopics = content.flatMap { it.topics }.toSet()
            
            assertTrue(allTopics.size >= 20, 
                "Should cover at least 20 different topics, but got ${allTopics.size}")
            
            // Check for key topic categories
            assertTrue(allTopics.any { it.contains("technology", ignoreCase = true) })
            assertTrue(allTopics.any { it.contains("fitness", ignoreCase = true) })
            assertTrue(allTopics.any { it.contains("travel", ignoreCase = true) })
            assertTrue(allTopics.any { it.contains("cooking", ignoreCase = true) })
        }

        @Test
        fun `should generate content from multiple authors`() {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            val authors = content.map { it.authorId }.toSet()
            
            assertTrue(authors.size >= 15, 
                "Should have content from at least 15 authors, but got ${authors.size}")
            
            // Check for followed authors
            val hasFollowedAuthors = content.any { 
                (it.metadata["is_followed_author"] as? Boolean) == true 
            }
            assertTrue(hasFollowedAuthors, "Should include content from followed authors")
        }

        @Test
        fun `should have realistic content metadata`() {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            
            content.take(10).forEach { item ->
                val metadata = item.metadata
                
                // Check required metadata fields
                assertTrue(metadata.containsKey("view_count"), "Should have view count")
                assertTrue(metadata.containsKey("like_count"), "Should have like count")
                assertTrue(metadata.containsKey("quality_score"), "Should have quality score")
                assertTrue(metadata.containsKey("trending_score"), "Should have trending score")
                
                // Validate ranges
                val viewCount = metadata["view_count"] as? Int ?: 0
                assertTrue(viewCount >= 100, "View count should be realistic")
                
                val qualityScore = metadata["quality_score"] as? Double ?: 0.0
                assertTrue(qualityScore in 0.0..1.0, "Quality score should be between 0 and 1")
                
                val trendingScore = metadata["trending_score"] as? Double ?: 0.0
                assertTrue(trendingScore in 0.0..1.0, "Trending score should be between 0 and 1")
            }
        }
    }

    @Nested
    @DisplayName("Content Filtering and Querying")
    inner class ContentFiltering {

        @Test
        fun `should filter content by engagement level`() {
            val highEngagement = EnhancedContentFixtures.getContentByEngagementLevel("high")
            val lowEngagement = EnhancedContentFixtures.getContentByEngagementLevel("low")
            
            assertTrue(highEngagement.isNotEmpty(), "Should have high engagement content")
            assertTrue(lowEngagement.isNotEmpty(), "Should have low engagement content")
            
            // Verify engagement levels
            highEngagement.forEach { item ->
                val viewCount = item.metadata["view_count"] as? Int ?: 0
                assertTrue(viewCount > 5000, "High engagement content should have > 5000 views")
            }
            
            lowEngagement.forEach { item ->
                val viewCount = item.metadata["view_count"] as? Int ?: 0
                assertTrue(viewCount < 1000, "Low engagement content should have < 1000 views")
            }
        }

        @Test
        fun `should filter content by author relationship`() {
            val followedContent = EnhancedContentFixtures.getContentByAuthorRelationship(true)
            val unfollowedContent = EnhancedContentFixtures.getContentByAuthorRelationship(false)
            
            assertTrue(followedContent.isNotEmpty(), "Should have content from followed authors")
            assertTrue(unfollowedContent.isNotEmpty(), "Should have content from unfollowed authors")
            
            // Verify author relationship
            followedContent.forEach { item ->
                val isFollowed = item.metadata["is_followed_author"] as? Boolean ?: false
                assertTrue(isFollowed, "Followed content should be marked as followed")
            }
            
            unfollowedContent.forEach { item ->
                val isFollowed = item.metadata["is_followed_author"] as? Boolean ?: true
                assertFalse(isFollowed, "Unfollowed content should not be marked as followed")
            }
        }

        @Test
        fun `should get trending content`() {
            val trending = EnhancedContentFixtures.getTrendingContent(0.7)
            
            assertTrue(trending.isNotEmpty(), "Should have trending content")
            
            trending.forEach { item ->
                val trendingScore = item.metadata["trending_score"] as? Double ?: 0.0
                assertTrue(trendingScore >= 0.7, "Trending content should have score >= 0.7")
            }
        }

        @Test
        fun `should get recent content`() {
            val recent = EnhancedContentFixtures.getRecentContent(24)
            
            assertTrue(recent.isNotEmpty(), "Should have recent content")
            
            recent.forEach { item ->
                val hoursDiff = java.time.Duration.between(item.createdAt, java.time.Instant.now()).toHours()
                assertTrue(hoursDiff <= 24, "Recent content should be within 24 hours")
            }
        }

        @Test
        fun `should get high quality content`() {
            val highQuality = EnhancedContentFixtures.getHighQualityContent(0.8)
            
            assertTrue(highQuality.isNotEmpty(), "Should have high quality content")
            
            highQuality.forEach { item ->
                val qualityScore = item.metadata["quality_score"] as? Double ?: 0.0
                assertTrue(qualityScore >= 0.8, "High quality content should have score >= 0.8")
            }
        }
    }

    @Nested
    @DisplayName("Diversity Testing Support")
    inner class DiversityTesting {

        @Test
        fun `should generate diversity test sets`() {
            val diversityTestSet = EnhancedContentFixtures.generateDiversityTestSet()
            
            assertTrue(diversityTestSet.containsKey("technology_heavy"))
            assertTrue(diversityTestSet.containsKey("fitness_heavy"))
            assertTrue(diversityTestSet.containsKey("mixed_balanced"))
            assertTrue(diversityTestSet.containsKey("single_author"))
            assertTrue(diversityTestSet.containsKey("multi_author"))
            
            val techHeavy = diversityTestSet["technology_heavy"]!!
            assertTrue(techHeavy.size >= 10, "Technology heavy set should have sufficient content")
            
            val fitnessHeavy = diversityTestSet["fitness_heavy"]!!
            assertTrue(fitnessHeavy.size >= 10, "Fitness heavy set should have sufficient content")
            
            val singleAuthor = diversityTestSet["single_author"]!!
            val authors = singleAuthor.map { it.authorId }.toSet()
            assertEquals(1, authors.size, "Single author set should have content from only one author")
            
            val multiAuthor = diversityTestSet["multi_author"]!!
            val multiAuthors = multiAuthor.map { it.authorId }.toSet()
            assertTrue(multiAuthors.size > 3, "Multi author set should have content from multiple authors")
        }
    }

    @Nested
    @DisplayName("Content Statistics and Verification")
    inner class ContentStatistics {

        @Test
        fun `should provide comprehensive content statistics`() {
            val stats = EnhancedContentFixtures.getContentStatistics()
            
            // Check required statistics
            assertTrue(stats.containsKey("total_items"))
            assertTrue(stats.containsKey("content_types"))
            assertTrue(stats.containsKey("topics"))
            assertTrue(stats.containsKey("authors"))
            assertTrue(stats.containsKey("followed_authors"))
            assertTrue(stats.containsKey("avg_base_score"))
            assertTrue(stats.containsKey("score_distribution"))
            
            // Verify values
            val totalItems = stats["total_items"] as Int
            assertTrue(totalItems >= 100, "Should have at least 100 total items")
            
            val followedAuthors = stats["followed_authors"] as Int
            assertTrue(followedAuthors > 0, "Should have some content from followed authors")
            
            val avgScore = stats["avg_base_score"] as Double
            assertTrue(avgScore > 0.0, "Should have positive average base score")
            
            @Suppress("UNCHECKED_CAST")
            val scoreDistribution = stats["score_distribution"] as Map<String, Int>
            assertTrue(scoreDistribution.containsKey("low"))
            assertTrue(scoreDistribution.containsKey("medium"))
            assertTrue(scoreDistribution.containsKey("high"))
        }

        @Test
        fun `should have balanced content distribution`() {
            val stats = EnhancedContentFixtures.getContentStatistics()
            
            @Suppress("UNCHECKED_CAST")
            val contentTypes = stats["content_types"] as Map<ContentType, Int>
            
            // Should have all content types represented
            assertTrue(contentTypes.containsKey(ContentType.TEXT))
            assertTrue(contentTypes.containsKey(ContentType.IMAGE))
            assertTrue(contentTypes.containsKey(ContentType.VIDEO))
            
            // No content type should dominate too heavily (max 70% of total)
            val totalItems = stats["total_items"] as Int
            contentTypes.values.forEach { count ->
                val percentage = count.toDouble() / totalItems
                assertTrue(percentage <= 0.7, "No single content type should exceed 70% of content")
            }
        }
    }

    @Nested
    @DisplayName("Content Structure Validation")
    inner class ContentStructure {

        @Test
        fun `should have proper content structure`() {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent().take(10)
            
            content.forEach { item ->
                // Validate stored content
                assertNotNull(item.content.id, "Content should have ID")
                assertNotNull(item.content.authorId, "Content should have author ID")
                assertNotNull(item.content.textContent, "Content should have text content")
                assertNotNull(item.content.contentType, "Content should have content type")
                assertTrue(item.content.tags.isNotEmpty(), "Content should have tags")
                
                // Validate PersonalizableItem
                assertTrue(item.baseScore > 0.0, "Content should have positive base score")
                assertTrue(item.topics.isNotEmpty(), "Content should have topics")
                assertNotNull(item.authorId, "Content should have author ID")
                assertNotNull(item.createdAt, "Content should have creation time")
            }
        }

        @Test
        fun `should have unique content IDs`() {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            val ids = content.map { it.content.id }
            val uniqueIds = ids.toSet()
            
            assertEquals(ids.size, uniqueIds.size, "All content items should have unique IDs")
        }

        @Test
        fun `should have realistic content lengths and engagement`() {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent().take(20)
            
            content.forEach { item ->
                val contentLength = item.metadata["content_length"] as? Int ?: 0
                val viewCount = item.metadata["view_count"] as? Int ?: 0
                val likeCount = item.metadata["like_count"] as? Int ?: 0
                
                assertTrue(contentLength > 0, "Content should have realistic length")
                assertTrue(viewCount >= likeCount, "View count should be >= like count")
                
                // Content type specific validations
                when (item.content.contentType) {
                    ContentType.VIDEO -> {
                        assertTrue(contentLength in 300..2700, "Video length should be 5-45 minutes")
                    }
                    ContentType.IMAGE -> {
                        assertTrue(contentLength in 1..10, "Image count should be 1-10")
                    }
                    ContentType.TEXT -> {
                        assertTrue(contentLength in 500..5000, "Text word count should be 500-5000")
                    }
                    ContentType.LINK, ContentType.POLL -> {
                        assertTrue(contentLength > 0, "Link/Poll content should have positive length")
                    }
                }
            }
        }
    }
}