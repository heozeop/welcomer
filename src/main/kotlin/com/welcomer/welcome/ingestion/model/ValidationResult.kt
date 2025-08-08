package com.welcomer.welcome.ingestion.model

/**
 * Result of content validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList(),
    val warnings: List<ValidationWarning> = emptyList()
) {
    companion object {
        fun valid() = ValidationResult(isValid = true)
        
        fun invalid(vararg errors: ValidationError) = ValidationResult(
            isValid = false, 
            errors = errors.toList()
        )
        
        fun invalid(errors: List<ValidationError>) = ValidationResult(
            isValid = false, 
            errors = errors
        )
    }
}

/**
 * Validation error details
 */
data class ValidationError(
    val field: String,
    val code: ValidationErrorCode,
    val message: String,
    val rejectedValue: Any? = null
)

/**
 * Validation warning (non-blocking issues)
 */
data class ValidationWarning(
    val field: String,
    val code: ValidationWarningCode,
    val message: String
)

/**
 * Validation error codes
 */
enum class ValidationErrorCode {
    REQUIRED_FIELD_MISSING,
    INVALID_FORMAT,
    CONTENT_TOO_LONG,
    CONTENT_TOO_SHORT,
    INVALID_URL,
    INVALID_MEDIA_TYPE,
    INVALID_FILE_SIZE,
    INVALID_CONTENT_TYPE,
    MALICIOUS_CONTENT_DETECTED,
    RATE_LIMIT_EXCEEDED,
    INVALID_LANGUAGE_CODE,
    INVALID_POLL_CONFIGURATION,
    INVALID_SCHEDULE_TIME,
    INVALID_TAG_FORMAT,
    INVALID_MENTION_FORMAT
}

/**
 * Validation warning codes
 */
enum class ValidationWarningCode {
    CONTENT_MAY_BE_TRUNCATED,
    MEDIA_QUALITY_LOW,
    LANGUAGE_DETECTED_DIFFERENT,
    POTENTIAL_SPAM_DETECTED,
    EXCESSIVE_HASHTAGS,
    EXCESSIVE_MENTIONS
}

/**
 * Sanitization result
 */
data class SanitizationResult(
    val sanitizedContent: String,
    val modificationsApplied: List<SanitizationModification> = emptyList(),
    val potentialThreatsRemoved: List<SecurityThreat> = emptyList()
)

/**
 * Sanitization modification applied to content
 */
data class SanitizationModification(
    val type: SanitizationType,
    val description: String,
    val originalValue: String?,
    val sanitizedValue: String?
)

/**
 * Security threat detected and removed
 */
data class SecurityThreat(
    val type: ThreatType,
    val description: String,
    val location: String?,
    val severity: ThreatSeverity
)

/**
 * Types of sanitization applied
 */
enum class SanitizationType {
    HTML_TAG_REMOVED,
    JAVASCRIPT_REMOVED,
    UNSAFE_ATTRIBUTE_REMOVED,
    MALICIOUS_URL_BLOCKED,
    EXCESSIVE_WHITESPACE_CLEANED,
    ENCODING_NORMALIZED
}

/**
 * Types of security threats
 */
enum class ThreatType {
    XSS_ATTEMPT,
    SCRIPT_INJECTION,
    MALICIOUS_LINK,
    PHISHING_ATTEMPT,
    MALWARE_LINK
}

/**
 * Threat severity levels
 */
enum class ThreatSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}