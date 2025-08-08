package com.welcomer.welcome.user.service

import com.welcomer.welcome.engagement.model.EngagementType
import com.welcomer.welcome.personalization.service.UserActivity
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service for retrieving user activity history for personalization
 */
interface UserHistoryService {
    /**
     * Get user's activity history within a specified number of days
     */
    suspend fun getUserHistory(userId: String, lookbackDays: Int): List<UserActivity>
    
    /**
     * Get user's engagement patterns with specific content types
     */
    suspend fun getUserEngagementPatterns(
        userId: String, 
        contentTypes: List<String>? = null,
        lookbackDays: Int = 30
    ): Map<String, Double>
    
    /**
     * Get user's source interaction history
     */
    suspend fun getUserSourceHistory(
        userId: String,
        lookbackDays: Int = 30
    ): Map<String, List<UserActivity>>
}

/**
 * Default implementation that provides mock user history data
 */
@Service
class DefaultUserHistoryService : UserHistoryService {
    
    override suspend fun getUserHistory(userId: String, lookbackDays: Int): List<UserActivity> {
        // In a real implementation, this would query the user's actual engagement history
        // For now, return sample data to enable testing
        val now = Instant.now()
        
        return listOf(
            UserActivity(
                contentId = "sample1",
                authorId = "author1",
                topics = listOf("technology", "ai"),
                engagementType = EngagementType.LIKE,
                engagementScore = 0.8,
                timestamp = now.minus(1, ChronoUnit.HOURS)
            ),
            UserActivity(
                contentId = "sample2", 
                authorId = "author2",
                topics = listOf("science", "research"),
                engagementType = EngagementType.SHARE,
                engagementScore = 0.9,
                timestamp = now.minus(3, ChronoUnit.HOURS)
            ),
            UserActivity(
                contentId = "sample3",
                authorId = "author1", 
                topics = listOf("technology", "programming"),
                engagementType = EngagementType.BOOKMARK,
                engagementScore = 0.7,
                timestamp = now.minus(6, ChronoUnit.HOURS)
            ),
            UserActivity(
                contentId = "sample4",
                authorId = "author3",
                topics = listOf("news", "politics"),
                engagementType = EngagementType.VIEW,
                engagementScore = 0.3,
                timestamp = now.minus(12, ChronoUnit.HOURS)
            ),
            UserActivity(
                contentId = "sample5",
                authorId = "author2",
                topics = listOf("entertainment", "movies"),
                engagementType = EngagementType.COMMENT,
                engagementScore = 0.6,
                timestamp = now.minus(1, ChronoUnit.DAYS)
            )
        ).filter { 
            it.timestamp.isAfter(now.minus(lookbackDays.toLong(), ChronoUnit.DAYS))
        }
    }
    
    override suspend fun getUserEngagementPatterns(
        userId: String,
        contentTypes: List<String>?,
        lookbackDays: Int
    ): Map<String, Double> {
        val history = getUserHistory(userId, lookbackDays)
        
        return history
            .filter { activity -> 
                contentTypes?.isEmpty() != false || 
                activity.topics.any { topic -> contentTypes.contains(topic) }
            }
            .groupBy { it.engagementType }
            .mapValues { (_, activities) -> 
                activities.map { it.engagementScore }.average()
            }
            .mapKeys { it.key.name }
    }
    
    override suspend fun getUserSourceHistory(
        userId: String,
        lookbackDays: Int
    ): Map<String, List<UserActivity>> {
        val history = getUserHistory(userId, lookbackDays)
        return history.groupBy { it.authorId }
    }
}