package com.welcomer.welcome.feed.repository

import com.welcomer.welcome.feed.service.FeedAnalyticsEvent
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Repository interface for analytics event persistence and aggregation
 */
interface AnalyticsRepository {
    /**
     * Record a single analytics event
     */
    suspend fun recordEvent(event: FeedAnalyticsEvent)

    /**
     * Record multiple analytics events in batch
     */
    suspend fun recordEventsBatch(events: List<FeedAnalyticsEvent>)

    /**
     * Get events within a time range
     */
    suspend fun getEvents(startTime: Instant, endTime: Instant): List<FeedAnalyticsEvent>

    /**
     * Get events for a specific user
     */
    suspend fun getUserEvents(userId: String, startTime: Instant, endTime: Instant): List<FeedAnalyticsEvent>

    /**
     * Get aggregated metrics for a time period
     */
    suspend fun getAggregatedMetrics(startTime: Instant, endTime: Instant): Map<String, Any>

    /**
     * Increment a daily metric counter
     */
    suspend fun incrementMetric(date: Instant, metricName: String, value: Long)

    /**
     * Get unique users within time range
     */
    suspend fun getUniqueUsers(startTime: Instant, endTime: Instant): Set<String>

    /**
     * Get event count by type
     */
    suspend fun getEventCountByType(startTime: Instant, endTime: Instant): Map<String, Long>

    /**
     * Delete old events (for data retention)
     */
    suspend fun deleteEventsBefore(cutoffTime: Instant): Int
}

/**
 * In-memory implementation of AnalyticsRepository.
 * In production, this would use a time-series database like InfluxDB, TimescaleDB, or ClickHouse
 * for high-volume analytics data.
 */
@Repository
class InMemoryAnalyticsRepository : AnalyticsRepository {

    // Store events with timestamp indexing for efficient time-range queries
    private val events = ConcurrentHashMap<String, FeedAnalyticsEvent>()
    
    // Index by user for user-specific queries
    private val userIndex = ConcurrentHashMap<String, MutableSet<String>>()
    
    // Index by timestamp (day) for daily aggregations
    private val dailyMetrics = ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicLong>>()
    
    // Time-based index for efficient range queries
    private val timeIndex = ConcurrentHashMap<Long, MutableSet<String>>() // epoch hour -> event IDs

    override suspend fun recordEvent(event: FeedAnalyticsEvent) {
        events[event.id] = event
        
        // Update user index
        userIndex.computeIfAbsent(event.userId) { mutableSetOf() }.add(event.id)
        
        // Update time index (group by hour for efficient range queries)
        val hourKey = event.timestamp.epochSecond / 3600
        timeIndex.computeIfAbsent(hourKey) { mutableSetOf() }.add(event.id)
    }

    override suspend fun recordEventsBatch(events: List<FeedAnalyticsEvent>) {
        events.forEach { recordEvent(it) }
    }

    override suspend fun getEvents(startTime: Instant, endTime: Instant): List<FeedAnalyticsEvent> {
        val startHour = startTime.epochSecond / 3600
        val endHour = endTime.epochSecond / 3600
        
        val candidateEventIds = mutableSetOf<String>()
        
        // Collect events from relevant time buckets
        for (hour in startHour..endHour) {
            timeIndex[hour]?.let { eventIds ->
                candidateEventIds.addAll(eventIds)
            }
        }
        
        // Filter by exact time range and return
        return candidateEventIds
            .mapNotNull { events[it] }
            .filter { event ->
                event.timestamp.isAfter(startTime) && event.timestamp.isBefore(endTime)
            }
            .sortedBy { it.timestamp }
    }

    override suspend fun getUserEvents(userId: String, startTime: Instant, endTime: Instant): List<FeedAnalyticsEvent> {
        val userEventIds = userIndex[userId] ?: return emptyList()
        
        return userEventIds
            .mapNotNull { events[it] }
            .filter { event ->
                event.timestamp.isAfter(startTime) && event.timestamp.isBefore(endTime)
            }
            .sortedBy { it.timestamp }
    }

