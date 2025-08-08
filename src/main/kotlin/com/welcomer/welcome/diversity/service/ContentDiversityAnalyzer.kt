package com.welcomer.welcome.diversity.service

import com.welcomer.welcome.diversity.model.*
import com.welcomer.welcome.feed.model.FeedEntry
import com.welcomer.welcome.ingestion.model.StoredContent
import com.welcomer.welcome.ingestion.model.TopicCategory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.*

/**
 * Service for analyzing content diversity across multiple dimensions
 */
interface ContentDiversityAnalyzer {
    /**
     * Calculate diversity scores for candidate content items relative to recent feed history
     */
    suspend fun calculateDiversityScores(
        candidateItems: List<StoredContent>,
        recentFeedHistory: List<FeedEntry>,
        config: DiversityConfig = DiversityConfig()
    ): List<DiversityAnalysisResult>

    /**
     * Analyze echo chamber characteristics in user's content consumption
     */
    suspend fun analyzeEchoChamber(
        userId: String,
        recentFeedHistory: List<FeedEntry>,
        config: DiversityConfig = DiversityConfig()
    ): EchoChamberAnalysis

    /**
     * Extract content features for diversity analysis
     */
    suspend fun extractContentFeatures(contentItems: List<StoredContent>): List<ContentFeatures>

    /**
     * Build content distribution from historical feed
     */
    suspend fun buildContentDistribution(
        userId: String,
        feedHistory: List<FeedEntry>,
        timeWindow: com.welcomer.welcome.diversity.model.TimeWindow
    ): com.welcomer.welcome.diversity.model.ContentDistribution
}

/**
 * Default implementation of ContentDiversityAnalyzer
 */
@Service
class DefaultContentDiversityAnalyzer : ContentDiversityAnalyzer {

    override suspend fun calculateDiversityScores(
        candidateItems: List<StoredContent>,
        recentFeedHistory: List<FeedEntry>,
        config: DiversityConfig
    ): List<DiversityAnalysisResult> {
        if (recentFeedHistory.size < config.minimumHistorySize) {
            // Not enough history for meaningful diversity analysis
            return candidateItems.map { createDefaultDiversityResult(it.id) }
        }

        // Extract features from recent history and candidates
        val historicalContent = recentFeedHistory.map { it.content }
        val historicalFeatures = extractContentFeatures(historicalContent)
        val candidateFeatures = extractContentFeatures(candidateItems)

        // Build historical distribution
        val distribution = buildContentDistribution(
            userId = recentFeedHistory.firstOrNull()?.content?.authorId ?: "",
            feedHistory = recentFeedHistory,
            timeWindow = com.welcomer.welcome.diversity.model.TimeWindow(
                startTime = Instant.now().minus(config.timeWindowDays.toLong(), ChronoUnit.DAYS),
                endTime = Instant.now(),
                duration = config.timeWindowDays * 24 * 60 * 60 * 1000L
            )
        )

        // Calculate diversity scores for each candidate
        return candidateFeatures.map { candidateFeature ->
            calculateSingleItemDiversity(candidateFeature, historicalFeatures, distribution, config)
        }
    }

    override suspend fun analyzeEchoChamber(
        userId: String,
        recentFeedHistory: List<FeedEntry>,
        config: DiversityConfig
    ): EchoChamberAnalysis {
        if (recentFeedHistory.isEmpty()) {
            return EchoChamberAnalysis(
                isEchoChamber = false,
                severity = EchoChamberSeverity.NONE,
                dominantPerspectives = emptyList(),
                missingPerspectives = emptyList(),
                topicConcentration = 0.0,
                sourceConcentration = 0.0,
                recommendations = emptyList()
            )
        }

        val features = extractContentFeatures(recentFeedHistory.map { it.content })
        
        // Calculate concentration metrics
        val topicConcentration = calculateConcentration(
            features.flatMap { it.topics }
        )
        
        val sourceConcentration = calculateConcentration(
            features.map { it.authorId }
        )
        
        val perspectiveConcentration = calculateConcentration(
            features.map { it.perspective }
        )

        // Determine severity
        val avgConcentration = (topicConcentration + sourceConcentration + perspectiveConcentration) / 3
        val severity = when {
            avgConcentration > 0.8 -> EchoChamberSeverity.SEVERE
            avgConcentration > 0.6 -> EchoChamberSeverity.MODERATE
            avgConcentration > 0.4 -> EchoChamberSeverity.MILD
            else -> EchoChamberSeverity.NONE
        }

        val isEchoChamber = severity != EchoChamberSeverity.NONE

        // Find dominant and missing perspectives
        val perspectiveCounts = features.groupingBy { it.perspective }.eachCount()
        val totalCount = features.size
        val dominantPerspectives = perspectiveCounts
            .filter { it.value.toDouble() / totalCount > 0.4 }
            .keys.toList()

        val allPerspectives = listOf("liberal", "conservative", "centrist", "neutral", "progressive")
        val missingPerspectives = allPerspectives.filter { perspective ->
            perspectiveCounts[perspective]?.toDouble()?.div(totalCount) ?: 0.0 < 0.1
        }

        val recommendations = generateEchoChamberRecommendations(
            severity, dominantPerspectives, missingPerspectives, topicConcentration, sourceConcentration
        )

        return EchoChamberAnalysis(
            isEchoChamber = isEchoChamber,
            severity = severity,
            dominantPerspectives = dominantPerspectives,
            missingPerspectives = missingPerspectives,
            topicConcentration = topicConcentration,
            sourceConcentration = sourceConcentration,
            recommendations = recommendations
        )
    }

