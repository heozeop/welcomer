package com.welcomer.welcome.bdd.fixtures

import com.welcomer.welcome.personalization.service.DeviceType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Simple test for UserPersona to verify Task 15 completion
 */
class UserPersonaSimpleTest {

    @Test
    fun `should create new user persona`() {
        val persona = UserPersona(
            type = PersonaType.NEW_USER,
            userId = "test-new-user",
            description = "Test new user"
        )

        assertEquals(PersonaType.NEW_USER, persona.type)
        assertEquals("test-new-user", persona.userId)
        assertEquals("Test new user", persona.description)
    }

    @Test
    fun `should generate persona data`() {
        val persona = UserPersona(
            type = PersonaType.NEW_USER,
            userId = "test-user",
            description = "Test user"
        )

        val personaData = persona.generatePersonaData()

        assertEquals("test-user", personaData.userId)
        assertEquals("Test user", personaData.description)
        assertNotNull(personaData.preferenceProfile)
        assertNotNull(personaData.userContext)
        assertNotNull(personaData.expectedBehavior)
    }

    @Test
    fun `should support persona configuration`() {
        val persona = UserPersona(
            type = PersonaType.CASUAL_USER,
            userId = "config-test",
            description = "Config test"
        ).withDeviceType(DeviceType.MOBILE)

        val personaData = persona.generatePersonaData()
        assertEquals(DeviceType.MOBILE, personaData.userContext.deviceType)
    }

    @Test
    fun `should generate different data for different persona types`() {
        val newUser = UserPersona(
            type = PersonaType.NEW_USER,
            userId = "new",
            description = "New user"
        ).generatePersonaData()

        val powerUser = UserPersona(
            type = PersonaType.POWER_USER,
            userId = "power",
            description = "Power user"
        ).generatePersonaData()

        // New users should have low confidence
        assertTrue(newUser.preferenceProfile.confidence <= 0.2)
        
        // Power users should have high confidence
        assertTrue(powerUser.preferenceProfile.confidence >= 0.9)
        
        // Power users should have more engagement history
        assertTrue(powerUser.engagementHistory.size > newUser.engagementHistory.size)
    }

    @Test
    fun `should handle all persona types`() {
        PersonaType.values().forEach { type ->
            val persona = UserPersona(
                type = type,
                userId = "test-${type.name}",
                description = "Test ${type.name}"
            )

            assertDoesNotThrow {
                val personaData = persona.generatePersonaData()
                assertNotNull(personaData.preferenceProfile)
                assertNotNull(personaData.userContext)
                assertNotNull(personaData.expectedBehavior)
            }
        }
    }

    @Test
    fun `should support topic interests configuration`() {
        val interests = mapOf("kotlin" to 0.9, "spring" to 0.8)
        val persona = UserPersona(
            type = PersonaType.TOPIC_FOCUSED_USER,
            userId = "tech-user",
            description = "Tech user"
        ).withTopicInterests(interests)

        val personaData = persona.generatePersonaData()
        
        // Should include configured interests
        assertTrue(personaData.preferenceProfile.topicInterests.containsKey("kotlin"))
        assertTrue(personaData.preferenceProfile.topicInterests.containsKey("spring"))
    }
}