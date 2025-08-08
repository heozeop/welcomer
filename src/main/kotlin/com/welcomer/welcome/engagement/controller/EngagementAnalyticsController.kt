package com.welcomer.welcome.engagement.controller

import com.welcomer.welcome.engagement.model.*
import com.welcomer.welcome.engagement.service.EngagementTrackingService
import com.welcomer.welcome.engagement.service.MetricsAggregationService
import com.welcomer.welcome.engagement.repository.EngagementRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * REST controller for engagement tracking and analytics
 */
@RestController
@RequestMapping("/api/v1/engagement")
@CrossOrigin(origins = ["*"])
class EngagementAnalyticsController(
    private val trackingService: EngagementTrackingService,
    private val metricsService: MetricsAggregationService,
    private val engagementRepository: EngagementRepository
) {

    /**
     * Track a user engagement event
     */
    @PostMapping("/track")
    suspend fun trackEngagement(
        @RequestBody request: TrackEngagementRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<EngagementResponse> {
        val userId = httpRequest.getAttribute("userId") as? String
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(EngagementResponse(false, message = "User authentication required"))

        return try {
            val response = trackingService.trackEngagement(userId, request)
            
            if (response.success) {
                ResponseEntity.ok(response)
            } else {
                ResponseEntity.badRequest().body(response)
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(EngagementResponse(false, message = "Failed to track engagement: ${e.message}"))
        }
    }

    /**
     * Batch track multiple engagement events
     */
    @PostMapping("/track/batch")
    suspend fun trackEngagementBatch(
        @RequestBody requests: List<TrackEngagementRequest>,
        httpRequest: HttpServletRequest
    ): ResponseEntity<BatchEngagementResponse> {
        val userId = httpRequest.getAttribute("userId") as? String
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BatchEngagementResponse(success = false, message = "User authentication required"))

        return try {
            val responses = trackingService.trackEngagementBatch(userId, requests)
            val successCount = responses.count { it.success }
            
            ResponseEntity.ok(BatchEngagementResponse(
                success = successCount == responses.size,
                totalRequests = requests.size,
                successfulRequests = successCount,
                failedRequests = responses.size - successCount,
                responses = responses
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BatchEngagementResponse(
                    success = false,
                    message = "Failed to track batch engagements: ${e.message}"
                ))
        }
    }

    /**
     * Get engagement metrics for content
     */
    @GetMapping("/content/{contentId}/metrics")
    suspend fun getContentMetrics(
        @PathVariable contentId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: LocalDateTime
    ): ResponseEntity<ContentMetricsResponse> {
        return try {
            val start = startTime.toInstant(ZoneOffset.UTC)
            val end = endTime.toInstant(ZoneOffset.UTC)
            
            val metrics = trackingService.getContentEngagementMetrics(contentId, start, end)
            
            if (metrics != null) {
                ResponseEntity.ok(ContentMetricsResponse(
                    success = true,
                    contentId = contentId,
                    metrics = metrics
                ))
            } else {
                ResponseEntity.ok(ContentMetricsResponse(
                    success = true,
                    contentId = contentId,
                    metrics = createEmptyMetrics(contentId, start, end),
                    message = "No engagement data found for the specified period"
                ))
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ContentMetricsResponse(
                    success = false,
                    contentId = contentId,
                    message = "Failed to retrieve metrics: ${e.message}"
                ))
        }
    }

    /**
     * Get windowed metrics for content
     */
    @GetMapping("/content/{contentId}/metrics/windowed")
    suspend fun getWindowedMetrics(
        @PathVariable contentId: String,
        @RequestParam window: TimeWindow
    ): ResponseEntity<WindowedMetricsResponse> {
        return try {
            val metrics = metricsService.getMetrics(contentId, window)
            
            ResponseEntity.ok(WindowedMetricsResponse(
                success = true,
                contentId = contentId,
                metrics = metrics
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(WindowedMetricsResponse(
                    success = false,
                    contentId = contentId,
                    message = "Failed to retrieve windowed metrics: ${e.message}"
                ))
        }
    }

    /**
     * Get trending content
     */
    @GetMapping("/trending")
    suspend fun getTrendingContent(
        @RequestParam(defaultValue = "DAILY") window: TimeWindow,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<TrendingContentResponse> {
        return try {
            val trending = metricsService.getTrendingContent(window, limit)
            
            ResponseEntity.ok(TrendingContentResponse(
                success = true,
                window = window,
                content = trending.map { (contentId, score) ->
                    TrendingItem(contentId, score)
                }
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(TrendingContentResponse(
                    success = false,
                    window = window,
                    message = "Failed to retrieve trending content: ${e.message}"
                ))
        }
    }

    /**
     * Get user engagement history
     */
    @GetMapping("/user/history")
    suspend fun getUserEngagementHistory(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: LocalDateTime?,
        @RequestParam(defaultValue = "100") limit: Int,
        httpRequest: HttpServletRequest
    ): ResponseEntity<UserEngagementHistoryResponse> {
        val userId = httpRequest.getAttribute("userId") as? String
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(UserEngagementHistoryResponse(
                    success = false,
                    message = "User authentication required"
                ))

        return try {
            val start = startTime?.toInstant(ZoneOffset.UTC)
            val end = endTime?.toInstant(ZoneOffset.UTC)
            
            val history = trackingService.getUserEngagementHistory(userId, start, end, limit)
            
            ResponseEntity.ok(UserEngagementHistoryResponse(
                success = true,
                userId = userId,
                engagements = history,
                totalCount = history.size
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(UserEngagementHistoryResponse(
                    success = false,
                    userId = userId,
                    message = "Failed to retrieve engagement history: ${e.message}"
                ))
        }
    }

    /**
     * Get user engagement summary
     */
    @GetMapping("/user/summary")
    suspend fun getUserEngagementSummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: LocalDateTime,
        httpRequest: HttpServletRequest
    ): ResponseEntity<UserSummaryResponse> {
        val userId = httpRequest.getAttribute("userId") as? String
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(UserSummaryResponse(
                    success = false,
                    message = "User authentication required"
                ))

        return try {
            val start = startTime.toInstant(ZoneOffset.UTC)
            val end = endTime.toInstant(ZoneOffset.UTC)
            
            val summary = engagementRepository.getUserSummary(userId, start, end)
            
            ResponseEntity.ok(UserSummaryResponse(
                success = true,
                userId = userId,
                summary = summary
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(UserSummaryResponse(
                    success = false,
                    userId = userId,
                    message = "Failed to retrieve user summary: ${e.message}"
                ))
        }
    }

    /**
     * Get analytics for multiple content items
     */
    @PostMapping("/analytics/batch")
    suspend fun getBatchAnalytics(
        @RequestBody request: BatchAnalyticsRequest
    ): ResponseEntity<BatchAnalyticsResponse> {
        return try {
            val metrics = metricsService.batchComputeMetrics(request.contentIds, request.window)
            
            ResponseEntity.ok(BatchAnalyticsResponse(
                success = true,
                metrics = metrics,
                totalRequested = request.contentIds.size,
                totalReturned = metrics.size
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BatchAnalyticsResponse(
                    success = false,
                    message = "Failed to retrieve batch analytics: ${e.message}"
                ))
        }
    }

    /**
     * Export engagement data
     */
    @GetMapping("/export")
    suspend fun exportEngagementData(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: LocalDateTime,
        @RequestParam(defaultValue = "CSV") format: ExportFormat,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ExportResponse> {
        // Check for admin permissions
        val userId = httpRequest.getAttribute("userId") as? String
        val isAdmin = userId?.startsWith("admin_") == true
        
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ExportResponse(
                    success = false,
                    message = "Admin permissions required for data export"
                ))
        }

        return try {
            val start = startTime.toInstant(ZoneOffset.UTC)
            val end = endTime.toInstant(ZoneOffset.UTC)
            
            // In production, this would generate actual export file
            val exportUrl = generateExport(start, end, format)
            
            ResponseEntity.ok(ExportResponse(
                success = true,
                exportUrl = exportUrl,
                format = format,
                message = "Export generated successfully"
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ExportResponse(
                    success = false,
                    message = "Failed to generate export: ${e.message}"
                ))
        }
    }

    // Helper methods

    private fun createEmptyMetrics(contentId: String, startTime: Instant, endTime: Instant): ContentEngagementMetrics {
        return ContentEngagementMetrics(
            contentId = contentId,
            totalViews = 0,
            uniqueViews = 0,
            totalLikes = 0,
            totalComments = 0,
            totalShares = 0,
            totalBookmarks = 0,
            averageDwellTime = null,
            engagementRate = 0.0,
            clickThroughRate = 0.0,
            hideRate = 0.0,
            reportCount = 0,
            lastEngagementAt = null,
            periodStart = startTime,
            periodEnd = endTime
        )
    }

    private fun generateExport(startTime: Instant, endTime: Instant, format: ExportFormat): String {
        // In production, would generate actual export file
        return "/exports/engagement_${startTime.epochSecond}_${endTime.epochSecond}.${format.name.lowercase()}"
    }
}

// Response models

data class BatchEngagementResponse(
    val success: Boolean,
    val totalRequests: Int = 0,
    val successfulRequests: Int = 0,
    val failedRequests: Int = 0,
    val responses: List<EngagementResponse> = emptyList(),
    val message: String? = null
)

data class ContentMetricsResponse(
    val success: Boolean,
    val contentId: String,
    val metrics: ContentEngagementMetrics? = null,
    val message: String? = null
)

data class WindowedMetricsResponse(
    val success: Boolean,
    val contentId: String,
    val metrics: WindowedMetrics? = null,
    val message: String? = null
)

data class TrendingContentResponse(
    val success: Boolean,
    val window: TimeWindow,
    val content: List<TrendingItem> = emptyList(),
    val message: String? = null
)

data class TrendingItem(
    val contentId: String,
    val score: Double
)

data class UserEngagementHistoryResponse(
    val success: Boolean,
    val userId: String? = null,
    val engagements: List<EngagementEvent> = emptyList(),
    val totalCount: Int = 0,
    val message: String? = null
)

data class UserSummaryResponse(
    val success: Boolean,
    val userId: String? = null,
    val summary: UserEngagementSummary? = null,
    val message: String? = null
)

data class BatchAnalyticsRequest(
    val contentIds: List<String>,
    val window: TimeWindow = TimeWindow.DAILY
)

data class BatchAnalyticsResponse(
    val success: Boolean,
    val metrics: Map<String, WindowedMetrics> = emptyMap(),
    val totalRequested: Int = 0,
    val totalReturned: Int = 0,
    val message: String? = null
)

enum class ExportFormat {
    CSV,
    JSON,
    EXCEL
}

data class ExportResponse(
    val success: Boolean,
    val exportUrl: String? = null,
    val format: ExportFormat? = null,
    val message: String? = null
)