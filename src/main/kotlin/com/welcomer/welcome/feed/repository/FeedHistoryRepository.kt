package com.welcomer.welcome.feed.repository

import com.welcomer.welcome.feed.model.FeedEntry
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository interface for managing user feed history
 */
interface FeedHistoryRepository {
    /**
     * Save a feed entry to user's history
     */
    suspend fun saveFeedEntry(userId: String, feedEntry: FeedEntry)

    /**
     * Save multiple feed entries to user's history
     */
    suspend fun saveFeedEntries(userId: String, feedEntries: List<FeedEntry>)

    /**
     * Get recent feed history for a user
     */
    suspend fun getRecentFeedHistory(
        userId: String, 
        limit: Int = 100,
        sinceTime: Instant? = null
    ): List<FeedEntry>

    /**
     * Get feed history within a time range
     */
    suspend fun getFeedHistory(
        userId: String,
        startTime: Instant,
        endTime: Instant,
        limit: Int = 200
    ): List<FeedEntry>

    /**
     * Delete old feed history entries for a user
     */
    suspend fun deleteOldHistory(userId: String, olderThan: Instant): Boolean

    /**
     * Get total count of feed entries for a user
     */
    suspend fun getFeedHistoryCount(userId: String): Long

    /**
     * Check if a content item exists in user's recent history
     */
    suspend fun hasSeenContent(userId: String, contentId: String, withinHours: Int = 24): Boolean
}

/**
 * In-memory implementation for development and testing
 */
@Repository
class InMemoryFeedHistoryRepository : FeedHistoryRepository {
    
    // Map of userId to list of FeedEntry
    private val feedHistoryData = ConcurrentHashMap<String, MutableList<FeedEntry>>()

    override suspend fun saveFeedEntry(userId: String, feedEntry: FeedEntry) {
        val userHistory = feedHistoryData.getOrPut(userId) { mutableListOf() }
        synchronized(userHistory) {
            // Remove duplicates and add new entry
            userHistory.removeIf { it.id == feedEntry.id }
            userHistory.add(0, feedEntry) // Add to beginning for chronological order
            
            // Limit history size to prevent memory issues
            if (userHistory.size > 1000) {
                userHistory.removeAt(userHistory.lastIndex)
            }
        }
    }

    override suspend fun saveFeedEntries(userId: String, feedEntries: List<FeedEntry>) {
        val userHistory = feedHistoryData.getOrPut(userId) { mutableListOf() }
        synchronized(userHistory) {
            feedEntries.forEach { entry ->
                // Remove duplicates and add new entry
                userHistory.removeIf { it.id == entry.id }
                userHistory.add(0, entry)
            }
            
            // Sort by generation time (most recent first)
            userHistory.sortByDescending { it.generatedAt }
            
            // Limit history size
            if (userHistory.size > 1000) {
                while (userHistory.size > 1000) {
                    userHistory.removeAt(userHistory.lastIndex)
                }
            }
        }
    }

    override suspend fun getRecentFeedHistory(
        userId: String, 
        limit: Int,
        sinceTime: Instant?
    ): List<FeedEntry> {
        val userHistory = feedHistoryData[userId] ?: return emptyList()
        
        synchronized(userHistory) {
            var filteredHistory = userHistory.asSequence()
            
            // Filter by time if specified
            sinceTime?.let { since ->
                filteredHistory = filteredHistory.filter { it.generatedAt.isAfter(since) }
            }
            
            return filteredHistory
                .sortedByDescending { it.generatedAt }
                .take(limit)
                .toList()
        }
    }

    override suspend fun getFeedHistory(
        userId: String,
        startTime: Instant,
        endTime: Instant,
        limit: Int
    ): List<FeedEntry> {
        val userHistory = feedHistoryData[userId] ?: return emptyList()
        
        synchronized(userHistory) {
            return userHistory
                .filter { entry ->
                    entry.generatedAt.isAfter(startTime) && entry.generatedAt.isBefore(endTime)
                }
                .sortedByDescending { it.generatedAt }
                .take(limit)
        }
    }

    override suspend fun deleteOldHistory(userId: String, olderThan: Instant): Boolean {
        val userHistory = feedHistoryData[userId] ?: return false
        
        synchronized(userHistory) {
            val sizeBefore = userHistory.size
            userHistory.removeIf { it.generatedAt.isBefore(olderThan) }
            return userHistory.size < sizeBefore
        }
    }

    override suspend fun getFeedHistoryCount(userId: String): Long {
        val userHistory = feedHistoryData[userId] ?: return 0L
        synchronized(userHistory) {
            return userHistory.size.toLong()
        }
    }

    override suspend fun hasSeenContent(userId: String, contentId: String, withinHours: Int): Boolean {
        val userHistory = feedHistoryData[userId] ?: return false
        val cutoffTime = Instant.now().minusSeconds(withinHours * 3600L)
        
        synchronized(userHistory) {
            return userHistory.any { entry ->
                entry.content.id == contentId && entry.generatedAt.isAfter(cutoffTime)
            }
        }
    }

    // Additional utility methods for testing and debugging
    
    fun clearAllHistory() {
        feedHistoryData.clear()
    }
    
    fun getUserHistorySize(userId: String): Int {
        val userHistory = feedHistoryData[userId] ?: return 0
        synchronized(userHistory) {
            return userHistory.size
        }
    }
    
    fun getAllUserIds(): Set<String> {
        return feedHistoryData.keys.toSet()
    }
}