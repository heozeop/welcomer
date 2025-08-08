package com.welcomer.welcome.user.service

import com.welcomer.welcome.user.model.*
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/**
 * Service interface for analyzing user behavior and inferring implicit preferences
 */
interface BehaviorAnalysisService {
    /**
     * Analyze user behavior and infer implicit preferences
     */
    suspend fun getImplicitPreferences(userId: String): ImplicitPreferenceResult

    /**
     * Process a new behavior event and update implicit preferences
     */
    suspend fun processBehaviorEvent(event: BehaviorEvent)

    /**
     * Get user behavior profile with aggregated metrics
     */
    suspend fun getUserBehaviorProfile(userId: String): UserBehaviorProfile?

    /**
     * Calculate topic interest score based on user interactions
     */
    suspend fun calculateTopicInterest(userId: String, topic: String): Double

    /**
     * Calculate content type preference score
     */
    suspend fun calculateContentTypePreference(userId: String, contentType: String): Double

    /**
     * Get trending topics for a user based on their recent behavior
     */
    suspend fun getTrendingTopicsForUser(userId: String, limit: Int = 10): List<Pair<String, Double>>

    /**
     * Detect behavior patterns and anomalies
     */
    suspend fun detectBehaviorPatterns(userId: String): Map<String, Any>
}

/**
 * Default implementation of behavior analysis service
 */
