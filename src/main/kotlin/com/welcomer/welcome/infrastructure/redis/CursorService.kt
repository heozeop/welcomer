package com.welcomer.welcome.infrastructure.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.uuid.Generators
import com.welcomer.welcome.infrastructure.cursor.CursorKeys
import com.welcomer.welcome.infrastructure.cursor.CursorService
import com.welcomer.welcome.infrastructure.cursor.CursorStatus
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

    override fun createCursor(state: CursorStatus, ttl: Duration): String {
        val cursorId = CursorKeys.newCursorId()
        val key = CursorKeys.key(cursorId)
        redis.opsForValue().set(key, mapper.writeValueAsString(state), CursorKeys.withJitter(ttl).toLong(DurationUnit.SECONDS))

        return cursorId
    }

    override fun getCursor(cursorId: String): CursorStatus? {
        val key = CursorKeys.key(cursorId)
        val value = redis.opsForValue().get(key) ?: return null
        return mapper.readValue(value, CursorStatus::class.java)
    }

    override fun updateCursor(cursorId: String, state: CursorStatus): Boolean {
        val key = CursorKeys.key(cursorId)
        return redis.opsForValue().setIfPresent(key, mapper.writeValueAsString(state)) ?: false
    }

    override fun deleteCursor(cursorId: String): Boolean {
        val key = CursorKeys.key(cursorId)
        return redis.delete(key) ?: false
    }

    override fun isCursorExists(cursorId: String): Boolean {
        val key = CursorKeys.key(cursorId)
        return redis.hasKey(key) ?: false
    }

}