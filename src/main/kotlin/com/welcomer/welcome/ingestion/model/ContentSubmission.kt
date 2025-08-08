package com.welcomer.welcome.ingestion.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

/**
 * Content submission request model for ingestion API
 */
data class ContentSubmission(
    @field:NotBlank(message = "Content type is required")
    @JsonProperty("contentType")
    val contentType: ContentType,

    @field:Size(max = 10000, message = "Text content must not exceed 10000 characters")
    @JsonProperty("textContent")
    val textContent: String? = null,

    @JsonProperty("linkUrl")
    val linkUrl: String? = null,

    @field:Valid
    @JsonProperty("mediaAttachments")
    val mediaAttachments: List<MediaAttachment>? = null,

    @JsonProperty("visibility")
    val visibility: ContentVisibility = ContentVisibility.PUBLIC,

    @JsonProperty("tags")
    val tags: List<String>? = null,

    @JsonProperty("mentions")
    val mentions: List<String>? = null,

    @JsonProperty("pollData")
    val pollData: PollData? = null,

    @JsonProperty("scheduledAt")
    val scheduledAt: Instant? = null,

    @JsonProperty("isSensitive")
    val isSensitive: Boolean = false,

    @JsonProperty("languageCode")
    val languageCode: String? = null
)

/**
 * Media attachment metadata
 */
data class MediaAttachment(
    @field:NotBlank(message = "Media type is required")
    @JsonProperty("mediaType")
    val mediaType: MediaType,

    @field:NotBlank(message = "File URL is required")
    @JsonProperty("fileUrl")
    val fileUrl: String,

    @JsonProperty("originalFilename")
    val originalFilename: String? = null,

    @JsonProperty("altText")
    val altText: String? = null,

    @JsonProperty("width")
    val width: Int? = null,

    @JsonProperty("height")
    val height: Int? = null,

    @JsonProperty("duration")
    val duration: Int? = null
)

/**
 * Poll data structure
 */
data class PollData(
    @field:NotNull(message = "Poll options are required")
    @field:Size(min = 2, max = 10, message = "Poll must have between 2 and 10 options")
    @JsonProperty("options")
    val options: List<PollOption>,

    @JsonProperty("allowMultipleChoices")
    val allowMultipleChoices: Boolean = false,

    @JsonProperty("expiresAt")
    val expiresAt: Instant? = null
)

/**
 * Poll option
 */
data class PollOption(
    @field:NotBlank(message = "Poll option text is required")
    @field:Size(max = 100, message = "Poll option text must not exceed 100 characters")
    @JsonProperty("text")
    val text: String
)

/**
 * Content types supported by the system
 */
enum class ContentType {
    @JsonProperty("text")
    TEXT,

    @JsonProperty("image")
    IMAGE,

    @JsonProperty("video")
    VIDEO,

    @JsonProperty("link")
    LINK,

    @JsonProperty("poll")
    POLL
}

/**
 * Media types supported by the system
 */
enum class MediaType {
    @JsonProperty("image")
    IMAGE,

    @JsonProperty("video")
    VIDEO,

    @JsonProperty("audio")
    AUDIO,

    @JsonProperty("document")
    DOCUMENT
}

/**
 * Content visibility levels
 */
enum class ContentVisibility {
    @JsonProperty("public")
    PUBLIC,

    @JsonProperty("followers")
    FOLLOWERS,

    @JsonProperty("private")
    PRIVATE,

    @JsonProperty("unlisted")
    UNLISTED
}