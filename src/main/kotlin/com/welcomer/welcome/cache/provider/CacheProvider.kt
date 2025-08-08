package com.welcomer.welcome.cache.provider

import java.time.Duration

/**
 * Generic cache provider interface for different caching implementations
 */
interface CacheProvider {
    
    /**
     * Get a value from the cache
     */
    suspend fun <T> get(key: String, type: Class<T>): T?
    
    /**
     * Set a value in the cache with TTL
     */
    suspend fun set(key: String, value: Any, ttl: Duration)
    
    /**
     * Delete a key from the cache
     */
    suspend fun delete(key: String): Boolean
    
    /**
     * Delete multiple keys from the cache
     */
    suspend fun deleteAll(keys: Collection<String>): Long
    
    /**
     * Find all keys matching a pattern
     */
    suspend fun findKeys(pattern: String): List<String>
    
    /**
     * Check if a key exists in the cache
     */
    suspend fun exists(key: String): Boolean
    
    /**
     * Set TTL for an existing key
     */
    suspend fun expire(key: String, ttl: Duration): Boolean
    
    /**
     * Get TTL for a key
     */
    suspend fun getTtl(key: String): Duration?
    
    /**
     * Increment a numeric value in cache
     */
    suspend fun increment(key: String, delta: Long = 1): Long
    
    /**
     * Clear all keys from cache
     */
    suspend fun clear()
    
    /**
     * Get cache statistics
     */
    suspend fun getStats(): CacheStats
}

/**
 * Cache statistics
 */
data class CacheStats(
    val hits: Long = 0,
    val misses: Long = 0,
    val evictions: Long = 0,
    val size: Long = 0,
    val memoryUsage: Long = 0
) {
    val hitRate: Double
        get() = if (hits + misses > 0) hits.toDouble() / (hits + misses) else 0.0
}