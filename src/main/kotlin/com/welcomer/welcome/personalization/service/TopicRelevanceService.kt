package com.welcomer.welcome.personalization.service

import org.springframework.stereotype.Service
import kotlin.math.*

/**
 * Enhanced topic relevance calculation service with semantic understanding and hierarchical scoring
 */
interface TopicRelevanceService {
    /**
     * Calculate comprehensive topic relevance between content and user preferences
     */
    suspend fun calculateEnhancedTopicRelevance(
        itemTopics: List<String>,
        userPreferences: Map<String, Double>,
        config: TopicRelevanceConfig = TopicRelevanceConfig()
    ): TopicRelevanceResult

    /**
     * Calculate similarity between two topics
     */
    fun calculateTopicSimilarity(topic1: String, topic2: String): Double

    /**
     * Get topic category for hierarchical matching
     */
    fun getTopicCategory(topic: String): TopicCategory

    /**
     * Calculate weighted relevance based on topic specificity
     */
    fun calculateSpecificityWeight(topic: String, userPreferences: Map<String, Double>): Double
}

/**
 * Configuration for topic relevance calculation
 */
data class TopicRelevanceConfig(
    val exactMatchWeight: Double = 1.0,
    val partialMatchWeight: Double = 0.7,
    val categoryMatchWeight: Double = 0.5,
    val semanticMatchWeight: Double = 0.6,
    val specificityBonus: Double = 0.2,
    val diversityPenalty: Double = 0.1,
    val enableSemanticMatching: Boolean = true,
    val enableCategoryFallback: Boolean = true,
    val minimumSimilarityThreshold: Double = 0.3
)

/**
 * Result of topic relevance calculation with detailed breakdown
 */
data class TopicRelevanceResult(
    val overallRelevance: Double,
    val topicBreakdown: List<TopicRelevanceBreakdown>,
    val dominantCategories: List<TopicCategory>,
    val specificityScore: Double,
    val diversityScore: Double,
    val explanations: List<String>
)

/**
 * Detailed breakdown for each topic match
 */
data class TopicRelevanceBreakdown(
    val itemTopic: String,
    val matchType: TopicMatchType,
    val matchedUserTopic: String?,
    val baseScore: Double,
    val weightedScore: Double,
    val similarityScore: Double,
    val category: TopicCategory
)

/**
 * Types of topic matches
 */
enum class TopicMatchType {
    EXACT_MATCH,
    PARTIAL_MATCH, 
    SEMANTIC_MATCH,
    CATEGORY_MATCH,
    NO_MATCH
}

/**
 * Topic categories for hierarchical matching
 */
enum class TopicCategory {
    TECHNOLOGY,
    SCIENCE,
    ENTERTAINMENT,
    SPORTS,
    POLITICS,
    BUSINESS,
    HEALTH,
    LIFESTYLE,
    EDUCATION,
    NEWS,
    ARTS,
    TRAVEL,
    FOOD,
    FINANCE,
    GAMING,
    FASHION,
    MUSIC,
    MOVIES,
    BOOKS,
    OTHER
}

/**
 * Default implementation with enhanced topic relevance calculation
 */
@Service
class DefaultTopicRelevanceService : TopicRelevanceService {

    // Topic hierarchy and semantic relationships
    private val topicHierarchy = mapOf(
        TopicCategory.TECHNOLOGY to listOf(
            "programming", "software", "ai", "machine-learning", "data-science", 
            "web-development", "mobile", "cybersecurity", "blockchain", "cloud"
        ),
        TopicCategory.SCIENCE to listOf(
            "physics", "chemistry", "biology", "astronomy", "medicine", 
            "research", "laboratory", "experiment", "discovery"
        ),
        TopicCategory.ENTERTAINMENT to listOf(
            "movies", "tv-shows", "celebrities", "music", "gaming", 
            "comedy", "drama", "documentary", "streaming"
        ),
        TopicCategory.SPORTS to listOf(
            "football", "basketball", "soccer", "tennis", "baseball",
            "olympics", "fitness", "workout", "athlete", "competition"
        ),
        TopicCategory.POLITICS to listOf(
            "election", "government", "policy", "democracy", "legislation",
            "voting", "campaign", "debate", "political"
        ),
        TopicCategory.BUSINESS to listOf(
            "startup", "entrepreneurship", "investing", "marketing", "finance",
            "economy", "stock-market", "cryptocurrency", "business-strategy"
        ),
        TopicCategory.HEALTH to listOf(
            "wellness", "nutrition", "mental-health", "fitness", "medical",
            "healthcare", "diet", "exercise", "therapy", "medicine"
        ),
        TopicCategory.LIFESTYLE to listOf(
            "fashion", "food", "travel", "home", "relationships", "parenting",
            "personal-development", "productivity", "minimalism"
        ),
        TopicCategory.EDUCATION to listOf(
            "learning", "university", "school", "course", "tutorial",
            "study", "knowledge", "academic", "scholarship"
        )
    )

