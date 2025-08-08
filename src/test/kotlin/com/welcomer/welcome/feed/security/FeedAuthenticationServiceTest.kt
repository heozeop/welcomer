package com.welcomer.welcome.feed.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeedAuthenticationServiceTest {

    private lateinit var authService: DefaultFeedAuthenticationService

    @BeforeEach
    fun setup() {
        authService = DefaultFeedAuthenticationService()
    }

    @Test
    fun `validateToken should return invalid result for empty token`() {
        val result = authService.validateToken("")
        
        assertFalse(result.isValid)
        assertEquals("Token is required", result.error)
        assertNull(result.userId)
    }

    @Test
    fun `validateToken should return invalid result for malformed token`() {
        val result = authService.validateToken("invalid-token")
        
        assertFalse(result.isValid)
        assertEquals("Invalid token format", result.error)
        assertNull(result.userId)
    }

    @Test
    fun `validateToken should return valid result for properly formatted Bearer token`() {
        val token = "Bearer abcdef123456789012345678901234567890"
        val result = authService.validateToken(token)
        
        assertTrue(result.isValid)
        assertNotNull(result.userId)
        assertTrue(result.permissions.contains("READ_FEED"))
        assertNotNull(result.expiresAt)
    }

    @Test
    fun `extractUserId should return user ID from valid token`() {
        val token = "Bearer validtoken123456789012345678901234"
        val userId = authService.extractUserId(token)
        
        assertNotNull(userId)
        assertTrue(userId!!.startsWith("user_"))
    }

    @Test
    fun `extractUserId should return null for invalid token`() {
        val userId = authService.extractUserId("invalid")
        
        assertNull(userId)
    }

    @Test
    fun `hasPermission should return true for valid user and endpoint`() {
        val hasPermission = authService.hasPermission("user123", "/api/v1/feed", "GET")
        
        assertTrue(hasPermission)
    }

    @Test
    fun `hasPermission should return false for admin endpoint with regular user`() {
        val hasPermission = authService.hasPermission("user123", "/api/v1/feed/stats", "GET")
        
        assertFalse(hasPermission)
    }

    @Test
    fun `hasPermission should return true for admin user and admin endpoint`() {
        val hasPermission = authService.hasPermission("admin_user", "/api/v1/feed/stats", "GET")
        
        assertTrue(hasPermission)
    }

    @Test
    fun `validateApiKey should return valid result for known API key`() {
        val result = authService.validateApiKey("api_key_content_12345")
        
        assertTrue(result.isValid)
        assertEquals("content-service", result.serviceId)
        assertTrue(result.permissions.contains("READ_FEED"))
    }

    @Test
    fun `validateApiKey should return invalid result for unknown API key`() {
        val result = authService.validateApiKey("unknown_key")
        
        assertFalse(result.isValid)
        assertEquals("Invalid API key", result.error)
        assertNull(result.serviceId)
    }

    @Test
    fun `generateApiKey should create API key with service ID`() {
        val apiKey = authService.generateApiKey("test-service")
        
        assertTrue(apiKey.startsWith("api_key_test-service_"))
        assertTrue(apiKey.length > 20)
    }
}