    override suspend fun extractContentFeatures(contentItems: List<StoredContent>): List<ContentFeatures> {
        return contentItems.map { content ->
            val metadata = content.extractedMetadata
            
            ContentFeatures(
                contentId = content.id,
                authorId = content.authorId,
                topics = metadata?.topics?.map { it.name } ?: content.tags,
                topicCategories = metadata?.topics?.map { it.category } ?: listOf(TopicCategory.OTHER),
                contentType = content.contentType.name.lowercase(),
                sentiment = metadata?.sentiment?.overallSentiment?.name?.lowercase(),
                language = metadata?.language?.detectedLanguage ?: content.languageCode,
                source = extractSourceDomain(content),
                perspective = inferPerspective(content, metadata),
                createdAt = content.createdAt,
                engagementPattern = null // Would be populated from engagement service
            )
        }
    }

    override suspend fun buildContentDistribution(
        userId: String,
        feedHistory: List<FeedEntry>,
        timeWindow: com.welcomer.welcome.diversity.model.TimeWindow
    ): com.welcomer.welcome.diversity.model.ContentDistribution {
        val features = extractContentFeatures(feedHistory.map { it.content })
        val totalItems = features.size

        return com.welcomer.welcome.diversity.model.ContentDistribution(
            userId = userId,
            topicDistribution = calculateDistribution(features.flatMap { it.topics }),
            categoryDistribution = calculateCategoryDistribution(features.flatMap { it.topicCategories }),
            sourceDistribution = calculateDistribution(features.map { it.authorId }),
            contentTypeDistribution = calculateDistribution(features.map { it.contentType }),
            sentimentDistribution = calculateDistribution(features.mapNotNull { it.sentiment }),
            languageDistribution = calculateDistribution(features.mapNotNull { it.language }),
            temporalDistribution = calculateTemporalDistribution(features),
            totalItems = totalItems,
            timeWindow = timeWindow
        )
    }

    // Private helper methods

    private fun calculateSingleItemDiversity(
        candidateFeature: ContentFeatures,
        historicalFeatures: List<ContentFeatures>,
        distribution: ContentDistribution,
        config: DiversityConfig
    ): DiversityAnalysisResult {
        val dimensionScores = mutableMapOf<DiversityDimension, Double>()

        // Topic diversity
        dimensionScores[DiversityDimension.TOPIC] = calculateTopicDiversity(
            candidateFeature.topics, distribution.topicDistribution
        )

        // Source diversity
        dimensionScores[DiversityDimension.SOURCE] = calculateSourceDiversity(
            candidateFeature.authorId, distribution.sourceDistribution
        )

        // Content type diversity
        dimensionScores[DiversityDimension.CONTENT_TYPE] = calculateContentTypeDiversity(
            candidateFeature.contentType, distribution.contentTypeDistribution
        )

        // Sentiment diversity
        dimensionScores[DiversityDimension.SENTIMENT] = calculateSentimentDiversity(
            candidateFeature.sentiment, distribution.sentimentDistribution
        )

        // Perspective diversity
        dimensionScores[DiversityDimension.PERSPECTIVE] = calculatePerspectiveDiversity(
            candidateFeature.perspective, historicalFeatures
        )

        // Language diversity
        dimensionScores[DiversityDimension.LANGUAGE] = calculateLanguageDiversity(
            candidateFeature.language, distribution.languageDistribution
        )

        // Recency diversity
        dimensionScores[DiversityDimension.RECENCY] = calculateRecencyDiversity(
            candidateFeature.createdAt, historicalFeatures
        )

        // Calculate overall score as weighted average
        val overallScore = dimensionScores.entries.sumOf { (dimension, score) ->
            score * (config.dimensionWeights[dimension] ?: 0.0)
        }

        val recommendations = generateDiversityRecommendations(dimensionScores, config)

        return DiversityAnalysisResult(
            contentId = candidateFeature.contentId,
            overallDiversityScore = overallScore,
            dimensionScores = dimensionScores,
            recommendations = recommendations
        )
    }

