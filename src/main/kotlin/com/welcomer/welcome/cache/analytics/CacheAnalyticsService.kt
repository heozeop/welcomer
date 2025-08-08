package com.welcomer.welcome.cache.analytics

import com.welcomer.welcome.cache.provider.CacheProvider
import com.welcomer.welcome.feed.model.FeedType
import com.welcomer.welcome.feed.service.FeedCacheService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min

/**
 * Service for tracking cache performance metrics and automatically optimizing caching parameters
 */
@Service
class CacheAnalyticsService(
    private val cacheProvider: CacheProvider,
    private val feedCacheService: FeedCacheService
) {
    
    private val analyticsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Metrics collection
    private val cacheMetrics = ConcurrentHashMap<String, CacheMetricEntry>()
    private val keyPatternMetrics = ConcurrentHashMap<String, PatternMetrics>()
    private val ttlOptimizations = ConcurrentHashMap<String, TTLOptimization>()
    
    // Global counters
    private val totalOperations = AtomicLong(0)
    private val totalHits = AtomicLong(0)
    private val totalMisses = AtomicLong(0)
    private val totalEvictions = AtomicLong(0)
    
    // Adaptive caching configuration
    private val adaptiveConfig = AtomicReference(AdaptiveCacheConfig())
    
    // Sliding window for analytics
    private val metricsWindow = Duration.ofHours(1)
    private val optimizationInterval = Duration.ofMinutes(15)
    
    companion object {
        private const val MIN_SAMPLES_FOR_OPTIMIZATION = 100
        private const val DEFAULT_TTL_MULTIPLIER = 1.0
        private const val MAX_TTL_MULTIPLIER = 3.0
        private const val MIN_TTL_MULTIPLIER = 0.1
    }
    
    init {
        startAnalyticsCollection()
    }
    
    /**
     * Record cache operation metrics
     */
    fun recordCacheHit(key: String, responseTime: Long = 0) {
        recordCacheOperation(key, CacheOperationType.HIT, responseTime)
        totalHits.incrementAndGet()
    }
    
    fun recordCacheMiss(key: String, responseTime: Long = 0) {
        recordCacheOperation(key, CacheOperationType.MISS, responseTime)
        totalMisses.incrementAndGet()
    }
    
    fun recordCacheEviction(key: String) {
        recordCacheOperation(key, CacheOperationType.EVICTION, 0)
        totalEvictions.incrementAndGet()
    }
    
    fun recordCacheWrite(key: String, size: Long = 0, responseTime: Long = 0) {
        recordCacheOperation(key, CacheOperationType.WRITE, responseTime, size)
    }
    
    /**
     * Get optimal TTL for a cache key based on historical patterns
     */
    fun getOptimalTtl(keyPattern: String, defaultTtl: Duration): Duration {
        val optimization = ttlOptimizations[keyPattern]
        if (optimization == null || optimization.sampleCount < MIN_SAMPLES_FOR_OPTIMIZATION) {
            return defaultTtl
        }
        
        val multiplier = optimization.recommendedMultiplier
        val optimizedTtl = Duration.ofSeconds((defaultTtl.seconds * multiplier).toLong())
        
        // Ensure TTL is within reasonable bounds
        return when {
            optimizedTtl < Duration.ofSeconds(30) -> Duration.ofSeconds(30)
            optimizedTtl > Duration.ofHours(24) -> Duration.ofHours(24)
            else -> optimizedTtl
        }
    }
    
    /**
     * Get comprehensive cache analytics report
     */
    fun getCacheAnalytics(): CacheAnalyticsReport {
        val now = Instant.now()
        val windowStart = now.minus(metricsWindow)
        
        // Filter metrics within the window
        val recentMetrics = cacheMetrics.values.filter { 
            it.lastUpdated.isAfter(windowStart) 
        }
        
        val totalOps = totalOperations.get()
        val hits = totalHits.get()
        val misses = totalMisses.get()
        
        val overallHitRate = if (totalOps > 0) hits.toDouble() / totalOps else 0.0
        
        // Calculate pattern-specific metrics
        val patternReports = keyPatternMetrics.map { (pattern, metrics) ->
            PatternAnalyticsReport(
                pattern = pattern,
                hitRate = metrics.calculateHitRate(),
                avgResponseTime = metrics.calculateAvgResponseTime(),
                operationCount = metrics.totalOperations.get(),
                recommendedTtl = ttlOptimizations[pattern]?.recommendedMultiplier ?: 1.0
            )
        }
        
        // Memory usage estimation
        val estimatedMemoryUsage = recentMetrics.sumOf { it.estimatedSize }
        
        return CacheAnalyticsReport(
            overallHitRate = overallHitRate,
            totalOperations = totalOps,
            totalHits = hits,
            totalMisses = misses,
            totalEvictions = totalEvictions.get(),
            avgResponseTime = calculateOverallAvgResponseTime(),
            memoryUsageBytes = estimatedMemoryUsage,
            patternReports = patternReports,
            optimizationRecommendations = generateOptimizationRecommendations(),
            reportGeneratedAt = now
        )
    }
    
    /**
     * Get real-time cache dashboard data
     */
    suspend fun getCacheDashboardData(): CacheDashboardData {
        val stats = cacheProvider.getStats()
        val config = adaptiveConfig.get()
        
        return CacheDashboardData(
            hitRate = stats.hitRate,
            missRate = 1.0 - stats.hitRate,
            operationsPerSecond = calculateOperationsPerSecond(),
            avgResponseTime = calculateOverallAvgResponseTime(),
            memoryUsage = stats.memoryUsage,
            evictionsPerMinute = calculateEvictionsPerMinute(),
            topHotKeys = getTopHotKeys(10),
            topMissedKeys = getTopMissedKeys(10),
            adaptiveSettings = config,
            healthStatus = assessCacheHealth()
        )
    }
    
    /**
     * Optimize cache configuration based on observed patterns
     */
    @Scheduled(fixedDelay = 15 * 60 * 1000) // Every 15 minutes
    fun performAutomaticOptimization() {
        analyticsScope.launch {
            try {
                optimizeTtlValues()
                optimizeCacheSize()
                cleanupOldMetrics()
                
                println("Cache optimization completed at ${Instant.now()}")
            } catch (e: Exception) {
                println("Error during cache optimization: ${e.message}")
            }
        }
    }
    
    /**
     * Generate optimization recommendations
     */
    fun generateOptimizationRecommendations(): List<OptimizationRecommendation> {
        val recommendations = mutableListOf<OptimizationRecommendation>()
        
        // Check overall hit rate
        val hitRate = totalHits.get().toDouble() / max(1L, totalOperations.get())
        if (hitRate < 0.7) {
            recommendations.add(
                OptimizationRecommendation(
                    type = RecommendationType.HIT_RATE,
                    description = "Overall hit rate is ${(hitRate * 100).toInt()}%. Consider increasing TTL values.",
                    priority = Priority.HIGH,
                    estimatedImpact = "Could improve hit rate by 10-15%"
                )
            )
        }
        
        // Check eviction rate
        val evictionRate = totalEvictions.get().toDouble() / max(1L, totalOperations.get())
        if (evictionRate > 0.1) {
            recommendations.add(
                OptimizationRecommendation(
                    type = RecommendationType.EVICTION_RATE,
                    description = "High eviction rate (${(evictionRate * 100).toInt()}%). Consider increasing cache size.",
                    priority = Priority.MEDIUM,
                    estimatedImpact = "Could reduce evictions by 20-30%"
                )
            )
        }
        
        // Check for hot keys
        val hotKeys = getTopHotKeys(5)
        if (hotKeys.isNotEmpty()) {
            recommendations.add(
                OptimizationRecommendation(
                    type = RecommendationType.HOT_KEYS,
                    description = "Detected ${hotKeys.size} hot keys that could benefit from longer TTL.",
                    priority = Priority.LOW,
                    estimatedImpact = "Could improve performance for high-traffic keys"
                )
            )
        }
        
        return recommendations
    }
    
    // Private implementation methods
    
    private fun startAnalyticsCollection() {
        // Start background metrics aggregation
        analyticsScope.launch {
            while (isActive) {
                try {
                    aggregateMetrics()
                    delay(Duration.ofSeconds(30).toMillis())
                } catch (e: Exception) {
                    println("Error in analytics collection: ${e.message}")
                    delay(Duration.ofMinutes(1).toMillis())
                }
            }
        }
    }
    
    private fun recordCacheOperation(
        key: String, 
        operation: CacheOperationType, 
        responseTime: Long,
        size: Long = 0
    ) {
        totalOperations.incrementAndGet()
        
        val pattern = extractKeyPattern(key)
        val now = Instant.now()
        
        // Update key-specific metrics
        cacheMetrics.compute(key) { _, existing ->
            existing?.update(operation, responseTime, size, now) 
                ?: CacheMetricEntry(key, now).update(operation, responseTime, size, now)
        }
        
        // Update pattern metrics
        keyPatternMetrics.compute(pattern) { _, existing ->
            existing?.update(operation, responseTime) 
                ?: PatternMetrics(pattern).update(operation, responseTime)
        }
    }
    
    private fun extractKeyPattern(key: String): String {
        // Extract pattern from key (e.g., "v1:feed:user123:HOME" -> "v1:feed:*:*")
        val parts = key.split(":")
        return if (parts.size >= 2) {
            "${parts[0]}:${parts[1]}:*"
        } else {
            "other:*"
        }
    }
    
    private suspend fun optimizeTtlValues() {
        keyPatternMetrics.forEach { (pattern, metrics) ->
            val optimization = calculateTtlOptimization(pattern, metrics)
            if (optimization.sampleCount >= MIN_SAMPLES_FOR_OPTIMIZATION) {
                ttlOptimizations[pattern] = optimization
            }
        }
    }
    
    private fun calculateTtlOptimization(pattern: String, metrics: PatternMetrics): TTLOptimization {
        val hitRate = metrics.calculateHitRate()
        val avgResponseTime = metrics.calculateAvgResponseTime()
        
        // Calculate recommended multiplier based on hit rate and response time
        val multiplier = when {
            hitRate > 0.9 && avgResponseTime < 10 -> 0.8 // Reduce TTL for very fast, highly cached items
            hitRate > 0.8 -> 1.0 // Keep current TTL
            hitRate > 0.6 -> 1.5 // Increase TTL moderately
            hitRate > 0.4 -> 2.0 // Increase TTL significantly
            else -> 2.5 // Low hit rate, increase TTL more
        }.coerceIn(MIN_TTL_MULTIPLIER, MAX_TTL_MULTIPLIER)
        
        return TTLOptimization(
            pattern = pattern,
            recommendedMultiplier = multiplier,
            sampleCount = metrics.totalOperations.get(),
            confidence = calculateConfidence(metrics.totalOperations.get()),
            lastUpdated = Instant.now()
        )
    }
    
    private suspend fun optimizeCacheSize() {
        val stats = cacheProvider.getStats()
        val config = adaptiveConfig.get()
        
        // Adjust cache size based on hit rate and eviction rate
        val newConfig = if (stats.hitRate < 0.7 && stats.evictions > 0) {
            config.copy(
                maxCacheSize = kotlin.math.min(config.maxCacheSize * 1.2, (config.maxCacheSize + 1000).toDouble()).toLong(),
                ttlMultiplier = kotlin.math.max(config.ttlMultiplier * 1.1, 2.0)
            )
        } else if (stats.hitRate > 0.9 && stats.evictions == 0L) {
            config.copy(
                ttlMultiplier = kotlin.math.max(config.ttlMultiplier * 0.9, 0.5)
            )
        } else {
            config
        }
        
        if (newConfig != config) {
            adaptiveConfig.set(newConfig)
            println("Updated adaptive cache config: $newConfig")
        }
    }
    
    private fun cleanupOldMetrics() {
        val cutoff = Instant.now().minus(metricsWindow)
        
        cacheMetrics.entries.removeIf { (_, entry) ->
            entry.lastUpdated.isBefore(cutoff)
        }
        
        // Keep pattern metrics for longer for better optimization
        val patternCutoff = Instant.now().minus(Duration.ofHours(24))
        keyPatternMetrics.entries.removeIf { (_, metrics) ->
            metrics.lastUpdated.isBefore(patternCutoff)
        }
    }
    
    private suspend fun aggregateMetrics() {
        // Aggregate metrics for dashboard and reporting
        // This could include calculating moving averages, percentiles, etc.
        
        val currentStats = cacheProvider.getStats()
        
        // Update global counters if the cache provider supports it
        if (currentStats.hits > totalHits.get()) {
            totalHits.set(currentStats.hits)
        }
        if (currentStats.misses > totalMisses.get()) {
            totalMisses.set(currentStats.misses)
        }
        if (currentStats.evictions > totalEvictions.get()) {
            totalEvictions.set(currentStats.evictions)
        }
    }
    
    private fun calculateOverallAvgResponseTime(): Double {
        val totalResponseTime = cacheMetrics.values.sumOf { it.totalResponseTime }
        val totalOps = max(1L, cacheMetrics.values.sumOf { it.operationCount })
        return totalResponseTime.toDouble() / totalOps
    }
    
    private fun calculateOperationsPerSecond(): Double {
        val windowStart = Instant.now().minus(Duration.ofMinutes(1))
        val recentOps = cacheMetrics.values.count { 
            it.lastUpdated.isAfter(windowStart) 
        }
        return recentOps / 60.0
    }
    
    private fun calculateEvictionsPerMinute(): Double {
        // This would need more sophisticated tracking
        return totalEvictions.get() / max(1.0, Duration.between(
            Instant.now().minus(metricsWindow), 
            Instant.now()
        ).toMinutes().toDouble())
    }
    
    private fun getTopHotKeys(limit: Int): List<HotKey> {
        return cacheMetrics.values
            .sortedByDescending { it.operationCount }
            .take(limit)
            .map { entry ->
                HotKey(
                    key = entry.key,
                    operationCount = entry.operationCount,
                    hitRate = entry.calculateHitRate()
                )
            }
    }
    
    private fun getTopMissedKeys(limit: Int): List<MissedKey> {
        return cacheMetrics.values
            .filter { it.missCount > 0 }
            .sortedByDescending { it.missCount }
            .take(limit)
            .map { entry ->
                MissedKey(
                    key = entry.key,
                    missCount = entry.missCount,
                    totalCount = entry.operationCount
                )
            }
    }
    
    private fun calculateConfidence(sampleCount: Long): Double {
        return kotlin.math.min(1.0, sampleCount.toDouble() / (MIN_SAMPLES_FOR_OPTIMIZATION * 3))
    }
    
    private fun assessCacheHealth(): CacheHealthStatus {
        val hitRate = totalHits.get().toDouble() / max(1L, totalOperations.get())
        val evictionRate = totalEvictions.get().toDouble() / max(1L, totalOperations.get())
        
        return when {
            hitRate > 0.8 && evictionRate < 0.05 -> CacheHealthStatus.EXCELLENT
            hitRate > 0.7 && evictionRate < 0.1 -> CacheHealthStatus.GOOD
            hitRate > 0.5 && evictionRate < 0.2 -> CacheHealthStatus.FAIR
            else -> CacheHealthStatus.POOR
        }
    }
}

