package com.welcomer.welcome.bdd.fixtures

import com.welcomer.welcome.ingestion.model.ContentType
import com.welcomer.welcome.personalization.service.PersonalizableItem

/**
 * Factory for creating specific content fixture sets for BDD scenarios
 * 
 * This factory provides pre-configured content sets for common testing scenarios,
 * making it easy to set up specific test conditions for personalization algorithms.
 */
object ContentFixtureFactory {

    /**
     * Scenarios for new user testing - diverse content with good baseline scores
     */
    object NewUserScenarios {
        
        fun getDiverseContentMix(): List<PersonalizableItem> {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            
            // Select balanced mix across different categories
            return listOf(
                // Technology (5 items)
                *content.filter { it.hasTopicTag("technology") }.take(5).toTypedArray(),
                // Fitness (5 items)  
                *content.filter { it.hasTopicTag("fitness") }.take(5).toTypedArray(),
                // Travel (5 items)
                *content.filter { it.hasTopicTag("travel") }.take(5).toTypedArray(),
                // Entertainment (5 items)
                *content.filter { it.hasTopicTag("entertainment") }.take(5).toTypedArray(),
                // Mixed other topics (5 items)
                *content.filter { 
                    !it.hasTopicTag("technology") && !it.hasTopicTag("fitness") && 
                    !it.hasTopicTag("travel") && !it.hasTopicTag("entertainment")
                }.take(5).toTypedArray()
            ).distinctBy { it.content.id }
        }

        fun getTrendingContent(): List<PersonalizableItem> {
            return EnhancedContentFixtures.getTrendingContent(0.8).take(15)
        }

        fun getHighQualityContent(): List<PersonalizableItem> {
            return EnhancedContentFixtures.getHighQualityContent(0.85).take(20)
        }
    }

    /**
     * Scenarios for power user testing - personalized content from followed sources
     */
    object PowerUserScenarios {
        
        fun getPersonalizedTechContent(): List<PersonalizableItem> {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            
            return content.filter { item ->
                item.hasTopicTag("technology") || item.hasTopicTag("programming") || 
                item.hasTopicTag("ai") || item.hasTopicTag("software-development")
            }.sortedByDescending { it.baseScore }.take(25)
        }

        fun getFollowedAuthorsContent(): List<PersonalizableItem> {
            return EnhancedContentFixtures.getContentByAuthorRelationship(followed = true).take(20)
        }

        fun getHighEngagementContent(): List<PersonalizableItem> {
            return EnhancedContentFixtures.getContentByEngagementLevel("high").take(15)
        }
    }

    /**
     * Scenarios for topic-focused user testing
     */
    object TopicFocusedScenarios {
        
        fun getFitnessHeavyContent(): List<PersonalizableItem> {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            
            return content.filter { item ->
                item.hasTopicTag("fitness") || item.hasTopicTag("health") || 
                item.hasTopicTag("wellness") || item.hasTopicTag("workout") ||
                item.hasTopicTag("nutrition")
            }.sortedByDescending { it.baseScore }.take(30)
        }

        fun getCookingFocusedContent(): List<PersonalizableItem> {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            
            return content.filter { item ->
                item.hasTopicTag("cooking") || item.hasTopicTag("recipe") || 
                item.hasTopicTag("food") || item.hasTopicTag("baking") ||
                item.hasTopicTag("nutrition")
            }.take(25)
        }

        fun getTechSpecialistContent(): List<PersonalizableItem> {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            
            return content.filter { item ->
                item.hasTopicTag("technology") || item.hasTopicTag("programming") || 
                item.hasTopicTag("ai") || item.hasTopicTag("machine-learning")
            }.take(35)
        }
    }

    /**
     * Scenarios for testing content diversity and anti-filter-bubble mechanisms
     */
    object DiversityScenarios {
        
        fun getFilterBubbleTestSet(): Map<String, List<PersonalizableItem>> {
            return mapOf(
                "homogeneous" to getSingleTopicContent("technology", 20),
                "diverse_balanced" to getDiverseBalancedContent(20),
                "author_bubble" to getSingleAuthorContent("tech-blogger-1", 15),
                "multi_author" to getMultiAuthorContent(15)
            )
        }

        fun getSerendipityTestContent(): List<PersonalizableItem> {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            
            // Content that's high quality but from unexpected topics/authors
            return content.filter { item ->
                val qualityScore = item.metadata["quality_score"] as? Double ?: 0.0
                val isFollowed = item.metadata["is_followed_author"] as? Boolean ?: false
                qualityScore >= 0.8 && !isFollowed
            }.shuffled().take(15)
        }

        private fun getSingleTopicContent(topic: String, count: Int): List<PersonalizableItem> {
            return EnhancedContentFixtures.generateComprehensiveTestContent()
                .filter { it.hasTopicTag(topic) }
                .take(count)
        }

        private fun getDiverseBalancedContent(count: Int): List<PersonalizableItem> {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            val topicGroups = content.groupBy { it.topics.first() }
            val itemsPerTopic = count / minOf(topicGroups.size, 10)
            
            return topicGroups.values.take(10).flatMap { group ->
                group.take(itemsPerTopic)
            }.take(count)
        }

