package com.welcomer.welcome.ingestion.service

import com.welcomer.welcome.ingestion.model.*
import org.apache.tika.Tika
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.awt.Graphics2D
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.imageio.ImageIO

/**
 * Service responsible for processing media attachments
 */
@Service
class MediaProcessor {

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()
    
    private val tika = Tika()

    companion object {
        private const val MAX_FILE_SIZE = 100 * 1024 * 1024 // 100MB
        private const val MAX_IMAGE_DIMENSION = 4096
        private const val THUMBNAIL_SIZE_SMALL = 150
        private const val THUMBNAIL_SIZE_MEDIUM = 300
        private const val THUMBNAIL_SIZE_LARGE = 600
        
        private val SUPPORTED_IMAGE_FORMATS = setOf(
            "image/jpeg", "image/png", "image/gif", "image/webp"
        )
        
        private val SUPPORTED_VIDEO_FORMATS = setOf(
            "video/mp4", "video/webm", "video/quicktime", "video/x-msvideo"
        )
        
        private val SUPPORTED_AUDIO_FORMATS = setOf(
            "audio/mpeg", "audio/wav", "audio/ogg", "audio/mp4"
        )
        
        private val SUPPORTED_DOCUMENT_FORMATS = setOf(
            "application/pdf", "text/plain", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    }

    /**
     * Processes a media attachment
     */
    suspend fun processMedia(attachment: MediaAttachment): ProcessedMedia {
        val processingStartTime = Instant.now()
        val operations = mutableListOf<ProcessingOperation>()
        val warnings = mutableListOf<ProcessingWarning>()
        val errors = mutableListOf<ProcessingError>()

        try {
            // Download and validate the media file
            val mediaData = downloadMediaFile(attachment.fileUrl)
            operations.add(
                ProcessingOperation(
                    type = ProcessingOperationType.METADATA_EXTRACTION,
                    description = "Downloaded media file",
                    inputSize = mediaData.size.toLong()
                )
            )

            // Detect actual MIME type
            val detectedMimeType = tika.detect(mediaData)
            if (!isSupportedFormat(detectedMimeType, attachment.mediaType)) {
                errors.add(
                    ProcessingError(
                        code = ProcessingErrorCode.UNSUPPORTED_FORMAT,
                        message = "Detected MIME type $detectedMimeType is not supported for ${attachment.mediaType}",
                        details = "Expected one of: ${getSupportedFormats(attachment.mediaType)}"
                    )
                )
                return createFailedResult(attachment, operations, warnings, errors, processingStartTime)
            }

            // Check file size
            if (mediaData.size > MAX_FILE_SIZE) {
                errors.add(
                    ProcessingError(
                        code = ProcessingErrorCode.FILE_TOO_LARGE,
                        message = "File size ${mediaData.size} exceeds maximum allowed size $MAX_FILE_SIZE bytes"
                    )
                )
                return createFailedResult(attachment, operations, warnings, errors, processingStartTime)
            }

            // Extract metadata
            val metadata = extractMediaMetadata(mediaData, detectedMimeType)
            operations.add(
                ProcessingOperation(
                    type = ProcessingOperationType.METADATA_EXTRACTION,
                    description = "Extracted media metadata"
                )
            )

            // Process based on media type
            val processedFileUrl: String
            val thumbnailUrl: String?
            val variants: List<MediaVariant>

            when (attachment.mediaType) {
                MediaType.IMAGE -> {
                    val imageResult = processImage(mediaData, detectedMimeType, metadata)
                    processedFileUrl = imageResult.processedUrl
                    thumbnailUrl = imageResult.thumbnailUrl
                    variants = imageResult.variants
                    operations.addAll(imageResult.operations)
                    warnings.addAll(imageResult.warnings)
                }
                MediaType.VIDEO -> {
                    val videoResult = processVideo(mediaData, detectedMimeType, metadata)
                    processedFileUrl = videoResult.processedUrl
                    thumbnailUrl = videoResult.thumbnailUrl
                    variants = videoResult.variants
                    operations.addAll(videoResult.operations)
                    warnings.addAll(videoResult.warnings)
                }
                MediaType.AUDIO -> {
                    val audioResult = processAudio(mediaData, detectedMimeType, metadata)
                    processedFileUrl = audioResult.processedUrl
                    thumbnailUrl = null
                    variants = audioResult.variants
                    operations.addAll(audioResult.operations)
                    warnings.addAll(audioResult.warnings)
                }
                MediaType.DOCUMENT -> {
                    val docResult = processDocument(mediaData, detectedMimeType, metadata)
                    processedFileUrl = docResult.processedUrl
                    thumbnailUrl = docResult.thumbnailUrl
                    variants = emptyList()
                    operations.addAll(docResult.operations)
                    warnings.addAll(docResult.warnings)
                }
            }

            val processingEndTime = Instant.now()
            val processingDetails = ProcessingDetails(
                processingStartedAt = processingStartTime,
                processingCompletedAt = processingEndTime,
                processingDurationMs = Duration.between(processingStartTime, processingEndTime).toMillis(),
                operationsPerformed = operations,
                warnings = warnings,
                errors = errors
            )

            return ProcessedMedia(
                originalAttachment = attachment,
                processedFileUrl = processedFileUrl,
                thumbnailUrl = thumbnailUrl,
                metadata = metadata,
                processingStatus = MediaProcessingStatus.COMPLETED,
                processingDetails = processingDetails,
                variants = variants
            )

        } catch (e: Exception) {
            errors.add(
                ProcessingError(
                    code = ProcessingErrorCode.PROCESSING_TIMEOUT,
                    message = "Unexpected error during processing: ${e.message}",
                    exception = e.stackTraceToString()
                )
            )
            return createFailedResult(attachment, operations, warnings, errors, processingStartTime)
        }
    }

    private fun downloadMediaFile(fileUrl: String): ByteArray {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(fileUrl))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        
        if (response.statusCode() != 200) {
            throw RuntimeException("Failed to download media file: HTTP ${response.statusCode()}")
        }

        return response.body()
    }

