package com.welcomer.welcome.feed.config

import com.welcomer.welcome.feed.middleware.FeedRateLimitFilter
import com.welcomer.welcome.feed.middleware.FeedRequestValidationFilter
import com.welcomer.welcome.feed.middleware.FeedSecurityFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * Configuration for feed API security and middleware
 */
@Configuration
class FeedSecurityConfig {

    /**
     * Register security filter with high priority
     */
    @Bean
    fun feedSecurityFilterRegistration(securityFilter: FeedSecurityFilter): FilterRegistrationBean<FeedSecurityFilter> {
        val registration = FilterRegistrationBean<FeedSecurityFilter>()
        registration.filter = securityFilter
        registration.urlPatterns = listOf("/api/v1/feed/*")
        registration.order = Ordered.HIGHEST_PRECEDENCE // Run first
        registration.setName("feedSecurityFilter")
        return registration
    }

    /**
     * Register request validation filter
     */
    @Bean
    fun feedValidationFilterRegistration(validationFilter: FeedRequestValidationFilter): FilterRegistrationBean<FeedRequestValidationFilter> {
        val registration = FilterRegistrationBean<FeedRequestValidationFilter>()
        registration.filter = validationFilter
        registration.urlPatterns = listOf("/api/v1/feed/*")
        registration.order = Ordered.HIGHEST_PRECEDENCE + 1 // Run after security
        registration.setName("feedValidationFilter")
        return registration
    }

    /**
     * Register rate limiting filter
     */
    @Bean
    fun feedRateLimitFilterRegistration(rateLimitFilter: FeedRateLimitFilter): FilterRegistrationBean<FeedRateLimitFilter> {
        val registration = FilterRegistrationBean<FeedRateLimitFilter>()
        registration.filter = rateLimitFilter
        registration.urlPatterns = listOf("/api/v1/feed/*")
        registration.order = Ordered.HIGHEST_PRECEDENCE + 2 // Run after validation
        registration.setName("feedRateLimitFilter")
        return registration
    }
}

/**
 * Custom exception handler for feed API errors
 */
@org.springframework.web.bind.annotation.RestControllerAdvice
@org.springframework.web.bind.annotation.RequestMapping("/api/v1/feed")
class FeedExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): org.springframework.http.ResponseEntity<Map<String, Any>> {
        val errorResponse = mapOf(
            "success" to false,
            "error" to (e.message ?: "Invalid request parameter"),
            "timestamp" to java.time.Instant.now().toString(),
            "status" to 400
        )
        return org.springframework.http.ResponseEntity.badRequest().body(errorResponse)
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(SecurityException::class)
    fun handleSecurity(e: SecurityException): org.springframework.http.ResponseEntity<Map<String, Any>> {
        val errorResponse = mapOf(
            "success" to false,
            "error" to "Access denied",
            "timestamp" to java.time.Instant.now().toString(),
            "status" to 403
        )
        return org.springframework.http.ResponseEntity.status(403).body(errorResponse)
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception::class)
    fun handleGeneral(e: Exception): org.springframework.http.ResponseEntity<Map<String, Any>> {
        val errorResponse = mapOf(
            "success" to false,
            "error" to "Internal server error",
            "timestamp" to java.time.Instant.now().toString(),
            "status" to 500
        )
        return org.springframework.http.ResponseEntity.internalServerError().body(errorResponse)
    }
}