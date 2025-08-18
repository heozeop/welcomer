package com.welcomer.welcome.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Instant.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime =LocalDateTime.ofInstant(this, zoneId)