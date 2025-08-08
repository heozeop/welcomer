package com.welcomer.welcome.ingestion.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Base interface for all content-related events
 */
sealed interface ContentEvent {
    val eventId: String
    val eventType: ContentEventType
    val timestamp: Instant
    val userId: String
    val contentId: String
    val version: String
    val metadata: Map<String, Any>
}

/**
 * Event published when new content is created
 */
data class ContentCreatedEvent(
    override val eventId: String,
    override val timestamp: Instant,
    override val userId: String,
    override val contentId: String,
    override val version: String = "1.0",
    override val metadata: Map<String, Any> = emptyMap(),
    
    @JsonProperty("contentType")
    val contentType: ContentType,
    
    @JsonProperty("visibility")
    val visibility: ContentVisibility,
    
    @JsonProperty("hasMedia")
    val hasMedia: Boolean,
    
    @JsonProperty("tags")
    val tags: List<String> = emptyList(),
    
    @JsonProperty("mentions")
    val mentions: List<String> = emptyList(),
    
    @JsonProperty("language")
    val language: String? = null,
    
    @JsonProperty("topics")
    val topics: List<String> = emptyList(),
    
    @JsonProperty("sentiment")
    val sentiment: String? = null
) : ContentEvent {
    override val eventType = ContentEventType.CONTENT_CREATED
}

/**
 * Event published when content is updated
 */
data class ContentUpdatedEvent(
    override val eventId: String,
    override val timestamp: Instant,
    override val userId: String,
    override val contentId: String,
    override val version: String = "1.0",
    override val metadata: Map<String, Any> = emptyMap(),
    
    @JsonProperty("updatedFields")
    val updatedFields: List<String>,
    
    @JsonProperty("previousStatus")
    val previousStatus: ContentStatus? = null,
    
    @JsonProperty("newStatus")
    val newStatus: ContentStatus? = null
) : ContentEvent {
    override val eventType = ContentEventType.CONTENT_UPDATED
}

/**
 * Event published when content is deleted
 */
data class ContentDeletedEvent(
    override val eventId: String,
    override val timestamp: Instant,
    override val userId: String,
    override val contentId: String,
    override val version: String = "1.0",
    override val metadata: Map<String, Any> = emptyMap(),
    
    @JsonProperty("deletionType")
    val deletionType: DeletionType,
    
    @JsonProperty("reason")
    val reason: String? = null
) : ContentEvent {
    override val eventType = ContentEventType.CONTENT_DELETED
}

/**
 * Event published when content is published (becomes visible)
 */
data class ContentPublishedEvent(
    override val eventId: String,
    override val timestamp: Instant,
    override val userId: String,
    override val contentId: String,
    override val version: String = "1.0",
    override val metadata: Map<String, Any> = emptyMap(),
    
    @JsonProperty("contentType")
    val contentType: ContentType,
    
    @JsonProperty("visibility")
    val visibility: ContentVisibility,
    
    @JsonProperty("authorFollowersCount")
    val authorFollowersCount: Int? = null,
    
    @JsonProperty("isScheduledPost")
    val isScheduledPost: Boolean = false
) : ContentEvent {
    override val eventType = ContentEventType.CONTENT_PUBLISHED
}

/**
 * Event published when media processing is completed
 */
data class MediaProcessedEvent(
    override val eventId: String,
    override val timestamp: Instant,
    override val userId: String,
    override val contentId: String,
    override val version: String = "1.0",
    override val metadata: Map<String, Any> = emptyMap(),
    
    @JsonProperty("mediaId")
    val mediaId: String,
    
    @JsonProperty("mediaType")
    val mediaType: MediaType,
    
    @JsonProperty("processingStatus")
    val processingStatus: MediaProcessingStatus,
    
    @JsonProperty("processingDurationMs")
    val processingDurationMs: Long? = null,
    
    @JsonProperty("fileSizeBytes")
    val fileSizeBytes: Long? = null,
    
    @JsonProperty("thumbnailGenerated")
    val thumbnailGenerated: Boolean = false
) : ContentEvent {
    override val eventType = ContentEventType.MEDIA_PROCESSED
}

/**
 * Event published when content metrics are updated
 */
data class ContentMetricsUpdatedEvent(
    override val eventId: String,
    override val timestamp: Instant,
    override val userId: String,
    override val contentId: String,
    override val version: String = "1.0",
    override val metadata: Map<String, Any> = emptyMap(),
    
    @JsonProperty("metricsType")
    val metricsType: MetricsType,
    
    @JsonProperty("newValue")
    val newValue: Int,
    
    @JsonProperty("previousValue")
    val previousValue: Int,
    
    @JsonProperty("engagementType")
    val engagementType: String? = null
) : ContentEvent {
    override val eventType = ContentEventType.CONTENT_METRICS_UPDATED
}

/**
 * Types of content events
 */
enum class ContentEventType {
    @JsonProperty("content_created")
    CONTENT_CREATED,
    
    @JsonProperty("content_updated") 
    CONTENT_UPDATED,
    
    @JsonProperty("content_deleted")
    CONTENT_DELETED,
    
    @JsonProperty("content_published")
    CONTENT_PUBLISHED,
    
    @JsonProperty("media_processed")
    MEDIA_PROCESSED,
    
    @JsonProperty("content_metrics_updated")
    CONTENT_METRICS_UPDATED
}

/**
 * Types of content deletion
 */
enum class DeletionType {
    @JsonProperty("soft_delete")
    SOFT_DELETE,
    
    @JsonProperty("hard_delete")
    HARD_DELETE,
    
    @JsonProperty("moderation_delete")
    MODERATION_DELETE
}

/**
 * Types of metrics being updated
 */
enum class MetricsType {
    @JsonProperty("likes_count")
    LIKES_COUNT,
    
    @JsonProperty("comments_count")
    COMMENTS_COUNT,
    
    @JsonProperty("shares_count")
    SHARES_COUNT,
    
    @JsonProperty("views_count")
    VIEWS_COUNT,
    
    @JsonProperty("replies_count")
    REPLIES_COUNT
}

/**
 * Event publication result
 */
data class EventPublicationResult(
    val success: Boolean,
    val eventId: String,
    val publishedAt: Instant,
    val error: PublicationError? = null,
    val retryCount: Int = 0
)

/**
 * Event publication error
 */
data class PublicationError(
    val code: PublicationErrorCode,
    val message: String,
    val details: String? = null,
    val retryable: Boolean = true
)

/**
 * Event publication error codes
 */
enum class PublicationErrorCode {
    BROKER_UNAVAILABLE,
    SERIALIZATION_ERROR,
    NETWORK_ERROR,
    AUTHENTICATION_ERROR,
    TOPIC_NOT_FOUND,
    MESSAGE_TOO_LARGE,
    RATE_LIMIT_EXCEEDED,
    UNKNOWN_ERROR
}