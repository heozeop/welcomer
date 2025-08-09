package com.welcomer.welcome.bdd.fixtures

import com.welcomer.welcome.ingestion.model.ContentStatus
import com.welcomer.welcome.ingestion.model.ContentType
import com.welcomer.welcome.ingestion.model.ContentVisibility
import com.welcomer.welcome.ingestion.model.StoredContent
import com.welcomer.welcome.personalization.service.PersonalizableItem
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Test fixtures for diverse content used in BDD scenarios
 */
object ContentRepositoryFixtures {

    /**
     * Get all test content items
     */
    fun getAllTestContent(): List<PersonalizableItem> {
        return listOf(
            *getTechnologyContent(),
            *getFitnessHealthContent(),
            *getTravelContent(),
            *getCookingContent(),
            *getPhotographyContent(),
            *getEntertainmentContent(),
            *getGeneralContent(),
            *getTrendingContent()
        )
    }

    /**
     * Technology-focused content
     */
    fun getTechnologyContent(): Array<PersonalizableItem> {
        val now = Instant.now()
        
        return arrayOf(
            createPersonalizableItem(
                id = "tech-1",
                authorId = "tech-blogger-1",
                contentType = ContentType.TEXT,
                title = "The Future of AI in Software Development",
                content = "Artificial Intelligence is revolutionizing how we write code. From automated testing to code generation, AI tools are becoming indispensable for modern developers. This comprehensive guide explores the latest trends and practical applications.",
                topics = listOf("technology", "ai", "programming", "software-development"),
                baseScore = 2.5,
                createdAt = now.minus(2, ChronoUnit.HOURS)
            ),
            createPersonalizableItem(
                id = "tech-2",
                authorId = "ai-researcher-2",
                contentType = ContentType.VIDEO,
                title = "Machine Learning Fundamentals Explained",
                content = "A 30-minute deep dive into machine learning concepts every developer should know. Covers supervised learning, neural networks, and practical implementation strategies.",
                topics = listOf("ai", "machine-learning", "education", "programming"),
                baseScore = 3.0,
                createdAt = now.minus(4, ChronoUnit.HOURS)
            ),
            createPersonalizableItem(
                id = "tech-3",
                authorId = "startup-founder-3",
                contentType = ContentType.TEXT,
                title = "Building Scalable Microservices Architecture",
                content = "Lessons learned from scaling our platform from 1K to 1M users. This article covers microservices design patterns, database architecture decisions, and deployment strategies that actually work in production.",
                topics = listOf("technology", "architecture", "scalability", "startup"),
                baseScore = 2.8,
                createdAt = now.minus(6, ChronoUnit.HOURS)
            ),
            createPersonalizableItem(
                id = "tech-4",
                authorId = "tech-writer-4",
                contentType = ContentType.IMAGE,
                title = "Infographic: Programming Languages Popularity 2024",
                content = "Visual breakdown of the most popular programming languages based on GitHub repositories, job postings, and developer surveys. JavaScript and Python continue to dominate.",
                topics = listOf("technology", "programming", "data-visualization"),
                baseScore = 1.8,
                createdAt = now.minus(1, ChronoUnit.DAYS)
            )
        )
    }

