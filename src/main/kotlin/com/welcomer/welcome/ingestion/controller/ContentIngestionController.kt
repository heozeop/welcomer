package com.welcomer.welcome.ingestion.controller

import com.welcomer.welcome.ingestion.model.*
import com.welcomer.welcome.ingestion.service.ContentIngestionService
import com.welcomer.welcome.ingestion.service.ContentIngestionResult
import com.welcomer.welcome.ingestion.service.ContentUpdateResult
import com.welcomer.welcome.ingestion.service.ContentDeletionResult
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for content ingestion operations
 */
@RestController
@RequestMapping("/api/v1/content")
class ContentIngestionController(
    private val contentIngestionService: ContentIngestionService
) {

    /**
     * Creates new content
     */
    @PostMapping
    suspend fun createContent(
        @Valid @RequestBody submission: ContentSubmission,
        @RequestHeader("X-User-ID") userId: String
    ): ResponseEntity<ContentIngestionResponse> {
        
        val result = contentIngestionService.ingestContent(submission, userId)
        
        return if (result.success) {
            ResponseEntity.status(HttpStatus.CREATED).body(
                ContentIngestionResponse.success(result)
            )
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ContentIngestionResponse.failure(result)
            )
        }
    }

    /**
     * Updates existing content
     */
    @PutMapping("/{contentId}")
    suspend fun updateContent(
        @PathVariable contentId: String,
        @Valid @RequestBody updateRequest: ContentUpdateRequest,
        @RequestHeader("X-User-ID") userId: String
    ): ResponseEntity<ContentUpdateResponse> {
        
        val result = contentIngestionService.updateContent(contentId, updateRequest, userId)
        
        return if (result.success) {
            ResponseEntity.ok(ContentUpdateResponse.success(result))
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ContentUpdateResponse.failure(result)
            )
        }
    }

    /**
     * Deletes content
     */
    @DeleteMapping("/{contentId}")
    suspend fun deleteContent(
        @PathVariable contentId: String,
        @RequestParam(defaultValue = "false") hardDelete: Boolean,
        @RequestHeader("X-User-ID") userId: String
    ): ResponseEntity<ContentDeletionResponse> {
        
        val result = contentIngestionService.deleteContent(contentId, userId, hardDelete)
        
        return if (result.success) {
            ResponseEntity.ok(ContentDeletionResponse.success(result))
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ContentDeletionResponse.failure(result)
            )
        }
    }

    /**
     * Retrieves content by ID
     */
    @GetMapping("/{contentId}")
    suspend fun getContent(
        @PathVariable contentId: String,
        @RequestHeader(value = "X-User-ID", required = false) userId: String?
    ): ResponseEntity<ContentResponse> {
        
        val content = contentIngestionService.getContent(contentId, userId)
        
        return if (content != null) {
            ResponseEntity.ok(ContentResponse.success(content))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ContentResponse.failure("Content not found")
            )
        }
    }

    /**
     * Retrieves user's content
     */
    @GetMapping("/users/{userId}")
    suspend fun getUserContent(
        @PathVariable userId: String,
        @RequestParam(required = false) contentType: ContentType?,
        @RequestParam(required = false) status: ContentStatus?,
        @RequestParam(required = false) visibility: ContentVisibility?,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<ContentPageResponse> {
        
        val filters = ContentQueryFilters(
            contentType = contentType,
            status = status,
            visibility = visibility
        )
        
        val result = contentIngestionService.getUserContent(userId, filters, limit, cursor)
        
        return ResponseEntity.ok(ContentPageResponse.success(result))
    }

    /**
     * Searches content
     */
    @GetMapping("/search")
    suspend fun searchContent(
        @RequestParam(required = false) contentType: ContentType?,
        @RequestParam(required = false) authorId: String?,
        @RequestParam(required = false) tags: List<String>?,
        @RequestParam(required = false) language: String?,
        @RequestParam(required = false) hasMedia: Boolean?,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<ContentPageResponse> {
        
        val filters = ContentQueryFilters(
            contentType = contentType,
            authorId = authorId,
            tags = tags,
            languageCode = language,
            hasMedia = hasMedia
        )
        
        val result = contentIngestionService.searchContent(filters, limit, cursor)
        
        return ResponseEntity.ok(ContentPageResponse.success(result))
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    suspend fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "healthy"))
    }
}

