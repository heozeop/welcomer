package com.welcomer.welcome.user.service

import com.welcomer.welcome.user.model.*
import com.welcomer.welcome.user.repository.PreferenceRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant

class UserPreferenceServiceTest {

    private lateinit var preferenceRepository: PreferenceRepository
    private lateinit var behaviorAnalysisService: BehaviorAnalysisService
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var userPreferenceService: UserPreferenceService

    @BeforeEach
    fun setup() {
        preferenceRepository = mock()
        behaviorAnalysisService = mock()
        eventPublisher = mock()
        userPreferenceService = DefaultUserPreferenceService(
            preferenceRepository,
            behaviorAnalysisService,
            eventPublisher
        )
    }

    @Test
    fun `getPreferences should return merged explicit and implicit preferences`() = runBlocking {
        val userId = "user123"
        
        // Mock explicit preferences
        val explicitPrefs = listOf(
            createTestPreference(userId, PreferenceType.TOPIC_INTEREST, "topic:technology", "interested", 0.8, PreferenceSource.EXPLICIT)
        )
        whenever(preferenceRepository.findByUserIdAndSource(userId, PreferenceSource.EXPLICIT))
            .thenReturn(explicitPrefs)
            
        // Mock implicit preferences
        val implicitPrefs = listOf(
            createTestPreference(userId, PreferenceType.CONTENT_TYPE, "content_type:image", "preferred", 0.6, PreferenceSource.IMPLICIT)
        )
        val implicitResult = ImplicitPreferenceResult(
            userId = userId,
            inferredPreferences = implicitPrefs,
            confidence = 0.7,
            analysisMetadata = emptyMap(),
            generatedAt = Instant.now()
        )
        whenever(behaviorAnalysisService.getImplicitPreferences(userId))
            .thenReturn(implicitResult)

        // Test
        val result = userPreferenceService.getPreferences(userId)

        // Verify
        assertNotNull(result)
        assertEquals(userId, result!!.userId)
        assertTrue(result.topicInterests.containsKey("technology"))
        assertTrue(result.contentTypePreferences.containsKey("image"))
        assertEquals(0.8, result.topicInterests["technology"]!!, 0.01)
    }

    @Test
    fun `updatePreferences should save preferences and publish event`() = runBlocking {
        val userId = "user123"
        val update = PreferenceUpdate(
            type = PreferenceType.TOPIC_INTEREST,
            key = "topic:technology",
            value = "interested",
            weight = 0.9,
            operation = PreferenceOperation.SET
        )
        val request = PreferenceUpdateRequest(listOf(update))

        // Mock repository save
        val savedPreference = createTestPreference(userId, update.type, update.key, update.value, update.weight, PreferenceSource.EXPLICIT)
        whenever(preferenceRepository.findByUserIdAndTypeAndKey(userId, update.type, update.key))
            .thenReturn(null) // New preference
        whenever(preferenceRepository.saveAll(any<List<UserPreference>>()))
            .thenReturn(listOf(savedPreference))

        // Test
        val result = userPreferenceService.updatePreferences(userId, request)

        // Verify
        assertTrue(result)
        verify(preferenceRepository).saveAll(any<List<UserPreference>>())
        verify(eventPublisher).publishEvent(any<PreferencesUpdatedEvent>())
    }

    @Test
    fun `updatePreference should handle DELETE operation`() = runBlocking {
        val userId = "user123"
        val update = PreferenceUpdate(
            type = PreferenceType.TOPIC_INTEREST,
            key = "topic:technology",
            value = "interested",
            weight = 0.9,
            operation = PreferenceOperation.DELETE
        )

        // Test
        val result = userPreferenceService.updatePreference(userId, update)

        // Verify
        assertTrue(result)
        verify(preferenceRepository).deleteByUserIdAndTypeAndKey(userId, update.type, update.key)
        verify(eventPublisher).publishEvent(any<PreferencesUpdatedEvent>())
    }

