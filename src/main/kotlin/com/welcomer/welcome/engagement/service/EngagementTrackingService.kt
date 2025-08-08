package com.welcomer.welcome.engagement.service

import com.welcomer.welcome.engagement.model.*
import com.welcomer.welcome.engagement.repository.EngagementRepository
import com.welcomer.welcome.engagement.event.EngagementEventPublisher
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * Core service for tracking user engagement with content
 */
interface EngagementTrackingService {
    /**
     * Track a user engagement event
     */
    suspend fun trackEngagement(
        userId: String,
        request: TrackEngagementRequest
    ): EngagementResponse

    /**
     * Validate engagement data before recording
     */
    fun validateEngagementData(
        userId: String,
        contentId: String,
        engagementType: EngagementType,
        metadata: Map<String, Any>
    ): ValidationResult

    /**
     * Check for duplicate engagements
     */
    suspend fun isDuplicateEngagement(
        userId: String,
        contentId: String,
        engagementType: EngagementType
    ): Boolean

    /**
     * Get engagement history for a user
     */
    suspend fun getUserEngagementHistory(
        userId: String,
        startTime: Instant? = null,
        endTime: Instant? = null,
        limit: Int = 100
    ): List<EngagementEvent>

    /**
     * Get engagement metrics for content
     */
    suspend fun getContentEngagementMetrics(
        contentId: String,
        startTime: Instant,
        endTime: Instant
    ): ContentEngagementMetrics?

    /**
     * Batch track multiple engagements
     */
    suspend fun trackEngagementBatch(
        userId: String,
        requests: List<TrackEngagementRequest>
    ): List<EngagementResponse>
}

/**
 * Default implementation of engagement tracking service
 */
class DefaultEngagementTrackingService(
    private val engagementRepository: EngagementRepository,
    private val metricsAggregator: MetricsAggregationService,
    private val eventPublisher: EngagementEventPublisher,
    private val config: EngagementTrackingConfig = EngagementTrackingConfig()
) : EngagementTrackingService {

    override suspend fun trackEngagement(
        userId: String,
        request: TrackEngagementRequest
    ): EngagementResponse {
        try {
            // Validate input data
            val validation = validateEngagementData(
                userId,
                request.contentId,
                request.engagementType,
                request.metadata
            )

            if (!validation.isValid) {
                return EngagementResponse(
                    success = false,
                    message = "Validation failed: ${validation.errors.joinToString(", ")}"
                )
            }

            // Check for duplicates if enabled
            if (config.enableDuplicateDetection) {
                val isDuplicate = isDuplicateEngagement(userId, request.contentId, request.engagementType)
                if (isDuplicate && shouldPreventDuplicate(request.engagementType)) {
                    return EngagementResponse(
                        success = false,
                        message = "Duplicate engagement detected"
                    )
                }
            }

            // Create engagement event
            val event = createEngagementEvent(userId, request)

            // Record the event
            val engagementId = engagementRepository.record(event)

            // Update aggregated metrics asynchronously
            metricsAggregator.updateMetrics(request.contentId, request.engagementType)

            // Publish event for downstream systems
            eventPublisher.publishEngagementEvent(event)

            return EngagementResponse(
                success = true,
                engagementId = engagementId,
                message = "Engagement tracked successfully"
            )

        } catch (e: Exception) {
            return EngagementResponse(
                success = false,
                message = "Failed to track engagement: ${e.message}"
            )
        }
    }

    override fun validateEngagementData(
        userId: String,
        contentId: String,
        engagementType: EngagementType,
        metadata: Map<String, Any>
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate user ID
        if (userId.isBlank()) {
            errors.add("User ID is required")
        }

        // Validate content ID
        if (contentId.isBlank()) {
            errors.add("Content ID is required")
        }

        // Validate engagement type specific rules
        when (engagementType) {
            EngagementType.DWELL_TIME -> {
                val duration = metadata["duration"] as? Long
                if (duration == null) {
                    errors.add("Duration is required for dwell time events")
                } else if (duration < config.minDwellTimeSeconds * 1000) {
                    errors.add("Dwell time is below minimum threshold")
                } else if (duration > config.maxDwellTimeSeconds * 1000) {
                    errors.add("Dwell time exceeds maximum threshold")
                }
            }

            EngagementType.COMMENT -> {
                val commentText = metadata["text"] as? String
                if (commentText.isNullOrBlank()) {
                    errors.add("Comment text is required")
                } else if (commentText.length > 5000) {
                    errors.add("Comment text exceeds maximum length")
                }
            }

            EngagementType.SHARE -> {
                val platform = metadata["platform"] as? String
                if (platform.isNullOrBlank()) {
                    errors.add("Share platform is required")
                }
            }

            EngagementType.REPORT -> {
                val reason = metadata["reason"] as? String
                if (reason.isNullOrBlank()) {
                    errors.add("Report reason is required")
                }
            }

            EngagementType.SCROLL -> {
                val scrollPercentage = metadata["percentage"] as? Double
                if (scrollPercentage == null) {
                    errors.add("Scroll percentage is required")
                } else if (scrollPercentage < 0 || scrollPercentage > 100) {
                    errors.add("Scroll percentage must be between 0 and 100")
                }
            }

            else -> {
                // No specific validation for other types
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    override suspend fun isDuplicateEngagement(
        userId: String,
        contentId: String,
        engagementType: EngagementType
    ): Boolean {
        if (!config.enableDuplicateDetection) {
            return false
        }

        val windowStart = Instant.now().minusSeconds(config.duplicateWindowMinutes * 60)
        return engagementRepository.existsSimilar(userId, contentId, engagementType, windowStart)
    }

    override suspend fun getUserEngagementHistory(
        userId: String,
        startTime: Instant?,
        endTime: Instant?,
        limit: Int
    ): List<EngagementEvent> {
        return engagementRepository.findByUserId(userId, startTime, endTime, limit)
    }

    override suspend fun getContentEngagementMetrics(
        contentId: String,
        startTime: Instant,
        endTime: Instant
    ): ContentEngagementMetrics? {
        return engagementRepository.getContentMetrics(contentId, startTime, endTime)
    }

    override suspend fun trackEngagementBatch(
        userId: String,
        requests: List<TrackEngagementRequest>
    ): List<EngagementResponse> {
        return requests.map { request ->
            trackEngagement(userId, request)
        }
    }

    // Private helper methods

    private fun createEngagementEvent(
        userId: String,
        request: TrackEngagementRequest
    ): EngagementEvent {
        return EngagementEvent(
            id = UUID.randomUUID().toString(),
            userId = userId,
            contentId = request.contentId,
            type = request.engagementType,
            metadata = request.metadata,
            timestamp = Instant.now(),
            sessionId = request.sessionId
        )
    }

    private fun shouldPreventDuplicate(engagementType: EngagementType): Boolean {
        // Prevent duplicate likes, bookmarks, etc. but allow multiple views, comments
        return engagementType in setOf(
            EngagementType.LIKE,
            EngagementType.BOOKMARK,
            EngagementType.HIDE,
            EngagementType.REPORT
        )
    }
}