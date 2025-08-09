package com.welcomer.welcome.bdd.fixtures

import com.welcomer.welcome.engagement.model.EngagementType
import com.welcomer.welcome.ingestion.model.ContentStatus
import com.welcomer.welcome.ingestion.model.ContentType
import com.welcomer.welcome.ingestion.model.ContentVisibility
import com.welcomer.welcome.ingestion.model.StoredContent
import com.welcomer.welcome.personalization.service.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Fixtures for contextual personalization testing
 * Provides content and personas optimized for time and location-based scenarios
 */
object ContextualFixtures {

    /**
     * Generate time-specific content for different parts of the day
     */
    fun generateTimeContextualContent(): Map<String, List<PersonalizableItem>> {
        val baseTime = Instant.now()
        
        return mapOf(
            "morning" to generateMorningContent(baseTime),
            "lunch" to generateLunchContent(baseTime),
            "evening" to generateEveningContent(baseTime),
            "late_night" to generateLateNightContent(baseTime),
            "weekend" to generateWeekendContent(baseTime),
            "commute" to generateCommuteContent(baseTime),
            "work_hours" to generateWorkHoursContent(baseTime)
        )
    }

    /**
     * Generate location-specific content for different regions
     */
    fun generateLocationContextualContent(): Map<String, List<PersonalizableItem>> {
        val baseTime = Instant.now()
        
        return mapOf(
            "new_york" to generateNewYorkContent(baseTime),
            "london" to generateLondonContent(baseTime),
            "tokyo" to generateTokyoContent(baseTime),
            "san_francisco" to generateSanFranciscoContent(baseTime),
            "seattle" to generateSeattleContent(baseTime),
            "local_general" to generateLocalContent(baseTime)
        )
    }

    /**
     * Generate weather-specific content
     */
    fun generateWeatherContextualContent(): Map<String, List<PersonalizableItem>> {
        val baseTime = Instant.now()
        
        return mapOf(
            "rainy" to generateRainyWeatherContent(baseTime),
            "sunny" to generateSunnyWeatherContent(baseTime),
            "winter" to generateWinterContent(baseTime),
            "summer" to generateSummerContent(baseTime)
        )
    }

    /**
     * Generate event-specific content
     */
    fun generateEventContextualContent(): Map<String, List<PersonalizableItem>> {
        val baseTime = Instant.now()
        
        return mapOf(
            "holiday" to generateHolidayContent(baseTime),
            "sporting_event" to generateSportingEventContent(baseTime),
            "breaking_news" to generateBreakingNewsContent(baseTime)
        )
    }