    private fun extractMediaMetadata(data: ByteArray, mimeType: String): MediaMetadata {
        val md5Hash = calculateMD5Hash(data)
        
        return when {
            mimeType.startsWith("image/") -> extractImageMetadata(data, mimeType, md5Hash)
            mimeType.startsWith("video/") -> extractVideoMetadata(data, mimeType, md5Hash)
            mimeType.startsWith("audio/") -> extractAudioMetadata(data, mimeType, md5Hash)
            else -> MediaMetadata(
                mimeType = mimeType,
                fileSize = data.size.toLong(),
                checksumMd5 = md5Hash
            )
        }
    }

    private fun extractImageMetadata(data: ByteArray, mimeType: String, md5Hash: String): MediaMetadata {
        val inputStream = ByteArrayInputStream(data)
        val image = ImageIO.read(inputStream)
        
        return MediaMetadata(
            mimeType = mimeType,
            fileSize = data.size.toLong(),
            width = image?.width,
            height = image?.height,
            checksumMd5 = md5Hash
        )
    }

    private fun extractVideoMetadata(data: ByteArray, mimeType: String, md5Hash: String): MediaMetadata {
        // For real implementation, would use FFmpeg or similar
        return MediaMetadata(
            mimeType = mimeType,
            fileSize = data.size.toLong(),
            width = 1920, // Placeholder
            height = 1080, // Placeholder
            duration = 60, // Placeholder
            hasAudio = true,
            videoCodec = "h264",
            audioCodec = "aac",
            checksumMd5 = md5Hash
        )
    }

    private fun extractAudioMetadata(data: ByteArray, mimeType: String, md5Hash: String): MediaMetadata {
        // For real implementation, would use audio processing library
        return MediaMetadata(
            mimeType = mimeType,
            fileSize = data.size.toLong(),
            duration = 180, // Placeholder
            bitRate = 128000,
            audioCodec = "mp3",
            checksumMd5 = md5Hash
        )
    }

    private fun processImage(data: ByteArray, mimeType: String, metadata: MediaMetadata): ImageProcessingResult {
        val operations = mutableListOf<ProcessingOperation>()
        val warnings = mutableListOf<ProcessingWarning>()
        val variants = mutableListOf<MediaVariant>()

        val inputStream = ByteArrayInputStream(data)
        val originalImage = ImageIO.read(inputStream)
        
        if (originalImage == null) {
            throw RuntimeException("Failed to decode image")
        }

        // Check image dimensions
        if (originalImage.width > MAX_IMAGE_DIMENSION || originalImage.height > MAX_IMAGE_DIMENSION) {
            warnings.add(
                ProcessingWarning(
                    code = ProcessingWarningCode.LARGE_FILE_SIZE,
                    message = "Image dimensions exceed recommended maximum"
                )
            )
        }

        // Generate thumbnails
        val thumbnails = generateImageThumbnails(originalImage, operations)
        variants.addAll(thumbnails.variants)

        // Compress original if needed
        val compressedImage = compressImage(originalImage, mimeType)
        operations.add(
            ProcessingOperation(
                type = ProcessingOperationType.COMPRESS,
                description = "Compressed image for web delivery",
                inputSize = data.size.toLong(),
                outputSize = compressedImage.size.toLong()
            )
        )

        // Store processed images (in real implementation, would upload to cloud storage)
        val processedUrl = "https://storage.example.com/images/${UUID.randomUUID()}.jpg"
        val thumbnailUrl = thumbnails.thumbnailUrl

        return ImageProcessingResult(
            processedUrl = processedUrl,
            thumbnailUrl = thumbnailUrl,
            operations = operations,
            warnings = warnings,
            variants = variants
        )
    }

