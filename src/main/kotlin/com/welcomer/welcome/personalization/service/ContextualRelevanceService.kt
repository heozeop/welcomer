package com.welcomer.welcome.personalization.service

import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.*

/**
 * Enhanced contextual relevance calculation service with location, temporal, and session awareness
 */
interface ContextualRelevanceService {
    /**
     * Calculate comprehensive contextual relevance based on user context and content
     */
    suspend fun calculateAdvancedContextualRelevance(
        item: PersonalizableItem,
        userContext: UserContext,
        config: ContextualRelevanceConfig = ContextualRelevanceConfig()
    ): ContextualRelevanceResult

    /**
     * Calculate temporal relevance (time-based patterns)
     */
    fun calculateTemporalRelevance(
        item: PersonalizableItem,
        userContext: UserContext,
        config: ContextualRelevanceConfig
    ): TemporalRelevanceResult

    /**
     * Calculate location-based relevance
     */
    fun calculateLocationRelevance(
        item: PersonalizableItem,
        userLocation: UserLocation?,
        config: ContextualRelevanceConfig
    ): LocationRelevanceResult

    /**
     * Calculate session context relevance
     */
    fun calculateSessionRelevance(
        item: PersonalizableItem,
        userContext: UserContext,
        config: ContextualRelevanceConfig
    ): SessionRelevanceResult

    /**
     * Calculate device-specific relevance
     */
    fun calculateDeviceRelevance(
        item: PersonalizableItem,
        deviceType: DeviceType,
        config: ContextualRelevanceConfig
    ): DeviceRelevanceResult
}

/**
 * Configuration for contextual relevance calculation
 */
data class ContextualRelevanceConfig(
    val temporalWeight: Double = 0.3,
    val locationWeight: Double = 0.2,
    val sessionWeight: Double = 0.25,
    val deviceWeight: Double = 0.25,
    val enableTemporalPatterns: Boolean = true,
    val enableLocationMatching: Boolean = true,
    val enableSessionAnalysis: Boolean = true,
    val enableDeviceOptimization: Boolean = true,
    val timeZoneAware: Boolean = true,
    val sessionContextDepth: Int = 10, // Number of previous activities to consider
    val temporalDecayHours: Double = 24.0, // How quickly temporal relevance decays
    val locationProximityThreshold: Double = 50.0 // km
)

/**
 * Comprehensive result of contextual relevance analysis
 */
data class ContextualRelevanceResult(
    val overallRelevance: Double,
    val temporalRelevance: TemporalRelevanceResult,
    val locationRelevance: LocationRelevanceResult,
    val sessionRelevance: SessionRelevanceResult,
    val deviceRelevance: DeviceRelevanceResult,
    val confidenceLevel: ContextualConfidence,
    val explanation: String,
    val contextFactorsApplied: List<String>
)

/**
 * Temporal relevance analysis result
 */
data class TemporalRelevanceResult(
    val timeOfDayScore: Double,
    val dayOfWeekScore: Double,
    val recencyScore: Double,
    val peakTimeAlignment: Double,
    val temporalTrend: TemporalContextTrend,
    val explanation: String
)

/**
 * Location relevance analysis result
 */
data class LocationRelevanceResult(
    val geographicRelevance: Double,
    val timezoneAlignment: Double,
    val localEventRelevance: Double,
    val culturalRelevance: Double,
    val explanation: String
)

/**
 * Session context relevance result
 */
data class SessionRelevanceResult(
    val sessionDurationAlignment: Double,
    val previousActivityRelevance: Double,
    val engagementPatternMatch: Double,
    val contextualContinuity: Double,
    val attentionSpanMatch: Double,
    val explanation: String
)

/**
 * Device-specific relevance result
 */
data class DeviceRelevanceResult(
    val contentFormatAlignment: Double,
    val deviceCapabilityMatch: Double,
    val interactionPatternMatch: Double,
    val displayOptimization: Double,
    val explanation: String
)

