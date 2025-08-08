package com.welcomer.welcome.ingestion.service

import com.welcomer.welcome.ingestion.model.*
import com.welcomer.welcome.ingestion.repository.ContentRepository
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * Main service for content ingestion orchestrating validation, processing, and storage
 */
@Service
class ContentIngestionService(
    private val contentValidator: ContentValidator,
    private val mediaProcessor: MediaProcessor,
    private val metadataExtractor: MetadataExtractor,
    private val contentRepository: ContentRepository,
    private val eventPublisher: EventPublisher
) {

    /**
     * Ingests new content submission
     */
    @Transactional
    @CircuitBreaker(name = "content-ingestion", fallbackMethod = "fallbackIngestContent")
    @Retry(name = "content-ingestion")
    suspend fun ingestContent(
        submission: ContentSubmission,
        userId: String
    ): ContentIngestionResult {
        
        try {
            // Step 1: Validate content submission
            val validationResult = contentValidator.validate(submission, userId)
            if (!validationResult.isValid) {
                return ContentIngestionResult.failure(
                    errors = validationResult.errors.map { 
                        IngestionError(it.code.name, it.message) 
                    }
                )
            }

            // Step 2: Sanitize text content if present
            val sanitizationResult = submission.textContent?.let { content ->
                contentValidator.sanitize(content)
            }

            // Step 3: Process media attachments if any
            val processedMedia = submission.mediaAttachments?.mapNotNull { attachment ->
                try {
                    mediaProcessor.processMedia(attachment)
                } catch (e: Exception) {
                    // Log error but continue processing other media
                    null
                }
            } ?: emptyList()

            // Step 4: Extract metadata from content
            val extractedMetadata = metadataExtractor.extractMetadata(submission)

            // Step 5: Extract link metadata if content contains links
            val enhancedMetadata = enhanceLinkMetadata(extractedMetadata)

            // Step 6: Create content creation request
            val creationRequest = ContentCreationRequest(
                authorId = userId,
                submission = submission,
                processedMedia = processedMedia,
                extractedMetadata = enhancedMetadata,
                sanitizedContent = sanitizationResult?.sanitizedContent
            )

            // Step 7: Store content in database
            val storedContent = contentRepository.save(creationRequest)

            // Step 8: Publish content created event
            val eventResult = eventPublisher.publishContentCreated(
                contentId = storedContent.id,
                userId = userId,
                content = storedContent,
                extractedMetadata = enhancedMetadata
            )

            // Step 9: Publish media processed events if applicable
            processedMedia.forEach { media ->
                eventPublisher.publishMediaProcessed(
                    contentId = storedContent.id,
                    userId = userId,
                    processedMedia = media
                )
            }

            // Step 10: Publish content published event if content is immediately visible
            if (storedContent.status == ContentStatus.PUBLISHED) {
                eventPublisher.publishContentPublished(
                    contentId = storedContent.id,
                    userId = userId,
                    contentType = storedContent.contentType,
                    visibility = storedContent.visibility,
                    isScheduledPost = submission.scheduledAt != null
                )
            }

            return ContentIngestionResult.success(
                contentId = storedContent.id,
                processedMedia = processedMedia,
                extractedMetadata = enhancedMetadata,
                sanitizationResult = sanitizationResult,
                warnings = validationResult.warnings.map { 
                    IngestionWarning(it.code.name, it.message) 
                }
            )

        } catch (e: Exception) {
            return ContentIngestionResult.failure(
                errors = listOf(
                    IngestionError(
                        code = "INGESTION_ERROR",
                        message = "Unexpected error during content ingestion: ${e.message}"
                    )
                )
            )
        }
    }

    /**
     * Updates existing content
     */
    @Transactional
    suspend fun updateContent(
        contentId: String,
        updateRequest: ContentUpdateRequest,
        userId: String
    ): ContentUpdateResult {
        
        try {
            // Verify content exists and user has permission
            val existingContent = contentRepository.findById(contentId)
                ?: return ContentUpdateResult.failure("Content not found")

            if (existingContent.authorId != userId) {
                return ContentUpdateResult.failure("Unauthorized to update this content")
            }

            // Validate update request
            val validationErrors = validateUpdateRequest(updateRequest)
            if (validationErrors.isNotEmpty()) {
                return ContentUpdateResult.failure(
                    message = "Validation failed",
                    errors = validationErrors
                )
            }

            // Perform update
            val updatedContent = contentRepository.update(updateRequest)
                ?: return ContentUpdateResult.failure("Failed to update content")

            // Determine what fields were updated
            val updatedFields = determineUpdatedFields(existingContent, updatedContent)

            // Publish content updated event
            eventPublisher.publishContentUpdated(
                contentId = contentId,
                userId = userId,
                updatedFields = updatedFields,
                previousStatus = existingContent.status,
                newStatus = updatedContent.status
            )

            return ContentUpdateResult.success(
                content = updatedContent,
                updatedFields = updatedFields
            )

        } catch (e: Exception) {
            return ContentUpdateResult.failure("Update failed: ${e.message}")
        }
    }

    /**
     * Deletes content (soft delete by default)
     */
    @Transactional
    suspend fun deleteContent(
        contentId: String,
        userId: String,
        hardDelete: Boolean = false
    ): ContentDeletionResult {
        
        try {
            val existingContent = contentRepository.findById(contentId)
                ?: return ContentDeletionResult.failure("Content not found")

            if (existingContent.authorId != userId) {
                return ContentDeletionResult.failure("Unauthorized to delete this content")
            }

            val success = if (hardDelete) {
                contentRepository.delete(contentId)
            } else {
                contentRepository.softDelete(contentId)
            }

            if (!success) {
                return ContentDeletionResult.failure("Failed to delete content")
            }

            // Publish content deleted event
            eventPublisher.publishContentDeleted(
                contentId = contentId,
                userId = userId,
                deletionType = if (hardDelete) DeletionType.HARD_DELETE else DeletionType.SOFT_DELETE
            )

            return ContentDeletionResult.success(
                contentId = contentId,
                deletionType = if (hardDelete) DeletionType.HARD_DELETE else DeletionType.SOFT_DELETE
            )

        } catch (e: Exception) {
            return ContentDeletionResult.failure("Deletion failed: ${e.message}")
        }
    }

    /**
     * Retrieves content by ID
     */
    suspend fun getContent(contentId: String, userId: String? = null): StoredContent? {
        val content = contentRepository.findById(contentId)
        
        // Apply visibility rules
        return when {
            content == null -> null
            content.visibility == ContentVisibility.PUBLIC -> content
            content.visibility == ContentVisibility.PRIVATE && content.authorId != userId -> null
            content.status == ContentStatus.DELETED -> null
            else -> content
        }
    }

    /**
     * Retrieves user's content with filtering
     */
    suspend fun getUserContent(
        userId: String,
        filters: ContentQueryFilters? = null,
        limit: Int = 20,
        cursor: String? = null
    ): ContentPage {
        return contentRepository.findByAuthor(userId, filters, limit, cursor)
    }

    /**
     * Searches content with filters
     */
    suspend fun searchContent(
        filters: ContentQueryFilters,
        limit: Int = 20,
        cursor: String? = null
    ): ContentPage {
        return contentRepository.findByFilters(filters, limit, cursor)
    }

    // Private helper methods

    private suspend fun enhanceLinkMetadata(metadata: ExtractedMetadata): ExtractedMetadata {
        val enhancedLinks = metadata.links.map { link ->
            try {
                val linkMetadata = metadataExtractor.extractLinkMetadata(link.url)
                link.copy(metadata = linkMetadata)
            } catch (e: Exception) {
                // If link metadata extraction fails, return original link
                link
            }
        }

        return metadata.copy(links = enhancedLinks)
    }

    private fun validateUpdateRequest(request: ContentUpdateRequest): List<IngestionError> {
        val errors = mutableListOf<IngestionError>()

        // Validate text content length if provided
        if (request.textContent != null && request.textContent.length > 10000) {
            errors.add(
                IngestionError(
                    code = "CONTENT_TOO_LONG",
                    message = "Content exceeds maximum length of 10000 characters"
                )
            )
        }

        // Validate scheduled time if provided
        if (request.scheduledAt != null && request.scheduledAt.isBefore(Instant.now())) {
            errors.add(
                IngestionError(
                    code = "INVALID_SCHEDULE_TIME",
                    message = "Scheduled time cannot be in the past"
                )
            )
        }

        return errors
    }

    private fun determineUpdatedFields(
        existing: StoredContent,
        updated: StoredContent
    ): List<String> {
        val updatedFields = mutableListOf<String>()

        if (existing.textContent != updated.textContent) updatedFields.add("textContent")
        if (existing.visibility != updated.visibility) updatedFields.add("visibility")
        if (existing.isSensitive != updated.isSensitive) updatedFields.add("isSensitive")
        if (existing.status != updated.status) updatedFields.add("status")
        if (existing.tags != updated.tags) updatedFields.add("tags")
        if (existing.publishedAt != updated.publishedAt) updatedFields.add("scheduledAt")

        return updatedFields
    }

    // Fallback method for circuit breaker
    suspend fun fallbackIngestContent(
        submission: ContentSubmission,
        userId: String,
        exception: Exception
    ): ContentIngestionResult {
        return ContentIngestionResult.failure(
            errors = listOf(
                IngestionError(
                    code = "SERVICE_UNAVAILABLE",
                    message = "Content ingestion service is temporarily unavailable. Please try again later."
                )
            )
        )
    }
}