    /**
     * Fitness and health content
     */
    fun getFitnessHealthContent(): Array<PersonalizableItem> {
        val now = Instant.now()
        
        return arrayOf(
            createPersonalizableItem(
                id = "fitness-1",
                authorId = "fitness-trainer-1",
                contentType = ContentType.VIDEO,
                title = "15-Minute Morning HIIT Workout",
                content = "Start your day with this energizing high-intensity interval training routine. Perfect for busy schedules - no equipment needed! Follow along with clear instructions and modifications for all fitness levels.",
                topics = listOf("fitness", "workout", "hiit", "morning-routine"),
                baseScore = 2.2,
                createdAt = now.minus(1, ChronoUnit.HOURS)
            ),
            createPersonalizableItem(
                id = "health-1",
                authorId = "nutritionist-2",
                contentType = ContentType.TEXT,
                title = "The Science Behind Intermittent Fasting",
                content = "A comprehensive review of the latest research on intermittent fasting. Learn about different IF protocols, their effects on metabolism, weight loss, and overall health. Includes practical tips for beginners.",
                topics = listOf("health", "nutrition", "intermittent-fasting", "weight-loss"),
                baseScore = 2.7,
                createdAt = now.minus(3, ChronoUnit.HOURS)
            ),
            createPersonalizableItem(
                id = "running-1",
                authorId = "running-coach-3",
                contentType = ContentType.TEXT,
                title = "Marathon Training Plan for Beginners",
                content = "Complete 16-week marathon training schedule designed for first-time marathon runners. Includes weekly mileage progression, cross-training recommendations, and injury prevention strategies.",
                topics = listOf("running", "fitness", "marathon", "training-plan"),
                baseScore = 2.4,
                createdAt = now.minus(8, ChronoUnit.HOURS)
            ),
            createPersonalizableItem(
                id = "wellness-1",
                authorId = "wellness-expert-4",
                contentType = ContentType.IMAGE,
                title = "7 Daily Habits for Better Mental Health",
                content = "Beautiful infographic showing evidence-based daily practices that can improve your mental wellbeing. From meditation to gratitude journaling, these small changes can make a big difference.",
                topics = listOf("wellness", "mental-health", "habits", "mindfulness"),
                baseScore = 1.9,
                createdAt = now.minus(12, ChronoUnit.HOURS)
            )
        )
    }

    /**
     * Travel content
     */
    fun getTravelContent(): Array<PersonalizableItem> {
        val now = Instant.now()
        
        return arrayOf(
            createPersonalizableItem(
                id = "travel-1",
                authorId = "travel-blogger-2",
                contentType = ContentType.IMAGE,
                title = "Hidden Gems of Patagonia: Photo Essay",
                content = "Stunning photography from a 3-week trek through Patagonia's most remote locations. Each image tells a story of untouched wilderness, dramatic landscapes, and the adventure of a lifetime.",
                topics = listOf("travel", "photography", "patagonia", "adventure"),
                baseScore = 2.6,
                createdAt = now.minus(5, ChronoUnit.HOURS)
            ),
            createPersonalizableItem(
                id = "travel-2",
                authorId = "digital-nomad-5",
                contentType = ContentType.TEXT,
                title = "Remote Work from Bali: Complete Guide 2024",
                content = "Everything you need to know about working remotely from Bali. Covers visa requirements, best coworking spaces, cost of living, internet reliability, and cultural considerations for digital nomads.",
                topics = listOf("travel", "remote-work", "bali", "digital-nomad"),
                baseScore = 2.3,
                createdAt = now.minus(7, ChronoUnit.HOURS)
            ),
            createPersonalizableItem(
                id = "travel-3",
                authorId = "backpacker-6",
                contentType = ContentType.VIDEO,
                title = "Budget Travel Tips: See Europe for $50/Day",
                content = "Practical strategies for traveling through Europe on a tight budget. Real examples, money-saving hacks, and insider tips from someone who's done it. Includes sample itineraries and cost breakdowns.",
                topics = listOf("travel", "budget-travel", "europe", "backpacking"),
                baseScore = 2.1,
                createdAt = now.minus(10, ChronoUnit.HOURS)
            )
        )
    }

