package com.welcomer.welcome.user.service

import com.welcomer.welcome.user.model.*
import com.welcomer.welcome.user.repository.PreferenceRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

/**
 * Main service interface for managing user preferences
 */
interface UserPreferenceService {
    /**
     * Get comprehensive user preferences (explicit + implicit)
     */
    suspend fun getPreferences(userId: String): UserPreferenceProfile?

    /**
     * Get only explicit preferences set by user
     */
    suspend fun getExplicitPreferences(userId: String): UserPreferenceProfile?

    /**
     * Get only implicit preferences inferred from behavior
     */
    suspend fun getImplicitPreferences(userId: String): UserPreferenceProfile?

    /**
     * Update user preferences
     */
    suspend fun updatePreferences(userId: String, request: PreferenceUpdateRequest): Boolean

    /**
     * Update a single preference
     */
    suspend fun updatePreference(userId: String, update: PreferenceUpdate): Boolean

    /**
     * Delete all preferences for a user
     */
    suspend fun deleteAllPreferences(userId: String): Boolean

    /**
     * Get preference statistics for a user
     */
    suspend fun getPreferenceStats(userId: String): PreferenceStats?

    /**
     * Refresh implicit preferences based on latest behavior
     */
    suspend fun refreshImplicitPreferences(userId: String): Boolean

    /**
     * Merge explicit and implicit preferences with configured weights
     */
    suspend fun mergePreferences(
        explicit: List<UserPreference>,
        implicit: List<UserPreference>,
        config: PreferenceMergingConfig = PreferenceMergingConfig()
    ): UserPreferenceProfile?
}

/**
 * Configuration for merging explicit and implicit preferences
 */
data class PreferenceMergingConfig(
    val explicitWeight: Double = 0.7, // Weight given to explicit preferences
    val implicitWeight: Double = 0.3, // Weight given to implicit preferences
    val confidenceThreshold: Double = 0.3, // Minimum confidence to include a preference
    val maxPreferencesPerType: Int = 50, // Maximum preferences per type to include
    val decayDays: Int = 90, // Days after which implicit preferences start decaying
    val conflictResolution: ConflictResolutionStrategy = ConflictResolutionStrategy.EXPLICIT_WINS
)

/**
 * Strategy for resolving conflicts between explicit and implicit preferences
 */
enum class ConflictResolutionStrategy {
    EXPLICIT_WINS,    // Explicit preferences always take precedence
    IMPLICIT_WINS,    // Implicit preferences always take precedence
    WEIGHTED_AVERAGE, // Average based on weights
    HIGHEST_CONFIDENCE // Choose preference with highest confidence
}

/**
 * Event published when user preferences are updated
 */
data class PreferencesUpdatedEvent(
    val userId: String,
    val updatedPreferences: List<UserPreference>,
    val timestamp: Instant
)

/**
 * Default implementation of UserPreferenceService
 */