    // Semantic similarity mappings (simplified - in production would use embeddings)
    private val semanticSimilarities = mapOf(
        "ai" to mapOf(
            "machine-learning" to 0.9,
            "data-science" to 0.8,
            "programming" to 0.7,
            "technology" to 0.6,
            "automation" to 0.8
        ),
        "programming" to mapOf(
            "software" to 0.9,
            "coding" to 0.95,
            "development" to 0.85,
            "web-development" to 0.8,
            "mobile-development" to 0.75
        ),
        "fitness" to mapOf(
            "health" to 0.8,
            "workout" to 0.9,
            "exercise" to 0.95,
            "wellness" to 0.7,
            "sports" to 0.6
        )
    )

    override suspend fun calculateEnhancedTopicRelevance(
        itemTopics: List<String>,
        userPreferences: Map<String, Double>,
        config: TopicRelevanceConfig
    ): TopicRelevanceResult {
        if (userPreferences.isEmpty() || itemTopics.isEmpty()) {
            return createNeutralResult(itemTopics)
        }

        val topicBreakdowns = mutableListOf<TopicRelevanceBreakdown>()
        val explanations = mutableListOf<String>()

        // Calculate relevance for each item topic
        itemTopics.forEach { itemTopic ->
            val breakdown = calculateSingleTopicRelevance(itemTopic, userPreferences, config)
            topicBreakdowns.add(breakdown)
            
            if (breakdown.matchType != TopicMatchType.NO_MATCH) {
                explanations.add(generateTopicExplanation(breakdown))
            }
        }

        // Calculate overall scores
        val overallRelevance = calculateOverallRelevance(topicBreakdowns, config)
        val dominantCategories = getDominantCategories(topicBreakdowns)
        val specificityScore = calculateAverageSpecificity(itemTopics, userPreferences)
        val diversityScore = calculateTopicDiversity(itemTopics)

        // Add diversity adjustments
        val adjustedRelevance = applyDiversityAdjustments(
            overallRelevance, 
            diversityScore, 
            specificityScore, 
            config
        )

        return TopicRelevanceResult(
            overallRelevance = adjustedRelevance,
            topicBreakdown = topicBreakdowns,
            dominantCategories = dominantCategories,
            specificityScore = specificityScore,
            diversityScore = diversityScore,
            explanations = explanations
        )
    }

    override fun calculateTopicSimilarity(topic1: String, topic2: String): Double {
        val normalizedTopic1 = topic1.lowercase().replace("-", " ")
        val normalizedTopic2 = topic2.lowercase().replace("-", " ")

        // Exact match
        if (normalizedTopic1 == normalizedTopic2) return 1.0

        // Check semantic similarity
        semanticSimilarities[normalizedTopic1]?.get(normalizedTopic2)?.let { return it }
        semanticSimilarities[normalizedTopic2]?.get(normalizedTopic1)?.let { return it }

        // Substring matching with position weighting
        val containsScore = when {
            normalizedTopic1.contains(normalizedTopic2) -> {
                val startIndex = normalizedTopic1.indexOf(normalizedTopic2)
                // Higher score for matches at the beginning
                0.8 - (startIndex.toDouble() / normalizedTopic1.length * 0.3)
            }
            normalizedTopic2.contains(normalizedTopic1) -> {
                val startIndex = normalizedTopic2.indexOf(normalizedTopic1)
                0.8 - (startIndex.toDouble() / normalizedTopic2.length * 0.3)
            }
            else -> 0.0
        }

        // Levenshtein distance for typos and variations
        val levDistance = calculateLevenshteinDistance(normalizedTopic1, normalizedTopic2)
        val maxLen = maxOf(normalizedTopic1.length, normalizedTopic2.length)
        val levScore = if (maxLen > 0) 1.0 - (levDistance.toDouble() / maxLen) else 0.0

        return maxOf(containsScore, levScore * 0.6).coerceIn(0.0, 1.0)
    }