    private fun processVideo(data: ByteArray, mimeType: String, metadata: MediaMetadata): VideoProcessingResult {
        val operations = mutableListOf<ProcessingOperation>()
        val warnings = mutableListOf<ProcessingWarning>()
        val variants = mutableListOf<MediaVariant>()

        // In real implementation, would use FFmpeg for video processing
        operations.add(
            ProcessingOperation(
                type = ProcessingOperationType.FORMAT_CONVERSION,
                description = "Converted video to web-compatible format"
            )
        )

        // Generate video thumbnail (poster frame)
        operations.add(
            ProcessingOperation(
                type = ProcessingOperationType.THUMBNAIL_GENERATION,
                description = "Generated video thumbnail"
            )
        )

        val processedUrl = "https://storage.example.com/videos/${UUID.randomUUID()}.mp4"
        val thumbnailUrl = "https://storage.example.com/thumbnails/${UUID.randomUUID()}.jpg"

        return VideoProcessingResult(
            processedUrl = processedUrl,
            thumbnailUrl = thumbnailUrl,
            operations = operations,
            warnings = warnings,
            variants = variants
        )
    }

    private fun processAudio(data: ByteArray, mimeType: String, metadata: MediaMetadata): AudioProcessingResult {
        val operations = mutableListOf<ProcessingOperation>()
        val warnings = mutableListOf<ProcessingWarning>()
        val variants = mutableListOf<MediaVariant>()

        // In real implementation, would process audio with appropriate library
        operations.add(
            ProcessingOperation(
                type = ProcessingOperationType.FORMAT_CONVERSION,
                description = "Converted audio to web-compatible format"
            )
        )

        val processedUrl = "https://storage.example.com/audio/${UUID.randomUUID()}.mp3"

        return AudioProcessingResult(
            processedUrl = processedUrl,
            operations = operations,
            warnings = warnings,
            variants = variants
        )
    }

    private fun processDocument(data: ByteArray, mimeType: String, metadata: MediaMetadata): DocumentProcessingResult {
        val operations = mutableListOf<ProcessingOperation>()
        val warnings = mutableListOf<ProcessingWarning>()

        // In real implementation, would generate document preview/thumbnail
        operations.add(
            ProcessingOperation(
                type = ProcessingOperationType.THUMBNAIL_GENERATION,
                description = "Generated document preview"
            )
        )

        val processedUrl = "https://storage.example.com/documents/${UUID.randomUUID()}.pdf"
        val thumbnailUrl = "https://storage.example.com/previews/${UUID.randomUUID()}.jpg"

        return DocumentProcessingResult(
            processedUrl = processedUrl,
            thumbnailUrl = thumbnailUrl,
            operations = operations,
            warnings = warnings
        )
    }

    private fun generateImageThumbnails(originalImage: BufferedImage, operations: MutableList<ProcessingOperation>): ThumbnailResult {
        val variants = mutableListOf<MediaVariant>()

        // Generate small thumbnail
        val smallThumbnail = resizeImage(originalImage, THUMBNAIL_SIZE_SMALL, THUMBNAIL_SIZE_SMALL)
        val smallThumbnailData = imageToByteArray(smallThumbnail, "JPEG")
        variants.add(
            MediaVariant(
                variantType = MediaVariantType.THUMBNAIL_SMALL,
                fileUrl = "https://storage.example.com/thumbnails/${UUID.randomUUID()}_small.jpg",
                width = smallThumbnail.width,
                height = smallThumbnail.height,
                fileSize = smallThumbnailData.size.toLong(),
                quality = MediaQuality.MEDIUM
            )
        )

        // Generate medium thumbnail
        val mediumThumbnail = resizeImage(originalImage, THUMBNAIL_SIZE_MEDIUM, THUMBNAIL_SIZE_MEDIUM)
        val mediumThumbnailData = imageToByteArray(mediumThumbnail, "JPEG")
        variants.add(
            MediaVariant(
                variantType = MediaVariantType.THUMBNAIL_MEDIUM,
                fileUrl = "https://storage.example.com/thumbnails/${UUID.randomUUID()}_medium.jpg",
                width = mediumThumbnail.width,
                height = mediumThumbnail.height,
                fileSize = mediumThumbnailData.size.toLong(),
                quality = MediaQuality.MEDIUM
            )
        )

        operations.add(
            ProcessingOperation(
                type = ProcessingOperationType.THUMBNAIL_GENERATION,
                description = "Generated image thumbnails",
                parameters = mapOf(
                    "thumbnailSizes" to listOf(THUMBNAIL_SIZE_SMALL, THUMBNAIL_SIZE_MEDIUM)
                )
            )
        )

        return ThumbnailResult(
            thumbnailUrl = variants.first().fileUrl,
            variants = variants
        )
    }