@Service
class DefaultUserPreferenceService(
    private val preferenceRepository: PreferenceRepository,
    private val behaviorAnalysisService: BehaviorAnalysisService,
    private val eventPublisher: ApplicationEventPublisher
) : UserPreferenceService {

    override suspend fun getPreferences(userId: String): UserPreferenceProfile? {
        // Get both explicit and implicit preferences
        val explicitPreferences = preferenceRepository.findByUserIdAndSource(userId, PreferenceSource.EXPLICIT)
        val implicitAnalysis = behaviorAnalysisService.getImplicitPreferences(userId)
        val implicitPreferences = implicitAnalysis.inferredPreferences

        if (explicitPreferences.isEmpty() && implicitPreferences.isEmpty()) {
            return null
        }

        return mergePreferences(explicitPreferences, implicitPreferences)
    }

    override suspend fun getExplicitPreferences(userId: String): UserPreferenceProfile? {
        return preferenceRepository.getPreferenceProfile(userId)
    }

    override suspend fun getImplicitPreferences(userId: String): UserPreferenceProfile? {
        val analysis = behaviorAnalysisService.getImplicitPreferences(userId)
        if (analysis.inferredPreferences.isEmpty()) return null

        return convertToProfile(userId, analysis.inferredPreferences, analysis.confidence)
    }

    override suspend fun updatePreferences(userId: String, request: PreferenceUpdateRequest): Boolean {
        val updatedPreferences = mutableListOf<UserPreference>()

        try {
            // Process each preference update
            for (update in request.preferences) {
                val result = processPreferenceUpdate(userId, update)
                result?.let { updatedPreferences.add(it) }
            }

            // Save all updates as a batch
            if (updatedPreferences.isNotEmpty()) {
                preferenceRepository.saveAll(updatedPreferences)

                // Publish event for feed system notification
                eventPublisher.publishEvent(
                    PreferencesUpdatedEvent(
                        userId = userId,
                        updatedPreferences = updatedPreferences,
                        timestamp = Instant.now()
                    )
                )

                return true
            }

            return false

        } catch (e: Exception) {
            // Log error and return false
            // In production, would use proper logging
            println("Error updating preferences for user $userId: ${e.message}")
            return false
        }
    }

    override suspend fun updatePreference(userId: String, update: PreferenceUpdate): Boolean {
        val request = PreferenceUpdateRequest(listOf(update))
        return updatePreferences(userId, request)
    }

    override suspend fun deleteAllPreferences(userId: String): Boolean {
        return try {
            preferenceRepository.deleteByUserId(userId)
            
            // Publish event
            eventPublisher.publishEvent(
                PreferencesUpdatedEvent(
                    userId = userId,
                    updatedPreferences = emptyList(),
                    timestamp = Instant.now()
                )
            )
            
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getPreferenceStats(userId: String): PreferenceStats? {
        return preferenceRepository.getPreferenceStats(userId)
    }

    override suspend fun refreshImplicitPreferences(userId: String): Boolean {
        return try {
            // Get fresh implicit preferences from behavior analysis
            val analysis = behaviorAnalysisService.getImplicitPreferences(userId)
            
            if (analysis.inferredPreferences.isNotEmpty()) {
                // Delete existing implicit preferences
                val existingImplicit = preferenceRepository.findByUserIdAndSource(userId, PreferenceSource.IMPLICIT)
                existingImplicit.forEach { preferenceRepository.deleteById(it.id) }
                
                // Save new implicit preferences
                preferenceRepository.saveAll(analysis.inferredPreferences)
                
                // Publish update event
                eventPublisher.publishEvent(
                    PreferencesUpdatedEvent(
                        userId = userId,
                        updatedPreferences = analysis.inferredPreferences,
                        timestamp = Instant.now()
                    )
                )
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun mergePreferences(
        explicit: List<UserPreference>,
        implicit: List<UserPreference>,
        config: PreferenceMergingConfig
    ): UserPreferenceProfile? {
        if (explicit.isEmpty() && implicit.isEmpty()) return null

        val userId = explicit.firstOrNull()?.userId ?: implicit.firstOrNull()?.userId ?: return null

        // Group preferences by type and key
        val explicitByKey = explicit.groupBy { "${it.type}:${it.key}" }.mapValues { it.value.first() }
        val implicitByKey = implicit.groupBy { "${it.type}:${it.key}" }.mapValues { it.value.first() }

        // Get all unique preference keys
        val allKeys = (explicitByKey.keys + implicitByKey.keys).toSet()

        val mergedPreferences = mutableListOf<UserPreference>()

        // Merge preferences for each key
        allKeys.forEach { key ->
            val explicitPref = explicitByKey[key]
            val implicitPref = implicitByKey[key]

            val mergedPref = when {
                explicitPref != null && implicitPref != null -> {
                    // Both exist - apply conflict resolution
                    resolveConflict(explicitPref, implicitPref, config)
                }
                explicitPref != null -> explicitPref
                implicitPref != null -> implicitPref
                else -> null
            }

            mergedPref?.let { pref ->
                if (pref.confidence >= config.confidenceThreshold) {
                    mergedPreferences.add(pref)
                }
            }
        }

        // Convert merged preferences to profile format
        return convertMergedPreferencesToProfile(userId, mergedPreferences, config)
    }

    // Private helper methods

    private suspend fun processPreferenceUpdate(userId: String, update: PreferenceUpdate): UserPreference? {
        val now = Instant.now()

        return when (update.operation) {
            PreferenceOperation.SET -> {
                // Create or update preference
                val existingPref = preferenceRepository.findByUserIdAndTypeAndKey(userId, update.type, update.key)
                
                if (existingPref != null) {
                    existingPref.copy(
                        value = update.value,
                        weight = update.weight,
                        updatedAt = now
                    )
                } else {
                    UserPreference(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        type = update.type,
                        key = update.key,
                        value = update.value,
                        weight = update.weight,
                        source = PreferenceSource.EXPLICIT,
                        confidence = 1.0,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            }

            PreferenceOperation.DELETE -> {
                preferenceRepository.deleteByUserIdAndTypeAndKey(userId, update.type, update.key)
                null
            }

            PreferenceOperation.INCREMENT -> {
                val existingPref = preferenceRepository.findByUserIdAndTypeAndKey(userId, update.type, update.key)
                existingPref?.copy(
                    weight = (existingPref.weight + update.weight).coerceAtMost(1.0),
                    updatedAt = now
                )
            }

            PreferenceOperation.DECREMENT -> {
                val existingPref = preferenceRepository.findByUserIdAndTypeAndKey(userId, update.type, update.key)
                existingPref?.copy(
                    weight = (existingPref.weight - update.weight).coerceAtLeast(0.0),
                    updatedAt = now
                )
            }
        }
    }

    private fun resolveConflict(
        explicit: UserPreference,
        implicit: UserPreference,
        config: PreferenceMergingConfig
    ): UserPreference {
        return when (config.conflictResolution) {
            ConflictResolutionStrategy.EXPLICIT_WINS -> explicit

            ConflictResolutionStrategy.IMPLICIT_WINS -> implicit

            ConflictResolutionStrategy.WEIGHTED_AVERAGE -> {
                val mergedWeight = (explicit.weight * config.explicitWeight) + 
                                 (implicit.weight * config.implicitWeight)
                val mergedConfidence = (explicit.confidence * config.explicitWeight) + 
                                      (implicit.confidence * config.implicitWeight)

                explicit.copy(
                    weight = mergedWeight,
                    confidence = mergedConfidence,
                    source = PreferenceSource.EXPLICIT, // Keep as explicit since user set it
                    metadata = explicit.metadata + mapOf("merged_from_implicit" to true),
                    updatedAt = Instant.now()
                )
            }

            ConflictResolutionStrategy.HIGHEST_CONFIDENCE -> {
                if (explicit.confidence >= implicit.confidence) explicit else implicit
            }
        }
    }

    private fun convertToProfile(userId: String, preferences: List<UserPreference>, confidence: Double): UserPreferenceProfile {
        val topicInterests = mutableMapOf<String, Double>()
        val contentTypePreferences = mutableMapOf<String, Double>()
        val languagePreferences = mutableListOf<String>()
        val followedAccounts = mutableSetOf<String>()
        val blockedUsers = mutableSetOf<String>()
        val blockedTopics = mutableSetOf<String>()
        val algorithmPreferences = mutableMapOf<String, String>()

        preferences.forEach { pref ->
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
                    // Handle other types as needed
                }
            }
        }

        return UserPreferenceProfile(
            userId = userId,
            topicInterests = topicInterests,
            contentTypePreferences = contentTypePreferences,
            languagePreferences = languagePreferences,
            followedAccounts = followedAccounts,
            blockedUsers = blockedUsers,
            blockedTopics = blockedTopics,
            algorithmPreferences = algorithmPreferences,
            lastUpdated = preferences.maxOfOrNull { it.updatedAt } ?: Instant.now(),
            confidence = confidence
        )
    }

    private fun convertMergedPreferencesToProfile(
        userId: String,
        preferences: List<UserPreference>,
        config: PreferenceMergingConfig
    ): UserPreferenceProfile {
        // Group preferences by type and apply limits
        val limitedPreferences = mutableListOf<UserPreference>()

        PreferenceType.values().forEach { type ->
            val prefsOfType = preferences.filter { it.type == type }
                .sortedByDescending { it.weight * it.confidence }
                .take(config.maxPreferencesPerType)
            limitedPreferences.addAll(prefsOfType)
        }

        // Calculate overall confidence as average of individual confidences
        val overallConfidence = if (limitedPreferences.isNotEmpty()) {
            limitedPreferences.map { it.confidence }.average()
        } else 0.0

        return convertToProfile(userId, limitedPreferences, overallConfidence)
    }
}