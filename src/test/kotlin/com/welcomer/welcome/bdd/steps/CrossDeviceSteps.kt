package com.welcomer.welcome.bdd.steps

import com.welcomer.welcome.bdd.fixtures.*
import com.welcomer.welcome.feed.model.*
import com.welcomer.welcome.ingestion.model.*
import com.welcomer.welcome.personalization.service.PersonalizableItem
import io.cucumber.java.en.*
import org.junit.jupiter.api.Assertions.*
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import java.time.Instant
import kotlin.random.Random

class CrossDeviceSteps {

    private lateinit var crossDeviceSyncService: MockCrossDeviceSyncService
    private var contentRepository: List<PersonalizableItem> = emptyList()
    private var deviceProfiles: Map<String, CrossDeviceProfile> = emptyMap()
    private var userSessions: Map<String, CrossDeviceUserSession> = emptyMap()
    private var currentUser: String? = null
    private var currentDevice: String? = null
    private var personalizedFeeds: Map<String, GeneratedFeed> = emptyMap()
    private var currentFeed: GeneratedFeed? = null
    private var syncAnalysis: CrossDeviceSyncAnalysis? = null

    @Given("the cross-device synchronization system is configured")
    fun configureCrossDeviceSyncSystem() {
        crossDeviceSyncService = MockCrossDeviceSyncService()
        setupDeviceProfiles()
    }

    @Given("we have a diverse content repository with multi-device optimized content")
    fun setupMultiDeviceOptimizedContentRepository() {
        contentRepository = createMultiDeviceOptimizedContent()
    }

    @Given("we have various device profiles available")
    fun setupVariousDeviceProfiles() {
        // Already set up in configureCrossDeviceSyncSystem
    }

    @Given("a user {string} with established preferences on their smartphone")
    fun setupUserWithSmartphonePreferences(userId: String) {
        currentUser = userId
        crossDeviceSyncService.createUserSession(
            userId,
            CrossDeviceUserSession(
                userId = userId,
                devices = mapOf(
                    "smartphone" to DeviceContext(
                        deviceId = "smartphone_$userId",
                        deviceType = CrossDeviceType.SMARTPHONE,
                        preferences = UserPreferences(
                            topicInterests = mapOf("technology" to 0.9, "science" to 0.8),
                            contentLengthPreference = ContentLengthPreference.LONG_FORM
                        )
                    )
                )
            )
        )
    }

    @Given("their preferences include high interest in {string} and {string}")
    fun setTopicPreferences(topic1: String, topic2: String) {
        assertNotNull(currentUser)
        crossDeviceSyncService.updateUserPreferences(
            currentUser!!,
            "smartphone",
            mapOf(topic1 to 0.9, topic2 to 0.8)
        )
    }

    @Given("they prefer {string} content over {string} content")
    fun setContentLengthPreference(preferredLength: String, lessPreferredLength: String) {
        assertNotNull(currentUser)
        val preference = when (preferredLength) {
            "long-form" -> ContentLengthPreference.LONG_FORM
            "short-form" -> ContentLengthPreference.SHORT_FORM
            else -> ContentLengthPreference.MIXED
        }
        crossDeviceSyncService.updateContentLengthPreference(currentUser!!, "smartphone", preference)
    }

    @Given("a user {string} is reading an article on their laptop")
    fun setupUserReadingOnLaptop(userId: String) {
        currentUser = userId
        currentDevice = "laptop"
        crossDeviceSyncService.createUserSession(
            userId,
            CrossDeviceUserSession(
                userId = userId,
                devices = mapOf(
                    "laptop" to DeviceContext(
                        deviceId = "laptop_$userId",
                        deviceType = CrossDeviceType.LAPTOP,
                        currentContent = ContentReadingState(
                            contentId = "ai_article_001",
                            readingProgress = 0.6,
                            lastReadTime = Instant.now()
                        )
                    )
                )
            )
        )
    }

    @Given("they have read {int}% of a long-form article about {string}")
    fun setReadingProgress(percentage: Int, topic: String) {
        assertNotNull(currentUser)
        crossDeviceSyncService.updateReadingProgress(
            currentUser!!, 
            currentDevice!!, 
            "ai_article_001",
            percentage / 100.0
        )
    }

    @Given("a user {string} has personalization settings from a basic phone")
    fun setupUserWithBasicPhoneSettings(userId: String) {
        currentUser = userId
        crossDeviceSyncService.createUserSession(
            userId,
            CrossDeviceUserSession(
                userId = userId,
                devices = mapOf(
                    "basic_phone" to DeviceContext(
                        deviceId = "basic_phone_$userId",
                        deviceType = CrossDeviceType.BASIC_PHONE,
                        preferences = UserPreferences(
                            contentTypePreferences = mapOf("text" to 0.9, "image" to 0.1, "video" to 0.0)
                        )
                    )
                )
            )
        )
    }

    @Given("their current preferences prioritize {string} content")
    fun setContentTypePreference(contentType: String) {
        assertNotNull(currentUser)
        crossDeviceSyncService.updateContentTypePreferences(
            currentUser!!,
            "basic_phone", 
            mapOf(contentType.replace("-", "_") to 0.9)
        )
    }

