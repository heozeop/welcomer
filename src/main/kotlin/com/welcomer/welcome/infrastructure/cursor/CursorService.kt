package com.welcomer.welcome.infrastructure.cursor

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface CursorService {
    fun createCursor(state: CursorStatus, ttl: Duration = 300.toDuration(DurationUnit.SECONDS)): String
    fun getCursor(cursorId: String): CursorStatus?
    fun updateCursor(cursorId: String, state: CursorStatus): Boolean
    fun deleteCursor(cursorId: String): Boolean
    fun isCursorExists(cursorId: String): Boolean
}