// Supporting data classes and enums

data class CacheMetricEntry(
    val key: String,
    var lastUpdated: Instant,
    var operationCount: Long = 0,
    var hitCount: Long = 0,
    var missCount: Long = 0,
    var evictionCount: Long = 0,
    var totalResponseTime: Long = 0,
    var estimatedSize: Long = 0
) {
    fun update(operation: CacheOperationType, responseTime: Long, size: Long = 0, timestamp: Instant): CacheMetricEntry {
        this.lastUpdated = timestamp
        this.operationCount++
        this.totalResponseTime += responseTime
        
        when (operation) {
            CacheOperationType.HIT -> hitCount++
            CacheOperationType.MISS -> missCount++
            CacheOperationType.EVICTION -> evictionCount++
            CacheOperationType.WRITE -> estimatedSize = max(estimatedSize, size)
        }
        
        return this
    }
    
    fun calculateHitRate(): Double {
        val total = hitCount + missCount
        return if (total > 0) hitCount.toDouble() / total else 0.0
    }
}

data class PatternMetrics(
    val pattern: String,
    var lastUpdated: Instant = Instant.now(),
    val totalOperations: AtomicLong = AtomicLong(0),
    val totalHits: AtomicLong = AtomicLong(0),
    val totalMisses: AtomicLong = AtomicLong(0),
    val totalResponseTime: AtomicLong = AtomicLong(0)
) {
    fun update(operation: CacheOperationType, responseTime: Long): PatternMetrics {
        lastUpdated = Instant.now()
        totalOperations.incrementAndGet()
        totalResponseTime.addAndGet(responseTime)
        
        when (operation) {
            CacheOperationType.HIT -> totalHits.incrementAndGet()
            CacheOperationType.MISS -> totalMisses.incrementAndGet()
            else -> { /* No specific tracking for other operations */ }
        }
        
        return this
    }
    
    fun calculateHitRate(): Double {
        val total = totalHits.get() + totalMisses.get()
        return if (total > 0) totalHits.get().toDouble() / total else 0.0
    }
    
    fun calculateAvgResponseTime(): Double {
        val ops = totalOperations.get()
        return if (ops > 0) totalResponseTime.get().toDouble() / ops else 0.0
    }
}

