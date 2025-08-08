package com.welcomer.welcome.feed.security

import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

/**
 * Service for handling JWT authentication and user validation for feed API
 */
interface FeedAuthenticationService {
    /**
     * Validate JWT token and extract user information
     */
    fun validateToken(token: String): AuthenticationResult

    /**
     * Extract user ID from valid token
     */
    fun extractUserId(token: String): String?

    /**
     * Check if user has permission to access feed endpoints
     */
    fun hasPermission(userId: String, endpoint: String, method: String): Boolean

    /**
     * Generate API key for service-to-service communication
     */
    fun generateApiKey(serviceId: String): String

    /**
     * Validate API key for internal services
     */
    fun validateApiKey(apiKey: String): ApiKeyResult
}

/**
 * Result of JWT token validation
 */
data class AuthenticationResult(
    val isValid: Boolean,
    val userId: String? = null,
    val permissions: Set<String> = emptySet(),
    val expiresAt: Instant? = null,
    val error: String? = null
)

/**
 * Result of API key validation
 */
data class ApiKeyResult(
    val isValid: Boolean,
    val serviceId: String? = null,
    val permissions: Set<String> = emptySet(),
    val error: String? = null
)

/**
 * User permission levels for feed access
 */
enum class FeedPermission {
    READ_FEED,           // Can read feeds
    REFRESH_FEED,        // Can refresh feeds
    UPDATE_PREFERENCES,  // Can update feed preferences
    ADMIN_STATS          // Can access admin statistics
}

@Service
class DefaultFeedAuthenticationService : FeedAuthenticationService {

    companion object {
        // In production, these would come from environment variables or secure configuration
        private const val JWT_SECRET = "your-secret-key-here" // Should be from config
        private val API_KEYS = mapOf(
            "content-service" to "api_key_content_12345",
            "analytics-service" to "api_key_analytics_67890"
        )
        
        // Default permissions by user type
        private val DEFAULT_USER_PERMISSIONS = setOf(
            FeedPermission.READ_FEED.name,
            FeedPermission.REFRESH_FEED.name,
            FeedPermission.UPDATE_PREFERENCES.name
        )
        
        private val ADMIN_PERMISSIONS = DEFAULT_USER_PERMISSIONS + setOf(
            FeedPermission.ADMIN_STATS.name
        )
    }

    override fun validateToken(token: String): AuthenticationResult {
        return try {
            // In production, use a proper JWT library like jjwt-api
            // For now, implement basic validation logic
            
            if (token.isBlank()) {
                return AuthenticationResult(false, error = "Token is required")
            }
            
            // Mock JWT validation - in production, decode and verify signature
            if (token.startsWith("Bearer ")) {
                val actualToken = token.removePrefix("Bearer ")
                
                // Simple mock validation - check if token looks like a UUID
                if (isValidTokenFormat(actualToken)) {
                    val userId = extractUserIdFromToken(actualToken)
                    val permissions = getUserPermissions(userId)
                    
                    return AuthenticationResult(
                        isValid = true,
                        userId = userId,
                        permissions = permissions,
                        expiresAt = Instant.now().plusSeconds(3600) // 1 hour from now
                    )
                }
            }
            
            AuthenticationResult(false, error = "Invalid token format")
            
        } catch (e: Exception) {
            AuthenticationResult(false, error = "Token validation failed: ${e.message}")
        }
    }

    override fun extractUserId(token: String): String? {
        val result = validateToken(token)
        return if (result.isValid) result.userId else null
    }

    override fun hasPermission(userId: String, endpoint: String, method: String): Boolean {
        val requiredPermission = getRequiredPermission(endpoint, method)
        val userPermissions = getUserPermissions(userId)
        
        return requiredPermission in userPermissions
    }

    override fun generateApiKey(serviceId: String): String {
        // In production, generate secure API keys and store in database
        return "api_key_${serviceId}_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    }

    override fun validateApiKey(apiKey: String): ApiKeyResult {
        return try {
            val serviceEntry = API_KEYS.entries.find { it.value == apiKey }
            
            if (serviceEntry != null) {
                ApiKeyResult(
                    isValid = true,
                    serviceId = serviceEntry.key,
                    permissions = getServicePermissions(serviceEntry.key)
                )
            } else {
                ApiKeyResult(false, error = "Invalid API key")
            }
            
        } catch (e: Exception) {
            ApiKeyResult(false, error = "API key validation failed: ${e.message}")
        }
    }

    // Private helper methods

    private fun isValidTokenFormat(token: String): Boolean {
        // Mock validation - in production, parse JWT structure
        return token.length >= 32 && token.matches(Regex("[a-zA-Z0-9-_]+"))
    }

    private fun extractUserIdFromToken(token: String): String {
        // Mock extraction - in production, decode JWT payload
        return "user_${token.take(8)}"
    }

    private fun getUserPermissions(userId: String): Set<String> {
        // In production, fetch from database based on user role
        return if (userId.startsWith("admin_")) {
            ADMIN_PERMISSIONS
        } else {
            DEFAULT_USER_PERMISSIONS
        }
    }

    private fun getRequiredPermission(endpoint: String, method: String): String {
        return when {
            endpoint.contains("/feed") && method == "GET" -> FeedPermission.READ_FEED.name
            endpoint.contains("/refresh") -> FeedPermission.REFRESH_FEED.name
            endpoint.contains("/preferences") && method == "PUT" -> FeedPermission.UPDATE_PREFERENCES.name
            endpoint.contains("/stats") -> FeedPermission.ADMIN_STATS.name
            else -> FeedPermission.READ_FEED.name // Default
        }
    }

    private fun getServicePermissions(serviceId: String): Set<String> {
        return when (serviceId) {
            "content-service" -> setOf(
                FeedPermission.READ_FEED.name,
                FeedPermission.REFRESH_FEED.name
            )
            "analytics-service" -> setOf(
                FeedPermission.READ_FEED.name,
                FeedPermission.ADMIN_STATS.name
            )
            else -> emptySet()
        }
    }
}