/**
 * Temporal trends in context
 */
enum class TemporalContextTrend {
    PEAK_HOURS,        // User is in their peak engagement hours
    OFF_PEAK,          // User is in low engagement period
    CONSISTENT,        // User engages consistently throughout day
    WEEKEND_PATTERN,   // Different weekend engagement
    WORKDAY_PATTERN,   // Workday engagement pattern
    IRREGULAR          // No clear pattern
}

/**
 * Confidence level in contextual relevance
 */
enum class ContextualConfidence {
    HIGH,    // Multiple strong context signals
    MEDIUM,  // Some context signals available
    LOW,     // Limited context information
    MINIMAL  // Very little context available
}

/**
 * Default implementation with advanced contextual relevance analysis
 */
@Service
class DefaultContextualRelevanceService : ContextualRelevanceService {

    // Time-based content preferences
    private val timeBasedContentTypes = mapOf(
        6..9 to listOf("news", "current-events", "business", "productivity", "morning-briefing"),
        10..11 to listOf("work", "productivity", "technology", "business", "learning"),
        12..14 to listOf("lifestyle", "food", "entertainment", "social", "quick-reads"),
        15..17 to listOf("work", "productivity", "news", "technology", "updates"),
        18..20 to listOf("entertainment", "sports", "social", "lifestyle", "personal"),
        21..23 to listOf("entertainment", "movies", "books", "relaxation", "personal-time")
    )

    // Device-content alignment
    private val deviceContentPreferences = mapOf(
        DeviceType.MOBILE to mapOf(
            "quick-reads" to 0.9,
            "social" to 0.85,
            "news" to 0.8,
            "photos" to 0.9,
            "short-videos" to 0.95
        ),
        DeviceType.TABLET to mapOf(
            "long-form" to 0.8,
            "magazines" to 0.9,
            "videos" to 0.85,
            "interactive" to 0.8,
            "visual-content" to 0.9
        ),
        DeviceType.DESKTOP to mapOf(
            "long-form" to 0.95,
            "work-related" to 0.9,
            "productivity" to 0.9,
            "detailed-analysis" to 0.9,
            "technical" to 0.85
        ),
        DeviceType.TV to mapOf(
            "videos" to 0.95,
            "entertainment" to 0.9,
            "movies" to 0.95,
            "streaming" to 0.9,
            "visual-heavy" to 0.9
        )
    )

    // Geographic content preferences
    private val geographicTopicBoosts = mapOf(
        "US" to listOf("american-football", "baseball", "thanksgiving", "july-4th"),
        "UK" to listOf("football", "cricket", "tea", "royal-family"),
        "JP" to listOf("anime", "manga", "cherry-blossoms", "technology"),
        "DE" to listOf("oktoberfest", "soccer", "engineering", "precision")
    )

