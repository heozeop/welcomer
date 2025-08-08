package com.welcomer.welcome.feed.service

import com.welcomer.welcome.feed.model.*
import com.welcomer.welcome.ingestion.model.StoredContent
import org.springframework.stereotype.Service
import kotlin.math.*

/**
 * Service for applying diversity rules to ensure feed variety
 */
interface FeedDiversityService {
    /**
     * Apply diversity rules to scored content list
     * Returns reordered list maintaining quality while ensuring diversity
     */
    fun applyDiversityRules(
        scoredContent: List<Pair<ContentCandidate, Double>>,
        config: DiversityConfig
    ): List<Pair<ContentCandidate, Double>>

    /**
     * Check if adding content would violate diversity constraints
     */
    fun violatesDiversityConstraints(
        content: ContentCandidate,
        existingFeed: List<FeedEntry>,
        config: DiversityConfig,
        position: Int
    ): Boolean

    /**
     * Calculate diversity penalty for content based on current feed state
     */
    fun calculateDiversityPenalty(
        content: ContentCandidate,
        existingFeed: List<FeedEntry>,
        config: DiversityConfig,
        position: Int
    ): Double

    /**
     * Ensure balanced representation of different content types
     */
    fun ensureContentTypeBalance(
        scoredContent: List<Pair<ContentCandidate, Double>>,
        targetDistribution: Map<String, Double>? = null
    ): List<Pair<ContentCandidate, Double>>
}

@Service
class DefaultFeedDiversityService : FeedDiversityService {

    companion object {
        // Diversity penalty factors
        private const val AUTHOR_PENALTY_FACTOR = 0.15
        private const val TOPIC_PENALTY_FACTOR = 0.10
        private const val CONTENT_TYPE_PENALTY_FACTOR = 0.05
        private const val PROXIMITY_PENALTY_MULTIPLIER = 2.0
        
        // Default content type distribution (can be overridden)
        private val DEFAULT_CONTENT_TYPE_DISTRIBUTION = mapOf(
            "text" to 0.5,
            "image" to 0.3,
            "video" to 0.15,
            "link" to 0.05
        )
    }

    override fun applyDiversityRules(
        scoredContent: List<Pair<ContentCandidate, Double>>,
        config: DiversityConfig
    ): List<Pair<ContentCandidate, Double>> {
        if (scoredContent.isEmpty()) return scoredContent

        // Sort by score initially (highest first)
        val sortedContent = scoredContent.sortedByDescending { it.second }
        val diversifiedFeed = mutableListOf<Pair<ContentCandidate, Double>>()
        val authorCounts = mutableMapOf<String, Int>()
        val topicCounts = mutableMapOf<String, Int>()
        val contentTypeCounts = mutableMapOf<String, Int>()
        val authorLastPositions = mutableMapOf<String, Int>()
        val topicLastPositions = mutableMapOf<String, MutableList<Int>>()

        // Process each content item
        for (candidate in sortedContent) {
            val content = candidate.first.content
            val position = diversifiedFeed.size

            // Check diversity constraints
            if (shouldIncludeContent(candidate.first, diversifiedFeed, config, authorCounts, topicCounts, contentTypeCounts)) {
                // Apply diversity penalty to score
                val diversityPenalty = calculateDiversityPenaltyInternal(
                    candidate.first,
                    diversifiedFeed,
                    config,
                    position,
                    authorLastPositions,
                    topicLastPositions
                )
                
                val adjustedScore = candidate.second * (1.0 - diversityPenalty)
                
                // Add to feed
                diversifiedFeed.add(candidate.first to adjustedScore)
                
                // Update counters
                authorCounts[content.authorId] = authorCounts.getOrDefault(content.authorId, 0) + 1
                content.tags.forEach { tag ->
                    topicCounts[tag] = topicCounts.getOrDefault(tag, 0) + 1
                }
                contentTypeCounts[content.contentType.name] = contentTypeCounts.getOrDefault(content.contentType.name, 0) + 1
                
                // Update positions
                authorLastPositions[content.authorId] = position
                content.tags.forEach { tag ->
                    topicLastPositions.getOrPut(tag) { mutableListOf() }.add(position)
                }
            }
        }

        // Apply content type balancing if enabled
        return if (config.enforceContentTypeBalance) {
            ensureContentTypeBalance(diversifiedFeed)
        } else {
            diversifiedFeed
        }
    }

    override fun violatesDiversityConstraints(
        content: ContentCandidate,
        existingFeed: List<FeedEntry>,
        config: DiversityConfig,
        position: Int
    ): Boolean {
        val authorCount = existingFeed.count { it.content.authorId == content.content.authorId }
        if (authorCount >= config.maxSameAuthor) return true

        // Check topic constraints
        val contentTopics = content.content.tags.toSet()
        contentTopics.forEach { topic ->
            val topicCount = existingFeed.count { entry -> entry.content.tags.contains(topic) }
            if (topicCount >= config.maxSameTopic) return true
        }

        // Check content type constraints
        val contentTypeCount = existingFeed.count { it.content.contentType == content.content.contentType }
        if (contentTypeCount >= config.maxSameContentType) return true

        // Check spacing constraints
        if (violatesSpacingConstraints(content, existingFeed, config, position)) return true

        return false
    }

