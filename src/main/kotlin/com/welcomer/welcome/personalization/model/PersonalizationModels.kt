package com.welcomer.welcome.personalization.model

/**
 * User preference profile for personalization
 */
data class UserPreferenceProfile(
    val userId: String,
    val topicInterests: Map<String, Double> = emptyMap(),
    val sourcePreferences: Map<String, Double> = emptyMap(),
    val contentTypePreferences: Map<String, Double> = emptyMap(),
    val timeBasedPreferences: Map<Int, Double> = emptyMap(), // Hour of day -> preference weight
    val languagePreferences: List<String> = emptyList(),
    val diversityPreference: Double = 0.5, // How much diversity vs familiarity user prefers
    val freshnessPreference: Double = 0.5, // How much fresh vs established content user prefers
    val engagementHistory: Map<String, Double> = emptyMap(), // Content type -> avg engagement score
    val lastUpdated: java.time.Instant = java.time.Instant.now()
)