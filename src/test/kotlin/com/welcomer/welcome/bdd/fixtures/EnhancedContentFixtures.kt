package com.welcomer.welcome.bdd.fixtures

import com.welcomer.welcome.ingestion.model.ContentStatus
import com.welcomer.welcome.ingestion.model.ContentType
import com.welcomer.welcome.ingestion.model.ContentVisibility
import com.welcomer.welcome.ingestion.model.StoredContent
import com.welcomer.welcome.personalization.service.PersonalizableItem
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Enhanced content fixtures factory for comprehensive BDD testing
 * 
 * This factory can generate at least 100 diverse content items across different types,
 * topics, authors, and engagement patterns for testing personalization algorithms.
 */
object EnhancedContentFixtures {

    // Content generation parameters
    private val topics = listOf(
        "technology", "programming", "ai", "machine-learning", "software-development",
        "fitness", "health", "nutrition", "workout", "wellness", "mental-health",
        "travel", "adventure", "backpacking", "digital-nomad", "photography",
        "cooking", "recipe", "baking", "plant-based", "meal-prep",
        "movies", "books", "music", "entertainment", "gaming",
        "science", "space", "astronomy", "physics", "biology",
        "business", "startup", "entrepreneurship", "finance", "investing",
        "education", "learning", "career", "productivity", "self-improvement",
        "art", "design", "creativity", "crafts", "diy",
        "environment", "sustainability", "climate", "renewable-energy",
        "sports", "football", "basketball", "running", "cycling",
        "fashion", "style", "beauty", "lifestyle", "home-decor",
        "pets", "dogs", "cats", "animals", "nature"
    )

    private val contentTypes = listOf(ContentType.TEXT, ContentType.IMAGE, ContentType.VIDEO)

    private val authors = (1..50).map { "author-$it" }

    private val followedAuthors = listOf(
        "tech-blogger-1", "ai-researcher-2", "startup-founder-3", "fitness-trainer-1",
        "nutritionist-2", "running-coach-3", "travel-blogger-2", "digital-nomad-5",
        "chef-3", "food-blogger-7", "photographer-1", "landscape-photographer-9"
    )

    /**
     * Generate comprehensive test content with at least 100 diverse items
     */
    fun generateComprehensiveTestContent(): List<PersonalizableItem> {
        val content = mutableListOf<PersonalizableItem>()

        // Add existing curated content
        content.addAll(ContentRepositoryFixtures.getAllTestContent())

        // Generate additional diverse content to reach 100+ items
        val baseTime = Instant.now()
        
        // Technology content (20 items)
        content.addAll(generateTopicContent("technology", 20, baseTime))
        
        // Fitness content (15 items) 
        content.addAll(generateTopicContent("fitness", 15, baseTime.minus(1, ChronoUnit.HOURS)))
        
        // Travel content (15 items)
        content.addAll(generateTopicContent("travel", 15, baseTime.minus(2, ChronoUnit.HOURS)))
        
        // Cooking content (10 items)
        content.addAll(generateTopicContent("cooking", 10, baseTime.minus(3, ChronoUnit.HOURS)))
        
        // Entertainment content (10 items)
        content.addAll(generateTopicContent("entertainment", 10, baseTime.minus(4, ChronoUnit.HOURS)))
        
        // Science content (10 items)
        content.addAll(generateTopicContent("science", 10, baseTime.minus(5, ChronoUnit.HOURS)))
        
        // Business content (8 items)
        content.addAll(generateTopicContent("business", 8, baseTime.minus(6, ChronoUnit.HOURS)))
        
        // Mixed diverse content (12 items)
        content.addAll(generateMixedContent(12, baseTime.minus(7, ChronoUnit.HOURS)))

        return content.distinctBy { it.content.id } // Remove any duplicates
    }

    /**
     * Generate content items for a specific topic
     */
    private fun generateTopicContent(topic: String, count: Int, baseTime: Instant): List<PersonalizableItem> {
        return (1..count).map { index ->
            val contentType = contentTypes.random()
            val author = if (Random.nextDouble() < 0.3) followedAuthors.random() else authors.random()
            val isFollowed = author in followedAuthors
            
            createPersonalizableItem(
                id = "${topic}-generated-$index",
                authorId = author,
                contentType = contentType,
                title = generateTitle(topic, contentType),
                content = generateContent(topic, contentType),
                topics = generateTopicTags(topic),
                baseScore = generateBaseScore(isFollowed),
                createdAt = baseTime.minus(Random.nextLong(0, 24), ChronoUnit.HOURS),
                metadata = generateMetadata(topic, contentType, isFollowed)
            )
        }
    }