    override suspend fun getAggregatedMetrics(startTime: Instant, endTime: Instant): Map<String, Any> {
        val relevantEvents = getEvents(startTime, endTime)
        
        val metrics = mutableMapOf<String, Any>()
        
        // Basic event counts
        metrics["total_events"] = relevantEvents.size.toLong()
        metrics["feed_views"] = relevantEvents.count { it.eventType.name == "FEED_VIEW" }.toLong()
        metrics["item_interactions"] = relevantEvents.count { it.eventType.name == "ITEM_INTERACTION" }.toLong()
        metrics["sessions"] = relevantEvents.count { it.eventType.name == "SESSION_METRICS" }.toLong()
        
        // Interaction type breakdown
        val interactionEvents = relevantEvents.filter { it.eventType.name == "ITEM_INTERACTION" }
        val interactionTypes = interactionEvents.groupBy { 
            it.metadata["interactionType"] as? String ?: "unknown" 
        }
        
        metrics["item_clicks"] = interactionTypes["click"]?.size?.toLong() ?: 0L
        metrics["item_likes"] = interactionTypes["like"]?.size?.toLong() ?: 0L
        metrics["item_shares"] = interactionTypes["share"]?.size?.toLong() ?: 0L
        metrics["item_comments"] = interactionTypes["comment"]?.size?.toLong() ?: 0L
        metrics["item_bookmarks"] = interactionTypes["bookmark"]?.size?.toLong() ?: 0L
        
        // User metrics
        metrics["unique_users"] = relevantEvents.map { it.userId }.toSet().size.toLong()
        
        // Performance metrics
        val generationEvents = relevantEvents.filter { it.eventType.name == "FEED_GENERATION" }
        val generationTimes = generationEvents.mapNotNull { 
            it.metadata["generationDuration"] as? Long 
        }
        
        if (generationTimes.isNotEmpty()) {
            metrics["avg_generation_time"] = generationTimes.average()
            metrics["total_generations"] = generationTimes.size.toLong()
        }
        
        return metrics
    }

    override suspend fun incrementMetric(date: Instant, metricName: String, value: Long) {
        val dateKey = date.toString().substring(0, 10) // YYYY-MM-DD format
        val dayMetrics = dailyMetrics.computeIfAbsent(dateKey) { ConcurrentHashMap() }
        dayMetrics.computeIfAbsent(metricName) { AtomicLong(0) }.addAndGet(value)
    }

    override suspend fun getUniqueUsers(startTime: Instant, endTime: Instant): Set<String> {
        return getEvents(startTime, endTime).map { it.userId }.toSet()
    }

    override suspend fun getEventCountByType(startTime: Instant, endTime: Instant): Map<String, Long> {
        val events = getEvents(startTime, endTime)
        return events.groupBy { it.eventType.name }
            .mapValues { it.value.size.toLong() }
    }

    override suspend fun deleteEventsBefore(cutoffTime: Instant): Int {
        val toDelete = events.filter { it.value.timestamp.isBefore(cutoffTime) }
        
        toDelete.forEach { (eventId, event) ->
            events.remove(eventId)
            
            // Clean up indices
            userIndex[event.userId]?.remove(eventId)
            val hourKey = event.timestamp.epochSecond / 3600
            timeIndex[hourKey]?.remove(eventId)
            
            // Clean up empty buckets
            if (userIndex[event.userId]?.isEmpty() == true) {
                userIndex.remove(event.userId)
            }
            if (timeIndex[hourKey]?.isEmpty() == true) {
                timeIndex.remove(hourKey)
            }
        }
        
        return toDelete.size
    }

    // Additional utility methods for monitoring and debugging
    
    /**
     * Get current repository statistics
     */
    fun getRepositoryStats(): AnalyticsRepositoryStats {
        return AnalyticsRepositoryStats(
            totalEvents = events.size.toLong(),
            totalUsers = userIndex.size.toLong(),
            timeSlots = timeIndex.size.toLong(),
            dailyMetricDays = dailyMetrics.size.toLong()
        )
    }

    /**
     * Get daily metrics for a specific date
     */
    suspend fun getDailyMetrics(date: Instant): Map<String, Long> {
        val dateKey = date.toString().substring(0, 10)
        val dayMetrics = dailyMetrics[dateKey] ?: return emptyMap()
        
        return dayMetrics.mapValues { it.value.get() }
    }

    /**
     * Clear all data (useful for testing)
     */
    fun clearAll() {
        events.clear()
        userIndex.clear()
        dailyMetrics.clear()
        timeIndex.clear()
    }

    /**
     * Get events for a specific item
     */
    suspend fun getItemEvents(itemId: String, startTime: Instant, endTime: Instant): List<FeedAnalyticsEvent> {
        return getEvents(startTime, endTime)
            .filter { it.itemId == itemId }
    }
}

/**
 * Statistics about the analytics repository
 */
data class AnalyticsRepositoryStats(
    val totalEvents: Long,
    val totalUsers: Long,
    val timeSlots: Long,
    val dailyMetricDays: Long
)