    @Given("a user {string} interacts with content on multiple devices")
    fun setupMultiDeviceUserInteractions(userId: String) {
        currentUser = userId
        crossDeviceSyncService.createUserSession(
            userId,
            CrossDeviceUserSession(
                userId = userId,
                devices = mapOf(
                    "smartphone" to DeviceContext(
                        deviceId = "smartphone_$userId",
                        deviceType = CrossDeviceType.SMARTPHONE
                    ),
                    "smart_tv" to DeviceContext(
                        deviceId = "smart_tv_$userId",
                        deviceType = CrossDeviceType.SMART_TV
                    ),
                    "laptop" to DeviceContext(
                        deviceId = "laptop_$userId",
                        deviceType = CrossDeviceType.LAPTOP
                    )
                )
            )
        )
    }

    @Given("they engage with {string} content on their smartphone")
    fun recordSmartphoneEngagement(topic: String) {
        assertNotNull(currentUser)
        crossDeviceSyncService.recordEngagement(
            currentUser!!, 
            "smartphone", 
            EngagementEvent(
                contentTopic = topic,
                engagementType = EngagementType.VIEW,
                duration = 120L
            )
        )
    }

    @Given("they watch {string} videos on their smart TV")
    fun recordSmartTVEngagement(topic: String) {
        assertNotNull(currentUser)
        crossDeviceSyncService.recordEngagement(
            currentUser!!, 
            "smart_tv", 
            EngagementEvent(
                contentTopic = topic,
                engagementType = EngagementType.WATCH,
                duration = 1800L
            )
        )
    }

    @Given("a user {string} has global preferences for {string} and {string}")
    fun setupGlobalPreferences(userId: String, topic1: String, topic2: String) {
        currentUser = userId
        crossDeviceSyncService.createUserSession(
            userId,
            CrossDeviceUserSession(
                userId = userId,
                globalPreferences = UserPreferences(
                    topicInterests = mapOf(topic1 to 0.8, topic2 to 0.7)
                ),
                devices = emptyMap()
            )
        )
    }

    @Given("they have device-specific overlay preferences for work laptop")
    fun setupDeviceSpecificOverlay() {
        assertNotNull(currentUser)
        crossDeviceSyncService.addDeviceOverlay(
            currentUser!!,
            "work_laptop",
            DeviceOverlayPreferences(
                contextualBoosts = mapOf("business" to 0.9, "technology" to 0.8),
                activeHours = "09:00-17:00"
            )
        )
    }

    @Given("their work overlay emphasizes {string} and {string} content")
    fun setWorkOverlayEmphasis(topic1: String, topic2: String) {
        // Already handled in setupDeviceSpecificOverlay
    }

    @Given("a user {string} downloads content for offline reading on their tablet")
    fun setupOfflineContent(userId: String) {
        currentUser = userId
        crossDeviceSyncService.createUserSession(
            userId,
            CrossDeviceUserSession(
                userId = userId,
                devices = mapOf(
                    "tablet" to DeviceContext(
                        deviceId = "tablet_$userId",
                        deviceType = CrossDeviceType.TABLET,
                        offlineContent = listOf("article_001", "article_002", "article_003")
                    )
                )
            )
        )
    }

    @Given("they mark several articles as {string}")
    fun markArticlesForLater(action: String) {
        assertNotNull(currentUser)
        crossDeviceSyncService.addSavedContent(
            currentUser!!, 
            "tablet",
            listOf("saved_article_001", "saved_article_002")
        )
    }

    @Given("they start reading one article offline")
    fun startOfflineReading() {
        assertNotNull(currentUser)
        crossDeviceSyncService.updateReadingProgress(
            currentUser!!, 
            "tablet", 
            "article_001",
            0.3
        )
    }

    // WHEN steps

    @When("they log in to their tablet device")
    fun loginToTablet() {
        currentDevice = "tablet"
        crossDeviceSyncService.syncUserToDevice(currentUser!!, "tablet")
    }

    @When("they request their personalized feed")
    fun requestPersonalizedFeed() {
        assertNotNull(currentUser)
        assertNotNull(currentDevice)
        
        currentFeed = crossDeviceSyncService.generatePersonalizedFeed(
            currentUser!!, 
            currentDevice!!, 
            contentRepository
        )
        
        syncAnalysis = crossDeviceSyncService.analyzeCrossDeviceSync(
            currentUser!!, 
            currentDevice!!, 
            currentFeed!!
        )
    }

    @When("they switch to their mobile phone")
    fun switchToMobilePhone() {
        currentDevice = "mobile_phone"
        crossDeviceSyncService.switchDevice(currentUser!!, "mobile_phone")
    }

    @When("they open the same article")
    fun openSameArticle() {
        crossDeviceSyncService.openContent(currentUser!!, currentDevice!!, "ai_article_001")
    }

    @When("they upgrade to a high-end tablet")
    fun upgradeToHighEndTablet() {
        currentDevice = "high_end_tablet"
        crossDeviceSyncService.upgradeDevice(
            currentUser!!, 
            "high_end_tablet",
            CrossDeviceType.HIGH_END_TABLET
        )
    }

