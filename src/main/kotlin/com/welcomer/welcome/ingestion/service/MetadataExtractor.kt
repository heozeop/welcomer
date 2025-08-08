package com.welcomer.welcome.ingestion.service

import com.welcomer.welcome.ingestion.model.*
import org.jsoup.Jsoup
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.regex.Pattern

/**
 * Service responsible for extracting metadata and enriching content
 */
@Service
class MetadataExtractor {

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    companion object {
        private val URL_PATTERN = Pattern.compile(
            "https?://(?:[-\\w.])+(?:[:\\d]+)?(?:/(?:[\\w/_.])*(?:\\?(?:[&\\w.=])*)?(?:#(?:\\w*))?)?",
            Pattern.CASE_INSENSITIVE
        )
        
        private val MENTION_PATTERN = Pattern.compile("@([\\w가-힣.]{1,30})")
        private val HASHTAG_PATTERN = Pattern.compile("#([\\w가-힣]{1,50})")
        
        private val STOP_WORDS = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "do", "does", "did", "will", "would", "could", "should", "may", "might", "must", "can", "shall",
            "이", "그", "저", "것", "수", "있", "하", "되", "같", "다", "보", "주", "좀", "잘", "만", "또", "더", "등", "들", "때", "년", "일", "말"
        )
        
        private val SENTIMENT_POSITIVE_WORDS = setOf(
            "good", "great", "excellent", "amazing", "wonderful", "fantastic", "awesome", "love", "like", "happy", "joy", "pleased", "satisfied",
            "좋", "훌륭", "멋진", "대단", "환상적", "좋아", "행복", "기쁨", "만족", "즐거", "재미"
        )
        
