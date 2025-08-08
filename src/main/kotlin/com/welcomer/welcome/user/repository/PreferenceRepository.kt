package com.welcomer.welcome.user.repository

import com.welcomer.welcome.user.model.*
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Repository interface for managing user preferences in the database
 */
interface PreferenceRepository {
    /**
     * Get all preferences for a user
     */
    suspend fun findByUserId(userId: String): List<UserPreference>

    /**
     * Get preferences for a user by type
     */
    suspend fun findByUserIdAndType(userId: String, type: PreferenceType): List<UserPreference>

    /**
     * Get a specific preference by user, type, and key
     */
    suspend fun findByUserIdAndTypeAndKey(
        userId: String, 
        type: PreferenceType, 
        key: String
    ): UserPreference?

    /**
     * Save a preference (create or update)
     */
    suspend fun save(preference: UserPreference): UserPreference

    /**
     * Save multiple preferences in a batch
     */
    suspend fun saveAll(preferences: List<UserPreference>): List<UserPreference>

    /**
     * Delete a preference by ID
     */
    suspend fun deleteById(id: String)

    /**
     * Delete preferences by user, type, and key
     */
    suspend fun deleteByUserIdAndTypeAndKey(userId: String, type: PreferenceType, key: String)

    /**
     * Delete all preferences for a user
     */
    suspend fun deleteByUserId(userId: String)

    /**
     * Get aggregated preference profile for a user
     */
    suspend fun getPreferenceProfile(userId: String): UserPreferenceProfile?

    /**
     * Get preferences by source (explicit, implicit, system)
     */
    suspend fun findByUserIdAndSource(userId: String, source: PreferenceSource): List<UserPreference>

    /**
     * Get expired preferences
     */
    suspend fun findExpiredPreferences(asOf: Instant = Instant.now()): List<UserPreference>

    /**
     * Clean up expired preferences
     */
    suspend fun deleteExpiredPreferences(asOf: Instant = Instant.now()): Int

    /**
     * Get preference statistics for a user
     */
    suspend fun getPreferenceStats(userId: String): PreferenceStats?

    /**
     * Check if user has any preferences
     */
    suspend fun hasPreferences(userId: String): Boolean
}

/**
 * Default implementation of PreferenceRepository using JOOQ
 */
@Repository
class DefaultPreferenceRepository : PreferenceRepository {

    companion object {
        // In a real implementation, these would map to actual database tables
        // For now, we'll use in-memory storage for demonstration
        private val preferences = mutableMapOf<String, UserPreference>()
        private val userPreferenceIndex = mutableMapOf<String, MutableSet<String>>()
    }

    override suspend fun findByUserId(userId: String): List<UserPreference> {
        val userPreferenceIds = userPreferenceIndex[userId] ?: emptySet()
        return userPreferenceIds.mapNotNull { preferences[it] }
            .sortedBy { it.createdAt }
    }

    override suspend fun findByUserIdAndType(userId: String, type: PreferenceType): List<UserPreference> {
        return findByUserId(userId).filter { it.type == type }
    }

    override suspend fun findByUserIdAndTypeAndKey(
        userId: String,
        type: PreferenceType,
        key: String
    ): UserPreference? {
        return findByUserId(userId).find { it.type == type && it.key == key }
    }

    override suspend fun save(preference: UserPreference): UserPreference {
        val now = Instant.now()
        val savedPreference = if (preferences.containsKey(preference.id)) {
            // Update existing
            preference.copy(updatedAt = now)
        } else {
            // Create new
            preference.copy(createdAt = now, updatedAt = now)
        }

        preferences[savedPreference.id] = savedPreference
        
        // Update index
        userPreferenceIndex.computeIfAbsent(savedPreference.userId) { mutableSetOf() }
            .add(savedPreference.id)

        return savedPreference
    }

    override suspend fun saveAll(preferences: List<UserPreference>): List<UserPreference> {
        return preferences.map { save(it) }
    }

    override suspend fun deleteById(id: String) {
        val preference = preferences.remove(id)
        preference?.let { pref ->
            userPreferenceIndex[pref.userId]?.remove(id)
        }
    }

    override suspend fun deleteByUserIdAndTypeAndKey(userId: String, type: PreferenceType, key: String) {
        val preference = findByUserIdAndTypeAndKey(userId, type, key)
        preference?.let { deleteById(it.id) }
    }

