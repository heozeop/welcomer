package com.welcomer.welcome.ingestion.service

import com.welcomer.welcome.ingestion.model.*
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URISyntaxException
import java.time.Instant
import java.util.*
import java.util.regex.Pattern

/**
 * Service responsible for validating and sanitizing content submissions
 */
@Service
class ContentValidator {

    companion object {
        private const val MAX_TEXT_LENGTH = 10000
        private const val MAX_LINK_URL_LENGTH = 2048
        private const val MAX_TAG_LENGTH = 50
        private const val MAX_TAGS_COUNT = 20
        private const val MAX_MENTIONS_COUNT = 10
        private const val MAX_POLL_OPTIONS = 10
        private const val MIN_POLL_OPTIONS = 2
        private const val MAX_POLL_OPTION_LENGTH = 100

        private val URL_PATTERN = Pattern.compile(
            "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
        )
        private val TAG_PATTERN = Pattern.compile("^[\\w가-힣]{1,$MAX_TAG_LENGTH}$")
        private val MENTION_PATTERN = Pattern.compile("^@[\\w가-힣.]{1,30}$")
        private val LANGUAGE_CODE_PATTERN = Pattern.compile("^[a-z]{2}(-[A-Z]{2})?$")
        
        // Known malicious patterns (simplified for demo)
        private val MALICIOUS_PATTERNS = listOf(
            Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL),
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("data:text/html", Pattern.CASE_INSENSITIVE)
        )

        // Suspicious URLs that might be phishing or malware
        private val SUSPICIOUS_DOMAINS = setOf(
            "bit.ly", "tinyurl.com", "shortened.link",
            "malicious-site.com", "phishing-site.net"
        )