    @Test
    fun `getExplicitPreferences should return only explicit preferences`() = runBlocking {
        val userId = "user123"
        val expectedProfile = UserPreferenceProfile(
            userId = userId,
            topicInterests = mapOf("technology" to 0.8),
            contentTypePreferences = mapOf("text" to 0.9),
            languagePreferences = listOf("en"),
            followedAccounts = emptySet(),
            blockedUsers = emptySet(),
            blockedTopics = emptySet(),
            algorithmPreferences = emptyMap(),
            lastUpdated = Instant.now(),
            confidence = 0.9
        )

        whenever(preferenceRepository.getPreferenceProfile(userId))
            .thenReturn(expectedProfile)

        // Test
        val result = userPreferenceService.getExplicitPreferences(userId)

        // Verify
        assertNotNull(result)
        assertEquals(expectedProfile, result)
        verify(preferenceRepository).getPreferenceProfile(userId)
    }

    @Test
    fun `refreshImplicitPreferences should update implicit preferences`() = runBlocking {
        val userId = "user123"
        
        // Mock existing implicit preferences
        val existingImplicitPrefs = listOf(
            createTestPreference(userId, PreferenceType.TOPIC_INTEREST, "topic:old_interest", "interested", 0.5, PreferenceSource.IMPLICIT)
        )
        whenever(preferenceRepository.findByUserIdAndSource(userId, PreferenceSource.IMPLICIT))
            .thenReturn(existingImplicitPrefs)

        // Mock new implicit preferences from behavior analysis
        val newImplicitPrefs = listOf(
            createTestPreference(userId, PreferenceType.TOPIC_INTEREST, "topic:new_interest", "interested", 0.7, PreferenceSource.IMPLICIT)
        )
        val analysisResult = ImplicitPreferenceResult(
            userId = userId,
            inferredPreferences = newImplicitPrefs,
            confidence = 0.8,
            analysisMetadata = emptyMap(),
            generatedAt = Instant.now()
        )
        whenever(behaviorAnalysisService.getImplicitPreferences(userId))
            .thenReturn(analysisResult)

        // Test
        val result = userPreferenceService.refreshImplicitPreferences(userId)

        // Verify
        assertTrue(result)
        verify(preferenceRepository).findByUserIdAndSource(userId, PreferenceSource.IMPLICIT)
        verify(preferenceRepository).deleteById(existingImplicitPrefs[0].id)
        verify(preferenceRepository).saveAll(newImplicitPrefs)
        verify(eventPublisher).publishEvent(any<PreferencesUpdatedEvent>())
    }

    @Test
    fun `mergePreferences should handle conflicts correctly`() = runBlocking {
        val userId = "user123"
        
        // Create conflicting preferences
        val explicitPref = createTestPreference(
            userId, PreferenceType.TOPIC_INTEREST, "topic:technology", "interested", 0.9, PreferenceSource.EXPLICIT, 0.8
        )
        val implicitPref = createTestPreference(
            userId, PreferenceType.TOPIC_INTEREST, "topic:technology", "interested", 0.6, PreferenceSource.IMPLICIT, 0.7
        )

        val explicit = listOf(explicitPref)
        val implicit = listOf(implicitPref)

        // Test with EXPLICIT_WINS strategy
        val config = PreferenceMergingConfig(conflictResolution = ConflictResolutionStrategy.EXPLICIT_WINS)
        val result = userPreferenceService.mergePreferences(explicit, implicit, config)

        // Verify explicit preference wins
        assertNotNull(result)
        assertEquals(0.9 * 0.8, result!!.topicInterests["technology"]!!, 0.01) // explicit weight * confidence
    }

    @Test
    fun `deleteAllPreferences should remove all preferences and publish event`() = runBlocking {
        val userId = "user123"

        // Test
        val result = userPreferenceService.deleteAllPreferences(userId)

        // Verify
        assertTrue(result)
        verify(preferenceRepository).deleteByUserId(userId)
        verify(eventPublisher).publishEvent(any<PreferencesUpdatedEvent>())
    }

    // Helper method to create test preferences
    private fun createTestPreference(
        userId: String,
        type: PreferenceType,
        key: String,
        value: String,
        weight: Double,
        source: PreferenceSource,
        confidence: Double = 1.0
    ): UserPreference {
        return UserPreference(
            id = "pref_${System.nanoTime()}",
            userId = userId,
            type = type,
            key = key,
            value = value,
            weight = weight,
            source = source,
            confidence = confidence,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}