    /**
     * Generate mixed content across various topics
     */
    private fun generateMixedContent(count: Int, baseTime: Instant): List<PersonalizableItem> {
        return (1..count).map { index ->
            val topic = topics.random()
            val contentType = contentTypes.random()
            val author = authors.random()
            
            createPersonalizableItem(
                id = "mixed-generated-$index",
                authorId = author,
                contentType = contentType,
                title = generateTitle(topic, contentType),
                content = generateContent(topic, contentType),
                topics = generateTopicTags(topic),
                baseScore = Random.nextDouble(1.0, 3.5),
                createdAt = baseTime.minus(Random.nextLong(0, 72), ChronoUnit.HOURS),
                metadata = generateMetadata(topic, contentType, false)
            )
        }
    }

    /**
     * Generate realistic titles based on topic and content type
     */
    private fun generateTitle(topic: String, contentType: ContentType): String {
        val titleTemplates = when (topic) {
            "technology" -> listOf(
                "Latest Trends in %s Development",
                "How %s is Changing the Tech Industry", 
                "Complete Guide to %s Programming",
                "Top 10 %s Tools for Developers",
                "Understanding %s: A Deep Dive"
            )
            "fitness" -> listOf(
                "Effective %s Workout Routine",
                "Transform Your Body with %s Training",
                "The Science Behind %s Exercise",
                "Complete %s Guide for Beginners",
                "Advanced %s Techniques Revealed"
            )
            "travel" -> listOf(
                "Hidden Gems of %s: Travel Guide",
                "Budget %s Travel Tips and Tricks",
                "Ultimate %s Adventure Experience",
                "Cultural Highlights of %s Region",
                "Solo Travel in %s: What to Know"
            )
            "cooking" -> listOf(
                "Master the Art of %s Cooking",
                "Quick and Easy %s Recipes",
                "Traditional %s Dishes Made Simple", 
                "Healthy %s Meal Ideas",
                "Professional %s Techniques at Home"
            )
            else -> listOf(
                "Everything You Need to Know About %s",
                "The Ultimate %s Guide",
                "Exploring the World of %s",
                "Mastering %s: Tips and Tricks",
                "Deep Dive into %s Fundamentals"
            )
        }
        
        val template = titleTemplates.random()
        val subtopic = generateSubtopic(topic)
        
        return when (contentType) {
            ContentType.VIDEO -> template.format(subtopic) + " [VIDEO]"
            ContentType.IMAGE -> template.format(subtopic) + " [INFOGRAPHIC]"
            else -> template.format(subtopic)
        }
    }

    /**
     * Generate subtopics for variety
     */
    private fun generateSubtopic(topic: String): String {
        return when (topic) {
            "technology" -> listOf("AI", "Cloud", "Mobile", "Web", "Backend", "Frontend", "DevOps").random()
            "fitness" -> listOf("Cardio", "Strength", "HIIT", "Yoga", "Pilates", "CrossFit", "Calisthenics").random()
            "travel" -> listOf("Europe", "Asia", "Adventure", "Budget", "Luxury", "Solo", "Family").random()
            "cooking" -> listOf("Italian", "Asian", "Healthy", "Quick", "Gourmet", "Vegan", "Dessert").random()
            "science" -> listOf("Physics", "Biology", "Chemistry", "Space", "Climate", "Medicine", "Research").random()
            "business" -> listOf("Startup", "Marketing", "Finance", "Leadership", "Strategy", "Innovation", "Growth").random()
            else -> topic.capitalize()
        }
    }

    /**
     * Generate realistic content based on topic and type
     */
    private fun generateContent(topic: String, contentType: ContentType): String {
        val contentTemplates = when (contentType) {
            ContentType.VIDEO -> "Watch this comprehensive video guide covering %s fundamentals, practical examples, and expert insights. Duration: ${Random.nextInt(5, 45)} minutes."
            ContentType.IMAGE -> "Visual guide showcasing %s concepts through high-quality images, diagrams, and step-by-step illustrations. Perfect for visual learners."
            else -> "In-depth article exploring %s with detailed analysis, practical tips, and real-world examples. Includes case studies and actionable insights."
        }
        
        return contentTemplates.format(topic) + " " + generateAdditionalContext(topic)
    }

