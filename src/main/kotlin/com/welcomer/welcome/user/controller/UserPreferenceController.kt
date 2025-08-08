package com.welcomer.welcome.user.controller

import com.welcomer.welcome.user.model.*
import com.welcomer.welcome.user.service.UserPreferenceService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for managing user preferences
 */
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = ["*"])
class UserPreferenceController(
    private val userPreferenceService: UserPreferenceService
) {

    /**
     * Get user preferences (merged explicit + implicit)
     */
    @GetMapping("/{userId}/preferences")
    suspend fun getUserPreferences(
        @PathVariable userId: String,
        @RequestParam(defaultValue = "all") type: String,
        request: HttpServletRequest
    ): ResponseEntity<UserPreferenceResponse> {
        
        // Validate user access (in production, would check authentication)
        val authenticatedUserId = request.getAttribute("userId") as? String
        if (authenticatedUserId != userId && !isAdminUser(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(UserPreferenceResponse.error("Access denied"))
        }

        return try {
            val profile = when (type.lowercase()) {
                "explicit" -> userPreferenceService.getExplicitPreferences(userId)
                "implicit" -> userPreferenceService.getImplicitPreferences(userId)
                "all" -> userPreferenceService.getPreferences(userId)
                else -> {
                    return ResponseEntity.badRequest()
                        .body(UserPreferenceResponse.error("Invalid type parameter. Must be 'explicit', 'implicit', or 'all'"))
                }
            }

            if (profile != null) {
                ResponseEntity.ok(UserPreferenceResponse.success(profile))
            } else {
                ResponseEntity.ok(UserPreferenceResponse.success(createDefaultProfile(userId)))
            }

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(UserPreferenceResponse.error("Failed to retrieve preferences: ${e.message}"))
        }
    }

    /**
     * Update user preferences
     */
    @PutMapping("/{userId}/preferences")
    suspend fun updateUserPreferences(
        @PathVariable userId: String,
        @Valid @RequestBody request: PreferenceUpdateRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<PreferenceUpdateResponse> {
        
        // Validate user access
        val authenticatedUserId = httpRequest.getAttribute("userId") as? String
        if (authenticatedUserId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(PreferenceUpdateResponse.error("Access denied"))
        }

        // Validate request
        val validationResult = validatePreferenceUpdateRequest(request)
        if (!validationResult.isValid) {
            return ResponseEntity.badRequest()
                .body(PreferenceUpdateResponse.error("Validation failed: ${validationResult.errors.joinToString(", ")}"))
        }

        return try {
            val success = userPreferenceService.updatePreferences(userId, request)
            
            if (success) {
                ResponseEntity.ok(PreferenceUpdateResponse.success("Preferences updated successfully"))
            } else {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PreferenceUpdateResponse.error("Failed to update preferences"))
            }

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PreferenceUpdateResponse.error("Failed to update preferences: ${e.message}"))
        }
    }

    /**
     * Update a single preference
     */
    @PatchMapping("/{userId}/preferences/{type}/{key}")
    suspend fun updateSinglePreference(
        @PathVariable userId: String,
        @PathVariable type: String,
        @PathVariable key: String,
        @Valid @RequestBody request: SinglePreferenceUpdateRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<PreferenceUpdateResponse> {
        
        // Validate user access
        val authenticatedUserId = httpRequest.getAttribute("userId") as? String
        if (authenticatedUserId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(PreferenceUpdateResponse.error("Access denied"))
        }

        return try {
            val preferenceType = PreferenceType.valueOf(type.uppercase())
            val update = PreferenceUpdate(
                type = preferenceType,
                key = key,
                value = request.value,
                weight = request.weight,
                operation = request.operation
            )

            val validationResult = validatePreferenceUpdate(update)
            if (!validationResult.isValid) {
                return ResponseEntity.badRequest()
                    .body(PreferenceUpdateResponse.error("Validation failed: ${validationResult.errors.joinToString(", ")}"))
            }

            val success = userPreferenceService.updatePreference(userId, update)
            
            if (success) {
                ResponseEntity.ok(PreferenceUpdateResponse.success("Preference updated successfully"))
            } else {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PreferenceUpdateResponse.error("Failed to update preference"))
            }

        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(PreferenceUpdateResponse.error("Invalid preference type: $type"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PreferenceUpdateResponse.error("Failed to update preference: ${e.message}"))
        }
    }

    /**
     * Delete all user preferences
     */
    @DeleteMapping("/{userId}/preferences")
    suspend fun deleteUserPreferences(
        @PathVariable userId: String,
        httpRequest: HttpServletRequest
    ): ResponseEntity<PreferenceUpdateResponse> {
        
        // Validate user access
        val authenticatedUserId = httpRequest.getAttribute("userId") as? String
        if (authenticatedUserId != userId && !isAdminUser(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(PreferenceUpdateResponse.error("Access denied"))
        }

        return try {
            val success = userPreferenceService.deleteAllPreferences(userId)
            
            if (success) {
                ResponseEntity.ok(PreferenceUpdateResponse.success("All preferences deleted successfully"))
            } else {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PreferenceUpdateResponse.error("Failed to delete preferences"))
            }

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PreferenceUpdateResponse.error("Failed to delete preferences: ${e.message}"))
        }
    }

    /**
     * Get user preference statistics
     */
    @GetMapping("/{userId}/preferences/stats")
    suspend fun getUserPreferenceStats(
        @PathVariable userId: String,
        request: HttpServletRequest
    ): ResponseEntity<PreferenceStatsResponse> {
        
        // Validate user access
        val authenticatedUserId = request.getAttribute("userId") as? String
        if (authenticatedUserId != userId && !isAdminUser(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(PreferenceStatsResponse.error("Access denied"))
        }

        return try {
            val stats = userPreferenceService.getPreferenceStats(userId)
            
            if (stats != null) {
                ResponseEntity.ok(PreferenceStatsResponse.success(stats))
            } else {
                ResponseEntity.ok(PreferenceStatsResponse.success(createEmptyStats(userId)))
            }

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PreferenceStatsResponse.error("Failed to retrieve preference stats: ${e.message}"))
        }
    }

    /**
     * Refresh implicit preferences based on recent behavior
     */
    @PostMapping("/{userId}/preferences/refresh")
    suspend fun refreshImplicitPreferences(
        @PathVariable userId: String,
        request: HttpServletRequest
    ): ResponseEntity<PreferenceUpdateResponse> {
        
        // Validate user access
        val authenticatedUserId = request.getAttribute("userId") as? String
        if (authenticatedUserId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(PreferenceUpdateResponse.error("Access denied"))
        }

        return try {
            val success = userPreferenceService.refreshImplicitPreferences(userId)
            
            if (success) {
                ResponseEntity.ok(PreferenceUpdateResponse.success("Implicit preferences refreshed successfully"))
            } else {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PreferenceUpdateResponse.error("Failed to refresh implicit preferences"))
            }

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PreferenceUpdateResponse.error("Failed to refresh preferences: ${e.message}"))
        }
    }

    // Private helper methods

    private fun validatePreferenceUpdateRequest(request: PreferenceUpdateRequest): ValidationResult {
        val errors = mutableListOf<String>()

        if (request.preferences.isEmpty()) {
            errors.add("At least one preference update is required")
        }

        if (request.preferences.size > 100) {
            errors.add("Maximum 100 preference updates allowed per request")
        }

        request.preferences.forEach { update ->
            val updateValidation = validatePreferenceUpdate(update)
            if (!updateValidation.isValid) {
                errors.addAll(updateValidation.errors)
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    private fun validatePreferenceUpdate(update: PreferenceUpdate): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate key format
        if (update.key.isBlank()) {
            errors.add("Preference key cannot be blank")
        }

        if (update.key.length > 200) {
            errors.add("Preference key cannot exceed 200 characters")
        }

        // Validate value
        if (update.value.length > 1000) {
            errors.add("Preference value cannot exceed 1000 characters")
        }

        // Validate weight
        if (update.weight < 0.0 || update.weight > 1.0) {
            errors.add("Preference weight must be between 0.0 and 1.0")
        }

        // Validate type-specific constraints
        when (update.type) {
            PreferenceType.TOPIC_INTEREST -> {
                if (!update.key.startsWith("topic:")) {
                    errors.add("Topic interest key must start with 'topic:'")
                }
            }
            PreferenceType.CONTENT_TYPE -> {
                if (!update.key.startsWith("content_type:")) {
                    errors.add("Content type key must start with 'content_type:'")
                }
                val contentType = update.key.removePrefix("content_type:")
                if (contentType !in setOf("text", "image", "video", "link", "poll")) {
                    errors.add("Invalid content type: $contentType")
                }
            }
            PreferenceType.LANGUAGE -> {
                if (update.value.length != 2) {
                    errors.add("Language preference value must be a 2-letter language code")
                }
            }
            PreferenceType.FOLLOWED_ACCOUNT, PreferenceType.BLOCKED_USER -> {
                if (!update.value.matches(Regex("[a-zA-Z0-9_-]+"))) {
                    errors.add("User ID must contain only alphanumeric characters, underscores, and hyphens")
                }
            }
            else -> {
                // Type-specific validation for other preference types
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    private fun isAdminUser(userId: String?): Boolean {
        // In production, this would check actual admin permissions
        return userId?.startsWith("admin_") == true
    }

    private fun createDefaultProfile(userId: String): UserPreferenceProfile {
        return UserPreferenceProfile(
            userId = userId,
            topicInterests = emptyMap(),
            contentTypePreferences = mapOf(
                "text" to 0.8,
                "image" to 0.6,
                "video" to 0.4
            ),
            languagePreferences = listOf("en"),
            followedAccounts = emptySet(),
            blockedUsers = emptySet(),
            blockedTopics = emptySet(),
            algorithmPreferences = mapOf(
                "feed_type" to "balanced",
                "diversity" to "medium"
            ),
            lastUpdated = java.time.Instant.now(),
            confidence = 0.3
        )
    }

    private fun createEmptyStats(userId: String): PreferenceStats {
        return PreferenceStats(
            userId = userId,
            totalPreferences = 0,
            explicitPreferences = 0,
            implicitPreferences = 0,
            preferencesByType = emptyMap(),
            averageConfidence = 0.0,
            lastActivityAt = null
        )
    }
}

// Response models

data class UserPreferenceResponse(
    val success: Boolean,
    val data: UserPreferenceProfile? = null,
    val error: String? = null,
    val timestamp: java.time.Instant = java.time.Instant.now()
) {
    companion object {
        fun success(data: UserPreferenceProfile) = UserPreferenceResponse(
            success = true,
            data = data
        )

        fun error(message: String) = UserPreferenceResponse(
            success = false,
            error = message
        )
    }
}

data class PreferenceUpdateResponse(
    val success: Boolean,
    val message: String,
    val timestamp: java.time.Instant = java.time.Instant.now()
) {
    companion object {
        fun success(message: String) = PreferenceUpdateResponse(
            success = true,
            message = message
        )

        fun error(message: String) = PreferenceUpdateResponse(
            success = false,
            message = message
        )
    }
}

data class PreferenceStatsResponse(
    val success: Boolean,
    val data: PreferenceStats? = null,
    val error: String? = null,
    val timestamp: java.time.Instant = java.time.Instant.now()
) {
    companion object {
        fun success(data: PreferenceStats) = PreferenceStatsResponse(
            success = true,
            data = data
        )

        fun error(message: String) = PreferenceStatsResponse(
            success = false,
            error = message
        )
    }
}

data class SinglePreferenceUpdateRequest(
    val value: String,
    val weight: Double = 1.0,
    val operation: PreferenceOperation = PreferenceOperation.SET
)

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)