/**
 * Response wrapper for content ingestion
 */
data class ContentIngestionResponse(
    val success: Boolean,
    val data: ContentIngestionData? = null,
    val error: ErrorDetails? = null
) {
    companion object {
        fun success(result: ContentIngestionResult) = ContentIngestionResponse(
            success = true,
            data = ContentIngestionData(
                contentId = result.contentId!!,
                processedMediaCount = result.processedMedia.size,
                extractedKeywords = result.extractedMetadata?.keywords ?: emptyList(),
                detectedLanguage = result.extractedMetadata?.language?.detectedLanguage,
                sentiment = result.extractedMetadata?.sentiment?.overallSentiment?.name,
                warnings = result.warnings.map { "${it.code}: ${it.message}" }
            )
        )

        fun failure(result: ContentIngestionResult) = ContentIngestionResponse(
            success = false,
            error = ErrorDetails(
                message = "Content ingestion failed",
                errors = result.errors.map { "${it.code}: ${it.message}" },
                warnings = result.warnings.map { "${it.code}: ${it.message}" }
            )
        )
    }
}

/**
 * Response wrapper for content updates
 */
data class ContentUpdateResponse(
    val success: Boolean,
    val data: ContentUpdateData? = null,
    val error: ErrorDetails? = null
) {
    companion object {
        fun success(result: ContentUpdateResult) = ContentUpdateResponse(
            success = true,
            data = ContentUpdateData(
                contentId = result.content!!.id,
                updatedFields = result.updatedFields
            )
        )

        fun failure(result: ContentUpdateResult) = ContentUpdateResponse(
            success = false,
            error = ErrorDetails(
                message = result.message ?: "Update failed",
                errors = result.errors.map { "${it.code}: ${it.message}" }
            )
        )
    }
}

/**
 * Response wrapper for content deletion
 */
data class ContentDeletionResponse(
    val success: Boolean,
    val data: ContentDeletionData? = null,
    val error: ErrorDetails? = null
) {
    companion object {
        fun success(result: ContentDeletionResult) = ContentDeletionResponse(
            success = true,
            data = ContentDeletionData(
                contentId = result.contentId!!,
                deletionType = result.deletionType!!.name
            )
        )

        fun failure(result: ContentDeletionResult) = ContentDeletionResponse(
            success = false,
            error = ErrorDetails(
                message = result.message ?: "Deletion failed",
                errors = emptyList()
            )
        )
    }
}

/**
 * Response wrapper for content retrieval
 */
data class ContentResponse(
    val success: Boolean,
    val data: StoredContent? = null,
    val error: ErrorDetails? = null
) {
    companion object {
        fun success(content: StoredContent) = ContentResponse(
            success = true,
            data = content
        )

        fun failure(message: String) = ContentResponse(
            success = false,
            error = ErrorDetails(message = message, errors = emptyList())
        )
    }
}

/**
 * Response wrapper for content page results
 */
data class ContentPageResponse(
    val success: Boolean,
    val data: ContentPage? = null,
    val error: ErrorDetails? = null
) {
    companion object {
        fun success(page: ContentPage) = ContentPageResponse(
            success = true,
            data = page
        )
    }
}

/**
 * Content ingestion response data
 */
data class ContentIngestionData(
    val contentId: String,
    val processedMediaCount: Int,
    val extractedKeywords: List<String>,
    val detectedLanguage: String?,
    val sentiment: String?,
    val warnings: List<String>
)

/**
 * Content update response data
 */
data class ContentUpdateData(
    val contentId: String,
    val updatedFields: List<String>
)

/**
 * Content deletion response data
 */
data class ContentDeletionData(
    val contentId: String,
    val deletionType: String
)

/**
 * Error details
 */
data class ErrorDetails(
    val message: String,
    val errors: List<String>,
    val warnings: List<String> = emptyList()
)