    override suspend fun calculateAdvancedContextualRelevance(
        item: PersonalizableItem,
        userContext: UserContext,
        config: ContextualRelevanceConfig
    ): ContextualRelevanceResult {
        val contextFactorsApplied = mutableListOf<String>()
        
        // Calculate individual relevance components
        val temporalRelevance = if (config.enableTemporalPatterns) {
            contextFactorsApplied.add("Temporal patterns")
            calculateTemporalRelevance(item, userContext, config)
        } else {
            createNeutralTemporalResult()
        }

        val locationRelevance = if (config.enableLocationMatching && userContext.location != null) {
            contextFactorsApplied.add("Location matching")
            calculateLocationRelevance(item, userContext.location, config)
        } else {
            createNeutralLocationResult()
        }

        val sessionRelevance = if (config.enableSessionAnalysis) {
            contextFactorsApplied.add("Session analysis")
            calculateSessionRelevance(item, userContext, config)
        } else {
            createNeutralSessionResult()
        }

        val deviceRelevance = if (config.enableDeviceOptimization) {
            contextFactorsApplied.add("Device optimization")
            calculateDeviceRelevance(item, userContext.deviceType, config)
        } else {
            createNeutralDeviceResult()
        }

        // Calculate overall relevance with weights
        val overallRelevance = (
            (temporalRelevance.timeOfDayScore * config.temporalWeight) +
            (locationRelevance.geographicRelevance * config.locationWeight) +
            (sessionRelevance.sessionDurationAlignment * config.sessionWeight) +
            (deviceRelevance.contentFormatAlignment * config.deviceWeight)
        ).coerceIn(0.0, 1.0)

        // Determine confidence level
        val confidenceLevel = determineConfidenceLevel(contextFactorsApplied.size, userContext)

        // Generate explanation
        val explanation = generateOverallExplanation(
            temporalRelevance, locationRelevance, sessionRelevance, deviceRelevance
        )

        return ContextualRelevanceResult(
            overallRelevance = overallRelevance,
            temporalRelevance = temporalRelevance,
            locationRelevance = locationRelevance,
            sessionRelevance = sessionRelevance,
            deviceRelevance = deviceRelevance,
            confidenceLevel = confidenceLevel,
            explanation = explanation,
            contextFactorsApplied = contextFactorsApplied
        )
    }

    override fun calculateTemporalRelevance(
        item: PersonalizableItem,
        userContext: UserContext,
        config: ContextualRelevanceConfig
    ): TemporalRelevanceResult {
        val currentHour = userContext.timeOfDay
        val currentDayOfWeek = userContext.dayOfWeek

        // Time of day relevance
        val timeOfDayScore = calculateTimeOfDayRelevance(item, currentHour)

        // Day of week relevance
        val dayOfWeekScore = calculateDayOfWeekRelevance(item, currentDayOfWeek)

        // Content recency relevance
        val recencyScore = calculateContentRecencyRelevance(item, config)

        // Peak time alignment (assume user is more engaged during certain hours)
        val peakTimeAlignment = calculatePeakTimeAlignment(currentHour)

        // Determine temporal trend
        val temporalTrend = determineTemporalTrend(currentHour, currentDayOfWeek)

        val explanation = generateTemporalExplanation(
            timeOfDayScore, dayOfWeekScore, recencyScore, temporalTrend
        )

        return TemporalRelevanceResult(
            timeOfDayScore = timeOfDayScore,
            dayOfWeekScore = dayOfWeekScore,
            recencyScore = recencyScore,
            peakTimeAlignment = peakTimeAlignment,
            temporalTrend = temporalTrend,
            explanation = explanation
        )
    }

    override fun calculateLocationRelevance(
        item: PersonalizableItem,
        userLocation: UserLocation?,
        config: ContextualRelevanceConfig
    ): LocationRelevanceResult {
        if (userLocation == null) {
            return createNeutralLocationResult()
        }

        // Geographic content relevance
        val geographicRelevance = calculateGeographicRelevance(item, userLocation)

        // Timezone alignment
        val timezoneAlignment = calculateTimezoneAlignment(item, userLocation)

        // Local events relevance (simplified)
        val localEventRelevance = calculateLocalEventRelevance(item, userLocation)

        // Cultural relevance
        val culturalRelevance = calculateCulturalRelevance(item, userLocation)

        val explanation = generateLocationExplanation(
            geographicRelevance, timezoneAlignment, localEventRelevance
        )

        return LocationRelevanceResult(
            geographicRelevance = geographicRelevance,
            timezoneAlignment = timezoneAlignment,
            localEventRelevance = localEventRelevance,
            culturalRelevance = culturalRelevance,
            explanation = explanation
        )
    }

