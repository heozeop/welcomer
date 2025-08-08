package com.welcomer.welcome.feed.controller

import com.welcomer.welcome.feed.model.*
import com.welcomer.welcome.feed.service.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * REST controller for feed operations
 */
@RestController
@RequestMapping("/api/v1/feed")
class FeedController(
    private val feedGenerationService: FeedGenerationService,
    private val cacheService: FeedCacheService,
    private val performanceService: FeedPerformanceService
) {

    /**
     * Generate personalized feed for user
     */
    @GetMapping("/{feedType}")
    suspend fun getFeed(
        @PathVariable feedType: String,
        @RequestParam(required = false) limit: Int = 50,
        @RequestParam(required = false) cursor: String? = null,
        @RequestParam(required = false) algorithmId: String? = null,
        @RequestParam(required = false, defaultValue = "false") refresh: Boolean = false,
        @RequestParam(required = false) contentType: String? = null,
        @RequestParam(required = false) topic: String? = null,
        @RequestParam(required = false) source: String? = null,
        @RequestParam(required = false) sort: String? = null,
        request: jakarta.servlet.http.HttpServletRequest
    ): ResponseEntity<FeedResponse> {
        // Extract user ID from request attributes (set by middleware)
        val userId = request.getAttribute("userId") as? String
            ?: return ResponseEntity.status(401).body(
                FeedResponse.error("User authentication required")
            )
        return try {
            val feedTypeEnum = FeedType.valueOf(feedType.uppercase())
            
            // Build filter parameters
            val filters = buildMap<String, Any> {
                contentType?.let { put("content_type", it) }
                topic?.let { put("topic", it) }
                source?.let { put("source", it) }
                sort?.let { put("sort", it) }
            }
            
            // Create cache key including filters
            val cacheKey = createCacheKey(userId, feedTypeEnum, limit, cursor, filters)
            
            // Check cache first (unless refresh is requested)
            if (!refresh) {
                cacheService.getCachedFeed(userId, feedTypeEnum)?.let { cachedFeed ->
                    // Only use cache if filters match
                    if (filtersMatch(cachedFeed, filters)) {
                        return ResponseEntity.ok(FeedResponse.from(cachedFeed, fromCache = true))
                    }
                }
            }

            // Generate new feed with filters
            val feedRequest = FeedGenerationRequest(
                userId = userId,
                feedType = feedTypeEnum,
                limit = minOf(limit, 100), // Cap at 100
                cursor = cursor,
                algorithmId = algorithmId,
                parameters = filters,
                refreshForced = refresh
            )

            val generatedFeed = performanceService.withPerformanceMonitoring("generate_feed") {
                feedGenerationService.generateFeed(feedRequest)
            }

            // Cache the generated feed
            cacheService.cacheFeed(generatedFeed)

            ResponseEntity.ok(FeedResponse.from(generatedFeed))
            
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                FeedResponse.error("Invalid feed type: $feedType. Valid types: ${FeedType.values().joinToString()}")
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                FeedResponse.error("Failed to generate feed: ${e.message}")
            )
        }
    }

    /**
     * Get available feed types for user
     */
    @GetMapping("/types")
    fun getFeedTypes(): ResponseEntity<FeedTypesResponse> {
        val feedTypes = FeedType.values().map { type ->
            FeedTypeInfo(
                type = type.name.lowercase(),
                displayName = type.name.lowercase().replaceFirstChar { it.uppercase() },
                description = getFeedTypeDescription(type)
            )
        }
        
        return ResponseEntity.ok(FeedTypesResponse(feedTypes))
    }

    /**
     * Get feed refresh - only new content since timestamp
     */
    @GetMapping("/{feedType}/refresh")
    suspend fun getRefresh(
        @PathVariable feedType: String,
        @RequestParam since: String, // ISO timestamp or cursor
        @RequestParam(required = false) limit: Int = 20,
        request: jakarta.servlet.http.HttpServletRequest
    ): ResponseEntity<FeedResponse> {
        val userId = request.getAttribute("userId") as? String
            ?: return ResponseEntity.status(401).body(
                FeedResponse.error("User authentication required")
            )

        return try {
            val feedTypeEnum = FeedType.valueOf(feedType.uppercase())
            val sinceInstant = try {
                java.time.Instant.parse(since)
            } catch (e: Exception) {
                return ResponseEntity.badRequest().body(
                    FeedResponse.error("Invalid since parameter. Use ISO timestamp format.")
                )
            }

            // Generate refresh feed (only new content)
            val feedRequest = FeedGenerationRequest(
                userId = userId,
                feedType = feedTypeEnum,
                limit = minOf(limit, 50), // Smaller limit for refreshes
                parameters = mapOf("since" to sinceInstant.toString()),
                refreshForced = true // Always generate fresh for refresh
            )

            val generatedFeed = performanceService.withPerformanceMonitoring("refresh_feed") {
                feedGenerationService.generateFeed(feedRequest)
            }

            ResponseEntity.ok(FeedResponse.from(generatedFeed))

        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                FeedResponse.error("Invalid feed type: $feedType")
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                FeedResponse.error("Failed to refresh feed: ${e.message}")
            )
        }
    }

    /**
     * Invalidate cache for specific feed type
     */
    @PostMapping("/{feedType}/refresh")
    suspend fun refreshFeed(
        @PathVariable feedType: String,
        request: jakarta.servlet.http.HttpServletRequest
    ): ResponseEntity<RefreshResponse> {
        val userId = request.getAttribute("userId") as? String
            ?: return ResponseEntity.status(401).body(
                RefreshResponse(
                    success = false,
                    message = "User authentication required",
                    timestamp = Instant.now()
                )
            )
        return try {
            val feedTypeEnum = FeedType.valueOf(feedType.uppercase())
            
            // Invalidate cache
            cacheService.invalidateFeed(userId, feedTypeEnum)
            
            ResponseEntity.ok(RefreshResponse(
                success = true,
                message = "Feed cache invalidated for $feedType",
                timestamp = Instant.now()
            ))
            
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                RefreshResponse(
                    success = false,
                    message = "Invalid feed type: $feedType",
                    timestamp = Instant.now()
                )
            )
        }
    }

    /**
     * Get feed generation statistics
     */
    @GetMapping("/stats")
    fun getFeedStats(): ResponseEntity<FeedStatsResponse> {
        val cacheStats = cacheService.getCacheStats()
        val performanceMetrics = performanceService.getPerformanceMetrics()
        
        return ResponseEntity.ok(
            FeedStatsResponse(
                cacheStats = cacheStats,
                performanceMetrics = performanceMetrics,
                timestamp = Instant.now()
            )
        )
    }

    /**
     * Update user feed preferences
     */
    @PutMapping("/preferences")
    suspend fun updateFeedPreferences(
        @RequestBody preferences: UpdatePreferencesRequest,
        request: jakarta.servlet.http.HttpServletRequest
    ): ResponseEntity<PreferencesResponse> {
        val userId = request.getAttribute("userId") as? String
            ?: return ResponseEntity.status(401).body(
                PreferencesResponse(
                    success = false,
                    message = "User authentication required",
                    timestamp = Instant.now()
                )
            )
        return try {
            // This would typically update user preferences in database
            // For now, just invalidate cache so new preferences take effect
            FeedType.values().forEach { feedType ->
                cacheService.invalidateFeed(userId, feedType)
            }
            
            ResponseEntity.ok(
                PreferencesResponse(
                    success = true,
                    message = "Feed preferences updated successfully",
                    timestamp = Instant.now()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                PreferencesResponse(
                    success = false,
                    message = "Failed to update preferences: ${e.message}",
                    timestamp = Instant.now()
                )
            )
        }
    }

    private fun getFeedTypeDescription(type: FeedType): String {
        return when (type) {
            FeedType.HOME -> "Personalized feed with content from followed users and recommendations"
            FeedType.FOLLOWING -> "Content from users you follow"
            FeedType.EXPLORE -> "Discover new content and creators"
            FeedType.TRENDING -> "Currently trending and popular content"
            FeedType.PERSONALIZED -> "AI-powered personalized recommendations"
        }
    }

    private fun createCacheKey(
        userId: String,
        feedType: FeedType,
        limit: Int,
        cursor: String?,
        filters: Map<String, Any>
    ): String {
        val filterString = filters.entries.sortedBy { it.key }
            .joinToString("&") { "${it.key}=${it.value}" }
        return "$userId:${feedType.name}:$limit:${cursor ?: ""}:$filterString"
    }

    private fun filtersMatch(feed: GeneratedFeed, filters: Map<String, Any>): Boolean {
        // Check if cached feed was generated with the same filters
        val feedFilters = feed.metadata.parameters
        return filters.all { (key, value) ->
            feedFilters[key] == value
        }
    }
}

