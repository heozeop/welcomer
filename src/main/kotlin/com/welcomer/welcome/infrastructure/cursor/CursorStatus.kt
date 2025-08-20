package com.welcomer.welcome.infrastructure.cursor


data class CursorStatus (
    val sources: Map<String, UInt> = emptyMap(),
    val version: Long = 0L
)