@Service
class DefaultBehaviorAnalysisService(
    private val config: BehaviorAnalysisConfig = BehaviorAnalysisConfig()
) : BehaviorAnalysisService {

    companion object {
        // In-memory storage for demonstration (in production, would use database)
        private val behaviorEvents = mutableMapOf<String, MutableList<BehaviorEvent>>()
        private val behaviorProfiles = mutableMapOf<String, UserBehaviorProfile>()
    }

    override suspend fun getImplicitPreferences(userId: String): ImplicitPreferenceResult {
        val userEvents = getUserEvents(userId)
        
        if (userEvents.size < config.minimumEventCountForInference) {
            return ImplicitPreferenceResult(
                userId = userId,
                inferredPreferences = emptyList(),
                confidence = 0.0,
                analysisMetadata = mapOf("reason" to "insufficient_data", "event_count" to userEvents.size),
                generatedAt = Instant.now()
            )
        }

        val preferences = mutableListOf<UserPreference>()
        val now = Instant.now()
        
        // Analyze topic interests
        val topicInterests = analyzeTopicInterests(userEvents)
        topicInterests.forEach { (topic, score) ->
            if (score >= config.topicConfidenceThreshold) {
                preferences.add(createImplicitPreference(
                    userId = userId,
                    type = PreferenceType.TOPIC_INTEREST,
                    key = "topic:$topic",
                    value = "interested",
                    weight = score,
                    confidence = calculateConfidence(userEvents, topic)
                ))
            }
        }

        // Analyze content type preferences
        val contentTypePreferences = analyzeContentTypePreferences(userEvents)
        contentTypePreferences.forEach { (contentType, score) ->
            if (score >= config.contentTypeConfidenceThreshold) {
                preferences.add(createImplicitPreference(
                    userId = userId,
                    type = PreferenceType.CONTENT_TYPE,
                    key = "content_type:$contentType",
                    value = "preferred",
                    weight = score,
                    confidence = calculateContentTypeConfidence(userEvents, contentType)
                ))
            }
        }

        // Analyze language preferences based on content interaction
        val languagePreferences = analyzeLanguagePreferences(userEvents)
        languagePreferences.take(3).forEach { (language, score) ->
            preferences.add(createImplicitPreference(
                userId = userId,
                type = PreferenceType.LANGUAGE,
                key = "language:$language",
                value = language,
                weight = score,
                confidence = score
            ))
        }

        val overallConfidence = if (preferences.isNotEmpty()) {
            preferences.map { it.confidence }.average()
        } else 0.0

        return ImplicitPreferenceResult(
            userId = userId,
            inferredPreferences = preferences,
            confidence = overallConfidence,
            analysisMetadata = mapOf(
                "total_events" to userEvents.size,
                "analysis_period_days" to config.recentActivityWindowDays,
                "topic_preferences_count" to topicInterests.size,
                "content_type_preferences_count" to contentTypePreferences.size
            ),
            generatedAt = now
        )
    }

    override suspend fun processBehaviorEvent(event: BehaviorEvent) {
        behaviorEvents.computeIfAbsent(event.userId) { mutableListOf() }.add(event)
        
        // Update behavior profile
        updateBehaviorProfile(event.userId)
        
        // Trigger preference recalculation if significant event
        if (isSignificantEvent(event)) {
            // In a real implementation, this would trigger an async job
            // to recalculate implicit preferences
        }
    }

    override suspend fun getUserBehaviorProfile(userId: String): UserBehaviorProfile? {
        return behaviorProfiles[userId] ?: run {
            val events = getUserEvents(userId)
            if (events.isEmpty()) return null
            
            val profile = buildBehaviorProfile(userId, events)
            behaviorProfiles[userId] = profile
            profile
        }
    }

    override suspend fun calculateTopicInterest(userId: String, topic: String): Double {
        val events = getUserEvents(userId)
        return calculateTopicScore(events, topic)
    }

    override suspend fun calculateContentTypePreference(userId: String, contentType: String): Double {
        val events = getUserEvents(userId)
        return calculateContentTypeScore(events, contentType)
    }

    override suspend fun getTrendingTopicsForUser(userId: String, limit: Int): List<Pair<String, Double>> {
        val events = getRecentUserEvents(userId, config.recentActivityWindowDays)
        val topicInterests = analyzeTopicInterests(events)
        
        return topicInterests.toList()
            .sortedByDescending { it.second }
            .take(limit)
    }

    override suspend fun detectBehaviorPatterns(userId: String): Map<String, Any> {
        val events = getUserEvents(userId)
        val patterns = mutableMapOf<String, Any>()
        
        // Detect time-based patterns
        val hourlyActivity = events.groupBy { it.timestamp.atZone(java.time.ZoneId.systemDefault()).hour }
        val mostActiveHours = hourlyActivity.entries
            .sortedByDescending { it.value.size }
            .take(3)
            .map { it.key }
        
        patterns["most_active_hours"] = mostActiveHours
        
        // Detect engagement patterns
        val engagementEvents = events.filter { 
            it.eventType in setOf(
                BehaviorEventType.CONTENT_LIKE,
                BehaviorEventType.CONTENT_SHARE,
                BehaviorEventType.CONTENT_COMMENT
            )
        }
        val viewEvents = events.filter { it.eventType == BehaviorEventType.CONTENT_VIEW }
        
        val engagementRate = if (viewEvents.isNotEmpty()) {
            engagementEvents.size.toDouble() / viewEvents.size
        } else 0.0
        
        patterns["engagement_rate"] = engagementRate
        patterns["total_events"] = events.size
        patterns["recent_activity"] = getRecentUserEvents(userId, 7).size
        
        // Detect content consumption patterns
        val dwellTimes = events.mapNotNull { it.duration?.toMillis() }
        if (dwellTimes.isNotEmpty()) {
            patterns["average_dwell_time_ms"] = dwellTimes.average()
            patterns["median_dwell_time_ms"] = dwellTimes.sorted()[dwellTimes.size / 2]
        }
        
        return patterns
    }

    // Private helper methods

    private fun getUserEvents(userId: String): List<BehaviorEvent> {
        return behaviorEvents[userId] ?: emptyList()
    }

    private fun getRecentUserEvents(userId: String, days: Int): List<BehaviorEvent> {
        val cutoff = Instant.now().minus(Duration.ofDays(days.toLong()))
        return getUserEvents(userId).filter { it.timestamp.isAfter(cutoff) }
    }

    private fun analyzeTopicInterests(events: List<BehaviorEvent>): Map<String, Double> {
        val topicScores = mutableMapOf<String, Double>()
        val now = Instant.now()
        
        events.forEach { event ->
            event.topic?.let { topic ->
                val baseScore = getEventScore(event.eventType)
                val timeDecay = calculateTimeDecay(event.timestamp, now)
                val finalScore = baseScore * timeDecay
                
                topicScores[topic] = topicScores.getOrDefault(topic, 0.0) + finalScore
            }
        }
        
        // Normalize scores to 0-1 range
        val maxScore = topicScores.values.maxOrNull() ?: 1.0
        return if (maxScore > 0) {
            topicScores.mapValues { it.value / maxScore }
        } else {
            topicScores
        }
    }

    private fun analyzeContentTypePreferences(events: List<BehaviorEvent>): Map<String, Double> {
        val contentTypeScores = mutableMapOf<String, Double>()
        val now = Instant.now()
        
        events.forEach { event ->
            event.contentType?.let { contentType ->
                val baseScore = getEventScore(event.eventType)
                val timeDecay = calculateTimeDecay(event.timestamp, now)
                val finalScore = baseScore * timeDecay
                
                contentTypeScores[contentType] = contentTypeScores.getOrDefault(contentType, 0.0) + finalScore
            }
        }
        
        // Normalize scores
        val maxScore = contentTypeScores.values.maxOrNull() ?: 1.0
        return if (maxScore > 0) {
            contentTypeScores.mapValues { it.value / maxScore }
        } else {
            contentTypeScores
        }
    }

    private fun analyzeLanguagePreferences(events: List<BehaviorEvent>): List<Pair<String, Double>> {
        // In a real implementation, this would analyze language from content metadata
        // For now, return mock preferences
        return listOf(
            "en" to 0.9,
            "es" to 0.3,
            "fr" to 0.1
        )
    }

    private fun calculateTopicScore(events: List<BehaviorEvent>, topic: String): Double {
        val topicEvents = events.filter { it.topic == topic }
        if (topicEvents.isEmpty()) return 0.0
        
        val now = Instant.now()
        val totalScore = topicEvents.sumOf { event ->
            val baseScore = getEventScore(event.eventType)
            val timeDecay = calculateTimeDecay(event.timestamp, now)
            baseScore * timeDecay
        }
        
        // Normalize by total events for this topic
        return totalScore / topicEvents.size
    }

    private fun calculateContentTypeScore(events: List<BehaviorEvent>, contentType: String): Double {
        val contentTypeEvents = events.filter { it.contentType == contentType }
        if (contentTypeEvents.isEmpty()) return 0.0
        
        val now = Instant.now()
        val totalScore = contentTypeEvents.sumOf { event ->
            val baseScore = getEventScore(event.eventType)
            val timeDecay = calculateTimeDecay(event.timestamp, now)
            baseScore * timeDecay
        }
        
        return totalScore / contentTypeEvents.size
    }

    private fun getEventScore(eventType: BehaviorEventType): Double {
        return config.engagementWeights[eventType] ?: 1.0
    }

    private fun calculateTimeDecay(eventTime: Instant, now: Instant): Double {
        val hoursSince = Duration.between(eventTime, now).toHours()
        return config.decayFactor.pow(hoursSince.toDouble() / 24.0) // Daily decay
    }

    private fun calculateConfidence(events: List<BehaviorEvent>, topic: String): Double {
        val topicEvents = events.filter { it.topic == topic }
        val totalEvents = events.size
        
        return if (totalEvents > 0) {
            // Confidence based on frequency and variety of interactions
            val frequency = topicEvents.size.toDouble() / totalEvents
            val variety = topicEvents.map { it.eventType }.toSet().size.toDouble() / BehaviorEventType.values().size
            (frequency + variety) / 2.0
        } else 0.0
    }

    private fun calculateContentTypeConfidence(events: List<BehaviorEvent>, contentType: String): Double {
        val contentTypeEvents = events.filter { it.contentType == contentType }
        val totalEvents = events.size
        
        return if (totalEvents > 0) {
            val frequency = contentTypeEvents.size.toDouble() / totalEvents
            val positiveInteractions = contentTypeEvents.count { 
                getEventScore(it.eventType) > 0 
            }.toDouble() / contentTypeEvents.size
            frequency * positiveInteractions
        } else 0.0
    }

    private fun createImplicitPreference(
        userId: String,
        type: PreferenceType,
        key: String,
        value: String,
        weight: Double,
        confidence: Double
    ): UserPreference {
        return UserPreference(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = type,
            key = key,
            value = value,
            weight = weight,
            source = PreferenceSource.IMPLICIT,
            confidence = confidence,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    private fun updateBehaviorProfile(userId: String) {
        val events = getUserEvents(userId)
        behaviorProfiles[userId] = buildBehaviorProfile(userId, events)
    }

    private fun buildBehaviorProfile(userId: String, events: List<BehaviorEvent>): UserBehaviorProfile {
        val recentEvents = getRecentUserEvents(userId, config.recentActivityWindowDays)
        
        val topicCounts = events.mapNotNull { it.topic }.groupBy { it }.mapValues { it.value.size.toLong() }
        val contentTypeCounts = events.mapNotNull { it.contentType }.groupBy { it }.mapValues { it.value.size.toLong() }
        
        val dwellTimes = events.mapNotNull { it.duration }
        val avgDwellTime = if (dwellTimes.isNotEmpty()) {
            Duration.ofMillis(dwellTimes.map { it.toMillis() }.average().toLong())
        } else null
        
        val engagementEvents = events.filter { 
            it.eventType in setOf(
                BehaviorEventType.CONTENT_LIKE,
                BehaviorEventType.CONTENT_SHARE,
                BehaviorEventType.CONTENT_COMMENT
            )
        }
        val viewEvents = events.filter { it.eventType == BehaviorEventType.CONTENT_VIEW }
        val engagementRate = if (viewEvents.isNotEmpty()) {
            engagementEvents.size.toDouble() / viewEvents.size
        } else 0.0
        
        val topInterests = analyzeTopicInterests(events)
        val topContentTypes = analyzeContentTypePreferences(events)
        
        val activeHours = events.groupBy { 
            it.timestamp.atZone(java.time.ZoneId.systemDefault()).hour 
        }.entries.sortedByDescending { it.value.size }
            .take(5)
            .map { it.key }
        
        return UserBehaviorProfile(
            userId = userId,
            totalEvents = events.size.toLong(),
            recentEvents = recentEvents.size.toLong(),
            topicInteractionCounts = topicCounts,
            contentTypeInteractionCounts = contentTypeCounts,
            averageDwellTime = avgDwellTime,
            engagementRate = engagementRate,
            preferredContentTypes = topContentTypes.entries.sortedByDescending { it.value }.take(5).map { it.key },
            preferredTopics = topInterests.entries.sortedByDescending { it.value }.take(10).map { it.key },
            activeHours = activeHours,
            lastActivityAt = events.maxOfOrNull { it.timestamp }
        )
    }

    private fun isSignificantEvent(event: BehaviorEvent): Boolean {
        return when (event.eventType) {
            BehaviorEventType.CONTENT_LIKE,
            BehaviorEventType.CONTENT_SHARE,
            BehaviorEventType.CONTENT_BOOKMARK,
            BehaviorEventType.USER_FOLLOW,
            BehaviorEventType.USER_BLOCK -> true
            else -> false
        }
    }
}