package com.welcomer.welcome.infrastructure.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.uuid.Generators
import com.welcomer.welcome.infrastructure.cursor.CursorKeys
import com.welcomer.welcome.infrastructure.cursor.CursorService
import com.welcomer.welcome.infrastructure.cursor.CursorStatus
import com.welcomer.welcome.message.dto.SearchType
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration




@Service
class RedisCursorService(
    private val redis: StringRedisTemplate,
    private val mapper: ObjectMapper
): CursorService {

    override fun createCursor(types: List<SearchType>, state: CursorStatus, ttl: Duration): String {
        val cursorId = CursorKeys.newCursorId()
        val key = CursorKeys.key(cursorId, types)
        val value = mapper.writeValueAsString(state)

        println(value)
        redis.opsForValue().set(key, value, CursorKeys.withJitter(ttl).toLong(DurationUnit.SECONDS))

        return cursorId
    }

    override fun getCursor(types: List<SearchType>, cursorId: String): CursorStatus? {
        val key = CursorKeys.key(cursorId, types)
        val value = redis.opsForValue().get(key) ?: return null
        return mapper.readValue(sanitize(value), CursorStatus::class.java)
    }

    override fun updateCursor(types: List<SearchType>, cursorId: String, state: CursorStatus): Boolean {
        val key = CursorKeys.key(cursorId, types)
        return redis.opsForValue().setIfPresent(key, mapper.writeValueAsString(state)) ?: false
    }

    override fun deleteCursor(types: List<SearchType>, cursorId: String): Boolean {
        val key = CursorKeys.key(cursorId, types)
        return redis.delete(key) ?: false
    }

    override fun isCursorExists(types: List<SearchType>, cursorId: String): Boolean {
        val key = CursorKeys.key(cursorId, types)
        return redis.hasKey(key) ?: false
    }

    fun sanitize(jsonish: String): String {
        val start = jsonish.indexOf('{')
        require(start >= 0) { "JSON '{' not found" }
        val end = jsonish.lastIndexOf('}')
        require(end >= start) { "JSON '}' not found" }
        return jsonish.substring(start, end + 1)
    }
}