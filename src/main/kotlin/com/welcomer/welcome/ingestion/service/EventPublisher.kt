package com.welcomer.welcome.ingestion.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.welcomer.welcome.ingestion.model.*
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Service responsible for publishing content-related events
 */
@Service
class EventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val CONTENT_EVENTS_TOPIC = "content-events"
        private const val MEDIA_EVENTS_TOPIC = "media-events"
        private const val METRICS_EVENTS_TOPIC = "metrics-events"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
    }

    /**
     * Publishes a content event to the appropriate topic
     */
    suspend fun publishEvent(event: ContentEvent): EventPublicationResult {
        return try {
            val topic = selectTopicForEvent(event)
            val eventJson = objectMapper.writeValueAsString(event)
            
            val sendResult = sendEventToKafka(topic, event.contentId, eventJson)
            
            EventPublicationResult(
                success = true,
                eventId = event.eventId,
                publishedAt = Instant.now()
            )
        } catch (e: Exception) {
            val error = mapExceptionToError(e)
            
            EventPublicationResult(
                success = false,
                eventId = event.eventId,
                publishedAt = Instant.now(),
                error = error
            )
        }
    }

    /**
     * Publishes a content created event
     */
    suspend fun publishContentCreated(
        contentId: String,
        userId: String,
        content: StoredContent,
        extractedMetadata: ExtractedMetadata?
    ): EventPublicationResult {
        val event = ContentCreatedEvent(
            eventId = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            userId = userId,
            contentId = contentId,
            contentType = content.contentType,
            visibility = content.visibility,
            hasMedia = content.mediaAttachments.isNotEmpty(),
            tags = content.tags,
            mentions = content.mentions,
            language = extractedMetadata?.language?.detectedLanguage,
            topics = extractedMetadata?.topics?.map { it.name } ?: emptyList(),
            sentiment = extractedMetadata?.sentiment?.overallSentiment?.name?.lowercase(),
            metadata = buildEventMetadata(content, extractedMetadata)
        )
        
        return publishEvent(event)
    }

    /**
     * Publishes a content updated event
     */
    suspend fun publishContentUpdated(
        contentId: String,
        userId: String,
        updatedFields: List<String>,
        previousStatus: ContentStatus? = null,
        newStatus: ContentStatus? = null
    ): EventPublicationResult {
        val event = ContentUpdatedEvent(
            eventId = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            userId = userId,
            contentId = contentId,
            updatedFields = updatedFields,
            previousStatus = previousStatus,
            newStatus = newStatus
        )
        
        return publishEvent(event)
    }

    /**
     * Publishes a content deleted event
     */
    suspend fun publishContentDeleted(
        contentId: String,
        userId: String,
        deletionType: DeletionType,
        reason: String? = null
    ): EventPublicationResult {
        val event = ContentDeletedEvent(
            eventId = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            userId = userId,
            contentId = contentId,
            deletionType = deletionType,
            reason = reason
        )
        
        return publishEvent(event)
    }

    /**
     * Publishes a content published event
     */
    suspend fun publishContentPublished(
        contentId: String,
        userId: String,
        contentType: ContentType,
        visibility: ContentVisibility,
        authorFollowersCount: Int? = null,
        isScheduledPost: Boolean = false
    ): EventPublicationResult {
        val event = ContentPublishedEvent(
            eventId = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            userId = userId,
            contentId = contentId,
            contentType = contentType,
            visibility = visibility,
            authorFollowersCount = authorFollowersCount,
            isScheduledPost = isScheduledPost
        )
        
        return publishEvent(event)
    }

    /**
     * Publishes a media processed event
     */
    suspend fun publishMediaProcessed(
        contentId: String,
        userId: String,
        processedMedia: ProcessedMedia
    ): EventPublicationResult {
        val event = MediaProcessedEvent(
            eventId = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            userId = userId,
            contentId = contentId,
            mediaId = UUID.randomUUID().toString(), // Would be actual media ID
            mediaType = processedMedia.originalAttachment.mediaType,
            processingStatus = processedMedia.processingStatus,
            processingDurationMs = processedMedia.processingDetails?.processingDurationMs,
            fileSizeBytes = processedMedia.metadata.fileSize,
            thumbnailGenerated = processedMedia.thumbnailUrl != null
        )
        
        return publishEvent(event)
    }

    /**
     * Publishes a content metrics updated event
     */
    suspend fun publishMetricsUpdated(
        contentId: String,
        userId: String,
        metricsType: MetricsType,
        newValue: Int,
        previousValue: Int,
        engagementType: String? = null
    ): EventPublicationResult {
        val event = ContentMetricsUpdatedEvent(
            eventId = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            userId = userId,
            contentId = contentId,
            metricsType = metricsType,
            newValue = newValue,
            previousValue = previousValue,
            engagementType = engagementType
        )
        
        return publishEvent(event)
    }

    /**
     * Publishes multiple events in batch
     */
    suspend fun publishEventsBatch(events: List<ContentEvent>): List<EventPublicationResult> {
        return events.map { event ->
            try {
                publishEvent(event)
            } catch (e: Exception) {
                EventPublicationResult(
                    success = false,
                    eventId = event.eventId,
                    publishedAt = Instant.now(),
                    error = mapExceptionToError(e)
                )
            }
        }
    }

    /**
     * Publishes event with retry logic
     */
    suspend fun publishEventWithRetry(
        event: ContentEvent, 
        maxAttempts: Int = MAX_RETRY_ATTEMPTS
    ): EventPublicationResult {
        var lastError: PublicationError? = null
        
        repeat(maxAttempts) { attempt ->
            val result = publishEvent(event)
            
            if (result.success) {
                return result.copy(retryCount = attempt)
            }
            
            lastError = result.error
            
            // Only retry if error is retryable
            if (result.error?.retryable != true) {
                return result
            }
            
            // Wait before retrying (exponential backoff)
            if (attempt < maxAttempts - 1) {
                val delayMs = INITIAL_RETRY_DELAY_MS * (1L shl attempt)
                kotlinx.coroutines.delay(delayMs)
            }
        }
        
        return EventPublicationResult(
            success = false,
            eventId = event.eventId,
            publishedAt = Instant.now(),
            error = lastError,
            retryCount = maxAttempts
        )
    }

    private suspend fun sendEventToKafka(
        topic: String, 
        key: String, 
        message: String
    ): SendResult<String, String> = suspendCoroutine { continuation ->
        val future = kafkaTemplate.send(topic, key, message)
        
        future.whenComplete { result, throwable ->
            when {
                throwable != null -> continuation.resumeWithException(throwable)
                result != null -> continuation.resume(result)
                else -> continuation.resumeWithException(RuntimeException("Null result from Kafka"))
            }
        }
    }

    private fun selectTopicForEvent(event: ContentEvent): String {
        return when (event.eventType) {
            ContentEventType.CONTENT_CREATED,
            ContentEventType.CONTENT_UPDATED,
            ContentEventType.CONTENT_DELETED,
            ContentEventType.CONTENT_PUBLISHED -> CONTENT_EVENTS_TOPIC
            
            ContentEventType.MEDIA_PROCESSED -> MEDIA_EVENTS_TOPIC
            
            ContentEventType.CONTENT_METRICS_UPDATED -> METRICS_EVENTS_TOPIC
        }
    }

    private fun buildEventMetadata(
        content: StoredContent, 
        extractedMetadata: ExtractedMetadata?
    ): Map<String, Any> {
        val metadata = mutableMapOf<String, Any>()
        
        metadata["contentLength"] = content.textContent?.length ?: 0
        metadata["hasLinkUrl"] = content.linkUrl != null
        metadata["isPoll"] = content.pollData != null
        metadata["mediaCount"] = content.mediaAttachments.size
        
        extractedMetadata?.let { meta ->
            metadata["keywordCount"] = meta.keywords.size
            metadata["entityCount"] = meta.entities.size
            metadata["linkCount"] = meta.links.size
            metadata["readabilityScore"] = meta.contentMetrics.readabilityScore ?: 0.0
            metadata["wordCount"] = meta.contentMetrics.wordCount
            metadata["sentimentConfidence"] = meta.sentiment?.confidence ?: 0.0
        }
        
        return metadata
    }

    private fun mapExceptionToError(exception: Exception): PublicationError {
        return when (exception) {
            is org.springframework.kafka.KafkaException -> PublicationError(
                code = PublicationErrorCode.BROKER_UNAVAILABLE,
                message = "Kafka broker is unavailable",
                details = exception.message,
                retryable = true
            )
            
            is com.fasterxml.jackson.core.JsonProcessingException -> PublicationError(
                code = PublicationErrorCode.SERIALIZATION_ERROR,
                message = "Failed to serialize event to JSON",
                details = exception.message,
                retryable = false
            )
            
            is java.net.ConnectException -> PublicationError(
                code = PublicationErrorCode.NETWORK_ERROR,
                message = "Network connection failed",
                details = exception.message,
                retryable = true
            )
            
            is SecurityException -> PublicationError(
                code = PublicationErrorCode.AUTHENTICATION_ERROR,
                message = "Authentication failed",
                details = exception.message,
                retryable = false
            )
            
            else -> PublicationError(
                code = PublicationErrorCode.UNKNOWN_ERROR,
                message = "Unknown error occurred",
                details = exception.message,
                retryable = true
            )
        }
    }
}