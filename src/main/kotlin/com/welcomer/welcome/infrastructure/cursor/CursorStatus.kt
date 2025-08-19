package com.welcomer.welcome.infrastructure.cursor


data class CursorStatus (
    val lastScore: Long? = null,
    val lastId: UInt? = null,
    val sources: Map<String, Long> = emptyMap(),
    val version: Long = 0L
)