// Response DTOs

data class FeedResponse(
    val success: Boolean,
    val data: FeedData? = null,
    val error: String? = null,
    val fromCache: Boolean = false,
    val timestamp: Instant = Instant.now()
) {
    companion object {
        fun from(feed: GeneratedFeed, fromCache: Boolean = false): FeedResponse {
            return FeedResponse(
                success = true,
                data = FeedData(
                    userId = feed.userId,
                    feedType = feed.feedType.name.lowercase(),
                    entries = feed.entries.map { entry ->
                        FeedEntryData(
                            id = entry.id,
                            contentId = entry.content.id,
                            authorId = entry.content.authorId,
                            contentType = entry.content.contentType.name.lowercase(),
                            textContent = entry.content.textContent,
                            linkUrl = entry.content.linkUrl,
                            mediaAttachments = entry.content.mediaAttachments?.map { media ->
                                MediaAttachmentData(
                                    id = media.id,
                                    mediaType = media.mediaType.name.lowercase(),
                                    fileUrl = media.fileUrl,
                                    thumbnailUrl = media.thumbnailUrl,
                                    width = media.width,
                                    height = media.height
                                )
                            } ?: emptyList(),
                            score = entry.score,
                            rank = entry.rank,
                            reasons = entry.reasons.map { reason ->
                                ReasonData(
                                    type = reason.type.name.lowercase(),
                                    description = reason.description,
                                    weight = reason.weight
                                )
                            },
                            sourceType = entry.sourceType.name.lowercase(),
                            boosted = entry.boosted,
                            createdAt = entry.content.createdAt,
                            generatedAt = entry.generatedAt
                        )
                    },
                    metadata = MetadataData(
                        algorithmId = feed.metadata.algorithmId,
                        algorithmVersion = feed.metadata.algorithmVersion,
                        generationDuration = feed.metadata.generationDuration,
                        contentCount = feed.metadata.contentCount,
                        candidateCount = feed.metadata.candidateCount,
                        generatedAt = feed.metadata.generatedAt
                    ),
                    nextCursor = feed.nextCursor,
                    hasMore = feed.hasMore
                ),
                fromCache = fromCache
            )
        }

        fun error(message: String): FeedResponse {
            return FeedResponse(success = false, error = message)
        }
    }
}

