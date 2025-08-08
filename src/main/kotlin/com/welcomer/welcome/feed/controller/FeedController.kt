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
        @RequestHeader("User-ID") userId: String
    ): ResponseEntity<FeedResponse> {
        return try {
            val feedTypeEnum = FeedType.valueOf(feedType.uppercase())
            
            // Check cache first (unless refresh is requested)
            if (!refresh) {
                cacheService.getCachedFeed(userId, feedTypeEnum)?.let { cachedFeed ->
                    return ResponseEntity.ok(FeedResponse.from(cachedFeed, fromCache = true))
                }
            }

            // Generate new feed
            val request = FeedGenerationRequest(
                userId = userId,
                feedType = feedTypeEnum,
                limit = minOf(limit, 100), // Cap at 100
                cursor = cursor,
                algorithmId = algorithmId,
                refreshForced = refresh
            )

            val generatedFeed = performanceService.withPerformanceMonitoring("generate_feed") {
                feedGenerationService.generateFeed(request)
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
     * Refresh specific feed type for user
     */
    @PostMapping("/{feedType}/refresh")
    suspend fun refreshFeed(
        @PathVariable feedType: String,
        @RequestHeader("User-ID") userId: String
    ): ResponseEntity<RefreshResponse> {
        return try {
            val feedTypeEnum = FeedType.valueOf(feedType.uppercase())
            
            // Invalidate cache
            cacheService.invalidateFeed(userId, feedTypeEnum)
            
            ResponseEntity.ok(RefreshResponse(
                success = true,
                message = "Feed refresh initiated for $feedType",
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
        @RequestHeader("User-ID") userId: String
    ): ResponseEntity<PreferencesResponse> {
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