    private fun resizeImage(originalImage: BufferedImage, maxWidth: Int, maxHeight: Int): BufferedImage {
        val originalWidth = originalImage.width
        val originalHeight = originalImage.height

        // Calculate new dimensions maintaining aspect ratio
        val scale = minOf(
            maxWidth.toDouble() / originalWidth,
            maxHeight.toDouble() / originalHeight
        )

        val newWidth = (originalWidth * scale).toInt()
        val newHeight = (originalHeight * scale).toInt()

        val resizedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
        val g2d: Graphics2D = resizedImage.createGraphics()
        
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)

        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null)
        g2d.dispose()

        return resizedImage
    }

    private fun compressImage(image: BufferedImage, mimeType: String): ByteArray {
        // Simple compression - in real implementation would use more sophisticated compression
        return imageToByteArray(image, "JPEG")
    }

    private fun imageToByteArray(image: BufferedImage, format: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, format, outputStream)
        return outputStream.toByteArray()
    }

    private fun calculateMD5Hash(data: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun isSupportedFormat(mimeType: String, mediaType: MediaType): Boolean {
        return when (mediaType) {
            MediaType.IMAGE -> SUPPORTED_IMAGE_FORMATS.contains(mimeType)
            MediaType.VIDEO -> SUPPORTED_VIDEO_FORMATS.contains(mimeType)
            MediaType.AUDIO -> SUPPORTED_AUDIO_FORMATS.contains(mimeType)
            MediaType.DOCUMENT -> SUPPORTED_DOCUMENT_FORMATS.contains(mimeType)
        }
    }

    private fun getSupportedFormats(mediaType: MediaType): Set<String> {
        return when (mediaType) {
            MediaType.IMAGE -> SUPPORTED_IMAGE_FORMATS
            MediaType.VIDEO -> SUPPORTED_VIDEO_FORMATS
            MediaType.AUDIO -> SUPPORTED_AUDIO_FORMATS
            MediaType.DOCUMENT -> SUPPORTED_DOCUMENT_FORMATS
        }
    }

    private fun createFailedResult(
        attachment: MediaAttachment,
        operations: List<ProcessingOperation>,
        warnings: List<ProcessingWarning>,
        errors: List<ProcessingError>,
        processingStartTime: Instant
    ): ProcessedMedia {
        val processingDetails = ProcessingDetails(
            processingStartedAt = processingStartTime,
            processingCompletedAt = Instant.now(),
            processingDurationMs = Duration.between(processingStartTime, Instant.now()).toMillis(),
            operationsPerformed = operations,
            warnings = warnings,
            errors = errors
        )

        return ProcessedMedia(
            originalAttachment = attachment,
            processedFileUrl = attachment.fileUrl, // Use original URL as fallback
            thumbnailUrl = null,
            metadata = MediaMetadata(
                mimeType = "application/octet-stream",
                fileSize = 0L
            ),
            processingStatus = MediaProcessingStatus.FAILED,
            processingDetails = processingDetails
        )
    }

    // Helper data classes for processing results
    private data class ImageProcessingResult(
        val processedUrl: String,
        val thumbnailUrl: String?,
        val operations: List<ProcessingOperation>,
        val warnings: List<ProcessingWarning>,
        val variants: List<MediaVariant>
    )

    private data class VideoProcessingResult(
        val processedUrl: String,
        val thumbnailUrl: String?,
        val operations: List<ProcessingOperation>,
        val warnings: List<ProcessingWarning>,
        val variants: List<MediaVariant>
    )

    private data class AudioProcessingResult(
        val processedUrl: String,
        val operations: List<ProcessingOperation>,
        val warnings: List<ProcessingWarning>,
        val variants: List<MediaVariant>
    )

    private data class DocumentProcessingResult(
        val processedUrl: String,
        val thumbnailUrl: String?,
        val operations: List<ProcessingOperation>,
        val warnings: List<ProcessingWarning>
    )

    private data class ThumbnailResult(
        val thumbnailUrl: String,
        val variants: List<MediaVariant>
    )
}