    override fun getTopicCategory(topic: String): TopicCategory {
        val normalizedTopic = topic.lowercase().replace("-", " ")
        
        return topicHierarchy.entries.firstOrNull { (_, keywords) ->
            keywords.any { keyword ->
                normalizedTopic.contains(keyword) || keyword.contains(normalizedTopic)
            }
        }?.key ?: TopicCategory.OTHER
    }

    override fun calculateSpecificityWeight(topic: String, userPreferences: Map<String, Double>): Double {
        // More specific topics (longer, hyphenated) get higher weights
        val lengthScore = min(topic.length.toDouble() / 20.0, 1.0)
        val hyphenScore = if (topic.contains("-")) 0.2 else 0.0
        val capitalScore = if (topic.any { it.isUpperCase() }) 0.1 else 0.0
        
        // Topics that appear in user preferences get specificity boost
        val preferenceBoost = if (userPreferences.containsKey(topic)) 0.3 else 0.0
        
        return (lengthScore + hyphenScore + capitalScore + preferenceBoost).coerceIn(0.0, 1.0)
    }

    // Private helper methods

    private fun calculateSingleTopicRelevance(
        itemTopic: String,
        userPreferences: Map<String, Double>,
        config: TopicRelevanceConfig
    ): TopicRelevanceBreakdown {
        val category = getTopicCategory(itemTopic)
        var bestMatch: Pair<String, Double>? = null
        var matchType = TopicMatchType.NO_MATCH
        var similarityScore = 0.0

        // 1. Check for exact matches
        userPreferences[itemTopic]?.let { score ->
            bestMatch = itemTopic to score
            matchType = TopicMatchType.EXACT_MATCH
            similarityScore = 1.0
        }

        // 2. Check for partial matches if no exact match
        if (bestMatch == null) {
            val partialMatches = userPreferences.entries.mapNotNull { (userTopic, score) ->
                val similarity = calculateTopicSimilarity(itemTopic, userTopic)
                if (similarity > config.minimumSimilarityThreshold) {
                    Triple(userTopic, score, similarity)
                } else null
            }.sortedByDescending { it.third }

            partialMatches.firstOrNull()?.let { (userTopic, score, similarity) ->
                bestMatch = userTopic to score
                similarityScore = similarity
                matchType = when {
                    similarity > 0.8 -> TopicMatchType.SEMANTIC_MATCH
                    similarity > 0.6 -> TopicMatchType.PARTIAL_MATCH
                    else -> TopicMatchType.SEMANTIC_MATCH
                }
            }
        }

        // 3. Check for category matches if no specific match
        if (bestMatch == null && config.enableCategoryFallback && category != TopicCategory.OTHER) {
            val categoryMatches = userPreferences.entries.filter { (userTopic, _) ->
                getTopicCategory(userTopic) == category
            }.maxByOrNull { it.value }

            categoryMatches?.let { (userTopic, score) ->
                bestMatch = userTopic to score
                matchType = TopicMatchType.CATEGORY_MATCH
                similarityScore = 0.4
            }
        }

        // Calculate final scores
        val baseScore = bestMatch?.second ?: 0.0
        val matchWeight = when (matchType) {
            TopicMatchType.EXACT_MATCH -> config.exactMatchWeight
            TopicMatchType.PARTIAL_MATCH -> config.partialMatchWeight
            TopicMatchType.SEMANTIC_MATCH -> config.semanticMatchWeight
            TopicMatchType.CATEGORY_MATCH -> config.categoryMatchWeight
            TopicMatchType.NO_MATCH -> 0.0
        }

        val specificityWeight = calculateSpecificityWeight(itemTopic, userPreferences)
        val weightedScore = (baseScore * matchWeight * similarityScore * (1.0 + specificityWeight * config.specificityBonus)).coerceIn(0.0, 1.0)

        return TopicRelevanceBreakdown(
            itemTopic = itemTopic,
            matchType = matchType,
            matchedUserTopic = bestMatch?.first,
            baseScore = baseScore,
            weightedScore = weightedScore,
            similarityScore = similarityScore,
            category = category
        )
    }

