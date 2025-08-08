package com.welcomer.welcome.cache.provider

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.CRC32

/**
 * Sharded Redis cache provider that distributes data across multiple Redis instances
 * Uses consistent hashing for key distribution to minimize cache misses during scaling
 */
class ShardedRedisCacheProvider(
    private val redisTemplates: List<ReactiveStringRedisTemplate>,
    private val objectMapper: ObjectMapper
) : CacheProvider {
    
    private val totalShards = redisTemplates.size
    private val shardStats = Array(totalShards) { ShardStats() }
    
    // Virtual nodes for consistent hashing (helps with distribution)
    private val virtualNodesPerShard = 150
    private val hashRing = createHashRing()
    
    init {
        require(redisTemplates.isNotEmpty()) { "At least one Redis template is required" }
        println("Initialized sharded cache with ${totalShards} shards")
    }

    override suspend fun <T> get(key: String, type: Class<T>): T? {
        val shard = getShardForKey(key)
        val template = redisTemplates[shard]
        
        return try {
            val value = template.opsForValue()
                .get(key)
                .awaitSingleOrNull()
            
            if (value != null) {
                shardStats[shard].hits.incrementAndGet()
                deserializeValue(value, type)
            } else {
                shardStats[shard].misses.incrementAndGet()
                null
            }
        } catch (e: Exception) {
            shardStats[shard].errors.incrementAndGet()
            handleShardException("get", key, shard, e)
            
            // Try next shard as fallback
            tryFallbackGet(key, type, shard)
        }
    }

    override suspend fun set(key: String, value: Any, ttl: Duration) {
        val shard = getShardForKey(key)
        val template = redisTemplates[shard]
        
        try {
            val serializedValue = serializeValue(value)
            
            if (ttl > Duration.ZERO) {
                template.opsForValue()
                    .set(key, serializedValue, ttl)
                    .awaitSingleOrNull()
            } else {
                template.opsForValue()
                    .set(key, serializedValue)
                    .awaitSingleOrNull()
            }
            
            shardStats[shard].writes.incrementAndGet()
            
        } catch (e: Exception) {
            shardStats[shard].errors.incrementAndGet()
            handleShardException("set", key, shard, e)
        }
    }

    override suspend fun delete(key: String): Boolean {
        val shard = getShardForKey(key)
        val template = redisTemplates[shard]
        
        return try {
            val result = template.opsForValue()
                .delete(key)
                .awaitSingleOrNull()
            
            shardStats[shard].deletes.incrementAndGet()
            result == true
            
        } catch (e: Exception) {
            shardStats[shard].errors.incrementAndGet()
            handleShardException("delete", key, shard, e)
            false
        }
    }

    override suspend fun deleteAll(keys: Collection<String>): Long {
        if (keys.isEmpty()) return 0L
        
        // Group keys by shard
        val keysByShard = keys.groupBy { key -> getShardForKey(key) }
        
        return coroutineScope {
            keysByShard.map { (shard, shardKeys) ->
                async {
                    try {
                        val template = redisTemplates[shard]
                        val result = template.delete(*shardKeys.toTypedArray())
                            .awaitSingleOrNull() ?: 0L
                        
                        shardStats[shard].deletes.addAndGet(result)
                        result
                        
                    } catch (e: Exception) {
                        shardStats[shard].errors.incrementAndGet()
                        handleShardException("deleteAll", shardKeys.toString(), shard, e)
                        0L
                    }
                }
            }.awaitAll().sum()
        }
    }

    override suspend fun findKeys(pattern: String): List<String> {
        return coroutineScope {
            redisTemplates.mapIndexed { shard, template ->
                async {
                    try {
                        val scanOptions = org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(pattern)
                            .count(1000)
                            .build()
                        
                        template.scan(scanOptions)
                            .collectList()
                            .awaitSingleOrNull() ?: emptyList()
                            
                    } catch (e: Exception) {
                        shardStats[shard].errors.incrementAndGet()
                        handleShardException("findKeys", pattern, shard, e)
                        emptyList<String>()
                    }
                }
            }.awaitAll().flatten()
        }
    }

    override suspend fun exists(key: String): Boolean {
        val shard = getShardForKey(key)
        val template = redisTemplates[shard]
        
        return try {
            val result = template.hasKey(key)
                .awaitSingleOrNull()
            
            result == true
            
        } catch (e: Exception) {
            shardStats[shard].errors.incrementAndGet()
            handleShardException("exists", key, shard, e)
            false
        }
    }

    override suspend fun expire(key: String, ttl: Duration): Boolean {
        val shard = getShardForKey(key)
        val template = redisTemplates[shard]
        
        return try {
            val result = template.expire(key, ttl)
                .awaitSingleOrNull()
            
            result == true
            
        } catch (e: Exception) {
            shardStats[shard].errors.incrementAndGet()
            handleShardException("expire", key, shard, e)
            false
        }
    }

    override suspend fun getTtl(key: String): Duration? {
        val shard = getShardForKey(key)
        val template = redisTemplates[shard]
        
        return try {
            val duration = template.getExpire(key)
                .awaitSingleOrNull()
            
            if (duration != null && !duration.isNegative) {
                duration
            } else {
                null
            }
            
        } catch (e: Exception) {
            shardStats[shard].errors.incrementAndGet()
            handleShardException("getTtl", key, shard, e)
            null
        }
    }

    override suspend fun increment(key: String, delta: Long): Long {
        val shard = getShardForKey(key)
        val template = redisTemplates[shard]
        
        return try {
            val result = template.opsForValue()
                .increment(key, delta)
                .awaitSingleOrNull()
            
            shardStats[shard].writes.incrementAndGet()
            result ?: delta
            
        } catch (e: Exception) {
            shardStats[shard].errors.incrementAndGet()
            handleShardException("increment", key, shard, e)
            delta
        }
    }

    override suspend fun clear() {
        coroutineScope {
            redisTemplates.mapIndexed { shard, template ->
                async {
                    try {
                        template.connectionFactory
                            .reactiveConnection
                            .serverCommands()
                            .flushAll()
                            .awaitSingleOrNull()
                            
                    } catch (e: Exception) {
                        shardStats[shard].errors.incrementAndGet()
                        handleShardException("clear", "all", shard, e)
                    }
                }
            }.awaitAll()
        }
    }

    override suspend fun getStats(): CacheStats {
        val totalHits = shardStats.sumOf { it.hits.get() }
        val totalMisses = shardStats.sumOf { it.misses.get() }
        val totalErrors = shardStats.sumOf { it.errors.get() }
        
        return CacheStats(
            hits = totalHits,
            misses = totalMisses,
            evictions = 0, // Would need Redis INFO to get evictions
            size = 0, // Would need to sum across all shards
            memoryUsage = 0 // Would need Redis INFO from all shards
        )
    }
    
    /**
     * Get detailed stats for each shard
     */
    fun getShardStats(): List<ShardStatsInfo> {
        return shardStats.mapIndexed { index, stats ->
            ShardStatsInfo(
                shardIndex = index,
                hits = stats.hits.get(),
                misses = stats.misses.get(),
                writes = stats.writes.get(),
                deletes = stats.deletes.get(),
                errors = stats.errors.get()
            )
        }
    }
    
    /**
     * Check health of all shards
     */
    suspend fun getShardHealth(): List<ShardHealth> {
        return coroutineScope {
            redisTemplates.mapIndexed { index, template ->
                async {
                    try {
                        template.connectionFactory
                            .reactiveConnection
                            .ping()
                            .awaitSingleOrNull()
                        
                        ShardHealth(index, true, null)
                        
                    } catch (e: Exception) {
                        ShardHealth(index, false, e.message)
                    }
                }
            }.awaitAll()
        }
    }
    
    /**
     * Rebalance data across shards (complex operation, typically done during maintenance)
     */
    suspend fun rebalanceShards(): RebalanceResult {
        // This is a complex operation that would:
        // 1. Analyze current key distribution
        // 2. Identify hotspots
        // 3. Move keys to better distribute load
        // 4. Update hash ring if needed
        
        // For now, return a placeholder
        return RebalanceResult(
            keysRebalanced = 0,
            shardsInvolved = totalShards,
            rebalanceTime = 0L
        )
    }
    
    // Private helper methods
    
    private fun getShardForKey(key: String): Int {
        return getShardFromHashRing(key)
    }
    
    private fun getShardFromHashRing(key: String): Int {
        val hash = hashKey(key)
        
        // Find the first virtual node with hash >= key hash
        val virtualNode = hashRing.entries.firstOrNull { it.key >= hash }
            ?: hashRing.entries.first() // Wrap around to first node
        
        return virtualNode.value
    }
    
    private fun hashKey(key: String): Long {
        val crc32 = CRC32()
        crc32.update(key.toByteArray(StandardCharsets.UTF_8))
        return crc32.value
    }
    
    private fun createHashRing(): Map<Long, Int> {
        val ring = mutableMapOf<Long, Int>()
        
        // Create virtual nodes for each shard
        for (shard in 0 until totalShards) {
            for (virtualNode in 0 until virtualNodesPerShard) {
                val virtualNodeKey = "${shard}:${virtualNode}"
                val hash = hashKey(virtualNodeKey)
                ring[hash] = shard
            }
        }
        
        return ring.toSortedMap()
    }
    
    private suspend fun <T> tryFallbackGet(key: String, type: Class<T>, failedShard: Int): T? {
        // Try the next shard as fallback (simple strategy)
        val fallbackShard = (failedShard + 1) % totalShards
        if (fallbackShard == failedShard) {
            return null // Only one shard available
        }
        
        return try {
            val template = redisTemplates[fallbackShard]
            val value = template.opsForValue()
                .get(key)
                .awaitSingleOrNull()
            
            if (value != null) {
                shardStats[fallbackShard].hits.incrementAndGet()
                deserializeValue(value, type)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Fallback shard $fallbackShard also failed for key $key: ${e.message}")
            null
        }
    }
    
    private fun serializeValue(value: Any): String {
        return when (value) {
            is String -> value
            is Number -> value.toString()
            is Boolean -> value.toString()
            else -> objectMapper.writeValueAsString(value)
        }
    }

    private fun <T> deserializeValue(value: String, type: Class<T>): T? {
        return try {
            when (type) {
                String::class.java -> value as T
                Int::class.java, Integer::class.java -> value.toInt() as T
                Long::class.java -> value.toLong() as T
                Double::class.java -> value.toDouble() as T
                Float::class.java -> value.toFloat() as T
                Boolean::class.java -> value.toBoolean() as T
                else -> objectMapper.readValue(value, type)
            }
        } catch (e: Exception) {
            println("Failed to deserialize value for type ${type.name}: ${e.message}")
            null
        }
    }
    
    private fun handleShardException(operation: String, key: String, shard: Int, exception: Exception) {
        println("Shard $shard failed operation '$operation' for key '$key': ${exception.message}")
        // In production, would implement circuit breaker and alerting
    }
}

// Supporting data classes

private data class ShardStats(
    val hits: AtomicLong = AtomicLong(0),
    val misses: AtomicLong = AtomicLong(0),
    val writes: AtomicLong = AtomicLong(0),
    val deletes: AtomicLong = AtomicLong(0),
    val errors: AtomicLong = AtomicLong(0)
)

data class ShardStatsInfo(
    val shardIndex: Int,
    val hits: Long,
    val misses: Long,
    val writes: Long,
    val deletes: Long,
    val errors: Long
) {
    val hitRate: Double
        get() = if (hits + misses > 0) hits.toDouble() / (hits + misses) else 0.0
}

data class ShardHealth(
    val shardIndex: Int,
    val isHealthy: Boolean,
    val errorMessage: String?
)

data class RebalanceResult(
    val keysRebalanced: Long,
    val shardsInvolved: Int,
    val rebalanceTime: Long // milliseconds
)

