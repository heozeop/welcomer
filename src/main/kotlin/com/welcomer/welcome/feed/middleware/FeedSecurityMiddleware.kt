package com.welcomer.welcome.feed.middleware

import com.welcomer.welcome.feed.security.FeedAuthenticationService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Instant

/**
 * Security middleware for feed API endpoints
 */
@Component
class FeedSecurityFilter(
    private val authService: FeedAuthenticationService,
    private val objectMapper: ObjectMapper = ObjectMapper()
) : OncePerRequestFilter() {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val API_KEY_HEADER = "X-API-Key"
        private const val USER_ID_ATTRIBUTE = "userId"
        private const val SERVICE_ID_ATTRIBUTE = "serviceId"
        
        // Endpoints that require authentication
        private val PROTECTED_PATTERNS = listOf(
            "/api/v1/feed",
            "/api/v1/feed/",
        )
        
        // Public endpoints that don't require authentication
        private val PUBLIC_ENDPOINTS = listOf(
            "/api/v1/health",
            "/api/v1/info"
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestPath = request.requestURI
        
        // Skip authentication for public endpoints
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response)
            return
        }
        
        // Skip authentication for non-protected endpoints
        if (!isProtectedEndpoint(requestPath)) {
            filterChain.doFilter(request, response)
            return
        }
        
        try {
            // Try API key authentication first (for service-to-service)
            val apiKey = request.getHeader(API_KEY_HEADER)
            if (!apiKey.isNullOrBlank()) {
                val apiResult = authService.validateApiKey(apiKey)
                if (apiResult.isValid) {
                    request.setAttribute(SERVICE_ID_ATTRIBUTE, apiResult.serviceId)
                    filterChain.doFilter(request, response)
                    return
                } else {
                    sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid API key: ${apiResult.error}")
                    return
                }
            }
            
            // Try JWT authentication
            val authHeader = request.getHeader(AUTHORIZATION_HEADER)
            if (authHeader.isNullOrBlank()) {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Authorization header is required")
                return
            }
            
            val authResult = authService.validateToken(authHeader)
            if (!authResult.isValid) {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid token: ${authResult.error}")
                return
            }
            
            // Check permissions
            val hasPermission = authService.hasPermission(
                authResult.userId!!,
                requestPath,
                request.method
            )
            
            if (!hasPermission) {
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "Insufficient permissions")
                return
            }
            
            // Set user context for downstream processing
            request.setAttribute(USER_ID_ATTRIBUTE, authResult.userId)
            
            // Continue with the request
            filterChain.doFilter(request, response)
            
        } catch (e: Exception) {
            logger.error("Authentication error for request ${request.requestURI}", e)
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Authentication service error")
        }
    }
    
    private fun isPublicEndpoint(path: String): Boolean {
        return PUBLIC_ENDPOINTS.any { pattern ->
            path.startsWith(pattern)
        }
    }
    
    private fun isProtectedEndpoint(path: String): Boolean {
        return PROTECTED_PATTERNS.any { pattern ->
            path.startsWith(pattern)
        }
    }
    
    private fun sendErrorResponse(response: HttpServletResponse, status: HttpStatus, message: String) {
        response.status = status.value()
        response.contentType = "application/json"
        
        val errorResponse = mapOf(
            "success" to false,
            "error" to message,
            "timestamp" to Instant.now().toString(),
            "status" to status.value()
        )
        
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}

/**
 * Request validation middleware for feed API
 */
@Component
class FeedRequestValidationFilter : OncePerRequestFilter() {

    companion object {
        private const val MAX_LIMIT = 100
        private const val DEFAULT_LIMIT = 50
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestPath = request.requestURI
        
        // Only validate feed API requests
        if (!requestPath.startsWith("/api/v1/feed")) {
            filterChain.doFilter(request, response)
            return
        }
        
        try {
            // Validate pagination parameters
            val limitParam = request.getParameter("limit")
            if (limitParam != null) {
                val limit = limitParam.toIntOrNull()
                if (limit == null || limit <= 0) {
                    sendValidationError(response, "limit must be a positive integer")
                    return
                }
                if (limit > MAX_LIMIT) {
                    sendValidationError(response, "limit cannot exceed $MAX_LIMIT")
                    return
                }
            }
            
            // Validate cursor format if provided
            val cursor = request.getParameter("cursor")
            if (cursor != null && !isValidCursor(cursor)) {
                sendValidationError(response, "Invalid cursor format")
                return
            }
            
            // Validate feed type
            val pathSegments = requestPath.split("/")
            if (pathSegments.size > 4) {
                val feedType = pathSegments[4] // /api/v1/feed/{feedType}
                if (feedType.isNotEmpty() && !isValidFeedType(feedType)) {
                    sendValidationError(response, "Invalid feed type: $feedType")
                    return
                }
            }
            
            // Validate filter parameters
            val contentType = request.getParameter("content_type")
            if (contentType != null && !isValidContentType(contentType)) {
                sendValidationError(response, "Invalid content type: $contentType")
                return
            }
            
            // Continue processing
            filterChain.doFilter(request, response)
            
        } catch (e: Exception) {
            logger.error("Request validation error for ${request.requestURI}", e)
            sendValidationError(response, "Request validation failed")
        }
    }
    