    override fun calculateSessionRelevance(
        item: PersonalizableItem,
        userContext: UserContext,
        config: ContextualRelevanceConfig
    ): SessionRelevanceResult {
        // Session duration alignment (different content for short vs long sessions)
        val sessionDurationAlignment = calculateSessionDurationAlignment(item, userContext)

        // Previous activity relevance
        val previousActivityRelevance = calculatePreviousActivityRelevance(item, userContext)

        // Engagement pattern match
        val engagementPatternMatch = calculateEngagementPatternMatch(item, userContext)

        // Contextual continuity (content flow)
        val contextualContinuity = calculateContextualContinuity(item, userContext)

        // Attention span match
        val attentionSpanMatch = calculateAttentionSpanMatch(item, userContext)

        val explanation = generateSessionExplanation(
            sessionDurationAlignment, previousActivityRelevance, contextualContinuity
        )

        return SessionRelevanceResult(
            sessionDurationAlignment = sessionDurationAlignment,
            previousActivityRelevance = previousActivityRelevance,
            engagementPatternMatch = engagementPatternMatch,
            contextualContinuity = contextualContinuity,
            attentionSpanMatch = attentionSpanMatch,
            explanation = explanation
        )
    }

    override fun calculateDeviceRelevance(
        item: PersonalizableItem,
        deviceType: DeviceType,
        config: ContextualRelevanceConfig
    ): DeviceRelevanceResult {
        // Content format alignment with device capabilities
        val contentFormatAlignment = calculateContentFormatAlignment(item, deviceType)

        // Device capability match
        val deviceCapabilityMatch = calculateDeviceCapabilityMatch(item, deviceType)

        // Interaction pattern match
        val interactionPatternMatch = calculateInteractionPatternMatch(item, deviceType)

        // Display optimization
        val displayOptimization = calculateDisplayOptimization(item, deviceType)

        val explanation = generateDeviceExplanation(
            contentFormatAlignment, deviceCapabilityMatch, deviceType
        )

        return DeviceRelevanceResult(
            contentFormatAlignment = contentFormatAlignment,
            deviceCapabilityMatch = deviceCapabilityMatch,
            interactionPatternMatch = interactionPatternMatch,
            displayOptimization = displayOptimization,
            explanation = explanation
        )
    }

    // Private helper methods for temporal relevance

    private fun calculateTimeOfDayRelevance(item: PersonalizableItem, hour: Int): Double {
        val preferredContentTypes = timeBasedContentTypes.entries
            .filter { hour in it.key }
            .flatMap { it.value }

        val itemTopicsLower = item.topics.map { it.lowercase() }
        val matchScore = preferredContentTypes.count { contentType ->
            itemTopicsLower.any { it.contains(contentType) }
        }.toDouble() / maxOf(preferredContentTypes.size, 1)

        return (0.5 + matchScore * 0.5).coerceIn(0.0, 1.0)
    }

    private fun calculateDayOfWeekRelevance(item: PersonalizableItem, dayOfWeek: Int): Double {
        val isWeekend = dayOfWeek in listOf(DayOfWeek.SATURDAY.value, DayOfWeek.SUNDAY.value)
        val itemTopicsLower = item.topics.map { it.lowercase() }

        return when {
            isWeekend && itemTopicsLower.any { it.contains("entertainment") || it.contains("leisure") || it.contains("hobby") } -> 0.8
            !isWeekend && itemTopicsLower.any { it.contains("work") || it.contains("business") || it.contains("productivity") } -> 0.8
            else -> 0.5
        }
    }

    private fun calculateContentRecencyRelevance(item: PersonalizableItem, config: ContextualRelevanceConfig): Double {
        val now = Instant.now()
        val hoursOld = ChronoUnit.HOURS.between(item.createdAt, now).toDouble()
        
        // Exponential decay for content age relevance
        return exp(-hoursOld / config.temporalDecayHours).coerceIn(0.0, 1.0)
    }

    private fun calculatePeakTimeAlignment(hour: Int): Double {
        // Assume peak engagement hours are 9-11 AM, 1-3 PM, 7-9 PM
        return when (hour) {
            in 9..11, in 13..15, in 19..21 -> 0.9
            in 8..8, in 12..12, in 16..18, in 20..22 -> 0.7
            else -> 0.4
        }
    }