        private fun getSingleAuthorContent(authorId: String, count: Int): List<PersonalizableItem> {
            return EnhancedContentFixtures.generateComprehensiveTestContent()
                .filter { it.authorId == authorId }
                .take(count)
        }

        private fun getMultiAuthorContent(count: Int): List<PersonalizableItem> {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            return content.groupBy { it.authorId }
                .mapValues { it.value.take(1) }
                .values.flatten().take(count)
        }
    }

    /**
     * Scenarios for mobile-first user testing
     */
    object MobileScenarios {
        
        fun getVisualHeavyContent(): List<PersonalizableItem> {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            
            return content.filter { item ->
                item.content.contentType == ContentType.IMAGE || 
                item.content.contentType == ContentType.VIDEO
            }.sortedByDescending { it.baseScore }.take(20)
        }

        fun getShortFormContent(): List<PersonalizableItem> {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            
            return content.filter { item ->
                val contentLength = item.metadata["content_length"] as? Int ?: Int.MAX_VALUE
                when (item.content.contentType) {
                    ContentType.TEXT -> contentLength <= 1000  // Short articles
                    ContentType.VIDEO -> contentLength <= 600  // Videos under 10 minutes
                    ContentType.IMAGE, ContentType.LINK, ContentType.POLL -> true
                }
            }.take(25)
        }

        fun getHighShareabilityContent(): List<PersonalizableItem> {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            
            return content.filter { item ->
                val shareCount = item.metadata["share_count"] as? Int ?: 0
                val viewCount = item.metadata["view_count"] as? Int ?: 1
                val shareRatio = shareCount.toDouble() / viewCount
                shareRatio >= 0.01 // At least 1% share rate
            }.take(15)
        }
    }

    /**
     * Scenarios for accessibility testing
     */
    object AccessibilityScenarios {
        
        fun getTextHeavyContent(): List<PersonalizableItem> {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            
            return content.filter { item ->
                item.content.contentType == ContentType.TEXT
            }.sortedByDescending { 
                it.metadata["quality_score"] as? Double ?: 0.0
            }.take(30)
        }

        fun getDescriptiveContent(): List<PersonalizableItem> {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            
            // Content with good descriptions and detailed text
            return content.filter { item ->
                val contentLength = item.metadata["content_length"] as? Int ?: 0
                when (item.content.contentType) {
                    ContentType.TEXT -> contentLength >= 1000  // Detailed articles
                    ContentType.IMAGE -> item.content.textContent?.length ?: 0 >= 200  // Good descriptions
                    ContentType.VIDEO -> item.content.textContent?.length ?: 0 >= 300  // Detailed descriptions
                    ContentType.LINK, ContentType.POLL -> item.content.textContent?.length ?: 0 >= 200
                }
            }.take(20)
        }
    }

    /**
     * Scenarios for performance testing
     */
    object PerformanceScenarios {
        
        fun getLargeContentSet(): List<PersonalizableItem> {
            return EnhancedContentFixtures.generateComprehensiveTestContent()
        }

        fun getHighVolumeEngagementContent(): List<PersonalizableItem> {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            
            return content.filter { item ->
                val viewCount = item.metadata["view_count"] as? Int ?: 0
                val likeCount = item.metadata["like_count"] as? Int ?: 0
                val commentCount = item.metadata["comment_count"] as? Int ?: 0
                
                viewCount > 5000 || likeCount > 500 || commentCount > 100
            }.sortedByDescending { 
                val views = it.metadata["view_count"] as? Int ?: 0
                val likes = it.metadata["like_count"] as? Int ?: 0
                views + likes * 10  // Weighted engagement score
            }
        }

        fun getComplexMetadataContent(): List<PersonalizableItem> {
            val content = EnhancedContentFixtures.generateComprehensiveTestContent()
            
            return content.filter { item ->
                item.metadata.size >= 8  // Items with rich metadata
            }.take(50)
        }
    }

    /**
     * Get content statistics for any content set
     */
    fun getContentSetStatistics(content: List<PersonalizableItem>): Map<String, Any> {
        return mapOf(
            "total_items" to content.size,
            "content_types" to content.groupingBy { it.content.contentType }.eachCount(),
            "topics" to content.flatMap { it.topics }.groupingBy { it }.eachCount(),
            "authors" to content.groupingBy { it.authorId }.eachCount(),
            "avg_base_score" to if (content.isNotEmpty()) content.map { it.baseScore }.average() else 0.0,
            "score_range" to mapOf(
                "min" to (content.minOfOrNull { it.baseScore } ?: 0.0),
                "max" to (content.maxOfOrNull { it.baseScore } ?: 0.0)
            ),
            "engagement_stats" to mapOf(
                "avg_views" to content.mapNotNull { it.metadata["view_count"] as? Int }.average(),
                "avg_likes" to content.mapNotNull { it.metadata["like_count"] as? Int }.average(),
                "avg_shares" to content.mapNotNull { it.metadata["share_count"] as? Int }.average()
            )
        )
    }

    // Extension function to check if an item has a specific topic tag
    private fun PersonalizableItem.hasTopicTag(topic: String): Boolean {
        return this.topics.any { it.equals(topic, ignoreCase = true) }
    }
}