    override fun calculateDiversityPenalty(
        content: ContentCandidate,
        existingFeed: List<FeedEntry>,
        config: DiversityConfig,
        position: Int
    ): Double {
        val authorLastPositions = mutableMapOf<String, Int>()
        val topicLastPositions = mutableMapOf<String, MutableList<Int>>()
        
        // Build position maps from existing feed
        existingFeed.forEachIndexed { index, entry ->
            authorLastPositions[entry.content.authorId] = index
            entry.content.tags.forEach { tag ->
                topicLastPositions.getOrPut(tag) { mutableListOf() }.add(index)
            }
        }
        
        return calculateDiversityPenaltyInternal(content, existingFeed.map { 
            it.content.let { content -> 
                ContentCandidate(content) to it.score 
            } 
        }, config, position, authorLastPositions, topicLastPositions)
    }

    override fun ensureContentTypeBalance(
        scoredContent: List<Pair<ContentCandidate, Double>>,
        targetDistribution: Map<String, Double>?
    ): List<Pair<ContentCandidate, Double>> {
        val distribution = targetDistribution ?: DEFAULT_CONTENT_TYPE_DISTRIBUTION
        val totalItems = scoredContent.size
        
        // Calculate target counts for each content type
        val targetCounts = distribution.mapValues { (_, percentage) ->
            (totalItems * percentage).roundToInt()
        }
        
        // Group content by type
        val contentByType = scoredContent.groupBy { it.first.content.contentType.name.lowercase() }
        
        val balancedFeed = mutableListOf<Pair<ContentCandidate, Double>>()
        val typeCounts = mutableMapOf<String, Int>()
        
        // Sort all content by score
        val allContentSorted = scoredContent.sortedByDescending { it.second }
        
        // Distribute content maintaining score order but ensuring type balance
        for (item in allContentSorted) {
            val contentType = item.first.content.contentType.name.lowercase()
            val currentCount = typeCounts.getOrDefault(contentType, 0)
            val targetCount = targetCounts.getOrDefault(contentType, Int.MAX_VALUE)
            
            if (currentCount < targetCount || balancedFeed.size >= totalItems * 0.8) {
                // Allow if under target or we're near the end
                balancedFeed.add(item)
                typeCounts[contentType] = currentCount + 1
                
                if (balancedFeed.size >= totalItems) break
            }
        }
        
        return balancedFeed
    }

    // Private helper methods

    private fun shouldIncludeContent(
        content: ContentCandidate,
        existingFeed: List<Pair<ContentCandidate, Double>>,
        config: DiversityConfig,
        authorCounts: Map<String, Int>,
        topicCounts: Map<String, Int>,
        contentTypeCounts: Map<String, Int>
    ): Boolean {
        // Check author limit
        val authorCount = authorCounts.getOrDefault(content.content.authorId, 0)
        if (authorCount >= config.maxSameAuthor) return false

        // Check topic limits
        content.content.tags.forEach { topic ->
            val topicCount = topicCounts.getOrDefault(topic, 0)
            if (topicCount >= config.maxSameTopic) return false
        }

        // Check content type limit
        val contentTypeCount = contentTypeCounts.getOrDefault(content.content.contentType.name, 0)
        if (contentTypeCount >= config.maxSameContentType) return false

        return true
    }

    private fun calculateDiversityPenaltyInternal(
        content: ContentCandidate,
        existingFeed: List<Pair<ContentCandidate, Double>>,
        config: DiversityConfig,
        position: Int,
        authorLastPositions: Map<String, Int>,
        topicLastPositions: Map<String, List<Int>>
    ): Double {
        var penalty = 0.0

        // Author proximity penalty
        authorLastPositions[content.content.authorId]?.let { lastPos ->
            val distance = position - lastPos
            if (distance < config.authorSpacing) {
                val proximityFactor = (config.authorSpacing - distance).toDouble() / config.authorSpacing
                penalty += AUTHOR_PENALTY_FACTOR * proximityFactor * PROXIMITY_PENALTY_MULTIPLIER
            }
        }

        // Topic proximity penalty
        content.content.tags.forEach { topic ->
            topicLastPositions[topic]?.let { positions ->
                positions.forEach { lastPos ->
                    val distance = position - lastPos
                    if (distance < config.topicSpacing) {
                        val proximityFactor = (config.topicSpacing - distance).toDouble() / config.topicSpacing
                        penalty += TOPIC_PENALTY_FACTOR * proximityFactor
                    }
                }
            }
        }

        // Content type frequency penalty
        val contentTypeCount = existingFeed.count { it.first.content.contentType == content.content.contentType }
        if (contentTypeCount > 0) {
            val typeDensity = contentTypeCount.toDouble() / existingFeed.size
            if (typeDensity > 0.4) { // If more than 40% is same type
                penalty += CONTENT_TYPE_PENALTY_FACTOR * (typeDensity - 0.4)
            }
        }

        return minOf(1.0, penalty) // Cap at 100% penalty
    }

    private fun violatesSpacingConstraints(
        content: ContentCandidate,
        existingFeed: List<FeedEntry>,
        config: DiversityConfig,
        position: Int
    ): Boolean {
        // Check author spacing
        for (i in maxOf(0, position - config.authorSpacing + 1) until position) {
            if (i < existingFeed.size && existingFeed[i].content.authorId == content.content.authorId) {
                return true
            }
        }

        // Check topic spacing
        val contentTopics = content.content.tags.toSet()
        for (i in maxOf(0, position - config.topicSpacing + 1) until position) {
            if (i < existingFeed.size) {
                val existingTopics = existingFeed[i].content.tags.toSet()
                if (contentTopics.intersect(existingTopics).isNotEmpty()) {
                    return true
                }
            }
        }

        return false
    }
}