        private val SENTIMENT_NEGATIVE_WORDS = setOf(
            "bad", "terrible", "awful", "hate", "dislike", "sad", "angry", "disappointed", "frustrated", "annoyed", "upset",
            "나쁜", "끔찍", "싫어", "화", "실망", "짜증", "속상", "슬픈", "불만", "문제"
        )
    }

    /**
     * Extracts comprehensive metadata from content submission
     */
    suspend fun extractMetadata(submission: ContentSubmission): ExtractedMetadata {
        val contentText = buildContentText(submission)
        
        return ExtractedMetadata(
            keywords = extractKeywords(contentText),
            topics = extractTopics(contentText),
            entities = extractEntities(contentText),
            links = extractLinks(contentText),
            mentions = extractMentions(contentText),
            hashtags = extractHashtags(contentText),
            language = detectLanguage(contentText),
            sentiment = analyzeSentiment(contentText),
            contentMetrics = calculateContentMetrics(contentText)
        )
    }

    /**
     * Extracts link preview metadata from URLs
     */
    suspend fun extractLinkMetadata(url: String): LinkMetadata? {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "WelcomerBot/1.0")
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() != 200) {
                return LinkMetadata(
                    isSecure = url.startsWith("https"),
                    statusCode = response.statusCode()
                )
            }

            val doc = Jsoup.parse(response.body())
            
            LinkMetadata(
                title = extractOpenGraphData(doc, "og:title") 
                    ?: doc.select("title").text().takeIf { it.isNotBlank() },
                description = extractOpenGraphData(doc, "og:description")
                    ?: doc.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() },
                imageUrl = extractOpenGraphData(doc, "og:image"),
                siteName = extractOpenGraphData(doc, "og:site_name"),
                author = doc.select("meta[name=author]").attr("content").takeIf { it.isNotBlank() },
                contentType = extractOpenGraphData(doc, "og:type") ?: "website",
                isSecure = url.startsWith("https"),
                statusCode = response.statusCode()
            )
        } catch (e: Exception) {
            LinkMetadata(
                isSecure = url.startsWith("https"),
                statusCode = null
            )
        }
    }

    private fun buildContentText(submission: ContentSubmission): String {
        return listOfNotNull(
            submission.textContent,
            submission.linkUrl,
            submission.pollData?.options?.joinToString(" ") { it.text },
            submission.tags?.joinToString(" ") { "#$it" },
            submission.mentions?.joinToString(" ")
        ).joinToString(" ")
    }

    private fun extractKeywords(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        
        val words = text.lowercase()
            .replace(Regex("[^\\w가-힣\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && !STOP_WORDS.contains(it) }
        
        val wordFrequency = words.groupingBy { it }.eachCount()
        
        return wordFrequency.entries
            .sortedByDescending { it.value }
            .take(20)
            .map { it.key }
    }

    private fun extractTopics(text: String): List<Topic> {
        val topics = mutableListOf<Topic>()
        val lowerText = text.lowercase()
        
        // Simple rule-based topic classification (in real implementation, would use ML)
        val topicKeywords = mapOf(
            TopicCategory.TECHNOLOGY to listOf("tech", "ai", "software", "computer", "digital", "programming", "코딩", "소프트웨어", "기술", "디지털"),
            TopicCategory.SPORTS to listOf("sports", "game", "match", "team", "player", "score", "스포츠", "경기", "선수", "팀"),
            TopicCategory.POLITICS to listOf("politics", "government", "election", "policy", "politician", "정치", "정부", "선거", "정책"),
            TopicCategory.ENTERTAINMENT to listOf("movie", "music", "show", "celebrity", "entertainment", "영화", "음악", "쇼", "연예", "엔터테인먼트"),
            TopicCategory.BUSINESS to listOf("business", "company", "market", "finance", "economy", "비즈니스", "회사", "시장", "경제", "금융"),
            TopicCategory.SCIENCE to listOf("science", "research", "study", "discovery", "experiment", "과학", "연구", "실험", "발견"),
            TopicCategory.HEALTH to listOf("health", "medical", "disease", "treatment", "doctor", "건강", "의료", "질병", "치료", "의사"),
            TopicCategory.EDUCATION to listOf("education", "school", "student", "teacher", "learning", "교육", "학교", "학생", "선생님", "학습")
        )
        
        topicKeywords.forEach { (category, keywords) ->
            val matchCount = keywords.count { lowerText.contains(it) }
            if (matchCount > 0) {
                val confidence = minOf(matchCount / keywords.size.toDouble(), 1.0)
                topics.add(Topic(category.name.lowercase(), confidence, category))
            }
        }
        
        return topics.sortedByDescending { it.confidence }.take(3)
    }

    private fun extractEntities(text: String): List<Entity> {
        val entities = mutableListOf<Entity>()
        
        // Simple pattern-based entity extraction (in real implementation, would use NER models)
        
        // Extract mentions as person entities
        val mentionMatcher = MENTION_PATTERN.matcher(text)
        while (mentionMatcher.find()) {
            entities.add(
                Entity(
                    text = mentionMatcher.group(1),
                    type = EntityType.PERSON,
                    confidence = 0.9,
                    startPosition = mentionMatcher.start(),
                    endPosition = mentionMatcher.end()
                )
            )
        }
        
        // Extract URLs as misc entities
        val urlMatcher = URL_PATTERN.matcher(text)
        while (urlMatcher.find()) {
            try {
                val uri = URI(urlMatcher.group())
                entities.add(
                    Entity(
                        text = urlMatcher.group(),
                        type = EntityType.MISC,
                        confidence = 0.95,
                        startPosition = urlMatcher.start(),
                        endPosition = urlMatcher.end(),
                        metadata = mapOf("domain" to (uri.host ?: "unknown"))
                    )
                )
            } catch (e: Exception) {
                // Ignore malformed URLs
            }
        }
        
        // Extract email addresses
        val emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val emailMatcher = emailPattern.matcher(text)
        while (emailMatcher.find()) {
            entities.add(
                Entity(
                    text = emailMatcher.group(),
                    type = EntityType.MISC,
                    confidence = 0.98,
                    startPosition = emailMatcher.start(),
                    endPosition = emailMatcher.end(),
                    metadata = mapOf("type" to "email")
                )
            )
        }
        
        return entities
    }

    private fun extractLinks(text: String): List<ExtractedLink> {
        val links = mutableListOf<ExtractedLink>()
        val matcher = URL_PATTERN.matcher(text)
        
        while (matcher.find()) {
            try {
                val url = matcher.group()
                val uri = URI(url)
                val domain = uri.host ?: "unknown"
                
                val linkType = when {
                    domain.contains("youtube.com") || domain.contains("youtu.be") -> LinkType.VIDEO
                    domain.contains("twitter.com") || domain.contains("x.com") -> LinkType.SOCIAL_MEDIA
                    domain.contains("instagram.com") -> LinkType.SOCIAL_MEDIA
                    domain.contains("facebook.com") -> LinkType.SOCIAL_MEDIA
                    url.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp)$", RegexOption.IGNORE_CASE)) -> LinkType.IMAGE
                    url.matches(Regex(".*\\.(pdf|doc|docx|ppt|pptx)$", RegexOption.IGNORE_CASE)) -> LinkType.DOCUMENT
                    else -> LinkType.EXTERNAL
                }
                
                links.add(
                    ExtractedLink(
                        url = url,
                        displayText = null,
                        domain = domain,
                        startPosition = matcher.start(),
                        endPosition = matcher.end(),
                        linkType = linkType
                    )
                )
            } catch (e: Exception) {
                // Ignore malformed URLs
            }
        }
        
        return links
    }

    private fun extractMentions(text: String): List<ExtractedMention> {
        val mentions = mutableListOf<ExtractedMention>()
        val matcher = MENTION_PATTERN.matcher(text)
        
        while (matcher.find()) {
            mentions.add(
                ExtractedMention(
                    username = matcher.group(1),
                    displayName = null,
                    startPosition = matcher.start(),
                    endPosition = matcher.end(),
                    mentionType = MentionType.USER_MENTION
                )
            )
        }
        
        return mentions
    }

    private fun extractHashtags(text: String): List<String> {
        val hashtags = mutableListOf<String>()
        val matcher = HASHTAG_PATTERN.matcher(text)
        
        while (matcher.find()) {
            hashtags.add(matcher.group(1))
        }
        
        return hashtags.distinct()
    }

    private fun detectLanguage(text: String): LanguageInfo? {
        if (text.isBlank()) return null
        
        // Simple language detection based on character patterns
        val koreanCount = text.count { it in '가'..'힣' }
        val englishCount = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        val totalChars = text.replace(Regex("\\s+"), "").length
        
        if (totalChars == 0) return null
        
        val koreanRatio = koreanCount.toDouble() / totalChars
        val englishRatio = englishCount.toDouble() / totalChars
        
        return when {
            koreanRatio > 0.3 -> LanguageInfo("ko", minOf(koreanRatio * 2, 1.0))
            englishRatio > 0.3 -> LanguageInfo("en", minOf(englishRatio * 2, 1.0))
            else -> LanguageInfo("und", 0.5) // Undetermined
        }
    }

    private fun analyzeSentiment(text: String): SentimentInfo? {
        if (text.isBlank()) return null
        
        val words = text.lowercase().split(Regex("\\s+"))
        val positiveCount = words.count { SENTIMENT_POSITIVE_WORDS.contains(it) }
        val negativeCount = words.count { SENTIMENT_NEGATIVE_WORDS.contains(it) }
        
        val totalSentimentWords = positiveCount + negativeCount
        if (totalSentimentWords == 0) {
            return SentimentInfo(
                overallSentiment = Sentiment.NEUTRAL,
                positiveScore = 0.33,
                neutralScore = 0.67,
                negativeScore = 0.0,
                confidence = 0.5
            )
        }
        
        val positiveScore = positiveCount.toDouble() / totalSentimentWords
        val negativeScore = negativeCount.toDouble() / totalSentimentWords
        val neutralScore = 1.0 - (positiveScore + negativeScore)
        
        val overallSentiment = when {
            positiveScore > 0.6 -> Sentiment.POSITIVE
            negativeScore > 0.6 -> Sentiment.NEGATIVE
            positiveScore > negativeScore -> Sentiment.POSITIVE
            negativeScore > positiveScore -> Sentiment.NEGATIVE
            else -> Sentiment.NEUTRAL
        }
        
        val confidence = maxOf(positiveScore, negativeScore, neutralScore)
        
        return SentimentInfo(
            overallSentiment = overallSentiment,
            positiveScore = positiveScore,
            neutralScore = neutralScore,
            negativeScore = negativeScore,
            confidence = confidence
        )
    }

    private fun calculateContentMetrics(text: String): ContentMetrics {
        if (text.isBlank()) {
            return ContentMetrics(
                characterCount = 0,
                wordCount = 0,
                sentenceCount = 0,
                paragraphCount = 0,
                uniqueWordsCount = 0,
                averageWordLength = 0.0,
                averageSentenceLength = 0.0
            )
        }
        
        val characterCount = text.length
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size
        val sentences = text.split(Regex("[.!?]+")).filter { it.trim().isNotBlank() }
        val sentenceCount = sentences.size
        val paragraphs = text.split(Regex("\n\\s*\n")).filter { it.trim().isNotBlank() }
        val paragraphCount = paragraphs.size
        
        val uniqueWords = words.map { it.lowercase().replace(Regex("[^\\w가-힣]"), "") }
            .filter { it.isNotBlank() }
            .toSet()
        val uniqueWordsCount = uniqueWords.size
        
        val averageWordLength = if (wordCount > 0) {
            words.sumOf { it.length }.toDouble() / wordCount
        } else 0.0
        
        val averageSentenceLength = if (sentenceCount > 0) {
            wordCount.toDouble() / sentenceCount
        } else 0.0
        
        val readabilityScore = calculateReadabilityScore(words, sentences)
        
        return ContentMetrics(
            characterCount = characterCount,
            wordCount = wordCount,
            sentenceCount = sentenceCount,
            paragraphCount = paragraphCount,
            readabilityScore = readabilityScore,
            complexityScore = calculateComplexityScore(words, uniqueWordsCount, wordCount),
            uniqueWordsCount = uniqueWordsCount,
            averageWordLength = averageWordLength,
            averageSentenceLength = averageSentenceLength
        )
    }

    private fun calculateReadabilityScore(words: List<String>, sentences: List<String>): Double {
        if (sentences.isEmpty() || words.isEmpty()) return 0.0
        
        val avgWordsPerSentence = words.size.toDouble() / sentences.size
        val avgSyllablesPerWord = words.sumOf { estimateSyllables(it) }.toDouble() / words.size
        
        // Simplified Flesch Reading Ease formula
        return 206.835 - (1.015 * avgWordsPerSentence) - (84.6 * avgSyllablesPerWord)
    }

    private fun calculateComplexityScore(words: List<String>, uniqueWordsCount: Int, totalWords: Int): Double {
        if (totalWords == 0) return 0.0
        
        val lexicalDiversity = uniqueWordsCount.toDouble() / totalWords
        val avgWordLength = words.sumOf { it.length }.toDouble() / totalWords
        
        // Combine lexical diversity and average word length for complexity
        return (lexicalDiversity * 0.6) + (avgWordLength / 10.0 * 0.4)
    }

    private fun estimateSyllables(word: String): Int {
        if (word.isEmpty()) return 0
        
        // Simple syllable estimation (works better for English)
        val vowels = "aeiouAEIOU"
        var syllableCount = 0
        var previousWasVowel = false
        
        for (char in word) {
            val isVowel = vowels.contains(char)
            if (isVowel && !previousWasVowel) {
                syllableCount++
            }
            previousWasVowel = isVowel
        }
        
        // Adjust for silent 'e'
        if (word.endsWith('e', ignoreCase = true)) {
            syllableCount--
        }
        
        return maxOf(syllableCount, 1)
    }

    private fun extractOpenGraphData(doc: org.jsoup.nodes.Document, property: String): String? {
        return doc.select("meta[property=$property]").attr("content").takeIf { it.isNotBlank() }
    }
}