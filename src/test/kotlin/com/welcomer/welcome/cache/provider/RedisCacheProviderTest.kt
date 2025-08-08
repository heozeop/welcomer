package com.welcomer.welcome.cache.provider

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.mockito.Mockito.lenient
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import reactor.core.publisher.Mono
import java.time.Duration
import kotlin.test.*

@ExtendWith(MockitoExtension::class)
class RedisCacheProviderTest {

    @Mock
    private lateinit var redisTemplate: ReactiveStringRedisTemplate

    @Mock
    private lateinit var valueOps: ReactiveValueOperations<String, String>

    private lateinit var objectMapper: ObjectMapper
    private lateinit var cacheProvider: RedisCacheProvider

    @BeforeEach
    fun setup() {
        objectMapper = ObjectMapper().registerKotlinModule()
        lenient().whenever(redisTemplate.opsForValue()).thenReturn(valueOps)
        cacheProvider = RedisCacheProvider(redisTemplate, objectMapper)
    }

    @Test
    fun `get should return cached string value`(): Unit = runBlocking {
        // Given
        val key = "test:key"
        val value = "test value"
        whenever(valueOps.get(key)).thenReturn(Mono.just(value))

        // When
        val result = cacheProvider.get(key, String::class.java)

        // Then
        assertEquals(value, result)
        verify(valueOps).get(key)
    }

    @Test
    fun `get should return null for missing key`(): Unit = runBlocking {
        // Given
        val key = "missing:key"
        whenever(valueOps.get(key)).thenReturn(Mono.empty())

        // When
        val result = cacheProvider.get(key, String::class.java)

        // Then
        assertNull(result)
        verify(valueOps).get(key)
    }

    @Test
    fun `get should deserialize complex objects`(): Unit = runBlocking {
        // Given
        val key = "test:object"
        val testData = TestData("test", 42, true)
        val serializedValue = objectMapper.writeValueAsString(testData)
        whenever(valueOps.get(key)).thenReturn(Mono.just(serializedValue))

        // When
        val result = cacheProvider.get(key, TestData::class.java)

        // Then
        assertEquals(testData, result)
        verify(valueOps).get(key)
    }

    @Test
    fun `set should store string value with TTL`(): Unit = runBlocking {
        // Given
        val key = "test:key"
        val value = "test value"
        val ttl = Duration.ofMinutes(5)
        whenever(valueOps.set(key, value, ttl)).thenReturn(Mono.just(true))

        // When
        cacheProvider.set(key, value, ttl)

        // Then
        verify(valueOps).set(key, value, ttl)
    }

    @Test
    fun `set should store complex objects as JSON`(): Unit = runBlocking {
        // Given
        val key = "test:object"
        val testData = TestData("test", 42, true)
        val expectedJson = objectMapper.writeValueAsString(testData)
        val ttl = Duration.ofMinutes(5)
        whenever(valueOps.set(key, expectedJson, ttl)).thenReturn(Mono.just(true))

        // When
        cacheProvider.set(key, testData, ttl)

        // Then
        verify(valueOps).set(key, expectedJson, ttl)
    }

    @Test
    fun `delete should remove key from cache`(): Unit = runBlocking {
        // Given
        val key = "test:key"
        whenever(valueOps.delete(key)).thenReturn(Mono.just(true))

        // When
        val result = cacheProvider.delete(key)

        // Then
        assertTrue(result)
        verify(valueOps).delete(key)
    }

    @Test
    fun `exists should check key existence`(): Unit = runBlocking {
        // Given
        val key = "test:key"
        whenever(redisTemplate.hasKey(key)).thenReturn(Mono.just(true))

        // When
        val result = cacheProvider.exists(key)

        // Then
        assertTrue(result)
        verify(redisTemplate).hasKey(key)
    }

    @Test
    fun `increment should increase numeric value`(): Unit = runBlocking {
        // Given
        val key = "counter:key"
        val delta = 5L
        val expectedResult = 15L
        whenever(valueOps.increment(key, delta)).thenReturn(Mono.just(expectedResult))

        // When
        val result = cacheProvider.increment(key, delta)

        // Then
        assertEquals(expectedResult, result)
        verify(valueOps).increment(key, delta)
    }

    @Test
    fun `get should handle Redis errors gracefully`(): Unit = runBlocking {
        // Given
        val key = "error:key"
        whenever(valueOps.get(key)).thenReturn(Mono.error(RuntimeException("Redis connection failed")))

        // When
        val result = cacheProvider.get(key, String::class.java)

        // Then
        assertNull(result)
        verify(valueOps).get(key)
    }

    @Test
    fun `set should handle Redis errors gracefully`(): Unit = runBlocking {
        // Given
        val key = "error:key"
        val value = "test value"
        val ttl = Duration.ofMinutes(5)
        whenever(valueOps.set(key, value, ttl)).thenReturn(Mono.error(RuntimeException("Redis connection failed")))

        // When & Then (should not throw)
        assertDoesNotThrow {
            runBlocking {
                cacheProvider.set(key, value, ttl)
            }
        }
        verify(valueOps).set(key, value, ttl)
    }

    private data class TestData(
        val name: String,
        val count: Int,
        val active: Boolean
    )
}