    /**
     * Cooking and food content
     */
    fun getCookingContent(): Array<PersonalizableItem> {
        val now = Instant.now()
        
        return arrayOf(
            createPersonalizableItem(
                id = "cooking-1",
                authorId = "chef-3",
                contentType = ContentType.VIDEO,
                title = "Perfect Homemade Pasta in 20 Minutes",
                content = "Learn to make restaurant-quality pasta from scratch. This step-by-step tutorial covers everything from mixing the dough to achieving the perfect texture. Plus three classic sauce recipes.",
                topics = listOf("cooking", "pasta", "italian-cuisine", "tutorial"),
                baseScore = 2.4,
                createdAt = now.minus(3, ChronoUnit.HOURS)
            ),
            createPersonalizableItem(
                id = "cooking-2",
                authorId = "food-blogger-7",
                contentType = ContentType.TEXT,
                title = "Plant-Based Meal Prep for Busy Professionals",
                content = "Complete guide to preparing healthy, plant-based meals for the entire week. Includes shopping lists, prep schedules, storage tips, and 15 delicious recipes that reheat beautifully.",
                topics = listOf("cooking", "meal-prep", "plant-based", "healthy-eating"),
                baseScore = 2.2,
                createdAt = now.minus(9, ChronoUnit.HOURS)
            ),
            createPersonalizableItem(
                id = "baking-1",
                authorId = "baker-8",
                contentType = ContentType.IMAGE,
                title = "Sourdough Bread Making: Visual Guide",
                content = "Beautiful step-by-step photography of the complete sourdough bread making process. From starter maintenance to achieving that perfect crust and crumb structure.",
                topics = listOf("baking", "sourdough", "bread", "artisan"),
                baseScore = 1.8,
                createdAt = now.minus(15, ChronoUnit.HOURS)
            )
        )
    }

    /**
     * Photography content
     */
    fun getPhotographyContent(): Array<PersonalizableItem> {
        val now = Instant.now()
        
        return arrayOf(
            createPersonalizableItem(
                id = "photo-1",
                authorId = "photographer-1",
                contentType = ContentType.IMAGE,
                title = "Golden Hour Portrait Techniques",
                content = "Masterclass in capturing stunning portraits during golden hour. Before/after examples showing proper positioning, camera settings, and post-processing techniques for that perfect glow.",
                topics = listOf("photography", "portrait", "golden-hour", "tutorial"),
                baseScore = 2.5,
                createdAt = now.minus(1, ChronoUnit.HOURS)
            ),
            createPersonalizableItem(
                id = "photo-2",
                authorId = "landscape-photographer-9",
                contentType = ContentType.VIDEO,
                title = "Landscape Photography in Harsh Weather",
                content = "Advanced techniques for capturing dramatic landscapes in challenging conditions. Covers equipment protection, safety considerations, and creative opportunities that bad weather provides.",
                topics = listOf("photography", "landscape", "weather", "outdoor"),
                baseScore = 2.3,
                createdAt = now.minus(6, ChronoUnit.HOURS)
            )
        )
    }

    /**
     * Entertainment content (movies, books, music)
     */
    fun getEntertainmentContent(): Array<PersonalizableItem> {
        val now = Instant.now()
        
        return arrayOf(
            createPersonalizableItem(
                id = "movie-1",
                authorId = "film-critic-10",
                contentType = ContentType.TEXT,
                title = "Sci-Fi Films That Predicted Our Future",
                content = "Analysis of science fiction movies from the past 50 years that accurately predicted modern technology and social changes. From Blade Runner's corporate dystopia to Her's AI relationships.",
                topics = listOf("movies", "sci-fi", "technology", "culture"),
                baseScore = 2.1,
                createdAt = now.minus(4, ChronoUnit.HOURS)
            ),
            createPersonalizableItem(
                id = "books-1",
                authorId = "book-reviewer-11",
                contentType = ContentType.TEXT,
                title = "Best Non-Fiction Books of 2024",
                content = "Curated list of the year's most impactful non-fiction releases. From memoirs to scientific discoveries, these books will change how you see the world. Includes reading recommendations by interest.",
                topics = listOf("books", "non-fiction", "reading", "recommendations"),
                baseScore = 1.9,
                createdAt = now.minus(11, ChronoUnit.HOURS)
            )
        )
    }