    private fun isValidCursor(cursor: String): Boolean {
        // Basic cursor validation - should be base64 encoded or UUID format
        return cursor.matches(Regex("[a-zA-Z0-9+/=_-]+"))
    }
    
    private fun isValidFeedType(feedType: String): Boolean {
        val validTypes = setOf("home", "following", "explore", "trending", "personalized")
        return feedType.lowercase() in validTypes
    }
    
    private fun isValidContentType(contentType: String): Boolean {
        val validTypes = setOf("text", "image", "video", "link", "poll")
        return contentType.lowercase() in validTypes
    }
    
    private fun sendValidationError(response: HttpServletResponse, message: String) {
        response.status = HttpStatus.BAD_REQUEST.value()
        response.contentType = "application/json"
        
        val errorResponse = mapOf(
            "success" to false,
            "error" to message,
            "timestamp" to Instant.now().toString(),
            "status" to HttpStatus.BAD_REQUEST.value()
        )
        
        val objectMapper = ObjectMapper()
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}

/**
 * Rate limiting middleware for feed API
 */
@Component
class FeedRateLimitFilter : OncePerRequestFilter() {

    // In-memory rate limiting (in production, use Redis)
    private val userRequestCounts = mutableMapOf<String, RequestWindow>()
    private val ipRequestCounts = mutableMapOf<String, RequestWindow>()
    
    companion object {
        private const val MAX_REQUESTS_PER_MINUTE_USER = 60
        private const val MAX_REQUESTS_PER_MINUTE_IP = 100
        private const val WINDOW_SIZE_SECONDS = 60
    }

    data class RequestWindow(
        var count: Int = 0,
        var windowStart: Long = System.currentTimeMillis()
    ) {
        fun isExpired(now: Long): Boolean {
            return (now - windowStart) > (WINDOW_SIZE_SECONDS * 1000)
        }
        
        fun reset(now: Long) {
            count = 0
            windowStart = now
        }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestPath = request.requestURI
        
        // Only apply rate limiting to feed endpoints
        if (!requestPath.startsWith("/api/v1/feed")) {
            filterChain.doFilter(request, response)
            return
        }
        
        val now = System.currentTimeMillis()
        val clientIp = getClientIp(request)
        val userId = request.getAttribute("userId") as? String
        
        try {
            // Check IP-based rate limit
            if (!checkIpRateLimit(clientIp, now)) {
                sendRateLimitError(response, "Too many requests from IP address")
                return
            }
            
            // Check user-based rate limit if user is authenticated
            if (userId != null && !checkUserRateLimit(userId, now)) {
                sendRateLimitError(response, "Too many requests for user")
                return
            }
            
            filterChain.doFilter(request, response)
            
        } catch (e: Exception) {
            logger.error("Rate limiting error", e)
            filterChain.doFilter(request, response) // Don't block on rate limit errors
        }
    }
    
    private fun checkIpRateLimit(ip: String, now: Long): Boolean {
        val window = ipRequestCounts.getOrPut(ip) { RequestWindow() }
        
        if (window.isExpired(now)) {
            window.reset(now)
        }
        
        if (window.count >= MAX_REQUESTS_PER_MINUTE_IP) {
            return false
        }
        
        window.count++
        return true
    }
    
    private fun checkUserRateLimit(userId: String, now: Long): Boolean {
        val window = userRequestCounts.getOrPut(userId) { RequestWindow() }
        
        if (window.isExpired(now)) {
            window.reset(now)
        }
        
        if (window.count >= MAX_REQUESTS_PER_MINUTE_USER) {
            return false
        }
        
        window.count++
        return true
    }
    
    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }
        
        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }
        
        return request.remoteAddr
    }
    
    private fun sendRateLimitError(response: HttpServletResponse, message: String) {
        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.contentType = "application/json"
        response.setHeader("Retry-After", "60") // Retry after 60 seconds
        
        val errorResponse = mapOf(
            "success" to false,
            "error" to message,
            "timestamp" to Instant.now().toString(),
            "status" to HttpStatus.TOO_MANY_REQUESTS.value()
        )
        
        val objectMapper = ObjectMapper()
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}