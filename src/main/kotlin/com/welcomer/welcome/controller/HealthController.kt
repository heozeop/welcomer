package com.welcomer.welcome.controller

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/health")
class HealthController {

    @GetMapping
    fun getHealth(): ResponseEntity<HealthResponse> {
        val health = checkHealth()
        val response = HealthResponse(
            status = health.status.code,
            timestamp = LocalDateTime.now(),
            details = health.details
        )
        
        return if (health.status == Status.UP) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.status(503).body(response)
        }
    }

    @GetMapping("/live")
    fun liveness(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "UP"))
    }

    @GetMapping("/ready")
    fun readiness(): ResponseEntity<Map<String, Any>> {
        val health = checkHealth()
        return if (health.status == Status.UP) {
            ResponseEntity.ok(mapOf(
                "status" to "UP",
                "checks" to health.details
            ))
        } else {
            ResponseEntity.status(503).body(mapOf(
                "status" to "DOWN",
                "checks" to health.details
            ))
        }
    }

    private fun checkHealth(): Health {
        return try {
            Health.up()
                .withDetail("service", "welcome-service")
                .withDetail("version", "0.0.1-SNAPSHOT")
                .build()
        } catch (e: Exception) {
            Health.down()
                .withDetail("error", e.message ?: "Unknown error")
                .build()
        }
    }
}

data class HealthResponse(
    val status: String,
    val timestamp: LocalDateTime,
    val details: Map<String, Any>
)