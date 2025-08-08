package com.welcomer.welcome.ingestion.model

import java.time.Instant

/**
 * Content entity as stored in the database
 */
data class StoredContent(
    val id: String,
    val authorId: String,
    val contentType: ContentType,
    val textContent: String? = null,
    val linkUrl: String? = null,
    val linkTitle: String? = null,
    val linkDescription: String? = null,
    val linkImageUrl: String? = null,
    val pollData: String? = null, // JSON serialized PollData
    val visibility: ContentVisibility,
    val status: ContentStatus,
    val replyToId: String? = null,
    val replyCount: Int = 0,
    val isSensitive: Boolean = false,
    val languageCode: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val publishedAt: Instant? = null,
    val deletedAt: Instant? = null,
    val mediaAttachments: List<StoredMediaAttachment> = emptyList(),
    val tags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val extractedMetadata: ExtractedMetadata? = null
)

/**
 * Media attachment entity as stored in the database
 */
data class StoredMediaAttachment(
    val id: String,
    val contentId: String,
    val mediaType: MediaType,
    val originalFilename: String? = null,
    val fileUrl: String,
    val thumbnailUrl: String? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null,
    val altText: String? = null,
    val processingStatus: MediaProcessingStatus,
    val metadata: String? = null, // JSON serialized MediaMetadata
    val displayOrder: Int = 0,
    val createdAt: Instant,
    val processedAt: Instant? = null
)

/**
 * Content status in the system
 */
enum class ContentStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED,
    DELETED,
    SCHEDULED,
    PROCESSING,
    FAILED
}

/**
 * Content creation request
 */
data class ContentCreationRequest(
    val authorId: String,
    val submission: ContentSubmission,
    val processedMedia: List<ProcessedMedia> = emptyList(),
    val extractedMetadata: ExtractedMetadata? = null,
    val sanitizedContent: String? = null
)

/**
 * Content update request
 */
data class ContentUpdateRequest(
    val contentId: String,
    val textContent: String? = null,
    val visibility: ContentVisibility? = null,
    val isSensitive: Boolean? = null,
    val status: ContentStatus? = null,
    val tags: List<String>? = null,
    val scheduledAt: Instant? = null
)

/**
 * Content query filters
 */
data class ContentQueryFilters(
    val authorId: String? = null,
    val contentType: ContentType? = null,
    val visibility: ContentVisibility? = null,
    val status: ContentStatus? = null,
    val languageCode: String? = null,
    val tags: List<String>? = null,
    val createdAfter: Instant? = null,
    val createdBefore: Instant? = null,
    val hasMedia: Boolean? = null,
    val isSensitive: Boolean? = null,
    val replyToId: String? = null
)

/**
 * Paginated content result
 */
data class ContentPage(
    val content: List<StoredContent>,
    val totalCount: Long,
    val hasNext: Boolean,
    val nextCursor: String? = null,
    val pageSize: Int
)

/**
 * Content statistics
 */
data class ContentStats(
    val contentId: String,
    val likesCount: Int = 0,
    val dislikesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val viewsCount: Int = 0,
    val uniqueViewsCount: Int = 0,
    val reachCount: Int = 0,
    val engagementRate: Double = 0.0,
    val viralScore: Double = 0.0,
    val updatedAt: Instant
)