    private fun determineTemporalTrend(hour: Int, dayOfWeek: Int): TemporalContextTrend {
        val isWeekend = dayOfWeek in listOf(DayOfWeek.SATURDAY.value, DayOfWeek.SUNDAY.value)
        
        return when {
            isWeekend -> TemporalContextTrend.WEEKEND_PATTERN
            hour in 9..17 -> TemporalContextTrend.WORKDAY_PATTERN
            hour in 19..21 -> TemporalContextTrend.PEAK_HOURS
            else -> TemporalContextTrend.OFF_PEAK
        }
    }

    // Private helper methods for location relevance

    private fun calculateGeographicRelevance(item: PersonalizableItem, location: UserLocation): Double {
        val country = location.country ?: return 0.5
        val relevantTopics = geographicTopicBoosts[country] ?: return 0.5
        
        val itemTopicsLower = item.topics.map { it.lowercase() }
        val matchScore = relevantTopics.count { topic ->
            itemTopicsLower.any { it.contains(topic) }
        }.toDouble() / maxOf(relevantTopics.size, 1)
        
        return (0.5 + matchScore * 0.5).coerceIn(0.0, 1.0)
    }

    private fun calculateTimezoneAlignment(item: PersonalizableItem, location: UserLocation): Double {
        // For now, return neutral score - would need timezone-aware content analysis
        return 0.5
    }

    private fun calculateLocalEventRelevance(item: PersonalizableItem, location: UserLocation): Double {
        // Simplified - in production would check against local events API
        val city = location.city?.lowercase() ?: return 0.5
        val itemTopicsLower = item.topics.map { it.lowercase() }
        
        return if (itemTopicsLower.any { it.contains(city) || it.contains("local") }) {
            0.8
        } else {
            0.5
        }
    }

    private fun calculateCulturalRelevance(item: PersonalizableItem, location: UserLocation): Double {
        // Simplified cultural relevance based on country
        return calculateGeographicRelevance(item, location)
    }

    // Private helper methods for session relevance

    private fun calculateSessionDurationAlignment(item: PersonalizableItem, userContext: UserContext): Double {
        val sessionMinutes = userContext.sessionDuration
        val itemTopicsLower = item.topics.map { it.lowercase() }
        
        return when {
            sessionMinutes < 5 && itemTopicsLower.any { it.contains("quick") || it.contains("brief") } -> 0.9
            sessionMinutes in 5..30 && itemTopicsLower.any { it.contains("medium") || it.contains("news") } -> 0.8
            sessionMinutes > 30 && itemTopicsLower.any { it.contains("detailed") || it.contains("analysis") } -> 0.9
            else -> 0.5
        }
    }

    private fun calculatePreviousActivityRelevance(item: PersonalizableItem, userContext: UserContext): Double {
        if (userContext.previousActivity.isEmpty()) return 0.5
        
        // Avoid showing recently seen content
        return if (item.content.id in userContext.previousActivity) {
            0.2
        } else {
            0.7
        }
    }

    private fun calculateEngagementPatternMatch(item: PersonalizableItem, userContext: UserContext): Double {
        // Simplified engagement pattern - would analyze historical engagement patterns
        return 0.5
    }

    private fun calculateContextualContinuity(item: PersonalizableItem, userContext: UserContext): Double {
        if (userContext.previousActivity.isEmpty()) return 0.5
        
        // Check if current item relates to previous activity topics
        // This is simplified - in production would track topic transitions
        return 0.6
    }

    private fun calculateAttentionSpanMatch(item: PersonalizableItem, userContext: UserContext): Double {
        // Match content length to expected attention span based on session
        val sessionMinutes = userContext.sessionDuration
        
        return when {
            sessionMinutes < 5 -> 0.8  // Assume short content preference for short sessions
            sessionMinutes > 30 -> 0.7 // Assume longer content tolerance for longer sessions
            else -> 0.6
        }
    }

