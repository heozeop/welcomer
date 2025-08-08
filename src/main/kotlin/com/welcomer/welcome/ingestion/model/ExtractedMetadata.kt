package com.welcomer.welcome.ingestion.model

import java.time.Instant

/**
 * Result of metadata extraction from content
 */
data class ExtractedMetadata(
    val keywords: List<String> = emptyList(),
    val topics: List<Topic> = emptyList(),
    val entities: List<Entity> = emptyList(),
    val links: List<ExtractedLink> = emptyList(),
    val mentions: List<ExtractedMention> = emptyList(),
    val hashtags: List<String> = emptyList(),
    val language: LanguageInfo? = null,
    val sentiment: SentimentInfo? = null,
    val contentMetrics: ContentMetrics,
    val extractedAt: Instant = Instant.now()
)

/**
 * Topic classification result
 */
data class Topic(
    val name: String,
    val confidence: Double,
    val category: TopicCategory
)

/**
 * Named entity extraction result
 */
data class Entity(
    val text: String,
    val type: EntityType,
    val confidence: Double,
    val startPosition: Int,
    val endPosition: Int,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Link extracted from content
 */
data class ExtractedLink(
    val url: String,
    val displayText: String?,
    val domain: String,
    val startPosition: Int,
    val endPosition: Int,
    val linkType: LinkType,
    val metadata: LinkMetadata? = null
)

/**
 * Mention extracted from content
 */
data class ExtractedMention(
    val username: String,
    val displayName: String?,
    val startPosition: Int,
    val endPosition: Int,
    val mentionType: MentionType
)

/**
 * Language detection result
 */
data class LanguageInfo(
    val detectedLanguage: String,
    val confidence: Double,
    val possibleLanguages: List<LanguageProbability> = emptyList()
)

/**
 * Language probability for ambiguous content
 */
data class LanguageProbability(
    val language: String,
    val probability: Double
)

/**
 * Sentiment analysis result
 */
data class SentimentInfo(
    val overallSentiment: Sentiment,
    val positiveScore: Double,
    val neutralScore: Double,
    val negativeScore: Double,
    val confidence: Double,
    val emotionalTone: List<EmotionalTone> = emptyList()
)

/**
 * Content metrics and statistics
 */
data class ContentMetrics(
    val characterCount: Int,
    val wordCount: Int,
    val sentenceCount: Int,
    val paragraphCount: Int,
    val readabilityScore: Double? = null,
    val complexityScore: Double? = null,
    val uniqueWordsCount: Int,
    val averageWordLength: Double,
    val averageSentenceLength: Double
)

/**
 * Link metadata extracted from URL
 */
data class LinkMetadata(
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val siteName: String? = null,
    val publishedDate: Instant? = null,
    val author: String? = null,
    val contentType: String? = null,
    val isSecure: Boolean,
    val statusCode: Int? = null,
    val extractedAt: Instant = Instant.now()
)

/**
 * Topic categories
 */
enum class TopicCategory {
    TECHNOLOGY,
    POLITICS,
    SPORTS,
    ENTERTAINMENT,
    BUSINESS,
    SCIENCE,
    HEALTH,
    EDUCATION,
    LIFESTYLE,
    NEWS,
    OPINION,
    OTHER
}

/**
 * Named entity types
 */
enum class EntityType {
    PERSON,
    ORGANIZATION,
    LOCATION,
    DATE,
    TIME,
    MONEY,
    PERCENTAGE,
    PRODUCT,
    EVENT,
    WORK_OF_ART,
    LAW,
    LANGUAGE,
    MISC
}

/**
 * Link types
 */
enum class LinkType {
    EXTERNAL,
    INTERNAL,
    SOCIAL_MEDIA,
    IMAGE,
    VIDEO,
    DOCUMENT,
    EMAIL,
    PHONE,
    HASHTAG_LINK
}

/**
 * Mention types
 */
enum class MentionType {
    USER_MENTION,
    REPLY_TO,
    QUOTE_MENTION
}

/**
 * Sentiment classifications
 */
enum class Sentiment {
    VERY_POSITIVE,
    POSITIVE,
    NEUTRAL,
    NEGATIVE,
    VERY_NEGATIVE,
    MIXED
}

/**
 * Emotional tones detected in content
 */
enum class EmotionalTone {
    JOY,
    ANGER,
    FEAR,
    SADNESS,
    SURPRISE,
    DISGUST,
    TRUST,
    ANTICIPATION,
    SARCASM,
    HUMOR
}