    override suspend fun deleteByUserId(userId: String) {
        val userPreferenceIds = userPreferenceIndex.remove(userId) ?: emptySet()
        userPreferenceIds.forEach { preferences.remove(it) }
    }

    override suspend fun getPreferenceProfile(userId: String): UserPreferenceProfile? {
        val userPreferences = findByUserId(userId)
        if (userPreferences.isEmpty()) return null

        val topicInterests = mutableMapOf<String, Double>()
        val contentTypePreferences = mutableMapOf<String, Double>()
        val languagePreferences = mutableListOf<String>()
        val followedAccounts = mutableSetOf<String>()
        val blockedUsers = mutableSetOf<String>()
        val blockedTopics = mutableSetOf<String>()
        val algorithmPreferences = mutableMapOf<String, String>()

        userPreferences.forEach { pref ->
            when (pref.type) {
                PreferenceType.TOPIC_INTEREST -> {
                    val topic = pref.key.removePrefix("topic:")
                    topicInterests[topic] = pref.weight * pref.confidence
                }
                PreferenceType.CONTENT_TYPE -> {
                    val contentType = pref.key.removePrefix("content_type:")
                    contentTypePreferences[contentType] = pref.weight * pref.confidence
                }
                PreferenceType.LANGUAGE -> {
                    languagePreferences.add(pref.value)
                }
                PreferenceType.FOLLOWED_ACCOUNT -> {
                    followedAccounts.add(pref.value)
                }
                PreferenceType.BLOCKED_USER -> {
                    blockedUsers.add(pref.value)
                }
                PreferenceType.BLOCKED_TOPIC -> {
                    blockedTopics.add(pref.value)
                }
                PreferenceType.FEED_ALGORITHM -> {
                    algorithmPreferences[pref.key] = pref.value
                }
                else -> {
                    // Handle other preference types as needed
                }
            }
        }

        val avgConfidence = userPreferences.map { it.confidence }.average()
        val lastUpdated = userPreferences.maxOf { it.updatedAt }

        return UserPreferenceProfile(
            userId = userId,
            topicInterests = topicInterests,
            contentTypePreferences = contentTypePreferences,
            languagePreferences = languagePreferences,
            followedAccounts = followedAccounts,
            blockedUsers = blockedUsers,
            blockedTopics = blockedTopics,
            algorithmPreferences = algorithmPreferences,
            lastUpdated = lastUpdated,
            confidence = avgConfidence
        )
    }

    override suspend fun findByUserIdAndSource(userId: String, source: PreferenceSource): List<UserPreference> {
        return findByUserId(userId).filter { it.source == source }
    }

    override suspend fun findExpiredPreferences(asOf: Instant): List<UserPreference> {
        return preferences.values.filter { pref ->
            pref.expiresAt != null && pref.expiresAt.isBefore(asOf)
        }
    }

    override suspend fun deleteExpiredPreferences(asOf: Instant): Int {
        val expiredPreferences = findExpiredPreferences(asOf)
        expiredPreferences.forEach { deleteById(it.id) }
        return expiredPreferences.size
    }

    override suspend fun getPreferenceStats(userId: String): PreferenceStats? {
        val userPreferences = findByUserId(userId)
        if (userPreferences.isEmpty()) return null

        val explicitCount = userPreferences.count { it.source == PreferenceSource.EXPLICIT }
        val implicitCount = userPreferences.count { it.source == PreferenceSource.IMPLICIT }
        val preferencesByType = userPreferences.groupBy { it.type }.mapValues { it.value.size }
        val avgConfidence = userPreferences.map { it.confidence }.average()
        val lastActivity = userPreferences.maxOfOrNull { it.updatedAt }

        return PreferenceStats(
            userId = userId,
            totalPreferences = userPreferences.size,
            explicitPreferences = explicitCount,
            implicitPreferences = implicitCount,
            preferencesByType = preferencesByType,
            averageConfidence = avgConfidence,
            lastActivityAt = lastActivity
        )
    }

    override suspend fun hasPreferences(userId: String): Boolean {
        return userPreferenceIndex.containsKey(userId) && 
               userPreferenceIndex[userId]!!.isNotEmpty()
    }
}