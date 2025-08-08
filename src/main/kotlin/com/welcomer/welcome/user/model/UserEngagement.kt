package com.welcomer.welcome.user.model

import com.welcomer.welcome.engagement.model.EngagementType
import java.time.Instant

/**
 * Represents a user engagement with content for analysis purposes
 */
data class UserEngagement(
    val id: String,
    val userId: String,
    val contentId: String,
    val engagementType: EngagementType,
    val score: Double, // Weighted engagement score
    val timestamp: Instant,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Service interface for user profile and engagement operations
 */
interface UserProfileService {
    /**
     * Get user's recent engagements
     */
    suspend fun getUserEngagements(userId: String, limit: Int = 100): List<UserEngagement>

    /**
     * Get user's engagement pattern analysis
     */
    suspend fun getUserEngagementPattern(userId: String): UserEngagementPattern?

    /**
     * Update user profile based on new engagement
     */
    suspend fun updateUserProfile(userId: String, engagement: UserEngagement)
}

/**
 * Analysis of a user's engagement patterns
 */
data class UserEngagementPattern(
    val userId: String,
    val topicPreferences: Map<String, Double>,
    val sourcePreferences: Map<String, Double>,
    val engagementTypeDistribution: Map<EngagementType, Double>,
    val averageEngagementScore: Double,
    val diversityScore: Double, // 0-1, higher means more diverse
    val analysisTimestamp: Instant
)