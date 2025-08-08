package com.welcomer.welcome.user.service

import com.welcomer.welcome.personalization.service.UserContext
import org.springframework.stereotype.Service

/**
 * Service for retrieving user context information for personalization
 */
interface UserContextService {
    /**
     * Get current user context including device, location, time patterns, etc.
     */
    suspend fun getUserContext(userId: String): UserContext
    
    /**
     * Update user context based on current session activity
     */
    suspend fun updateSessionContext(
        userId: String, 
        sessionDuration: Long, 
        recentActivity: List<String>
    ): UserContext
}

/**
 * Default implementation that provides basic user context
 */
@Service
class DefaultUserContextService : UserContextService {
    
    override suspend fun getUserContext(userId: String): UserContext {
        // In a real implementation, this would fetch from user sessions, device detection, etc.
        val now = java.time.Instant.now()
        val zoneId = java.time.ZoneId.systemDefault()
        val zonedTime = now.atZone(zoneId)
        
        return UserContext(
            timeOfDay = zonedTime.hour,
            dayOfWeek = zonedTime.dayOfWeek.value,
            deviceType = com.welcomer.welcome.personalization.service.DeviceType.UNKNOWN,
            location = null,
            sessionDuration = 0,
            previousActivity = emptyList(),
            contextualPreferences = emptyMap()
        )
    }
    
    override suspend fun updateSessionContext(
        userId: String,
        sessionDuration: Long,
        recentActivity: List<String>
    ): UserContext {
        val baseContext = getUserContext(userId)
        return baseContext.copy(
            sessionDuration = sessionDuration,
            previousActivity = recentActivity
        )
    }
}