    // Private helper methods for device relevance

    private fun calculateContentFormatAlignment(item: PersonalizableItem, deviceType: DeviceType): Double {
        val contentTypePrefs = deviceContentPreferences[deviceType] ?: return 0.5
        val contentTypeName = item.content.contentType.name.lowercase()
        val itemTopicsLower = item.topics.map { it.lowercase() }
        
        val alignmentScores = contentTypePrefs.entries.mapNotNull { (prefType, score) ->
            if (contentTypeName.contains(prefType) || itemTopicsLower.any { it.contains(prefType) }) {
                score
            } else null
        }
        
        return alignmentScores.maxOrNull() ?: 0.5
    }

    private fun calculateDeviceCapabilityMatch(item: PersonalizableItem, deviceType: DeviceType): Double {
        val contentType = item.content.contentType.name
        
        return when (deviceType) {
            DeviceType.MOBILE -> when (contentType) {
                "TEXT" -> 0.8
                "IMAGE" -> 0.9
                "VIDEO" -> 0.6
                else -> 0.5
            }
            DeviceType.TABLET -> 0.8 // Good for most content types
            DeviceType.DESKTOP -> when (contentType) {
                "TEXT" -> 0.9
                "VIDEO" -> 0.9
                "IMAGE" -> 0.8
                else -> 0.7
            }
            DeviceType.TV -> when (contentType) {
                "VIDEO" -> 0.95
                "IMAGE" -> 0.7
                "TEXT" -> 0.3
                else -> 0.4
            }
            else -> 0.5
        }
    }

    private fun calculateInteractionPatternMatch(item: PersonalizableItem, deviceType: DeviceType): Double {
        // Simplified interaction pattern matching
        return when (deviceType) {
            DeviceType.MOBILE -> 0.8  // Optimized for touch, quick interactions
            DeviceType.TABLET -> 0.7  // Good for reading, moderate interaction
            DeviceType.DESKTOP -> 0.9 // Full interaction capabilities
            DeviceType.TV -> 0.5      // Limited interaction
            else -> 0.5
        }
    }

    private fun calculateDisplayOptimization(item: PersonalizableItem, deviceType: DeviceType): Double {
        // Simplified display optimization score
        return when (deviceType) {
            DeviceType.MOBILE -> 0.7  // Small screen considerations
            DeviceType.TABLET -> 0.8  // Medium screen, good for most content
            DeviceType.DESKTOP -> 0.9 // Large screen, optimal display
            DeviceType.TV -> 0.6      // Large screen but different viewing distance
            else -> 0.5
        }
    }

    // Helper methods for creating neutral results

    private fun createNeutralTemporalResult() = TemporalRelevanceResult(
        timeOfDayScore = 0.5,
        dayOfWeekScore = 0.5,
        recencyScore = 0.5,
        peakTimeAlignment = 0.5,
        temporalTrend = TemporalContextTrend.CONSISTENT,
        explanation = "No temporal patterns analyzed"
    )

    private fun createNeutralLocationResult() = LocationRelevanceResult(
        geographicRelevance = 0.5,
        timezoneAlignment = 0.5,
        localEventRelevance = 0.5,
        culturalRelevance = 0.5,
        explanation = "No location information available"
    )

    private fun createNeutralSessionResult() = SessionRelevanceResult(
        sessionDurationAlignment = 0.5,
        previousActivityRelevance = 0.5,
        engagementPatternMatch = 0.5,
        contextualContinuity = 0.5,
        attentionSpanMatch = 0.5,
        explanation = "No session analysis performed"
    )

    private fun createNeutralDeviceResult() = DeviceRelevanceResult(
        contentFormatAlignment = 0.5,
        deviceCapabilityMatch = 0.5,
        interactionPatternMatch = 0.5,
        displayOptimization = 0.5,
        explanation = "No device optimization applied"
    )

