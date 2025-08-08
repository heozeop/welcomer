package com.welcomer.welcome.ingestion.service

import com.welcomer.welcome.ingestion.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName

class ContentValidatorTest {

    private val contentValidator = ContentValidator()

    @Test
    @DisplayName("Valid text content should pass validation")
    fun testValidTextContent() {
        val submission = ContentSubmission(
            contentType = ContentType.TEXT,
            textContent = "This is a valid text post"
        )

        val result = contentValidator.validate(submission, "user123")

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    @DisplayName("Text content exceeding maximum length should fail validation")
    fun testTextContentTooLong() {
        val longText = "x".repeat(10001)
        val submission = ContentSubmission(
            contentType = ContentType.TEXT,
            textContent = longText
        )

        val result = contentValidator.validate(submission, "user123")

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == ValidationErrorCode.CONTENT_TOO_LONG })
    }

    @Test
    @DisplayName("TEXT type without text content should fail validation")
    fun testTextTypeWithoutContent() {
        val submission = ContentSubmission(
            contentType = ContentType.TEXT,
            textContent = null
        )

        val result = contentValidator.validate(submission, "user123")

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == ValidationErrorCode.REQUIRED_FIELD_MISSING })
    }

    @Test
    @DisplayName("LINK type without URL should fail validation")
    fun testLinkTypeWithoutUrl() {
        val submission = ContentSubmission(
            contentType = ContentType.LINK,
            linkUrl = null
        )

        val result = contentValidator.validate(submission, "user123")

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == ValidationErrorCode.REQUIRED_FIELD_MISSING })
    }

    @Test
    @DisplayName("Invalid URL format should fail validation")
    fun testInvalidUrlFormat() {
        val submission = ContentSubmission(
            contentType = ContentType.LINK,
            linkUrl = "not-a-valid-url"
        )

        val result = contentValidator.validate(submission, "user123")

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == ValidationErrorCode.INVALID_URL })
    }

    @Test
    @DisplayName("Valid URL should pass validation")
    fun testValidUrl() {
        val submission = ContentSubmission(
            contentType = ContentType.LINK,
            linkUrl = "https://example.com"
        )

        val result = contentValidator.validate(submission, "user123")

        assertTrue(result.isValid)
    }

    @Test
    @DisplayName("Too many tags should fail validation")
    fun testTooManyTags() {
        val manyTags = (1..21).map { "tag$it" }
        val submission = ContentSubmission(
            contentType = ContentType.TEXT,
            textContent = "Test post",
            tags = manyTags
        )

        val result = contentValidator.validate(submission, "user123")

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == ValidationErrorCode.CONTENT_TOO_LONG })
    }

    @Test
    @DisplayName("Invalid tag format should fail validation")
    fun testInvalidTagFormat() {
        val submission = ContentSubmission(
            contentType = ContentType.TEXT,
            textContent = "Test post",
            tags = listOf("valid-tag", "invalid tag with spaces", "another_valid_tag")
        )

        val result = contentValidator.validate(submission, "user123")

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == ValidationErrorCode.INVALID_TAG_FORMAT })
    }

    @Test
    @DisplayName("Invalid mention format should fail validation")
    fun testInvalidMentionFormat() {
        val submission = ContentSubmission(
            contentType = ContentType.TEXT,
            textContent = "Test post",
            mentions = listOf("@validuser", "invalid-mention-without-at", "@another_valid_user")
        )

        val result = contentValidator.validate(submission, "user123")

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == ValidationErrorCode.INVALID_MENTION_FORMAT })
    }

    @Test
    @DisplayName("Valid poll should pass validation")
    fun testValidPoll() {
        val submission = ContentSubmission(
            contentType = ContentType.POLL,
            pollData = PollData(
                options = listOf(
                    PollOption("Option 1"),
                    PollOption("Option 2")
                )
            )
        )

        val result = contentValidator.validate(submission, "user123")

        assertTrue(result.isValid)
    }

    @Test
    @DisplayName("Poll with insufficient options should fail validation")
    fun testPollWithInsufficientOptions() {
        val submission = ContentSubmission(
            contentType = ContentType.POLL,
            pollData = PollData(
                options = listOf(PollOption("Only option"))
            )
        )

        val result = contentValidator.validate(submission, "user123")

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == ValidationErrorCode.INVALID_POLL_CONFIGURATION })
    }

    @Test
    @DisplayName("Content sanitization should remove malicious scripts")
    fun testContentSanitization() {
        val maliciousContent = "Hello <script>alert('xss')</script> world"
        
        val result = contentValidator.sanitize(maliciousContent)

        assertFalse(result.sanitizedContent.contains("<script>"))
        assertTrue(result.potentialThreatsRemoved.isNotEmpty())
        assertTrue(result.potentialThreatsRemoved.any { it.type == ThreatType.SCRIPT_INJECTION })
    }

    @Test
    @DisplayName("Content sanitization should clean excessive whitespace")
    fun testWhitespaceSanitization() {
        val messyContent = "Hello    world   with    lots     of    spaces"
        
        val result = contentValidator.sanitize(messyContent)

        assertEquals("Hello world with lots of spaces", result.sanitizedContent)
        assertTrue(result.modificationsApplied.any { it.type == SanitizationType.EXCESSIVE_WHITESPACE_CLEANED })
    }

    @Test
    @DisplayName("Valid content should pass sanitization unchanged")
    fun testValidContentSanitization() {
        val cleanContent = "This is clean, safe content."
        
        val result = contentValidator.sanitize(cleanContent)

        assertEquals(cleanContent, result.sanitizedContent)
        assertTrue(result.potentialThreatsRemoved.isEmpty())
        assertTrue(result.modificationsApplied.isEmpty())
    }
}