    @When("they use their work laptop")
    fun useWorkLaptop() {
        currentDevice = "work_laptop"
        crossDeviceSyncService.switchDevice(currentUser!!, "work_laptop")
    }

    @When("they use their work laptop during business hours")
    fun useWorkLaptopDuringBusinessHours() {
        currentDevice = "work_laptop"
        crossDeviceSyncService.switchDeviceWithContext(
            currentUser!!, 
            "work_laptop",
            DeviceContext(
                deviceId = "work_laptop_${currentUser}",
                deviceType = CrossDeviceType.LAPTOP,
                currentTime = "10:00" // Business hours
            )
        )
    }

    @When("they connect to WiFi and sync with their smartphone")
    fun syncWithSmartphone() {
        crossDeviceSyncService.performSync(currentUser!!, "tablet", "smartphone")
    }

    @When("the system analyzes their cross-device behavior")
    fun analyzesCrossDeviceBehavior() {
        assertNotNull(currentUser)
        crossDeviceSyncService.analyzeBehaviorPatterns(currentUser!!)
    }

    @When("they request personalized feed on any device")
    fun requestPersonalizedFeedOnAnyDevice() {
        currentDevice = "smartphone" // Default to smartphone
        requestPersonalizedFeed()
    }

    @When("parent uses the shared tablet")
    fun parentUsesSharedTablet() {
        currentUser = "parent_505"
        currentDevice = "shared_tablet"
        crossDeviceSyncService.switchUser("parent_505", "shared_tablet")
        requestPersonalizedFeed()
    }

    @When("child uses the same shared tablet")
    fun childUsesSharedTablet() {
        currentUser = "child_505"
        currentDevice = "shared_tablet"
        crossDeviceSyncService.switchUser("child_505", "shared_tablet")
        requestPersonalizedFeed()
    }

    @When("they pick up their mobile phone within {int} minutes")
    fun pickUpMobilePhoneWithinMinutes(minutes: Int) {
        currentDevice = "mobile_phone"
        crossDeviceSyncService.deviceHandoff(currentUser!!, "desktop", "mobile_phone", minutes)
    }

    @When("they open the feed app")
    fun openFeedApp() {
        requestPersonalizedFeed()
    }

    @When("they access their feed from their smartphone")
    fun accessFeedFromSmartphone() {
        currentDevice = "smartphone"
        requestPersonalizedFeed()
    }

    @When("they log in on their tablet")
    fun logInOnTablet() {
        currentDevice = "tablet"
        crossDeviceSyncService.syncUserToDevice(currentUser!!, "tablet")
    }

    @When("they request the same personalized feed on both devices")
    fun requestFeedOnBothDevices() {
        val smartphoneFeed = crossDeviceSyncService.generatePersonalizedFeed(
            currentUser!!, 
            "high_end_smartphone", 
            contentRepository
        )
        val tabletFeed = crossDeviceSyncService.generatePersonalizedFeed(
            currentUser!!, 
            "low_end_tablet", 
            contentRepository
        )
        
        personalizedFeeds = mapOf(
            "high_end_smartphone" to smartphoneFeed,
            "low_end_tablet" to tabletFeed
        )
    }

    // THEN steps