    // Helper methods for generating explanations

    private fun generateTemporalExplanation(
        timeOfDayScore: Double,
        dayOfWeekScore: Double,
        recencyScore: Double,
        trend: TemporalContextTrend
    ): String {
        val explanations = mutableListOf<String>()
        
        if (timeOfDayScore > 0.7) explanations.add("Content aligns well with current time of day")
        if (dayOfWeekScore > 0.7) explanations.add("Appropriate for current day of week")
        if (recencyScore > 0.7) explanations.add("Recent content matches temporal preferences")
        if (trend == TemporalContextTrend.PEAK_HOURS) explanations.add("Content shown during peak engagement hours")
        
        return explanations.takeIf { it.isNotEmpty() }?.joinToString("; ") 
            ?: "Standard temporal relevance applied"
    }

    private fun generateLocationExplanation(
        geoRelevance: Double,
        timezoneAlignment: Double,
        localEventRelevance: Double
    ): String {
        val explanations = mutableListOf<String>()
        
        if (geoRelevance > 0.7) explanations.add("Content relevant to your geographic region")
        if (localEventRelevance > 0.7) explanations.add("Related to local events or interests")
        
        return explanations.takeIf { it.isNotEmpty() }?.joinToString("; ") 
            ?: "Basic location relevance applied"
    }

    private fun generateSessionExplanation(
        sessionAlignment: Double,
        activityRelevance: Double,
        continuity: Double
    ): String {
        val explanations = mutableListOf<String>()
        
        if (sessionAlignment > 0.7) explanations.add("Content length matches your session duration")
        if (activityRelevance > 0.7) explanations.add("Content flows well with your recent activity")
        if (continuity > 0.7) explanations.add("Maintains contextual continuity")
        
        return explanations.takeIf { it.isNotEmpty() }?.joinToString("; ") 
            ?: "Basic session relevance applied"
    }

    private fun generateDeviceExplanation(
        formatAlignment: Double,
        capabilityMatch: Double,
        deviceType: DeviceType
    ): String {
        val explanations = mutableListOf<String>()
        
        if (formatAlignment > 0.7) explanations.add("Content optimized for your ${deviceType.name.lowercase()}")
        if (capabilityMatch > 0.8) explanations.add("Content takes advantage of device capabilities")
        
        return explanations.takeIf { it.isNotEmpty() }?.joinToString("; ") 
            ?: "Standard device optimization applied"
    }

    private fun generateOverallExplanation(
        temporal: TemporalRelevanceResult,
        location: LocationRelevanceResult,
        session: SessionRelevanceResult,
        device: DeviceRelevanceResult
    ): String {
        val allExplanations = listOf(temporal.explanation, location.explanation, session.explanation, device.explanation)
            .filter { it.isNotEmpty() && !it.contains("No ") && !it.contains("Basic ") && !it.contains("Standard ") }
        
        return allExplanations.takeIf { it.isNotEmpty() }?.joinToString("; ")
            ?: "Contextual relevance calculated using available context signals"
    }

    private fun determineConfidenceLevel(factorsApplied: Int, userContext: UserContext): ContextualConfidence {
        val contextRichness = listOfNotNull(
            userContext.location,
            if (userContext.sessionDuration > 0) true else null,
            if (userContext.previousActivity.isNotEmpty()) true else null,
            if (userContext.deviceType != DeviceType.UNKNOWN) true else null
        ).size
        
        return when {
            factorsApplied >= 3 && contextRichness >= 3 -> ContextualConfidence.HIGH
            factorsApplied >= 2 && contextRichness >= 2 -> ContextualConfidence.MEDIUM
            factorsApplied >= 1 || contextRichness >= 1 -> ContextualConfidence.LOW
            else -> ContextualConfidence.MINIMAL
        }
    }
}