    /**
     * General interest content
     */
    fun getGeneralContent(): Array<PersonalizableItem> {
        val now = Instant.now()
        
        return arrayOf(
            createPersonalizableItem(
                id = "general-1",
                authorId = "lifestyle-blogger-12",
                contentType = ContentType.TEXT,
                title = "The Art of Saying No: Protecting Your Time",
                content = "Practical strategies for setting boundaries and prioritizing what matters most. Learn how to decline requests gracefully while maintaining relationships and focusing on your goals.",
                topics = listOf("productivity", "lifestyle", "boundaries", "time-management"),
                baseScore = 1.7,
                createdAt = now.minus(13, ChronoUnit.HOURS)
            ),
            createPersonalizableItem(
                id = "general-2",
                authorId = "psychology-writer-13",
                contentType = ContentType.TEXT,
                title = "Understanding Cognitive Biases in Daily Life",
                content = "Exploration of common cognitive biases that affect our decision-making. Real-world examples and practical tips for recognizing and overcoming these mental shortcuts.",
                topics = listOf("psychology", "decision-making", "cognitive-science", "self-improvement"),
                baseScore = 2.0,
                createdAt = now.minus(16, ChronoUnit.HOURS)
            )
        )
    }

    /**
     * Trending content that should appeal to new users
     */
    fun getTrendingContent(): Array<PersonalizableItem> {
        val now = Instant.now()
        
        return arrayOf(
            createPersonalizableItem(
                id = "trending-1",
                authorId = "viral-creator-14",
                contentType = ContentType.VIDEO,
                title = "Life Lessons from a 100-Year-Old",
                content = "Heartwarming interview with a centenarian sharing wisdom about life, love, and happiness. This viral video has touched millions with its simple yet profound insights.",
                topics = listOf("inspiration", "wisdom", "life-lessons", "viral"),
                baseScore = 3.5, // High trending score
                createdAt = now.minus(30, ChronoUnit.MINUTES), // Very recent
                metadata = mapOf("trending_score" to 0.95, "viral_factor" to 0.9)
            ),
            createPersonalizableItem(
                id = "trending-2",
                authorId = "news-aggregator-15",
                contentType = ContentType.TEXT,
                title = "Breaking: Major Scientific Discovery Announced",
                content = "Scientists at leading research institutions announce a breakthrough that could revolutionize renewable energy. The discovery promises to make clean energy more efficient and accessible worldwide.",
                topics = listOf("science", "renewable-energy", "breakthrough", "news"),
                baseScore = 3.2,
                createdAt = now.minus(45, ChronoUnit.MINUTES),
                metadata = mapOf("trending_score" to 0.9, "breaking_news" to true)
            ),
            createPersonalizableItem(
                id = "trending-3",
                authorId = "popular-educator-16",
                contentType = ContentType.IMAGE,
                title = "Mind-Blowing Space Photography from James Webb",
                content = "Latest images from the James Webb Space Telescope reveal unprecedented detail of distant galaxies. These stunning visuals are reshaping our understanding of the early universe.",
                topics = listOf("space", "astronomy", "photography", "science"),
                baseScore = 3.0,
                createdAt = now.minus(1, ChronoUnit.HOURS),
                metadata = mapOf("trending_score" to 0.85, "shareability" to 0.95)
            )
        )
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
     * Get content by topic for specific test scenarios
     */
    fun getContentByTopic(topic: String): List<PersonalizableItem> {
        return getAllTestContent().filter { item ->
            item.topics.any { it.contains(topic, ignoreCase = true) }
        }
    }

    /**
     * Get content by author for source affinity testing
     */
    fun getContentByAuthor(authorId: String): List<PersonalizableItem> {
        return getAllTestContent().filter { item ->
            item.authorId == authorId
        }
    }

    /**
     * Get content by type for content type preference testing
     */
    fun getContentByType(contentType: ContentType): List<PersonalizableItem> {
        return getAllTestContent().filter { item ->
            item.content.contentType == contentType
        }
    }

    /**
     * Get recent content (within specified hours)
     */
    fun getRecentContent(withinHours: Long = 6): List<PersonalizableItem> {
        val cutoff = Instant.now().minus(withinHours, ChronoUnit.HOURS)
        return getAllTestContent().filter { item ->
            item.createdAt.isAfter(cutoff)
        }
    }

    /**
     * Get trending content with high engagement scores
     */
    fun getTrendingContentItems(): List<PersonalizableItem> {
        return getTrendingContent().toList()
    }
}