    private fun calculateTopicDiversity(
        candidateTopics: List<String>,
        historicalTopicDistribution: Map<String, Double>
    ): Double {
        if (candidateTopics.isEmpty()) return 0.5 // Neutral score for content without topics
        
        // Calculate how novel these topics are compared to historical distribution
        val noveltyScores = candidateTopics.map { topic ->
            val historicalFreq = historicalTopicDistribution[topic] ?: 0.0
            // Higher score for topics that appear less frequently in history
            1.0 - min(historicalFreq, 1.0)
        }
        
        return noveltyScores.average()
    }

    private fun calculateSourceDiversity(
        candidateSource: String,
        historicalSourceDistribution: Map<String, Double>
    ): Double {
        val historicalFreq = historicalSourceDistribution[candidateSource] ?: 0.0
        return 1.0 - min(historicalFreq, 1.0)
    }

    private fun calculateContentTypeDiversity(
        candidateType: String,
        historicalTypeDistribution: Map<String, Double>
    ): Double {
        val historicalFreq = historicalTypeDistribution[candidateType] ?: 0.0
        return 1.0 - min(historicalFreq, 1.0)
    }

    private fun calculateSentimentDiversity(
        candidateSentiment: String?,
        historicalSentimentDistribution: Map<String, Double>
    ): Double {
        candidateSentiment ?: return 0.5
        val historicalFreq = historicalSentimentDistribution[candidateSentiment] ?: 0.0
        return 1.0 - min(historicalFreq, 1.0)
    }

    private fun calculatePerspectiveDiversity(
        candidatePerspective: String,
        historicalFeatures: List<ContentFeatures>
    ): Double {
        if (historicalFeatures.isEmpty()) return 1.0
        
        val perspectiveCounts = historicalFeatures.groupingBy { it.perspective }.eachCount()
        val totalCount = historicalFeatures.size.toDouble()
        val candidateFreq = perspectiveCounts[candidatePerspective]?.div(totalCount) ?: 0.0
        
        return 1.0 - min(candidateFreq, 1.0)
    }

    private fun calculateLanguageDiversity(
        candidateLanguage: String?,
        historicalLanguageDistribution: Map<String, Double>
    ): Double {
        candidateLanguage ?: return 0.5
        val historicalFreq = historicalLanguageDistribution[candidateLanguage] ?: 0.0
        return 1.0 - min(historicalFreq, 1.0)
    }

    private fun calculateRecencyDiversity(
        candidateTime: Instant,
        historicalFeatures: List<ContentFeatures>
    ): Double {
        if (historicalFeatures.isEmpty()) return 1.0
        
        // Calculate temporal distribution and see how the candidate fits
        val timeBuckets = historicalFeatures.groupBy { 
            it.createdAt.truncatedTo(ChronoUnit.HOURS) 
        }
        
        val candidateBucket = candidateTime.truncatedTo(ChronoUnit.HOURS)
        val bucketCount = timeBuckets[candidateBucket]?.size ?: 0
        val avgBucketSize = timeBuckets.values.map { it.size }.average()
        
        // Higher diversity score for time periods with less content
        return 1.0 - min(bucketCount / avgBucketSize, 1.0)
    }

    private fun calculateConcentration(items: List<String>): Double {
        if (items.isEmpty()) return 0.0
        
        val counts = items.groupingBy { it }.eachCount()
        val total = items.size.toDouble()
        
        // Calculate Herfindahl-Hirschman Index (concentration measure)
        return counts.values.sumOf { count ->
            val proportion = count / total
            proportion * proportion
        }
    }

