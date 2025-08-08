package com.welcomer.welcome.ingestion.model

import java.time.Instant

/**
 * Result of media processing operations
 */
data class ProcessedMedia(
    val originalAttachment: MediaAttachment,
    val processedFileUrl: String,
    val thumbnailUrl: String? = null,
    val metadata: MediaMetadata,
    val processingStatus: MediaProcessingStatus,
    val processingDetails: ProcessingDetails? = null,
    val variants: List<MediaVariant> = emptyList()
)

/**
 * Detailed metadata extracted from media files
 */
data class MediaMetadata(
    val mimeType: String,
    val fileSize: Long,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null,
    val bitRate: Int? = null,
    val frameRate: Double? = null,
    val colorSpace: String? = null,
    val hasAudio: Boolean? = null,
    val audioCodec: String? = null,
    val videoCodec: String? = null,
    val exifData: Map<String, Any>? = null,
    val checksumMd5: String? = null,
    val extractedAt: Instant = Instant.now()
)

/**
 * Media processing status
 */
enum class MediaProcessingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    VALIDATION_FAILED,
    VIRUS_DETECTED
}

/**
 * Details about the processing operation
 */
data class ProcessingDetails(
    val processingStartedAt: Instant,
    val processingCompletedAt: Instant? = null,
    val processingDurationMs: Long? = null,
    val operationsPerformed: List<ProcessingOperation>,
    val warnings: List<ProcessingWarning> = emptyList(),
    val errors: List<ProcessingError> = emptyList()
)

/**
 * Individual processing operation
 */
data class ProcessingOperation(
    val type: ProcessingOperationType,
    val description: String,
    val inputSize: Long? = null,
    val outputSize: Long? = null,
    val parameters: Map<String, Any> = emptyMap()
)

/**
 * Processing warning (non-critical issues)
 */
data class ProcessingWarning(
    val code: ProcessingWarningCode,
    val message: String,
    val details: String? = null
)

/**
 * Processing error
 */
data class ProcessingError(
    val code: ProcessingErrorCode,
    val message: String,
    val details: String? = null,
    val exception: String? = null
)

/**
 * Different variants of processed media
 */
data class MediaVariant(
    val variantType: MediaVariantType,
    val fileUrl: String,
    val width: Int? = null,
    val height: Int? = null,
    val fileSize: Long,
    val quality: MediaQuality
)

/**
 * Types of processing operations
 */
enum class ProcessingOperationType {
    FORMAT_CONVERSION,
    RESIZE,
    COMPRESS,
    THUMBNAIL_GENERATION,
    METADATA_EXTRACTION,
    VIRUS_SCAN,
    CONTENT_ANALYSIS,
    WATERMARK_ADDITION,
    OPTIMIZATION
}

/**
 * Processing warning codes
 */
enum class ProcessingWarningCode {
    LOW_QUALITY_INPUT,
    LARGE_FILE_SIZE,
    UNUSUAL_ASPECT_RATIO,
    MISSING_METADATA,
    POTENTIAL_COPYRIGHT_CONTENT,
    COMPRESSION_ARTIFACTS
}

/**
 * Processing error codes
 */
enum class ProcessingErrorCode {
    UNSUPPORTED_FORMAT,
    CORRUPTED_FILE,
    FILE_TOO_LARGE,
    PROCESSING_TIMEOUT,
    INSUFFICIENT_RESOURCES,
    VIRUS_DETECTED,
    INVALID_MEDIA_TYPE,
    NETWORK_ERROR
}

/**
 * Types of media variants
 */
enum class MediaVariantType {
    ORIGINAL,
    THUMBNAIL_SMALL,    // 150x150
    THUMBNAIL_MEDIUM,   // 300x300
    THUMBNAIL_LARGE,    // 600x600
    COMPRESSED_LOW,     // Low quality for mobile
    COMPRESSED_MEDIUM,  // Medium quality
    COMPRESSED_HIGH,    // High quality
    PREVIEW,           // For videos
    POSTER_FRAME      // Single frame from video
}

/**
 * Media quality levels
 */
enum class MediaQuality {
    LOW,
    MEDIUM,
    HIGH,
    LOSSLESS
}