data class FeedData(
    val userId: String,
    val feedType: String,
    val entries: List<FeedEntryData>,
    val metadata: MetadataData,
    val nextCursor: String?,
    val hasMore: Boolean
)

data class FeedEntryData(
    val id: String,
    val contentId: String,
    val authorId: String,
    val contentType: String,
    val textContent: String?,
    val linkUrl: String?,
    val mediaAttachments: List<MediaAttachmentData>,
    val score: Double,
    val rank: Int,
    val reasons: List<ReasonData>,
    val sourceType: String,
    val boosted: Boolean,
    val createdAt: Instant,
    val generatedAt: Instant
)

data class MediaAttachmentData(
    val id: String,
    val mediaType: String,
    val fileUrl: String,
    val thumbnailUrl: String?,
    val width: Int?,
    val height: Int?
)

data class ReasonData(
    val type: String,
    val description: String,
    val weight: Double
)

data class MetadataData(
    val algorithmId: String,
    val algorithmVersion: String,
    val generationDuration: Long,
    val contentCount: Int,
    val candidateCount: Int,
    val generatedAt: Instant
)

data class FeedTypesResponse(
    val feedTypes: List<FeedTypeInfo>,
    val timestamp: Instant = Instant.now()
)

data class FeedTypeInfo(
    val type: String,
    val displayName: String,
    val description: String
)

data class RefreshResponse(
    val success: Boolean,
    val message: String,
    val timestamp: Instant
)

data class FeedStatsResponse(
    val cacheStats: CacheStats,
    val performanceMetrics: Map<String, Any>,
    val timestamp: Instant
)

data class PreferencesResponse(
    val success: Boolean,
    val message: String,
    val timestamp: Instant
)

data class UpdatePreferencesRequest(
    val interests: List<String>? = null,
    val preferredContentTypes: Set<String>? = null,
    val blockedUsers: Set<String>? = null,
    val blockedTopics: Set<String>? = null,
    val languagePreferences: List<String>? = null
)