        private val SAFELIST = Safelist.basicWithImages()
            .addTags("p", "br", "strong", "em", "u", "s", "blockquote", "pre", "code")
            .addAttributes("img", "alt", "title")
    }

    /**
     * Validates a content submission
     */
    fun validate(submission: ContentSubmission, userId: String): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()

        // Validate content type and corresponding fields
        errors.addAll(validateContentTypeConsistency(submission))

        // Validate text content
        submission.textContent?.let { text ->
            errors.addAll(validateTextContent(text))
            warnings.addAll(checkTextContentWarnings(text))
        }

        // Validate link URL
        submission.linkUrl?.let { url ->
            errors.addAll(validateLinkUrl(url))
            warnings.addAll(checkLinkWarnings(url))
        }

        // Validate media attachments
        submission.mediaAttachments?.let { attachments ->
            errors.addAll(validateMediaAttachments(attachments))
        }

        // Validate tags
        submission.tags?.let { tags ->
            errors.addAll(validateTags(tags))
            warnings.addAll(checkTagWarnings(tags))
        }

        // Validate mentions
        submission.mentions?.let { mentions ->
            errors.addAll(validateMentions(mentions))
            warnings.addAll(checkMentionWarnings(mentions))
        }

        // Validate poll data
        submission.pollData?.let { poll ->
            errors.addAll(validatePollData(poll))
        }

        // Validate scheduled time
        submission.scheduledAt?.let { scheduledAt ->
            errors.addAll(validateScheduledTime(scheduledAt))
        }

        // Validate language code
        submission.languageCode?.let { langCode ->
            errors.addAll(validateLanguageCode(langCode))
        }

        // Check for malicious content
        errors.addAll(checkForMaliciousContent(submission))

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Sanitizes content to remove potential security threats
     */
    fun sanitize(content: String): SanitizationResult {
        val modifications = mutableListOf<SanitizationModification>()
        val threats = mutableListOf<SecurityThreat>()
        var sanitizedContent = content

        // Check for malicious patterns before sanitization
        MALICIOUS_PATTERNS.forEach { pattern ->
            val matcher = pattern.matcher(sanitizedContent)
            while (matcher.find()) {
                threats.add(
                    SecurityThreat(
                        type = when {
                            matcher.group().contains("script") -> ThreatType.SCRIPT_INJECTION
                            matcher.group().contains("javascript:") -> ThreatType.XSS_ATTEMPT
                            else -> ThreatType.XSS_ATTEMPT
                        },
                        description = "Potentially malicious code detected: ${matcher.group().take(50)}...",
                        location = "Position ${matcher.start()}-${matcher.end()}",
                        severity = ThreatSeverity.HIGH
                    )
                )
            }
        }

        // Use Jsoup to sanitize HTML content
        val originalLength = sanitizedContent.length
        sanitizedContent = Jsoup.clean(sanitizedContent, SAFELIST)

        if (sanitizedContent.length != originalLength) {
            modifications.add(
                SanitizationModification(
                    type = SanitizationType.HTML_TAG_REMOVED,
                    description = "Unsafe HTML tags and attributes removed",
                    originalValue = content,
                    sanitizedValue = sanitizedContent
                )
            )
        }

        // Clean excessive whitespace
        val whitespaceCleanedContent = sanitizedContent.replace(Regex("\\s+"), " ").trim()
        if (whitespaceCleanedContent != sanitizedContent) {
            modifications.add(
                SanitizationModification(
                    type = SanitizationType.EXCESSIVE_WHITESPACE_CLEANED,
                    description = "Excessive whitespace normalized",
                    originalValue = sanitizedContent,
                    sanitizedValue = whitespaceCleanedContent
                )
            )
            sanitizedContent = whitespaceCleanedContent
        }

        return SanitizationResult(
            sanitizedContent = sanitizedContent,
            modificationsApplied = modifications,
            potentialThreatsRemoved = threats
        )
    }

    private fun validateContentTypeConsistency(submission: ContentSubmission): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        when (submission.contentType) {
            ContentType.TEXT -> {
                if (submission.textContent.isNullOrBlank()) {
                    errors.add(
                        ValidationError(
                            field = "textContent",
                            code = ValidationErrorCode.REQUIRED_FIELD_MISSING,
                            message = "Text content is required for TEXT type posts"
                        )
                    )
                }
            }
            ContentType.LINK -> {
                if (submission.linkUrl.isNullOrBlank()) {
                    errors.add(
                        ValidationError(
                            field = "linkUrl",
                            code = ValidationErrorCode.REQUIRED_FIELD_MISSING,
                            message = "Link URL is required for LINK type posts"
                        )
                    )
                }
            }
            ContentType.IMAGE, ContentType.VIDEO -> {
                if (submission.mediaAttachments.isNullOrEmpty()) {
                    errors.add(
                        ValidationError(
                            field = "mediaAttachments",
                            code = ValidationErrorCode.REQUIRED_FIELD_MISSING,
                            message = "Media attachments are required for ${submission.contentType} type posts"
                        )
                    )
                }
            }
            ContentType.POLL -> {
                if (submission.pollData == null) {
                    errors.add(
                        ValidationError(
                            field = "pollData",
                            code = ValidationErrorCode.REQUIRED_FIELD_MISSING,
                            message = "Poll data is required for POLL type posts"
                        )
                    )
                }
            }
        }

        return errors
    }

    private fun validateTextContent(text: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (text.length > MAX_TEXT_LENGTH) {
            errors.add(
                ValidationError(
                    field = "textContent",
                    code = ValidationErrorCode.CONTENT_TOO_LONG,
                    message = "Text content exceeds maximum length of $MAX_TEXT_LENGTH characters",
                    rejectedValue = text.length
                )
            )
        }

        return errors
    }

    private fun validateLinkUrl(url: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (url.length > MAX_LINK_URL_LENGTH) {
            errors.add(
                ValidationError(
                    field = "linkUrl",
                    code = ValidationErrorCode.CONTENT_TOO_LONG,
                    message = "URL exceeds maximum length of $MAX_LINK_URL_LENGTH characters",
                    rejectedValue = url.length
                )
            )
        }

        if (!URL_PATTERN.matcher(url).matches()) {
            errors.add(
                ValidationError(
                    field = "linkUrl",
                    code = ValidationErrorCode.INVALID_URL,
                    message = "Invalid URL format",
                    rejectedValue = url
                )
            )
        }

        try {
            val uri = URI(url)
            if (SUSPICIOUS_DOMAINS.contains(uri.host?.lowercase())) {
                errors.add(
                    ValidationError(
                        field = "linkUrl",
                        code = ValidationErrorCode.MALICIOUS_CONTENT_DETECTED,
                        message = "URL from suspicious domain detected",
                        rejectedValue = uri.host
                    )
                )
            }
        } catch (e: URISyntaxException) {
            errors.add(
                ValidationError(
                    field = "linkUrl",
                    code = ValidationErrorCode.INVALID_URL,
                    message = "Malformed URL",
                    rejectedValue = url
                )
            )
        }

        return errors
    }

    private fun validateMediaAttachments(attachments: List<MediaAttachment>): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        attachments.forEachIndexed { index, attachment ->
            // Validate file URL
            if (!URL_PATTERN.matcher(attachment.fileUrl).matches()) {
                errors.add(
                    ValidationError(
                        field = "mediaAttachments[$index].fileUrl",
                        code = ValidationErrorCode.INVALID_URL,
                        message = "Invalid media file URL",
                        rejectedValue = attachment.fileUrl
                    )
                )
            }

            // Validate dimensions for images/videos
            if (attachment.mediaType == MediaType.IMAGE || attachment.mediaType == MediaType.VIDEO) {
                if (attachment.width != null && attachment.width <= 0) {
                    errors.add(
                        ValidationError(
                            field = "mediaAttachments[$index].width",
                            code = ValidationErrorCode.INVALID_FORMAT,
                            message = "Width must be positive",
                            rejectedValue = attachment.width
                        )
                    )
                }

                if (attachment.height != null && attachment.height <= 0) {
                    errors.add(
                        ValidationError(
                            field = "mediaAttachments[$index].height",
                            code = ValidationErrorCode.INVALID_FORMAT,
                            message = "Height must be positive",
                            rejectedValue = attachment.height
                        )
                    )
                }
            }

            // Validate duration for video/audio
            if ((attachment.mediaType == MediaType.VIDEO || attachment.mediaType == MediaType.AUDIO) 
                && attachment.duration != null && attachment.duration <= 0) {
                errors.add(
                    ValidationError(
                        field = "mediaAttachments[$index].duration",
                        code = ValidationErrorCode.INVALID_FORMAT,
                        message = "Duration must be positive",
                        rejectedValue = attachment.duration
                    )
                )
            }
        }

        return errors
    }

    private fun validateTags(tags: List<String>): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (tags.size > MAX_TAGS_COUNT) {
            errors.add(
                ValidationError(
                    field = "tags",
                    code = ValidationErrorCode.CONTENT_TOO_LONG,
                    message = "Too many tags (maximum $MAX_TAGS_COUNT allowed)",
                    rejectedValue = tags.size
                )
            )
        }

        tags.forEachIndexed { index, tag ->
            if (!TAG_PATTERN.matcher(tag).matches()) {
                errors.add(
                    ValidationError(
                        field = "tags[$index]",
                        code = ValidationErrorCode.INVALID_TAG_FORMAT,
                        message = "Invalid tag format",
                        rejectedValue = tag
                    )
                )
            }
        }

        return errors
    }

    private fun validateMentions(mentions: List<String>): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (mentions.size > MAX_MENTIONS_COUNT) {
            errors.add(
                ValidationError(
                    field = "mentions",
                    code = ValidationErrorCode.CONTENT_TOO_LONG,
                    message = "Too many mentions (maximum $MAX_MENTIONS_COUNT allowed)",
                    rejectedValue = mentions.size
                )
            )
        }

        mentions.forEachIndexed { index, mention ->
            if (!MENTION_PATTERN.matcher(mention).matches()) {
                errors.add(
                    ValidationError(
                        field = "mentions[$index]",
                        code = ValidationErrorCode.INVALID_MENTION_FORMAT,
                        message = "Invalid mention format (should start with @ and contain valid username)",
                        rejectedValue = mention
                    )
                )
            }
        }

        return errors
    }

    private fun validatePollData(poll: PollData): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (poll.options.size < MIN_POLL_OPTIONS) {
            errors.add(
                ValidationError(
                    field = "pollData.options",
                    code = ValidationErrorCode.INVALID_POLL_CONFIGURATION,
                    message = "Poll must have at least $MIN_POLL_OPTIONS options",
                    rejectedValue = poll.options.size
                )
            )
        }

        if (poll.options.size > MAX_POLL_OPTIONS) {
            errors.add(
                ValidationError(
                    field = "pollData.options",
                    code = ValidationErrorCode.INVALID_POLL_CONFIGURATION,
                    message = "Poll cannot have more than $MAX_POLL_OPTIONS options",
                    rejectedValue = poll.options.size
                )
            )
        }

        poll.options.forEachIndexed { index, option ->
            if (option.text.length > MAX_POLL_OPTION_LENGTH) {
                errors.add(
                    ValidationError(
                        field = "pollData.options[$index].text",
                        code = ValidationErrorCode.CONTENT_TOO_LONG,
                        message = "Poll option text exceeds maximum length of $MAX_POLL_OPTION_LENGTH characters",
                        rejectedValue = option.text.length
                    )
                )
            }
        }

        poll.expiresAt?.let { expiresAt ->
            if (expiresAt.isBefore(Instant.now().plusSeconds(300))) { // Must expire at least 5 minutes from now
                errors.add(
                    ValidationError(
                        field = "pollData.expiresAt",
                        code = ValidationErrorCode.INVALID_SCHEDULE_TIME,
                        message = "Poll expiration time must be at least 5 minutes in the future",
                        rejectedValue = expiresAt
                    )
                )
            }
        }

        return errors
    }

    private fun validateScheduledTime(scheduledAt: Instant): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (scheduledAt.isBefore(Instant.now())) {
            errors.add(
                ValidationError(
                    field = "scheduledAt",
                    code = ValidationErrorCode.INVALID_SCHEDULE_TIME,
                    message = "Scheduled time cannot be in the past",
                    rejectedValue = scheduledAt
                )
            )
        }

        // Prevent scheduling too far in the future (e.g., more than 1 year)
        if (scheduledAt.isAfter(Instant.now().plusSeconds(31536000))) {
            errors.add(
                ValidationError(
                    field = "scheduledAt",
                    code = ValidationErrorCode.INVALID_SCHEDULE_TIME,
                    message = "Cannot schedule content more than 1 year in advance",
                    rejectedValue = scheduledAt
                )
            )
        }

        return errors
    }

    private fun validateLanguageCode(languageCode: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (!LANGUAGE_CODE_PATTERN.matcher(languageCode).matches()) {
            errors.add(
                ValidationError(
                    field = "languageCode",
                    code = ValidationErrorCode.INVALID_LANGUAGE_CODE,
                    message = "Invalid language code format (expected: 'en' or 'en-US')",
                    rejectedValue = languageCode
                )
            )
        }

        return errors
    }

    private fun checkForMaliciousContent(submission: ContentSubmission): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val contentToCheck = listOfNotNull(
            submission.textContent,
            submission.linkUrl
        ).joinToString(" ")

        MALICIOUS_PATTERNS.forEach { pattern ->
            if (pattern.matcher(contentToCheck).find()) {
                errors.add(
                    ValidationError(
                        field = "content",
                        code = ValidationErrorCode.MALICIOUS_CONTENT_DETECTED,
                        message = "Potentially malicious content detected",
                        rejectedValue = null
                    )
                )
            }
        }

        return errors
    }

    private fun checkTextContentWarnings(text: String): List<ValidationWarning> {
        val warnings = mutableListOf<ValidationWarning>()

        // Warn if content is very long (close to limit)
        if (text.length > MAX_TEXT_LENGTH * 0.9) {
            warnings.add(
                ValidationWarning(
                    field = "textContent",
                    code = ValidationWarningCode.CONTENT_MAY_BE_TRUNCATED,
                    message = "Content is close to the maximum length limit"
                )
            )
        }

        return warnings
    }

    private fun checkLinkWarnings(url: String): List<ValidationWarning> {
        val warnings = mutableListOf<ValidationWarning>()

        try {
            val uri = URI(url)
            // Check for URL shorteners (not malicious but potentially concerning)
            val shortenerDomains = setOf("bit.ly", "tinyurl.com", "t.co", "goo.gl")
            if (shortenerDomains.contains(uri.host?.lowercase())) {
                warnings.add(
                    ValidationWarning(
                        field = "linkUrl",
                        code = ValidationWarningCode.POTENTIAL_SPAM_DETECTED,
                        message = "URL appears to use a link shortener"
                    )
                )
            }
        } catch (e: URISyntaxException) {
            // Already handled in validation
        }

        return warnings
    }

    private fun checkTagWarnings(tags: List<String>): List<ValidationWarning> {
        val warnings = mutableListOf<ValidationWarning>()

        if (tags.size > 10) {
            warnings.add(
                ValidationWarning(
                    field = "tags",
                    code = ValidationWarningCode.EXCESSIVE_HASHTAGS,
                    message = "Large number of tags may reduce content visibility"
                )
            )
        }

        return warnings
    }

    private fun checkMentionWarnings(mentions: List<String>): List<ValidationWarning> {
        val warnings = mutableListOf<ValidationWarning>()

        if (mentions.size > 5) {
            warnings.add(
                ValidationWarning(
                    field = "mentions",
                    code = ValidationWarningCode.EXCESSIVE_MENTIONS,
                    message = "Large number of mentions may appear as spam"
                )
            )
        }

        return warnings
    }
}