    private fun calculateOverallRelevance(
        breakdowns: List<TopicRelevanceBreakdown>,
        config: TopicRelevanceConfig
    ): Double {
        if (breakdowns.isEmpty()) return 0.5

        val scores = breakdowns.map { it.weightedScore }
        val hasMatches = breakdowns.any { it.matchType != TopicMatchType.NO_MATCH }
        
        if (!hasMatches) return 0.3 // Low relevance if no matches

        // Use weighted average with exponential scaling for high matches
        val avgScore = scores.average()
        val maxScore = scores.maxOrNull() ?: 0.0
        val matchCount = breakdowns.count { it.matchType != TopicMatchType.NO_MATCH }
        
        // Boost for multiple good matches
        val matchBonus = min(matchCount * 0.1, 0.3)
        
        return ((avgScore * 0.7) + (maxScore * 0.3) + matchBonus).coerceIn(0.0, 1.0)
    }

    private fun getDominantCategories(breakdowns: List<TopicRelevanceBreakdown>): List<TopicCategory> {
        return breakdowns
            .filter { it.matchType != TopicMatchType.NO_MATCH }
            .groupBy { it.category }
            .entries
            .sortedByDescending { it.value.size }
            .take(3)
            .map { it.key }
    }

    private fun calculateAverageSpecificity(topics: List<String>, userPreferences: Map<String, Double>): Double {
        if (topics.isEmpty()) return 0.0
        return topics.map { calculateSpecificityWeight(it, userPreferences) }.average()
    }

    private fun calculateTopicDiversity(topics: List<String>): Double {
        if (topics.size <= 1) return 0.0
        
        val categories = topics.map { getTopicCategory(it) }.toSet()
        return categories.size.toDouble() / topics.size
    }

    private fun applyDiversityAdjustments(
        baseRelevance: Double,
        diversityScore: Double,
        specificityScore: Double,
        config: TopicRelevanceConfig
    ): Double {
        // Slight boost for diverse topics
        val diversityBonus = diversityScore * 0.1
        
        // Slight boost for specific topics
        val specificityBonus = specificityScore * config.specificityBonus
        
        return (baseRelevance + diversityBonus + specificityBonus).coerceIn(0.0, 1.0)
    }

    private fun generateTopicExplanation(breakdown: TopicRelevanceBreakdown): String {
        return when (breakdown.matchType) {
            TopicMatchType.EXACT_MATCH -> 
                "Exact match for your interest in '${breakdown.itemTopic}'"
            TopicMatchType.PARTIAL_MATCH -> 
                "Related to your interest in '${breakdown.matchedUserTopic}'"
            TopicMatchType.SEMANTIC_MATCH -> 
                "Similar to your interest in '${breakdown.matchedUserTopic}'"
            TopicMatchType.CATEGORY_MATCH -> 
                "Matches your interest in ${breakdown.category.name.lowercase()} topics"
            TopicMatchType.NO_MATCH -> ""
        }
    }

    private fun createNeutralResult(itemTopics: List<String>): TopicRelevanceResult {
        val neutralBreakdowns = itemTopics.map { topic ->
            TopicRelevanceBreakdown(
                itemTopic = topic,
                matchType = TopicMatchType.NO_MATCH,
                matchedUserTopic = null,
                baseScore = 0.5,
                weightedScore = 0.5,
                similarityScore = 0.0,
                category = getTopicCategory(topic)
            )
        }

        return TopicRelevanceResult(
            overallRelevance = 0.5,
            topicBreakdown = neutralBreakdowns,
            dominantCategories = emptyList(),
            specificityScore = 0.5,
            diversityScore = 0.5,
            explanations = listOf("No user preferences available for topic matching")
        )
    }

    private fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i-1] == s2[j-1]) {
                    dp[i-1][j-1]
                } else {
                    1 + minOf(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
                }
            }
        }
        
        return dp[s1.length][s2.length]
    }
}