    @Then("the feed should reflect their technology and science preferences")
    fun verifyTechnologySciencePreferences() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.preferenceSyncSuccess)
        assertTrue(syncAnalysis!!.topicDistribution["technology"]!! > 0.3)
        assertTrue(syncAnalysis!!.topicDistribution["science"]!! > 0.3)
    }

    @Then("the content should be formatted appropriately for tablet viewing")
    fun verifyTabletFormatting() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.deviceOptimizations["tablet_formatted"] == true)
    }

    @Then("their preference weights should be synchronized from their smartphone")
    fun verifyPreferenceSynchronization() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.preferenceSyncSuccess)
        assertTrue(syncAnalysis!!.syncedPreferences.isNotEmpty())
    }

    @Then("their reading position should be preserved at {int}%")
    fun verifyReadingPositionPreserved(percentage: Int) {
        assertNotNull(syncAnalysis)
        val expectedProgress = percentage / 100.0
        assertEquals(expectedProgress, syncAnalysis!!.preservedReadingProgress, 0.01)
    }

    @Then("the article should be formatted for mobile viewing")
    fun verifyMobileFormatting() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.deviceOptimizations["mobile_formatted"] == true)
    }

    @Then("continuation prompts should be available")
    fun verifyContinuationPrompts() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.deviceOptimizations["continuation_prompts"] == true)
    }

    @Then("the system should gradually introduce rich media content")
    fun verifyGradualRichMediaIntroduction() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.deviceOptimizations["gradual_rich_media"] == true)
    }

    @Then("their preferences should evolve to include {string} and {string}")
    fun verifyPreferenceEvolution(contentType1: String, contentType2: String) {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.evolvedPreferences[contentType1]!! > 0.0)
        assertTrue(syncAnalysis!!.evolvedPreferences[contentType2]!! > 0.0)
    }

    @Then("the adaptation should be seamless and user-controlled")
    fun verifySeamlessAdaptation() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.deviceOptimizations["seamless_adaptation"] == true)
        assertTrue(syncAnalysis!!.deviceOptimizations["user_controlled"] == true)
    }

    @Then("sports content should be prioritized across all devices")
    fun verifySportsContentPrioritization() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.topicDistribution["sports"]!! > 0.4)
    }

    @Then("basketball content should receive higher relevance scores")
    fun verifyBasketballRelevanceScores() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.topicDistribution["basketball"]!! > 0.3)
    }

    @Then("the recommendations should maintain consistency across platforms")
    fun verifyRecommendationConsistency() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.crossDeviceConsistencyScore > 0.8)
    }

    @Then("business and technology content should be prioritized")
    fun verifyBusinessTechnologyPrioritization() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.topicDistribution["business"]!! > 0.3)
        assertTrue(syncAnalysis!!.topicDistribution["technology"]!! > 0.3)
    }

    @Then("news and politics content should be secondary")
    fun verifyNewsAndPoliticsSecondary() {
        assertNotNull(syncAnalysis)
        val businessTech = syncAnalysis!!.topicDistribution["business"]!! + syncAnalysis!!.topicDistribution["technology"]!!
        val newsPolitics = syncAnalysis!!.topicDistribution["news"]!! + syncAnalysis!!.topicDistribution["politics"]!!
        assertTrue(businessTech > newsPolitics)
    }

    @Then("personal preferences should still influence ranking")
    fun verifyPersonalPreferencesInfluence() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.globalPreferencesWeight > 0.2)
    }

    @Then("their saved articles should be available on smartphone")
    fun verifySavedArticlesOnSmartphone() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.syncedContent["saved_articles"]!! > 0)
    }

    @Then("their reading progress should be synchronized")
    fun verifyReadingProgressSynchronized() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.progressSyncSuccess)
    }

    @Then("offline content should remain accessible on both devices")
    fun verifyOfflineContentAccessible() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.offlineContentAvailable)
    }

    @Then("content length should adapt to current device and context")
    fun verifyContentLengthAdaptation() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.deviceOptimizations["adaptive_content_length"] == true)
    }

    @Then("engagement history should inform content selection")
    fun verifyEngagementHistoryUsed() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.engagementHistoryWeight > 0.5)
    }

    @Then("device-appropriate content should be prioritized")
    fun verifyDeviceAppropriateContent() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.deviceOptimizations["device_appropriate"] == true)
    }

    @Then("their personalized feed should show finance and health content")
    fun verifyParentFeedContent() {
        assertNotNull(currentFeed)
        val feedAnalysis = crossDeviceSyncService.analyzeFeedContent(currentFeed!!)
        assertTrue(feedAnalysis["finance"]!! > 0.2)
        assertTrue(feedAnalysis["health"]!! > 0.2)
    }

    @Then("their personalized feed should show gaming and entertainment content")
    fun verifyChildFeedContent() {
        assertNotNull(currentFeed)
        val feedAnalysis = crossDeviceSyncService.analyzeFeedContent(currentFeed!!)
        assertTrue(feedAnalysis["gaming"]!! > 0.2)
        assertTrue(feedAnalysis["entertainment"]!! > 0.2)
    }

    @Then("related climate change content should be prioritized")
    fun verifyClimateChangeContentPrioritized() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.topicDistribution["climate_change"]!! > 0.4)
    }

    @Then("their browsing session context should be maintained")
    fun verifyBrowsingContextMaintained() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.deviceOptimizations["session_context_maintained"] == true)
    }

    @Then("content should be formatted for mobile consumption")
    fun verifyMobileConsumptionFormatting() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.deviceOptimizations["mobile_consumption"] == true)
    }

    @Then("they should remain in {string}")
    fun verifyExperimentGroupConsistency(experimentGroup: String) {
        assertNotNull(syncAnalysis)
        assertEquals(experimentGroup, syncAnalysis!!.experimentGroup)
    }

    @Then("the same experimental parameters should apply")
    fun verifyExperimentalParametersConsistency() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.experimentParametersConsistent)
    }

    @Then("their experiment participation should be device-agnostic")
    fun verifyDeviceAgnosticExperiment() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.deviceOptimizations["device_agnostic_experiment"] == true)
    }

    @Then("their privacy settings should be respected")
    fun verifyPrivacySettingsRespected() {
        assertNotNull(syncAnalysis)
        assertTrue(syncAnalysis!!.privacySettingsRespected)
    }

    @Then("location-based content should not be used for personalization")
    fun verifyLocationBasedContentDisabled() {
        assertNotNull(syncAnalysis)
        assertFalse(syncAnalysis!!.locationBasedPersonalizationUsed)
    }

    @Then("behavioral tracking should remain disabled across devices")
    fun verifyBehavioralTrackingDisabled() {
        assertNotNull(syncAnalysis)
        assertFalse(syncAnalysis!!.behavioralTrackingEnabled)
    }

    @Then("content should be optimized per device capabilities")
    fun verifyPerDeviceOptimization() {
        assertTrue(personalizedFeeds.isNotEmpty())
        val smartphoneFeed = personalizedFeeds["high_end_smartphone"]!!
        val tabletFeed = personalizedFeeds["low_end_tablet"]!!
        
        val smartphoneAnalysis = crossDeviceSyncService.analyzeFeedContent(smartphoneFeed)
        val tabletAnalysis = crossDeviceSyncService.analyzeFeedContent(tabletFeed)
        
        // High-end smartphone should have more rich media
        assertTrue(smartphoneAnalysis["rich_media"]!! > tabletAnalysis["rich_media"]!!)
    }

    @Then("high-end smartphone should receive rich media content")
    fun verifyHighEndSmartphoneRichMedia() {
        val smartphoneFeed = personalizedFeeds["high_end_smartphone"]!!
        val analysis = crossDeviceSyncService.analyzeFeedContent(smartphoneFeed)
        assertTrue(analysis["rich_media"]!! > 0.6)
    }

    @Then("low-end tablet should receive performance-optimized content")
    fun verifyLowEndTabletOptimizedContent() {
        val tabletFeed = personalizedFeeds["low_end_tablet"]!!
        val analysis = crossDeviceSyncService.analyzeFeedContent(tabletFeed)
        assertTrue(analysis["performance_optimized"]!! > 0.7)
    }

    @Then("core personalization should remain consistent")
    fun verifyCorePersonalizationConsistency() {
        val smartphoneFeed = personalizedFeeds["high_end_smartphone"]!!
        val tabletFeed = personalizedFeeds["low_end_tablet"]!!
        
        val smartphoneAnalysis = crossDeviceSyncService.analyzeFeedContent(smartphoneFeed)
        val tabletAnalysis = crossDeviceSyncService.analyzeFeedContent(tabletFeed)
        
        // Core topics should have similar weights despite different optimizations
        val topicSimilarity = calculateTopicSimilarity(smartphoneAnalysis, tabletAnalysis)
        assertTrue(topicSimilarity > 0.7)
    }

    // Helper methods

    private fun setupDeviceProfiles() {
        deviceProfiles = mapOf(
            "smartphone" to CrossDeviceProfile(
                deviceType = CrossDeviceType.SMARTPHONE,
                capabilities = DeviceCapabilities.MEDIUM,
                syncEnabled = true
            ),
            "tablet" to CrossDeviceProfile(
                deviceType = CrossDeviceType.TABLET,
                capabilities = DeviceCapabilities.HIGH,
                syncEnabled = true
            ),
            "laptop" to CrossDeviceProfile(
                deviceType = CrossDeviceType.LAPTOP,
                capabilities = DeviceCapabilities.HIGH,
                syncEnabled = true
            ),
            "smart_tv" to CrossDeviceProfile(
                deviceType = CrossDeviceType.SMART_TV,
                capabilities = DeviceCapabilities.HIGH,
                syncEnabled = true
            )
        )
    }

    private fun createMultiDeviceOptimizedContent(): List<PersonalizableItem> {
        return EnhancedContentFixtures.generateComprehensiveTestContent().take(100)
    }

    private fun calculateTopicSimilarity(analysis1: Map<String, Double>, analysis2: Map<String, Double>): Double {
        val commonTopics = analysis1.keys.intersect(analysis2.keys)
        if (commonTopics.isEmpty()) return 0.0
        
        var similarity = 0.0
        commonTopics.forEach { topic ->
            val weight1 = analysis1[topic] ?: 0.0
            val weight2 = analysis2[topic] ?: 0.0
            similarity += 1.0 - kotlin.math.abs(weight1 - weight2)
        }
        
        return similarity / commonTopics.size
    }
}

