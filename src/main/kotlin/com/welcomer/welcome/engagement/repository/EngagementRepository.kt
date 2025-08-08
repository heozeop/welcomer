package com.welcomer.welcome.engagement.repository

import com.welcomer.welcome.engagement.model.*
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository interface for engagement data persistence
 */
interface EngagementRepository {
    /**
     * Record a new engagement event
     */
    suspend fun record(event: EngagementEvent): String

    /**
     * Batch record multiple engagement events
     */
    suspend fun recordBatch(events: List<EngagementEvent>): List<String>

    /**
     * Get engagement events by content ID
     */
    suspend fun findByContentId(
        contentId: String,
        startTime: Instant? = null,
        endTime: Instant? = null,
        limit: Int = 100
    ): List<EngagementEvent>

    /**
     * Get engagement events by user ID
     */
    suspend fun findByUserId(
        userId: String,
        startTime: Instant? = null,
        endTime: Instant? = null,
        limit: Int = 100
    ): List<EngagementEvent>

    /**
     * Get engagement events by type
     */
    suspend fun findByType(
        type: EngagementType,
        startTime: Instant? = null,
        endTime: Instant? = null,
        limit: Int = 100
    ): List<EngagementEvent>

    /**
     * Check if a similar engagement exists (for duplicate detection)
     */
    suspend fun existsSimilar(
        userId: String,
        contentId: String,
        type: EngagementType,
        windowStart: Instant
    ): Boolean

    /**
     * Count engagements by type for content
     */
    suspend fun countByContentAndType(
        contentId: String,
        type: EngagementType,
        startTime: Instant? = null,
        endTime: Instant? = null
    ): Long

    /**
     * Count unique users who engaged with content
     */
    suspend fun countUniqueUsers(
        contentId: String,
        startTime: Instant? = null,
        endTime: Instant? = null
    ): Long

    /**
     * Get aggregated metrics for content
     */
    suspend fun getContentMetrics(
        contentId: String,
        startTime: Instant,
        endTime: Instant
    ): ContentEngagementMetrics?

    /**
     * Get user engagement summary
     */
    suspend fun getUserSummary(
        userId: String,
        startTime: Instant,
        endTime: Instant
    ): UserEngagementSummary?

    /**
     * Delete old engagement events
     */
    suspend fun deleteOlderThan(timestamp: Instant): Int

    /**
     * Get top engaged content
     */
    suspend fun getTopEngagedContent(
        startTime: Instant,
        endTime: Instant,
        limit: Int = 10
    ): List<Pair<String, Long>>
}

/**
 * In-memory implementation of EngagementRepository
 * In production, this would use a time-series database like InfluxDB or TimescaleDB
 */
@Repository
class InMemoryEngagementRepository : EngagementRepository {

    private val events = ConcurrentHashMap<String, EngagementEvent>()
    private val contentIndex = ConcurrentHashMap<String, MutableSet<String>>()
    private val userIndex = ConcurrentHashMap<String, MutableSet<String>>()
    private val typeIndex = ConcurrentHashMap<EngagementType, MutableSet<String>>()

    override suspend fun record(event: EngagementEvent): String {
        val eventId = event.id
        events[eventId] = event

        // Update indices
        contentIndex.computeIfAbsent(event.contentId) { mutableSetOf() }.add(eventId)
        userIndex.computeIfAbsent(event.userId) { mutableSetOf() }.add(eventId)
        typeIndex.computeIfAbsent(event.type) { mutableSetOf() }.add(eventId)

        return eventId
    }

    override suspend fun recordBatch(events: List<EngagementEvent>): List<String> {
        return events.map { record(it) }
    }