    /**
     * Generate additional context for content
     */
    private fun generateAdditionalContext(topic: String): String {
        val contexts = when (topic) {
            "technology" -> listOf(
                "Includes code examples and implementation details.",
                "Features industry best practices and optimization tips.",
                "Covers latest frameworks and development tools.",
                "Discusses scalability and performance considerations."
            )
            "fitness" -> listOf(
                "Suitable for all fitness levels with modifications provided.",
                "Includes proper form demonstrations and safety tips.",
                "Features nutritional guidance and recovery strategies.",
                "Backed by sports science research and expert advice."
            )
            "travel" -> listOf(
                "Includes budget breakdowns and money-saving tips.",
                "Features local insights and cultural recommendations.",
                "Covers transportation and accommodation options.",
                "Includes safety tips and emergency preparations."
            )
            else -> listOf(
                "Based on extensive research and expert knowledge.",
                "Includes practical tips and real-world applications.",
                "Features step-by-step instructions and examples.",
                "Suitable for beginners and advanced users alike."
            )
        }
        return contexts.random()
    }

    /**
     * Generate topic tags with primary and secondary topics
     */
    private fun generateTopicTags(primaryTopic: String): List<String> {
        val tags = mutableListOf(primaryTopic)
        
        // Add related topics
        val relatedTopics = when (primaryTopic) {
            "technology" -> listOf("programming", "software-development", "innovation")
            "fitness" -> listOf("health", "wellness", "exercise")
            "travel" -> listOf("adventure", "culture", "photography")
            "cooking" -> listOf("food", "nutrition", "recipe")
            "science" -> listOf("research", "discovery", "innovation")
            "business" -> listOf("entrepreneurship", "strategy", "growth")
            else -> listOf("education", "learning")
        }
        
        // Add 1-2 related topics
        tags.addAll(relatedTopics.shuffled().take(Random.nextInt(1, 3)))
        
        return tags.distinct()
    }

    /**
     * Generate base score based on content characteristics
     */
    private fun generateBaseScore(isFromFollowedAuthor: Boolean): Double {
        val baseRange = if (isFromFollowedAuthor) Pair(2.0, 4.0) else Pair(1.0, 3.0)
        return Random.nextDouble(baseRange.first, baseRange.second)
    }

    /**
     * Generate metadata for content personalization testing
     */
    private fun generateMetadata(topic: String, contentType: ContentType, isFollowed: Boolean): Map<String, Any> {
        val metadata = mutableMapOf<String, Any>()
        
        // Engagement metrics
        metadata["view_count"] = Random.nextInt(100, 10000)
        metadata["like_count"] = Random.nextInt(10, 1000)
        metadata["share_count"] = Random.nextInt(1, 100)
        metadata["comment_count"] = Random.nextInt(0, 200)
        
        // Content characteristics
        metadata["content_length"] = when (contentType) {
            ContentType.VIDEO -> Random.nextInt(300, 2700) // 5-45 minutes in seconds
            ContentType.IMAGE -> Random.nextInt(1, 10) // Number of images
            else -> Random.nextInt(500, 5000) // Word count
        }
        
        // Trending and quality scores
        metadata["trending_score"] = Random.nextDouble(0.0, 1.0)
        metadata["quality_score"] = Random.nextDouble(0.6, 1.0)
        
        // Author relationship
        metadata["is_followed_author"] = isFollowed
        
        // Topic relevance (higher for focused content)
        metadata["topic_relevance"] = Random.nextDouble(0.7, 1.0)
        
        // Recency boost (newer content gets higher score)
        val hoursSinceCreation = Random.nextLong(0, 72)
        metadata["recency_boost"] = maxOf(0.1, 1.0 - (hoursSinceCreation / 72.0))
        
        return metadata
    }

