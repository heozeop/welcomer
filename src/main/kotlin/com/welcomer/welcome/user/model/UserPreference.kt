package com.welcomer.welcome.user.model

import java.time.Instant

/**
 * Represents different types of user preferences
 */
enum class PreferenceType {
    TOPIC_INTEREST,
    CONTENT_TYPE,
    LANGUAGE,
    FOLLOWED_ACCOUNT,
    BLOCKED_USER,
    BLOCKED_TOPIC,
    NOTIFICATION_SETTING,
    FEED_ALGORITHM
}

/**
 * Represents the source of a preference
 */
enum class PreferenceSource {
    EXPLICIT, // User explicitly set this preference
    IMPLICIT, // Inferred from user behavior
    SYSTEM    // Set by system defaults or policies
}

/**
 * Data class representing a user preference entry
 */
data class UserPreference(
    val id: String,
    val userId: String,
    val type: PreferenceType,
    val key: String, // The preference identifier (e.g., "topic:technology", "content_type:video")
    val value: String, // The preference value (e.g., "liked", "blocked", weight score)
    val weight: Double = 1.0, // Strength of the preference (0.0 to 1.0)
    val source: PreferenceSource,
    val confidence: Double = 1.0, // Confidence in this preference (0.0 to 1.0)
    val metadata: Map<String, Any> = emptyMap(), // Additional metadata
    val createdAt: Instant,
    val updatedAt: Instant,
    val expiresAt: Instant? = null // Some preferences may expire
)

/**
 * Aggregated user preferences for easy consumption by feed systems
 */
data class UserPreferenceProfile(
    val userId: String,
    val topicInterests: Map<String, Double>, // topic -> interest score
    val contentTypePreferences: Map<String, Double>, // content_type -> preference score
    val languagePreferences: List<String>, // preferred languages in order
    val followedAccounts: Set<String>, // user IDs of followed accounts
    val blockedUsers: Set<String>, // user IDs of blocked users
    val blockedTopics: Set<String>, // blocked topic names
    val algorithmPreferences: Map<String, String>, // algorithm settings
    val lastUpdated: Instant,
    val confidence: Double // Overall confidence in this profile
)

/**
 * Request model for updating user preferences
 */
data class PreferenceUpdateRequest(
    val preferences: List<PreferenceUpdate>
)

data class PreferenceUpdate(
    val type: PreferenceType,
    val key: String,
    val value: String,
    val weight: Double = 1.0,
    val operation: PreferenceOperation = PreferenceOperation.SET
)

enum class PreferenceOperation {
    SET,    // Set the preference value
    DELETE, // Remove the preference
    INCREMENT, // Increase weight/value
    DECREMENT  // Decrease weight/value
}

/**
 * Statistics about user preferences
 */
data class PreferenceStats(
    val userId: String,
    val totalPreferences: Int,
    val explicitPreferences: Int,
    val implicitPreferences: Int,
    val preferencesByType: Map<PreferenceType, Int>,
    val averageConfidence: Double,
    val lastActivityAt: Instant?
)