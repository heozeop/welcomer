package com.welcomer.welcome.infrastructure.cursor

import com.fasterxml.uuid.Generators
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object CursorKeys {
    fun key(cursorId: String) = "cursor:$cursorId"
    fun newCursorId(): String = Generators.timeBasedGenerator().generate().toString()
    fun withJitter(ttl: Duration, jitterSpec: Int = 60): Duration = ttl.plus(
        Random.nextInt(-jitterSpec, jitterSpec).toDuration(
            DurationUnit.SECONDS))
}