    override suspend fun findByContentId(
        contentId: String,
        startTime: Instant?,
        endTime: Instant?,
        limit: Int
    ): List<EngagementEvent> {
        val eventIds = contentIndex[contentId] ?: return emptyList()
        
        return eventIds.mapNotNull { events[it] }
            .filter { event ->
                (startTime == null || event.timestamp.isAfter(startTime)) &&
                (endTime == null || event.timestamp.isBefore(endTime))
            }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    override suspend fun findByUserId(
        userId: String,
        startTime: Instant?,
        endTime: Instant?,
        limit: Int
    ): List<EngagementEvent> {
        val eventIds = userIndex[userId] ?: return emptyList()
        
        return eventIds.mapNotNull { events[it] }
            .filter { event ->
                (startTime == null || event.timestamp.isAfter(startTime)) &&
                (endTime == null || event.timestamp.isBefore(endTime))
            }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    override suspend fun findByType(
        type: EngagementType,
        startTime: Instant?,
        endTime: Instant?,
        limit: Int
    ): List<EngagementEvent> {
        val eventIds = typeIndex[type] ?: return emptyList()
        
        return eventIds.mapNotNull { events[it] }
            .filter { event ->
                (startTime == null || event.timestamp.isAfter(startTime)) &&
                (endTime == null || event.timestamp.isBefore(endTime))
            }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    override suspend fun existsSimilar(
        userId: String,
        contentId: String,
        type: EngagementType,
        windowStart: Instant
    ): Boolean {
        val userEvents = userIndex[userId] ?: return false
        
        return userEvents.any { eventId ->
            val event = events[eventId]
            event != null && 
            event.contentId == contentId && 
            event.type == type && 
            event.timestamp.isAfter(windowStart)
        }
    }

    override suspend fun countByContentAndType(
        contentId: String,
        type: EngagementType,
        startTime: Instant?,
        endTime: Instant?
    ): Long {
        return findByContentId(contentId, startTime, endTime, Int.MAX_VALUE)
            .count { it.type == type }
            .toLong()
    }

    override suspend fun countUniqueUsers(
        contentId: String,
        startTime: Instant?,
        endTime: Instant?
    ): Long {
        return findByContentId(contentId, startTime, endTime, Int.MAX_VALUE)
            .map { it.userId }
            .toSet()
            .size
            .toLong()
    }

    override suspend fun getContentMetrics(
        contentId: String,
        startTime: Instant,
        endTime: Instant
    ): ContentEngagementMetrics {
        val contentEvents = findByContentId(contentId, startTime, endTime, Int.MAX_VALUE)
        
        val totalViews = contentEvents.count { it.type == EngagementType.VIEW }.toLong()
        val uniqueViews = contentEvents.filter { it.type == EngagementType.VIEW }
            .map { it.userId }.toSet().size.toLong()
        val totalLikes = contentEvents.count { it.type == EngagementType.LIKE }.toLong()
        val totalComments = contentEvents.count { it.type == EngagementType.COMMENT }.toLong()
        val totalShares = contentEvents.count { it.type == EngagementType.SHARE }.toLong()
        val totalBookmarks = contentEvents.count { it.type == EngagementType.BOOKMARK }.toLong()
        val reportCount = contentEvents.count { it.type == EngagementType.REPORT }.toLong()
        val hideCount = contentEvents.count { it.type == EngagementType.HIDE }.toLong()
        
        val dwellTimes = contentEvents
            .filter { it.type == EngagementType.DWELL_TIME }
            .mapNotNull { it.metadata["duration"] as? Long }
        
        val averageDwellTime = if (dwellTimes.isNotEmpty()) {
            java.time.Duration.ofMillis(dwellTimes.average().toLong())
        } else null
        
        val engagementRate = if (totalViews > 0) {
            (totalLikes + totalComments + totalShares).toDouble() / totalViews
        } else 0.0
        
        val clickCount = contentEvents.count { it.type == EngagementType.CLICK }.toLong()
        val clickThroughRate = if (totalViews > 0) {
            clickCount.toDouble() / totalViews
        } else 0.0
        
        val hideRate = if (totalViews > 0) {
            hideCount.toDouble() / totalViews
        } else 0.0
        
        val lastEngagement = contentEvents.maxByOrNull { it.timestamp }?.timestamp
        
        return ContentEngagementMetrics(
            contentId = contentId,
            totalViews = totalViews,
            uniqueViews = uniqueViews,
            totalLikes = totalLikes,
            totalComments = totalComments,
            totalShares = totalShares,
            totalBookmarks = totalBookmarks,
            averageDwellTime = averageDwellTime,
            engagementRate = engagementRate,
            clickThroughRate = clickThroughRate,
            hideRate = hideRate,
            reportCount = reportCount,
            lastEngagementAt = lastEngagement,
            periodStart = startTime,
            periodEnd = endTime
        )
    }

    override suspend fun getUserSummary(
        userId: String,
        startTime: Instant,
        endTime: Instant
    ): UserEngagementSummary {
        val userEvents = findByUserId(userId, startTime, endTime, Int.MAX_VALUE)
        
        val engagementsByType = userEvents.groupBy { it.type }
            .mapValues { it.value.size.toLong() }
        
        val topContent = userEvents.groupBy { it.contentId }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }
        
        val sessionDurations = userEvents
            .groupBy { it.sessionId }
            .mapNotNull { (sessionId, events) ->
                if (sessionId != null && events.size > 1) {
                    val duration = java.time.Duration.between(
                        events.minOf { it.timestamp },
                        events.maxOf { it.timestamp }
                    )
                    duration
                } else null
            }
        
        val avgSessionDuration = if (sessionDurations.isNotEmpty()) {
            val totalMillis = sessionDurations.sumOf { it.toMillis() }
            java.time.Duration.ofMillis(totalMillis / sessionDurations.size)
        } else null
        
        val lastActivity = userEvents.maxByOrNull { it.timestamp }?.timestamp
        
        return UserEngagementSummary(
            userId = userId,
            totalEngagements = userEvents.size.toLong(),
            engagementsByType = engagementsByType,
            topEngagedContent = topContent,
            averageSessionDuration = avgSessionDuration,
            lastActivityAt = lastActivity,
            periodStart = startTime,
            periodEnd = endTime
        )
    }

    override suspend fun deleteOlderThan(timestamp: Instant): Int {
        val toDelete = events.filter { it.value.timestamp.isBefore(timestamp) }
        
        toDelete.forEach { (eventId, event) ->
            events.remove(eventId)
            contentIndex[event.contentId]?.remove(eventId)
            userIndex[event.userId]?.remove(eventId)
            typeIndex[event.type]?.remove(eventId)
        }
        
        return toDelete.size
    }

    override suspend fun getTopEngagedContent(
        startTime: Instant,
        endTime: Instant,
        limit: Int
    ): List<Pair<String, Long>> {
        return events.values
            .filter { event ->
                event.timestamp.isAfter(startTime) && 
                event.timestamp.isBefore(endTime) &&
                event.type in setOf(
                    EngagementType.VIEW,
                    EngagementType.LIKE,
                    EngagementType.COMMENT,
                    EngagementType.SHARE
                )
            }
            .groupBy { it.contentId }
            .mapValues { it.value.size.toLong() }
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }
}