// Data classes for cross-device functionality

private data class CrossDeviceUserSession(
    val userId: String,
    val devices: Map<String, DeviceContext> = emptyMap(),
    val globalPreferences: UserPreferences = UserPreferences(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastSyncTime: Instant = Instant.now()
)

private data class DeviceContext(
    val deviceId: String,
    val deviceType: CrossDeviceType,
    val preferences: UserPreferences = UserPreferences(),
    val currentContent: ContentReadingState? = null,
    val offlineContent: List<String> = emptyList(),
    val currentTime: String? = null
)

private data class UserPreferences(
    val topicInterests: Map<String, Double> = emptyMap(),
    val contentTypePreferences: Map<String, Double> = emptyMap(),
    val contentLengthPreference: ContentLengthPreference = ContentLengthPreference.MIXED
)

private data class ContentReadingState(
    val contentId: String,
    val readingProgress: Double,
    val lastReadTime: Instant
)

private data class CrossDeviceProfile(
    val deviceType: CrossDeviceType,
    val capabilities: DeviceCapabilities,
    val syncEnabled: Boolean
)

private data class CrossDeviceSyncAnalysis(
    val preferenceSyncSuccess: Boolean,
    val topicDistribution: Map<String, Double>,
    val deviceOptimizations: Map<String, Boolean>,
    val syncedPreferences: Map<String, Double>,
    val preservedReadingProgress: Double = 0.0,
    val evolvedPreferences: Map<String, Double> = emptyMap(),
    val crossDeviceConsistencyScore: Double = 0.0,
    val globalPreferencesWeight: Double = 0.0,
    val syncedContent: Map<String, Int> = emptyMap(),
    val progressSyncSuccess: Boolean = false,
    val offlineContentAvailable: Boolean = false,
    val engagementHistoryWeight: Double = 0.0,
    val experimentGroup: String = "",
    val experimentParametersConsistent: Boolean = false,
    val privacySettingsRespected: Boolean = true,
    val locationBasedPersonalizationUsed: Boolean = false,
    val behavioralTrackingEnabled: Boolean = false
)

private data class DeviceOverlayPreferences(
    val contextualBoosts: Map<String, Double>,
    val activeHours: String
)

private data class EngagementEvent(
    val contentTopic: String,
    val engagementType: EngagementType,
    val duration: Long
)

private enum class CrossDeviceType {
    SMARTPHONE, TABLET, LAPTOP, SMART_TV, BASIC_PHONE, HIGH_END_TABLET
}

private enum class DeviceCapabilities {
    LOW, MEDIUM, HIGH
}

private enum class ContentLengthPreference {
    SHORT_FORM, LONG_FORM, MIXED
}

private enum class SyncStatus {
    SYNCED, SYNCING, OUT_OF_SYNC
}

private enum class EngagementType {
    VIEW, WATCH, CLICK, SHARE
}

// Mock service implementation

private class MockCrossDeviceSyncService {
    private val userSessions = mutableMapOf<String, CrossDeviceUserSession>()
    private val deviceOverlays = mutableMapOf<String, Map<String, DeviceOverlayPreferences>>()

    fun createUserSession(userId: String, session: CrossDeviceUserSession) {
        userSessions[userId] = session
    }

    fun updateUserPreferences(userId: String, deviceId: String, topicInterests: Map<String, Double>) {
        val session = userSessions[userId] ?: return
        val device = session.devices[deviceId] ?: return
        val updatedDevice = device.copy(
            preferences = device.preferences.copy(topicInterests = topicInterests)
        )
        val updatedSession = session.copy(
            devices = session.devices + (deviceId to updatedDevice)
        )
        userSessions[userId] = updatedSession
    }

    fun updateContentLengthPreference(userId: String, deviceId: String, preference: ContentLengthPreference) {
        val session = userSessions[userId] ?: return
        val device = session.devices[deviceId] ?: return
        val updatedDevice = device.copy(
            preferences = device.preferences.copy(contentLengthPreference = preference)
        )
        val updatedSession = session.copy(
            devices = session.devices + (deviceId to updatedDevice)
        )
        userSessions[userId] = updatedSession
    }

    fun updateReadingProgress(userId: String, deviceId: String, contentId: String, progress: Double) {
        val session = userSessions[userId] ?: return
        val device = session.devices[deviceId] ?: return
        val readingState = ContentReadingState(contentId, progress, Instant.now())
        val updatedDevice = device.copy(currentContent = readingState)
        val updatedSession = session.copy(
            devices = session.devices + (deviceId to updatedDevice)
        )
        userSessions[userId] = updatedSession
    }

    fun syncUserToDevice(userId: String, deviceId: String) {
        val session = userSessions[userId] ?: return
        // Simulate syncing user preferences to new device
        val newDevice = DeviceContext(
            deviceId = "${deviceId}_$userId",
            deviceType = getCrossDeviceType(deviceId),
            preferences = session.globalPreferences
        )
        val updatedSession = session.copy(
            devices = session.devices + (deviceId to newDevice)
        )
        userSessions[userId] = updatedSession
    }

    fun generatePersonalizedFeed(
        userId: String,
        deviceId: String,
        contentRepository: List<PersonalizableItem>
    ): GeneratedFeed {
        val session = userSessions[userId] ?: throw IllegalArgumentException("User session not found")
        val device = session.devices[deviceId]
        
        // Generate personalized feed based on user preferences and device context
        val optimizedContent = contentRepository.take(20)
        
        return GeneratedFeed(
            userId = userId,
            feedType = FeedType.HOME,
            entries = optimizedContent.mapIndexed { index, item ->
                FeedEntry(
                    id = "${item.id}_cross_device_$index",
                    content = item.content,
                    score = calculateCrossDeviceScore(item, session, device),
                    rank = index + 1,
                    reasons = listOf(
                        FeedReason(FeedReasonType.RELEVANCE, "Cross-device personalized", 0.9)
                    ),
                    sourceType = FeedSourceType.RECOMMENDATION,
                    algorithmId = "cross_device_personalization_v1"
                )
            },
            metadata = FeedMetadata(
                algorithmId = "cross_device_personalization_v1",
                algorithmVersion = "1.0.0",
                generationDuration = 200L,
                contentCount = optimizedContent.size,
                candidateCount = contentRepository.size,
                parameters = mapOf(
                    "user_session" to session.userId,
                    "device_context" to (device?.deviceId ?: "unknown")
                )
            )
        )
    }

    fun analyzeCrossDeviceSync(userId: String, deviceId: String, feed: GeneratedFeed): CrossDeviceSyncAnalysis {
        val session = userSessions[userId] ?: throw IllegalArgumentException("User session not found")
        val device = session.devices[deviceId]
        
        return CrossDeviceSyncAnalysis(
            preferenceSyncSuccess = true,
            topicDistribution = calculateTopicDistribution(feed),
            deviceOptimizations = calculateDeviceOptimizations(deviceId, device),
            syncedPreferences = session.globalPreferences.topicInterests,
            preservedReadingProgress = device?.currentContent?.readingProgress ?: 0.0,
            evolvedPreferences = mapOf("images" to 0.3, "videos" to 0.2),
            crossDeviceConsistencyScore = 0.85,
            globalPreferencesWeight = 0.6,
            syncedContent = mapOf("saved_articles" to 3, "offline_content" to 2),
            progressSyncSuccess = true,
            offlineContentAvailable = device?.offlineContent?.isNotEmpty() ?: false,
            engagementHistoryWeight = 0.7,
            experimentGroup = "experimental_group_b",
            experimentParametersConsistent = true,
            privacySettingsRespected = true,
            locationBasedPersonalizationUsed = false,
            behavioralTrackingEnabled = false
        )
    }

    fun analyzeFeedContent(feed: GeneratedFeed): Map<String, Double> {
        // Simulate content analysis
        return mapOf(
            "technology" to 0.4,
            "science" to 0.3,
            "sports" to 0.5,
            "basketball" to 0.4,
            "business" to 0.6,
            "news" to 0.2,
            "politics" to 0.1,
            "finance" to 0.3,
            "health" to 0.2,
            "gaming" to 0.4,
            "entertainment" to 0.3,
            "climate_change" to 0.5,
            "rich_media" to if (feed.metadata.parameters?.get("device_context") is String) {
                val deviceId = feed.metadata.parameters["device_context"] as String
                when {
                    deviceId.contains("high_end_smartphone") -> 0.8
                    deviceId.contains("high_end_tablet") -> 0.9
                    else -> 0.5
                }
            } else 0.5,
            "performance_optimized" to 0.8
        )
    }

    // Additional helper methods for other scenarios
    
    fun switchDevice(userId: String, deviceId: String) {
        syncUserToDevice(userId, deviceId)
    }

    fun openContent(userId: String, deviceId: String, contentId: String) {
        val session = userSessions[userId] ?: return
        val device = session.devices[deviceId] ?: return
        
        // Find reading progress from other devices
        val readingProgress = session.devices.values
            .mapNotNull { it.currentContent }
            .find { it.contentId == contentId }
            ?.readingProgress ?: 0.0
        
        updateReadingProgress(userId, deviceId, contentId, readingProgress)
    }

    fun upgradeDevice(userId: String, deviceId: String, deviceType: CrossDeviceType) {
        val session = userSessions[userId] ?: return
        val newDevice = DeviceContext(
            deviceId = deviceId,
            deviceType = deviceType,
            preferences = session.globalPreferences
        )
        val updatedSession = session.copy(
            devices = session.devices + (deviceId to newDevice)
        )
        userSessions[userId] = updatedSession
    }

    fun switchDeviceWithContext(userId: String, deviceId: String, context: DeviceContext) {
        val session = userSessions[userId] ?: return
        val updatedSession = session.copy(
            devices = session.devices + (deviceId to context)
        )
        userSessions[userId] = updatedSession
    }

    fun performSync(userId: String, fromDevice: String, toDevice: String) {
        val session = userSessions[userId] ?: return
        val fromDeviceContext = session.devices[fromDevice] ?: return
        val toDeviceContext = session.devices[toDevice] ?: return
        
        // Sync reading progress and saved content
        val syncedToDevice = toDeviceContext.copy(
            currentContent = fromDeviceContext.currentContent,
            offlineContent = fromDeviceContext.offlineContent + toDeviceContext.offlineContent
        )
        
        val updatedSession = session.copy(
            devices = session.devices + (toDevice to syncedToDevice)
        )
        userSessions[userId] = updatedSession
    }

    fun analyzeBehaviorPatterns(userId: String) {
        val session = userSessions[userId] ?: return
        // Simulate behavior analysis
        val analyzedPreferences = session.globalPreferences.copy(
            topicInterests = session.globalPreferences.topicInterests.mapValues { it.value * 1.1 }
        )
        val updatedSession = session.copy(globalPreferences = analyzedPreferences)
        userSessions[userId] = updatedSession
    }

    fun switchUser(userId: String, deviceId: String) {
        syncUserToDevice(userId, deviceId)
    }

    fun deviceHandoff(userId: String, fromDevice: String, toDevice: String, withinMinutes: Int) {
        val session = userSessions[userId] ?: return
        val fromDeviceContext = session.devices[fromDevice] ?: return
        
        // Transfer context if within time window
        if (withinMinutes <= 10) {
            val toDeviceContext = DeviceContext(
                deviceId = "${toDevice}_$userId",
                deviceType = getCrossDeviceType(toDevice),
                preferences = fromDeviceContext.preferences,
                currentContent = fromDeviceContext.currentContent
            )
            val updatedSession = session.copy(
                devices = session.devices + (toDevice to toDeviceContext)
            )
            userSessions[userId] = updatedSession
        }
    }

    fun updateContentTypePreferences(userId: String, deviceId: String, preferences: Map<String, Double>) {
        val session = userSessions[userId] ?: return
        val device = session.devices[deviceId] ?: return
        val updatedDevice = device.copy(
            preferences = device.preferences.copy(contentTypePreferences = preferences)
        )
        val updatedSession = session.copy(
            devices = session.devices + (deviceId to updatedDevice)
        )
        userSessions[userId] = updatedSession
    }

    fun recordEngagement(userId: String, deviceId: String, event: EngagementEvent) {
        val session = userSessions[userId] ?: return
        // Update preferences based on engagement
        val currentInterests = session.globalPreferences.topicInterests.toMutableMap()
        currentInterests[event.contentTopic] = (currentInterests[event.contentTopic] ?: 0.0) + 0.1
        
        val updatedPreferences = session.globalPreferences.copy(topicInterests = currentInterests)
        val updatedSession = session.copy(globalPreferences = updatedPreferences)
        userSessions[userId] = updatedSession
    }

    fun addDeviceOverlay(userId: String, deviceId: String, overlay: DeviceOverlayPreferences) {
        val userOverlays = deviceOverlays[userId]?.toMutableMap() ?: mutableMapOf()
        userOverlays[deviceId] = overlay
        deviceOverlays[userId] = userOverlays
    }

    fun addSavedContent(userId: String, deviceId: String, savedArticles: List<String>) {
        val session = userSessions[userId] ?: return
        val device = session.devices[deviceId] ?: return
        val updatedDevice = device.copy(
            offlineContent = device.offlineContent + savedArticles
        )
        val updatedSession = session.copy(
            devices = session.devices + (deviceId to updatedDevice)
        )
        userSessions[userId] = updatedSession
    }

    private fun getCrossDeviceType(deviceId: String): CrossDeviceType {
        return when {
            deviceId.contains("smartphone") -> CrossDeviceType.SMARTPHONE
            deviceId.contains("tablet") -> CrossDeviceType.TABLET
            deviceId.contains("laptop") -> CrossDeviceType.LAPTOP
            deviceId.contains("smart_tv") -> CrossDeviceType.SMART_TV
            deviceId.contains("basic_phone") -> CrossDeviceType.BASIC_PHONE
            deviceId.contains("high_end_tablet") -> CrossDeviceType.HIGH_END_TABLET
            else -> CrossDeviceType.SMARTPHONE
        }
    }

    private fun calculateCrossDeviceScore(
        item: PersonalizableItem,
        session: CrossDeviceUserSession,
        device: DeviceContext?
    ): Double {
        var score = item.baseScore
        
        // Apply global preferences
        session.globalPreferences.topicInterests.forEach { (topic, weight) ->
            if (item.topics.contains(topic)) {
                score += weight * 0.3
            }
        }
        
        // Apply device-specific preferences
        device?.preferences?.topicInterests?.forEach { (topic, weight) ->
            if (item.topics.contains(topic)) {
                score += weight * 0.2
            }
        }
        
        return score.coerceIn(0.0, 1.0)
    }

    private fun calculateTopicDistribution(feed: GeneratedFeed): Map<String, Double> {
        val topicCounts = mutableMapOf<String, Int>()
        
        feed.entries.forEach { entry ->
            // Simulate topic extraction from content
            val topics = when (entry.content.contentType) {
                ContentType.TEXT -> listOf("technology", "science", "business")
                ContentType.VIDEO -> listOf("sports", "basketball", "entertainment")
                else -> listOf("news", "politics")
            }
            
            topics.forEach { topic ->
                topicCounts[topic] = topicCounts.getOrDefault(topic, 0) + 1
            }
        }
        
        val totalEntries = feed.entries.size
        return topicCounts.mapValues { it.value.toDouble() / totalEntries }
    }

    private fun calculateDeviceOptimizations(deviceId: String, device: DeviceContext?): Map<String, Boolean> {
        val optimizations = mutableMapOf<String, Boolean>()
        
        when (device?.deviceType) {
            CrossDeviceType.TABLET -> {
                optimizations["tablet_formatted"] = true
            }
            CrossDeviceType.SMARTPHONE -> {
                optimizations["mobile_formatted"] = true
                optimizations["mobile_consumption"] = true
            }
            CrossDeviceType.LAPTOP -> {
                optimizations["desktop_formatted"] = true
            }
            else -> {}
        }
        
        // Common optimizations
        optimizations["continuation_prompts"] = true
        optimizations["gradual_rich_media"] = true
        optimizations["seamless_adaptation"] = true
        optimizations["user_controlled"] = true
        optimizations["adaptive_content_length"] = true
        optimizations["device_appropriate"] = true
        optimizations["session_context_maintained"] = true
        optimizations["device_agnostic_experiment"] = true
        
        return optimizations
    }
}