    private fun generateMorningContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..15).map { index ->
            val topics = when (index % 5) {
                0 -> listOf("news", "current-events", "morning")
                1 -> listOf("motivation", "productivity", "morning")
                2 -> listOf("health", "fitness", "morning-routine")
                3 -> listOf("education", "learning", "morning-read")
                else -> listOf("weather", "traffic", "morning-updates")
            }
            
            createContextualContent(
                id = "morning-content-$index",
                title = generateMorningTitle(topics.first()),
                topics = topics,
                contentType = ContentType.TEXT,
                metadata = mapOf(
                    "energizing" to true,
                    "time_context" to "morning",
                    "tone" to "energizing",
                    "reading_time" to Random.nextInt(2, 8), // 2-8 minutes
                    "quality_score" to Random.nextDouble(0.7, 0.9)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateLunchContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..10).map { index ->
            val topics = when (index % 4) {
                0 -> listOf("quick-read", "news", "lunch")
                1 -> listOf("lifestyle", "food", "restaurants")
                2 -> listOf("technology", "brief", "updates")
                else -> listOf("entertainment", "light", "casual")
            }
            
            createContextualContent(
                id = "lunch-content-$index",
                title = generateLunchTitle(topics.first()),
                topics = topics,
                contentType = if (index % 3 == 0) ContentType.IMAGE else ContentType.TEXT,
                metadata = mapOf(
                    "content_length" to Random.nextInt(300, 1200), // Short content
                    "time_context" to "lunch",
                    "consumption_time" to Random.nextInt(3, 10), // 3-10 minutes
                    "quality_score" to Random.nextDouble(0.6, 0.8)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateEveningContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..15).map { index ->
            val topics = when (index % 5) {
                0 -> listOf("entertainment", "movies", "evening")
                1 -> listOf("lifestyle", "relaxation", "wind-down")
                2 -> listOf("cooking", "recipes", "dinner")
                3 -> listOf("books", "reading", "evening")
                else -> listOf("music", "art", "culture")
            }
            
            createContextualContent(
                id = "evening-content-$index",
                title = generateEveningTitle(topics.first()),
                topics = topics,
                contentType = if (index % 3 == 0) ContentType.VIDEO else ContentType.TEXT,
                metadata = mapOf(
                    "relaxing" to true,
                    "time_context" to "evening",
                    "tone" to "relaxing",
                    "content_length" to Random.nextInt(800, 3000), // Longer reads
                    "quality_score" to Random.nextDouble(0.7, 0.9)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateLateNightContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..10).map { index ->
            val topics = when (index % 4) {
                0 -> listOf("creative", "art", "inspiration")
                1 -> listOf("international", "global", "late-night")
                2 -> listOf("philosophy", "thought-provoking", "deep")
                else -> listOf("science", "research", "contemplative")
            }
            
            createContextualContent(
                id = "late-night-content-$index",
                title = generateLateNightTitle(topics.first()),
                topics = topics,
                contentType = ContentType.TEXT,
                metadata = mapOf(
                    "time_context" to "late_night",
                    "tone" to "contemplative",
                    "thought_provoking" to true,
                    "content_length" to Random.nextInt(1000, 4000), // Deeper reads
                    "quality_score" to Random.nextDouble(0.8, 0.95)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateWeekendContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..12).map { index ->
            val topics = when (index % 4) {
                0 -> listOf("leisure", "hobby", "weekend")
                1 -> listOf("family", "relationships", "personal")
                2 -> listOf("travel", "adventure", "exploration")
                else -> listOf("crafts", "diy", "home-improvement")
            }
            
            createContextualContent(
                id = "weekend-content-$index",
                title = generateWeekendTitle(topics.first()),
                topics = topics,
                contentType = if (index % 2 == 0) ContentType.IMAGE else ContentType.TEXT,
                metadata = mapOf(
                    "relaxing" to true,
                    "time_context" to "weekend",
                    "pace" to "relaxed",
                    "quality_score" to Random.nextDouble(0.7, 0.85)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateCommuteContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..8).map { index ->
            val topics = when (index % 3) {
                0 -> listOf("podcast", "audio", "commute")
                1 -> listOf("news", "brief", "updates")
                else -> listOf("traffic", "transportation", "local")
            }
            
            createContextualContent(
                id = "commute-content-$index",
                title = generateCommuteTitle(topics.first()),
                topics = topics,
                contentType = if (index % 2 == 0) ContentType.VIDEO else ContentType.TEXT,
                metadata = mapOf(
                    "time_context" to "commute",
                    "hands_free" to (index % 2 == 0),
                    "content_length" to Random.nextInt(300, 2400), // 5-40 minutes
                    "commute_friendly" to true,
                    "quality_score" to Random.nextDouble(0.6, 0.8)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateWorkHoursContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..12).map { index ->
            val topics = when (index % 4) {
                0 -> listOf("professional", "career", "business")
                1 -> listOf("industry", "technology", "innovation")
                2 -> listOf("skills", "education", "development")
                else -> listOf("productivity", "workplace", "management")
            }
            
            createContextualContent(
                id = "work-content-$index",
                title = generateWorkTitle(topics.first()),
                topics = topics,
                contentType = ContentType.TEXT,
                metadata = mapOf(
                    "time_context" to "work_hours",
                    "professional" to true,
                    "workplace_appropriate" to true,
                    "quality_score" to Random.nextDouble(0.8, 0.95)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateNewYorkContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..8).map { index ->
            val topics = when (index % 3) {
                0 -> listOf("new-york", "local", "nyc")
                1 -> listOf("finance", "wall-street", "business")
                else -> listOf("culture", "arts", "broadway")
            }
            
            createContextualContent(
                id = "nyc-content-$index",
                title = generateLocationTitle("New York", topics.first()),
                topics = topics,
                contentType = ContentType.TEXT,
                metadata = mapOf(
                    "location" to "new_york",
                    "local_relevance" to true,
                    "timezone" to "US/Eastern",
                    "quality_score" to Random.nextDouble(0.7, 0.9)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateLondonContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..8).map { index ->
            val topics = when (index % 3) {
                0 -> listOf("london", "uk", "local")
                1 -> listOf("british", "culture", "history")
                else -> listOf("europe", "international", "brexit")
            }
            
            createContextualContent(
                id = "london-content-$index",
                title = generateLocationTitle("London", topics.first()),
                topics = topics,
                contentType = ContentType.TEXT,
                metadata = mapOf(
                    "location" to "london",
                    "local_relevance" to true,
                    "timezone" to "Europe/London",
                    "cultural_context" to "british",
                    "quality_score" to Random.nextDouble(0.7, 0.9)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateTokyoContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..8).map { index ->
            val topics = when (index % 3) {
                0 -> listOf("tokyo", "japan", "local")
                1 -> listOf("technology", "innovation", "asian")
                else -> listOf("culture", "tradition", "modern")
            }
            
            createContextualContent(
                id = "tokyo-content-$index",
                title = generateLocationTitle("Tokyo", topics.first()),
                topics = topics,
                contentType = ContentType.TEXT,
                metadata = mapOf(
                    "location" to "tokyo",
                    "local_relevance" to true,
                    "timezone" to "Asia/Tokyo",
                    "cultural_context" to "japanese",
                    "international_perspective" to true,
                    "quality_score" to Random.nextDouble(0.7, 0.9)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateSanFranciscoContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..8).map { index ->
            val topics = when (index % 3) {
                0 -> listOf("san-francisco", "bay-area", "local")
                1 -> listOf("technology", "startups", "silicon-valley")
                else -> listOf("innovation", "venture-capital", "tech")
            }
            
            createContextualContent(
                id = "sf-content-$index",
                title = generateLocationTitle("San Francisco", topics.first()),
                topics = topics,
                contentType = ContentType.TEXT,
                metadata = mapOf(
                    "location" to "san_francisco",
                    "local_relevance" to true,
                    "timezone" to "US/Pacific",
                    "tech_hub" to true,
                    "quality_score" to Random.nextDouble(0.7, 0.9)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateSeattleContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..6).map { index ->
            val topics = when (index % 3) {
                0 -> listOf("seattle", "pacific-northwest", "local")
                1 -> listOf("technology", "amazon", "microsoft")
                else -> listOf("coffee", "rain", "culture")
            }
            
            createContextualContent(
                id = "seattle-content-$index",
                title = generateLocationTitle("Seattle", topics.first()),
                topics = topics,
                contentType = ContentType.TEXT,
                metadata = mapOf(
                    "location" to "seattle",
                    "local_relevance" to true,
                    "timezone" to "US/Pacific",
                    "weather_context" to "rainy",
                    "quality_score" to Random.nextDouble(0.7, 0.9)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateLocalContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..10).map { index ->
            val topics = when (index % 4) {
                0 -> listOf("local", "community", "neighborhood")
                1 -> listOf("events", "local-news", "happenings")
                2 -> listOf("restaurants", "local-business", "recommendations")
                else -> listOf("weather", "traffic", "local-updates")
            }
            
            createContextualContent(
                id = "local-content-$index",
                title = generateLocalTitle(topics.first()),
                topics = topics,
                contentType = ContentType.TEXT,
                metadata = mapOf(
                    "local_relevance" to true,
                    "community_focused" to true,
                    "quality_score" to Random.nextDouble(0.6, 0.8)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateRainyWeatherContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..6).map { index ->
            val topics = when (index % 3) {
                0 -> listOf("indoor", "cozy", "reading")
                1 -> listOf("cooking", "home", "comfort")
                else -> listOf("rainy", "weather", "activities")
            }
            
            createContextualContent(
                id = "rainy-content-$index",
                title = generateWeatherTitle("rainy", topics.first()),
                topics = topics,
                contentType = ContentType.TEXT,
                metadata = mapOf(
                    "weather_context" to "rainy",
                    "indoor_activity" to true,
                    "cozy" to true,
                    "quality_score" to Random.nextDouble(0.6, 0.8)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateSunnyWeatherContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..6).map { index ->
            val topics = when (index % 3) {
                0 -> listOf("outdoor", "sunny", "activities")
                1 -> listOf("sports", "hiking", "adventure")
                else -> listOf("travel", "exploration", "nature")
            }
            
            createContextualContent(
                id = "sunny-content-$index",
                title = generateWeatherTitle("sunny", topics.first()),
                topics = topics,
                contentType = ContentType.TEXT,
                metadata = mapOf(
                    "weather_context" to "sunny",
                    "outdoor_activity" to true,
                    "energizing" to true,
                    "quality_score" to Random.nextDouble(0.6, 0.8)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateWinterContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..6).map { index ->
            val topics = when (index % 3) {
                0 -> listOf("winter", "seasonal", "cold")
                1 -> listOf("indoor", "warm", "comfort")
                else -> listOf("holidays", "winter-activities", "cozy")
            }
            
            createContextualContent(
                id = "winter-content-$index",
                title = generateSeasonalTitle("winter", topics.first()),
                topics = topics,
                contentType = ContentType.TEXT,
                metadata = mapOf(
                    "seasonal_context" to "winter",
                    "season" to "winter",
                    "cozy" to true,
                    "quality_score" to Random.nextDouble(0.6, 0.8)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateSummerContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..6).map { index ->
            val topics = when (index % 3) {
                0 -> listOf("summer", "seasonal", "hot")
                1 -> listOf("outdoor", "vacation", "travel")
                else -> listOf("beach", "summer-activities", "fun")
            }
            
            createContextualContent(
                id = "summer-content-$index",
                title = generateSeasonalTitle("summer", topics.first()),
                topics = topics,
                contentType = ContentType.TEXT,
                metadata = mapOf(
                    "seasonal_context" to "summer",
                    "season" to "summer",
                    "energizing" to true,
                    "quality_score" to Random.nextDouble(0.6, 0.8)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateHolidayContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..8).map { index ->
            val topics = when (index % 4) {
                0 -> listOf("thanksgiving", "family", "gratitude")
                1 -> listOf("cooking", "recipes", "traditional")
                2 -> listOf("celebration", "holiday", "togetherness")
                else -> listOf("tradition", "family-time", "reflection")
            }
            
            createContextualContent(
                id = "holiday-content-$index",
                title = generateHolidayTitle(topics.first()),
                topics = topics,
                contentType = ContentType.TEXT,
                metadata = mapOf(
                    "event_context" to "holiday",
                    "holiday_appropriate" to true,
                    "family_focused" to true,
                    "quality_score" to Random.nextDouble(0.7, 0.9)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateSportingEventContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..6).map { index ->
            val topics = when (index % 3) {
                0 -> listOf("world-cup", "soccer", "sports")
                1 -> listOf("celebration", "victory", "excitement")
                else -> listOf("commentary", "analysis", "live-updates")
            }
            
            createContextualContent(
                id = "sports-event-$index",
                title = generateSportsTitle(topics.first()),
                topics = topics,
                contentType = if (index % 2 == 0) ContentType.VIDEO else ContentType.TEXT,
                metadata = mapOf(
                    "event_context" to "sporting_event",
                    "real_time" to true,
                    "exciting" to true,
                    "energizing" to true,
                    "recency_boost" to 0.9,
                    "quality_score" to Random.nextDouble(0.7, 0.9)
                ),
                baseTime = baseTime
            )
        }
    }

    private fun generateBreakingNewsContent(baseTime: Instant): List<PersonalizableItem> {
        return (1..5).map { index ->
            val topics = when (index % 2) {
                0 -> listOf("breaking", "news", "urgent")
                else -> listOf("developing", "updates", "live")
            }
            
            createContextualContent(
                id = "breaking-news-$index",
                title = generateBreakingNewsTitle(topics.first()),
                topics = topics,
                contentType = ContentType.TEXT,
                metadata = mapOf(
                    "event_context" to "breaking_news",
                    "urgent" to true,
                    "real_time" to true,
                    "recency_boost" to 1.0,
                    "quality_score" to Random.nextDouble(0.8, 0.95)
                ),
                baseTime = baseTime.minus(Random.nextLong(0, 30), ChronoUnit.MINUTES) // Very recent
            )
        }
    }

    // Helper methods for creating contextual content
    private fun createContextualContent(
        id: String,
        title: String,
        topics: List<String>,
        contentType: ContentType,
        metadata: Map<String, Any>,
        baseTime: Instant
    ): PersonalizableItem {
        val storedContent = StoredContent(
            id = id,
            authorId = "contextual-author-${Random.nextInt(1, 20)}",
            contentType = contentType,
            textContent = "$title\n\nContextual content for ${topics.joinToString(", ")} targeting.",
            visibility = ContentVisibility.PUBLIC,
            status = ContentStatus.PUBLISHED,
            tags = topics,
            createdAt = baseTime.minus(Random.nextLong(0, 12), ChronoUnit.HOURS),
            updatedAt = baseTime.minus(Random.nextLong(0, 6), ChronoUnit.HOURS)
        )
        
        val enhancedMetadata = metadata.toMutableMap()
        
        // Add standard metadata
        enhancedMetadata["view_count"] = Random.nextInt(100, 5000)
        enhancedMetadata["like_count"] = Random.nextInt(10, (enhancedMetadata["view_count"] as Int) / 10)
        enhancedMetadata["share_count"] = Random.nextInt(1, (enhancedMetadata["like_count"] as Int) / 5)
        enhancedMetadata["comment_count"] = Random.nextInt(0, (enhancedMetadata["like_count"] as Int) / 3)
        enhancedMetadata["trending_score"] = Random.nextDouble(0.0, 1.0)
        
        // Set content length if not already set
        if (!enhancedMetadata.containsKey("content_length")) {
            enhancedMetadata["content_length"] = when (contentType) {
                ContentType.VIDEO -> Random.nextInt(300, 1800) // 5-30 minutes in seconds
                ContentType.IMAGE -> Random.nextInt(1, 5) // Number of images
                else -> Random.nextInt(500, 3000) // Word count
            }
        }
        
        val baseScore = Random.nextDouble(1.5, 3.5)
        
        return PersonalizableItem(
            content = storedContent,
            baseScore = baseScore,
            metadata = enhancedMetadata
        )
    }

    // Title generation helpers
    private fun generateMorningTitle(topic: String): String {
        val templates = mapOf(
            "news" to listOf("Morning Headlines: What You Need to Know", "Today's Top Stories", "Breaking: Morning News Update"),
            "motivation" to listOf("Start Your Day Right: Morning Motivation", "Rise and Shine: Daily Inspiration", "Morning Boost: Energize Your Day"),
            "health" to listOf("Healthy Morning Routines That Work", "Morning Fitness: Quick Start Guide", "Energizing Breakfast Ideas"),
            "education" to listOf("Learn Something New This Morning", "Morning Read: Educational Insights", "Today's Learning Opportunity")
        )
        return templates[topic]?.random() ?: "Morning $topic Content"
    }

    private fun generateLunchTitle(topic: String): String {
        val templates = mapOf(
            "quick-read" to listOf("Quick Lunch Read: 5-Minute Insights", "Bite-Sized Knowledge for Your Break", "Fast Facts for Busy People"),
            "food" to listOf("Best Lunch Spots Near You", "Quick Lunch Recipes", "Healthy Lunch Ideas"),
            "technology" to listOf("Tech Update: What's New Today", "Quick Tech Briefing", "Lunch Break Tech News")
        )
        return templates[topic]?.random() ?: "Lunch Break $topic"
    }

    private fun generateEveningTitle(topic: String): String {
        val templates = mapOf(
            "entertainment" to listOf("Tonight's Must-Watch Entertainment", "Evening Relaxation Guide", "Unwind with Great Content"),
            "cooking" to listOf("Tonight's Dinner Inspiration", "Easy Evening Recipes", "Cook Something Special Tonight"),
            "lifestyle" to listOf("Evening Wind-Down Routine", "Relaxing Evening Activities", "Make Tonight Special")
        )
        return templates[topic]?.random() ?: "Evening $topic"
    }

    private fun generateLateNightTitle(topic: String): String {
        val templates = mapOf(
            "creative" to listOf("Late Night Creativity: Art That Inspires", "Midnight Muse: Creative Inspiration", "Night Owl Creativity"),
            "international" to listOf("Around the World: Global Perspectives", "International Insights for Night Owls", "Global Stories from Different Time Zones"),
            "philosophy" to listOf("Deep Thoughts for Late Hours", "Philosophical Reflections", "Night Contemplations")
        )
        return templates[topic]?.random() ?: "Late Night $topic"
    }

    private fun generateWeekendTitle(topic: String): String {
        return "Weekend $topic: Relax and Enjoy"
    }

    private fun generateCommuteTitle(topic: String): String {
        return "Commute-Friendly $topic Content"
    }

    private fun generateWorkTitle(topic: String): String {
        return "Professional $topic Insights"
    }

    private fun generateLocationTitle(location: String, topic: String): String {
        return "$location $topic: Local Insights"
    }

    private fun generateLocalTitle(topic: String): String {
        return "Local $topic Updates"
    }

    private fun generateWeatherTitle(weather: String, topic: String): String {
        return "$weather Day $topic Activities"
    }

    private fun generateSeasonalTitle(season: String, topic: String): String {
        return "$season $topic Guide"
    }

    private fun generateHolidayTitle(topic: String): String {
        return "Holiday $topic: Celebrate Together"
    }

    private fun generateSportsTitle(topic: String): String {
        return "Live: $topic Championship Updates"
    }

    private fun generateBreakingNewsTitle(topic: String): String {
        return "BREAKING: $topic Development"
    }

    /**
     * Get all contextual content combined
     */
    fun getAllContextualContent(): List<PersonalizableItem> {
        return listOf(
            generateTimeContextualContent().values,
            generateLocationContextualContent().values,
            generateWeatherContextualContent().values,
            generateEventContextualContent().values
        ).flatten().flatten()
    }

    /**
     * Get contextual content statistics
     */
    fun getContextualContentStats(): Map<String, Any> {
        val allContent = getAllContextualContent()
        
        return mapOf(
            "total_items" to allContent.size,
            "time_contexts" to generateTimeContextualContent().keys,
            "location_contexts" to generateLocationContextualContent().keys,
            "weather_contexts" to generateWeatherContextualContent().keys,
            "event_contexts" to generateEventContextualContent().keys,
            "content_types" to allContent.groupingBy { it.content.contentType }.eachCount(),
            "avg_quality_score" to allContent.mapNotNull { it.metadata["quality_score"] as? Double }.average()
        )
    }
}