    /**
     * Helper function to create PersonalizableItem with StoredContent
     */
    private fun createPersonalizableItem(
        id: String,
        authorId: String,
        contentType: ContentType,
        title: String,
        content: String,
        topics: List<String>,
        baseScore: Double,
        createdAt: Instant,
        metadata: Map<String, Any> = emptyMap()
    ): PersonalizableItem {
        val storedContent = StoredContent(
            id = id,
            authorId = authorId,
            contentType = contentType,
            textContent = "$title\n\n$content",
            visibility = ContentVisibility.PUBLIC,
            status = ContentStatus.PUBLISHED,
            tags = topics,
            createdAt = createdAt,
            updatedAt = createdAt
        )
        
        return PersonalizableItem(
            content = storedContent,
            baseScore = baseScore,
            metadata = metadata
        )
    }

    /**
     * Get content filtered by specific criteria for testing
     */
    fun getContentByEngagementLevel(level: String): List<PersonalizableItem> {
        val allContent = generateComprehensiveTestContent()
        return when (level.lowercase()) {
            "high" -> allContent.filter { (it.metadata["view_count"] as? Int ?: 0) > 5000 }
            "medium" -> allContent.filter { 
                val views = (it.metadata["view_count"] as? Int ?: 0)
                views in 1000..5000
            }
            "low" -> allContent.filter { (it.metadata["view_count"] as? Int ?: 0) < 1000 }
            else -> allContent
        }
    }

    /**
     * Get content from followed vs non-followed authors
     */
    fun getContentByAuthorRelationship(followed: Boolean): List<PersonalizableItem> {
        return generateComprehensiveTestContent().filter { 
            (it.metadata["is_followed_author"] as? Boolean ?: false) == followed
        }
    }

    /**
     * Get trending content (high trending scores)
     */
    fun getTrendingContent(threshold: Double = 0.7): List<PersonalizableItem> {
        return generateComprehensiveTestContent().filter { 
            (it.metadata["trending_score"] as? Double ?: 0.0) >= threshold
        }
    }

    /**
     * Get content by recency (within specified hours)
     */
    fun getRecentContent(withinHours: Long = 24): List<PersonalizableItem> {
        val cutoff = Instant.now().minus(withinHours, ChronoUnit.HOURS)
        return generateComprehensiveTestContent().filter { 
            it.createdAt.isAfter(cutoff)
        }
    }

    /**
     * Get high-quality content for testing quality filters
     */
    fun getHighQualityContent(threshold: Double = 0.8): List<PersonalizableItem> {
        return generateComprehensiveTestContent().filter { 
            (it.metadata["quality_score"] as? Double ?: 0.0) >= threshold
        }
    }

    /**
     * Generate content with specific distribution for testing diversity algorithms
     */
    fun generateDiversityTestSet(): Map<String, List<PersonalizableItem>> {
        val allContent = generateComprehensiveTestContent()
        
        return mapOf(
            "technology_heavy" to allContent.filter { 
                it.topics.any { topic -> topic in listOf("technology", "programming", "ai") }
            }.take(20),
            "fitness_heavy" to allContent.filter { 
                it.topics.any { topic -> topic in listOf("fitness", "health", "wellness") }
            }.take(20),
            "mixed_balanced" to allContent.groupBy { it.topics.first() }
                .mapValues { it.value.take(2) }.values.flatten(),
            "single_author" to allContent.filter { it.authorId == "tech-blogger-1" },
            "multi_author" to allContent.groupBy { it.authorId }
                .mapValues { it.value.take(1) }.values.flatten()
        )
    }

    /**
     * Get content statistics for testing and verification
     */
    fun getContentStatistics(): Map<String, Any> {
        val allContent = generateComprehensiveTestContent()
        
        return mapOf(
            "total_items" to allContent.size,
            "content_types" to allContent.groupingBy { it.content.contentType }.eachCount(),
            "topics" to allContent.flatMap { it.topics }.groupingBy { it }.eachCount(),
            "authors" to allContent.groupingBy { it.authorId }.eachCount(),
            "followed_authors" to allContent.count { 
                (it.metadata["is_followed_author"] as? Boolean) == true 
            },
            "avg_base_score" to allContent.map { it.baseScore }.average(),
            "score_distribution" to mapOf(
                "low" to allContent.count { it.baseScore < 2.0 },
                "medium" to allContent.count { it.baseScore in 2.0..3.0 },
                "high" to allContent.count { it.baseScore > 3.0 }
            )
        )
    }
}