data class TTLOptimization(
    val pattern: String,
    val recommendedMultiplier: Double,
    val sampleCount: Long,
    val confidence: Double,
    val lastUpdated: Instant
)

data class AdaptiveCacheConfig(
    val maxCacheSize: Long = 10000,
    val ttlMultiplier: Double = 1.0,
    val adaptiveEnabled: Boolean = true,
    val optimizationInterval: Duration = Duration.ofMinutes(15)
)

enum class CacheOperationType {
    HIT, MISS, EVICTION, WRITE
}

enum class CacheHealthStatus {
    EXCELLENT, GOOD, FAIR, POOR
}

enum class RecommendationType {
    HIT_RATE, EVICTION_RATE, HOT_KEYS, TTL_OPTIMIZATION, MEMORY_USAGE
}

enum class Priority {
    HIGH, MEDIUM, LOW
}

data class CacheAnalyticsReport(
    val overallHitRate: Double,
    val totalOperations: Long,
    val totalHits: Long,
    val totalMisses: Long,
    val totalEvictions: Long,
    val avgResponseTime: Double,
    val memoryUsageBytes: Long,
    val patternReports: List<PatternAnalyticsReport>,
    val optimizationRecommendations: List<OptimizationRecommendation>,
    val reportGeneratedAt: Instant
)

data class PatternAnalyticsReport(
    val pattern: String,
    val hitRate: Double,
    val avgResponseTime: Double,
    val operationCount: Long,
    val recommendedTtl: Double
)

data class OptimizationRecommendation(
    val type: RecommendationType,
    val description: String,
    val priority: Priority,
    val estimatedImpact: String
)

data class CacheDashboardData(
    val hitRate: Double,
    val missRate: Double,
    val operationsPerSecond: Double,
    val avgResponseTime: Double,
    val memoryUsage: Long,
    val evictionsPerMinute: Double,
    val topHotKeys: List<HotKey>,
    val topMissedKeys: List<MissedKey>,
    val adaptiveSettings: AdaptiveCacheConfig,
    val healthStatus: CacheHealthStatus
)

data class HotKey(
    val key: String,
    val operationCount: Long,
    val hitRate: Double
)

data class MissedKey(
    val key: String,
    val missCount: Long,
    val totalCount: Long
)