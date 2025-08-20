package com.welcomer.welcome.infrastructure.cursor

import com.welcomer.welcome.message.dto.SearchType
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface CursorService {
    fun createCursor(types: List<SearchType>, state: CursorStatus, ttl: Duration = 300.toDuration(DurationUnit.SECONDS)): String
    fun getCursor(types: List<SearchType>, cursorId: String): CursorStatus?
    fun updateCursor(types: List<SearchType>, cursorId: String, state: CursorStatus): Boolean
    fun deleteCursor(types: List<SearchType>, cursorId: String): Boolean
    fun isCursorExists(types: List<SearchType>, cursorId: String): Boolean
}