/**
 * Result of content ingestion operation
 */
data class ContentIngestionResult(
    val success: Boolean,
    val contentId: String? = null,
    val processedMedia: List<ProcessedMedia> = emptyList(),
    val extractedMetadata: ExtractedMetadata? = null,
    val sanitizationResult: SanitizationResult? = null,
    val warnings: List<IngestionWarning> = emptyList(),
    val errors: List<IngestionError> = emptyList(),
    val timestamp: Instant = Instant.now()
) {
    companion object {
        fun success(
            contentId: String,
            processedMedia: List<ProcessedMedia> = emptyList(),
            extractedMetadata: ExtractedMetadata? = null,
            sanitizationResult: SanitizationResult? = null,
            warnings: List<IngestionWarning> = emptyList()
        ) = ContentIngestionResult(
            success = true,
            contentId = contentId,
            processedMedia = processedMedia,
            extractedMetadata = extractedMetadata,
            sanitizationResult = sanitizationResult,
            warnings = warnings
        )

        fun failure(
            errors: List<IngestionError>,
            warnings: List<IngestionWarning> = emptyList()
        ) = ContentIngestionResult(
            success = false,
            errors = errors,
            warnings = warnings
        )
    }
}

/**
 * Result of content update operation
 */
data class ContentUpdateResult(
    val success: Boolean,
    val content: StoredContent? = null,
    val updatedFields: List<String> = emptyList(),
    val message: String? = null,
    val errors: List<IngestionError> = emptyList()
) {
    companion object {
        fun success(
            content: StoredContent,
            updatedFields: List<String>
        ) = ContentUpdateResult(
            success = true,
            content = content,
            updatedFields = updatedFields
        )

        fun failure(
            message: String,
            errors: List<IngestionError> = emptyList()
        ) = ContentUpdateResult(
            success = false,
            message = message,
            errors = errors
        )
    }
}

/**
 * Result of content deletion operation
 */
data class ContentDeletionResult(
    val success: Boolean,
    val contentId: String? = null,
    val deletionType: DeletionType? = null,
    val message: String? = null
) {
    companion object {
        fun success(
            contentId: String,
            deletionType: DeletionType
        ) = ContentDeletionResult(
            success = true,
            contentId = contentId,
            deletionType = deletionType
        )

        fun failure(message: String) = ContentDeletionResult(
            success = false,
            message = message
        )
    }
}

/**
 * Content ingestion error
 */
data class IngestionError(
    val code: String,
    val message: String
)

/**
 * Content ingestion warning
 */
data class IngestionWarning(
    val code: String,
    val message: String
)