    private fun <T> calculateDistribution(items: List<T>): Map<String, Double> {
        if (items.isEmpty()) return emptyMap()
        
        val counts = items.groupingBy { it.toString() }.eachCount()
        val total = items.size.toDouble()
        
        return counts.mapValues { it.value / total }
    }

    private fun calculateCategoryDistribution(categories: List<TopicCategory>): Map<TopicCategory, Double> {
        if (categories.isEmpty()) return emptyMap()
        
        val counts = categories.groupingBy { it }.eachCount()
        val total = categories.size.toDouble()
        
        return counts.mapValues { it.value / total }
    }

    private fun calculateTemporalDistribution(features: List<ContentFeatures>): Map<String, Double> {
        if (features.isEmpty()) return emptyMap()
        
        val hourCounts = features.groupingBy { 
            it.createdAt.atZone(java.time.ZoneId.systemDefault()).hour.toString()
        }.eachCount()
        
        val total = features.size.toDouble()
        return hourCounts.mapValues { it.value / total }
    }

    private fun extractSourceDomain(content: StoredContent): String {
        return content.linkUrl?.let { url ->
            try {
                java.net.URL(url).host
            } catch (e: Exception) {
                "unknown"
            }
        } ?: "internal"
    }

    private fun inferPerspective(content: StoredContent, metadata: com.welcomer.welcome.ingestion.model.ExtractedMetadata?): String {
        // Simple perspective inference - would be enhanced with NLP models
        val sentiment = metadata?.sentiment?.overallSentiment?.name?.lowercase()
        val topics = metadata?.topics?.map { it.name } ?: emptyList()
        
        return when {
            topics.any { it.contains("conservative", ignoreCase = true) } -> "conservative"
            topics.any { it.contains("liberal", ignoreCase = true) } -> "liberal"
            topics.any { it.contains("progressive", ignoreCase = true) } -> "progressive"
            sentiment == "negative" -> "critical"
            sentiment == "positive" -> "supportive"
            else -> "neutral"
        }
    }

    private fun generateDiversityRecommendations(
        dimensionScores: Map<DiversityDimension, Double>,
        config: DiversityConfig
    ): List<DiversityRecommendation> {
        val recommendations = mutableListOf<DiversityRecommendation>()
        
        dimensionScores.forEach { (dimension, score) ->
            if (score < config.diversityThreshold) {
                val priority = when {
                    score < 0.3 -> RecommendationPriority.HIGH
                    score < 0.5 -> RecommendationPriority.MEDIUM
                    else -> RecommendationPriority.LOW
                }
                
                recommendations.add(
                    DiversityRecommendation(
                        dimension = dimension,
                        type = RecommendationType.INCREASE_VARIETY,
                        description = "Low diversity in ${dimension.name.lowercase()} dimension",
                        priority = priority,
                        suggestedAction = "Include content from different ${dimension.name.lowercase()} categories",
                        impactScore = 1.0 - score
                    )
                )
            }
        }
        
        return recommendations
    }

    private fun generateEchoChamberRecommendations(
        severity: EchoChamberSeverity,
        dominantPerspectives: List<String>,
        missingPerspectives: List<String>,
        topicConcentration: Double,
        sourceConcentration: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (severity != EchoChamberSeverity.NONE) {
            recommendations.add("Diversify content sources to include different perspectives")
            
            if (topicConcentration > 0.7) {
                recommendations.add("Explore content from different topic categories")
            }
            
            if (sourceConcentration > 0.7) {
                recommendations.add("Follow more authors with different viewpoints")
            }
            
            if (missingPerspectives.isNotEmpty()) {
                recommendations.add("Consider content representing ${missingPerspectives.joinToString(", ")} perspectives")
            }
            
            if (severity == EchoChamberSeverity.SEVERE) {
                recommendations.add("Your content consumption shows very limited diversity - consider actively seeking opposing viewpoints")
            }
        }
        
        return recommendations
    }

    private fun createDefaultDiversityResult(contentId: String): DiversityAnalysisResult {
        // Default neutral diversity scores when not enough historical data
        val neutralScore = 0.5
        return DiversityAnalysisResult(
            contentId = contentId,
            overallDiversityScore = neutralScore,
            dimensionScores = DiversityDimension.values().associateWith { neutralScore },
            recommendations = emptyList()
        )
    }
}