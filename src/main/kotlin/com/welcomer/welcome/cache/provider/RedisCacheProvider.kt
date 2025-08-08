package com.welcomer.welcome.cache.provider

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

/**
 * Redis-based cache provider implementation with connection pooling and error handling
 */
@Component
class RedisCacheProvider(
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val objectMapper: ObjectMapper
) : CacheProvider {
    
    // Statistics tracking
    private val hits = AtomicLong(0)
    private val misses = AtomicLong(0)
    private val evictions = AtomicLong(0)
    
    // Connection health monitoring
    @Volatile
    private var isHealthy = true
    private val healthCheckInterval = Duration.ofSeconds(30)
    
    init {
        // Start background health monitoring
        CoroutineScope(Dispatchers.IO).launch {
            startHealthMonitoring()
        }
    }

    override suspend fun <T> get(key: String, type: Class<T>): T? {
        return try {
            val value = redisTemplate.opsForValue()
                .get(key)
                .awaitSingleOrNull()
            
            if (value != null) {
                hits.incrementAndGet()
                deserializeValue(value, type)
            } else {
                misses.incrementAndGet()
                null
            }
        } catch (e: Exception) {
            misses.incrementAndGet()
            handleRedisException("get", key, e)
            null
        }
    }

    override suspend fun set(key: String, value: Any, ttl: Duration) {
        try {
            val serializedValue = serializeValue(value)
            
            if (ttl > Duration.ZERO) {
                redisTemplate.opsForValue()
                    .set(key, serializedValue, ttl)
                    .awaitSingleOrNull()
            } else {
                redisTemplate.opsForValue()
                    .set(key, serializedValue)
                    .awaitSingleOrNull()
            }
        } catch (e: Exception) {
            handleRedisException("set", key, e)
        }
    }

    override suspend fun delete(key: String): Boolean {
        return try {
            val result = redisTemplate.opsForValue()
                .delete(key)
                .awaitSingleOrNull()
            
            result == true
        } catch (e: Exception) {
            handleRedisException("delete", key, e)
            false
        }
    }

    override suspend fun deleteAll(keys: Collection<String>): Long {
        return try {
            if (keys.isEmpty()) return 0L
            
            val result = redisTemplate.delete(*keys.toTypedArray())
                .awaitSingleOrNull()
            
            result ?: 0L
        } catch (e: Exception) {
            handleRedisException("deleteAll", keys.toString(), e)
            0L
        }
    }

    override suspend fun findKeys(pattern: String): List<String> {
        return try {
            val scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build()
            
            redisTemplate.scan(scanOptions)
                .collectList()
                .awaitSingleOrNull() ?: emptyList()
        } catch (e: Exception) {
            handleRedisException("findKeys", pattern, e)
            emptyList()
        }
    }

    override suspend fun exists(key: String): Boolean {
        return try {
            val result = redisTemplate.hasKey(key)
                .awaitSingleOrNull()
            
            result == true
        } catch (e: Exception) {
            handleRedisException("exists", key, e)
            false
        }
    }

    override suspend fun expire(key: String, ttl: Duration): Boolean {
        return try {
            val result = redisTemplate.expire(key, ttl)
                .awaitSingleOrNull()
            
            result == true
        } catch (e: Exception) {
            handleRedisException("expire", key, e)
            false
        }
    }

    override suspend fun getTtl(key: String): Duration? {
        return try {
            val secondsDuration = redisTemplate.getExpire(key)
                .awaitSingleOrNull()
            
            if (secondsDuration != null && !secondsDuration.isNegative) {
                secondsDuration
            } else {
                null
            }
        } catch (e: Exception) {
            handleRedisException("getTtl", key, e)
            null
        }
    }

    override suspend fun increment(key: String, delta: Long): Long {
        return try {
            val result = redisTemplate.opsForValue()
                .increment(key, delta)
                .awaitSingleOrNull()
            
            result ?: delta
        } catch (e: Exception) {
            handleRedisException("increment", key, e)
            delta
        }
    }

    override suspend fun clear() {
        try {
            redisTemplate.connectionFactory
                .reactiveConnection
                .serverCommands()
                .flushAll()
                .awaitSingleOrNull()
        } catch (e: Exception) {
            handleRedisException("clear", "all", e)
        }
    }

    override suspend fun getStats(): CacheStats {
        return try {
            val info = redisTemplate.connectionFactory
                .reactiveConnection
                .serverCommands()
                .info("stats")
                .awaitSingleOrNull()
            
            parseRedisStats(info?.toString())
        } catch (e: Exception) {
            // Return local stats if Redis stats unavailable
            CacheStats(
                hits = hits.get(),
                misses = misses.get(),
                evictions = evictions.get()
            )
        }
    }
    
    /**
     * Check if Redis connection is healthy
     */
    suspend fun isHealthy(): Boolean {
        return try {
            redisTemplate.connectionFactory
                .reactiveConnection
                .ping()
                .awaitSingleOrNull()
            
            isHealthy = true
            true
        } catch (e: Exception) {
            isHealthy = false
            false
        }
    }
    
    /**
     * Get connection pool statistics
     */
    suspend fun getPoolStats(): Map<String, Any> {
        return try {
            val info = redisTemplate.connectionFactory
                .reactiveConnection
                .serverCommands()
                .info("clients")
                .awaitSingleOrNull()
            
            parseConnectionStats(info?.toString() ?: "")
        } catch (e: Exception) {
            mapOf("error" to (e.message ?: "Unknown error"))
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

    private fun parseRedisStats(info: String?): CacheStats {
        if (info.isNullOrBlank()) {
            return CacheStats(
                hits = hits.get(),
                misses = misses.get(),
                evictions = evictions.get()
            )
        }
        
        val stats = info.lines()
            .filter { it.contains(':') }
            .associate { line ->
                val parts = line.split(':')
                parts[0] to parts[1]
            }
        
        return CacheStats(
            hits = stats["keyspace_hits"]?.toLongOrNull() ?: hits.get(),
            misses = stats["keyspace_misses"]?.toLongOrNull() ?: misses.get(),
            evictions = stats["evicted_keys"]?.toLongOrNull() ?: evictions.get(),
            size = stats["connected_clients"]?.toLongOrNull() ?: 0,
            memoryUsage = stats["used_memory"]?.toLongOrNull() ?: 0
        )
    }
    
    private fun parseConnectionStats(info: String): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()
        
        info.lines()
            .filter { it.contains(':') }
            .forEach { line ->
                val parts = line.split(':')
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    
                    stats[key] = value.toLongOrNull() ?: value
                }
            }
        
        return stats
    }

    private suspend fun startHealthMonitoring() {
        while (true) {
            try {
                delay(healthCheckInterval.toMillis())
                isHealthy()
            } catch (e: Exception) {
                println("Health monitoring error: ${e.message}")
            }
        }
    }

    private fun handleRedisException(operation: String, key: String, exception: Exception) {
        isHealthy = false
        println("Redis operation '$operation' failed for key '$key': ${exception.message}")
        
        // In production, would use proper logging and metrics
        // Could implement circuit breaker pattern here
    }
}

