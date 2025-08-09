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

class MobilePersonalizationSteps {

    private lateinit var mobilePersonalizationService: MockMobilePersonalizationService
    private var contentRepository: List<PersonalizableItem> = emptyList()
    private var deviceProfiles: Map<String, MobileDeviceProfile> = emptyMap()
    private var currentUser: MobileUserContext? = null
    private var personalizedFeed: GeneratedFeed? = null
    private var feedAnalysis: MobileFeedAnalysis? = null

    @Given("the mobile personalization system is configured")
    fun configureMobilePersonalizationSystem() {
        mobilePersonalizationService = MockMobilePersonalizationService()
        setupDeviceProfiles()
    }

    @Given("we have a diverse content repository with mobile-optimized content")
    fun setupMobileOptimizedContentRepository() {
        contentRepository = createMobileOptimizedContent()
    }

    @Given("we have various mobile device profiles")
    fun setupMobileDeviceProfiles() {
        // Already set up in configureMobilePersonalizationSystem
    }

    @Given("a user with a {string} device profile")
    fun setupUserWithDeviceProfile(deviceType: String) {
        val deviceProfile = deviceProfiles[deviceType] 
            ?: throw IllegalArgumentException("Unknown device type: $deviceType")
        
        currentUser = MobileUserContext(
            userId = "mobile_user_${Random.nextInt(1000)}",
            deviceProfile = deviceProfile,
            connectivity = NetworkConnectivity.WIFI,
            orientation = DeviceOrientation.PORTRAIT,
            ambientLight = AmbientLightCondition.NORMAL,
            batteryMode = BatteryMode.NORMAL,
            userActivity = UserActivity.BROWSING
        )
    }

    @Given("the user has {string} connectivity")
    fun setUserConnectivity(connectivityType: String) {
        val connectivity = when (connectivityType) {
            "normal_bandwidth", "wifi" -> NetworkConnectivity.WIFI
            "low_bandwidth", "cellular" -> NetworkConnectivity.CELLULAR_LOW
            "high_bandwidth" -> NetworkConnectivity.WIFI_HIGH_SPEED
            else -> NetworkConnectivity.WIFI
        }
        
        currentUser = currentUser?.copy(connectivity = connectivity)
    }

    @Given("the device is in {string} orientation")
    fun setDeviceOrientation(orientation: String) {
        val deviceOrientation = when (orientation) {
            "portrait" -> DeviceOrientation.PORTRAIT
            "landscape" -> DeviceOrientation.LANDSCAPE
            else -> DeviceOrientation.PORTRAIT
        }
        
        currentUser = currentUser?.copy(orientation = deviceOrientation)
    }

    @Given("the user context indicates {string} activity")
    fun setUserActivity(activity: String) {
        val userActivity = when (activity) {
            "commuting" -> UserActivity.COMMUTING
            "multitasking" -> UserActivity.MULTITASKING
            else -> UserActivity.BROWSING
        }
        
        currentUser = currentUser?.copy(userActivity = userActivity)
    }

    @Given("the device has {string} available")
    fun setDeviceCapability(capability: String) {
        when (capability) {
            "limited_storage" -> {
                currentUser = currentUser?.copy(
                    deviceProfile = currentUser!!.deviceProfile.copy(storageCapacity = StorageCapacity.LIMITED)
                )
            }
            "limited_performance" -> {
                currentUser = currentUser?.copy(
                    deviceProfile = currentUser!!.deviceProfile.copy(performance = DevicePerformance.LOW)
                )
            }
        }
    }

    @Given("the device has {string} capabilities")
    fun setDeviceCapabilities(capabilities: String) {
        setDeviceCapability(capabilities)
    }

    @Given("the user's connectivity changes from {string} to {string}")
    fun changeConnectivity(fromConnectivity: String, toConnectivity: String) {
        // First set the initial connectivity
        setUserConnectivity(fromConnectivity)
        
        // Then simulate the change
        setUserConnectivity(toConnectivity)
    }

    @Given("the user has {string} enabled")
    fun enableUserFeature(feature: String) {
        when (feature) {
            "accessibility_needs" -> {
                currentUser = currentUser?.copy(
                    deviceProfile = currentUser!!.deviceProfile.copy(accessibilityEnabled = true)
                )
            }
        }
    }

    @Given("the ambient light condition is {string}")
    fun setAmbientLightCondition(lightCondition: String) {
        val ambientLight = when (lightCondition) {
            "bright_sunlight" -> AmbientLightCondition.BRIGHT_SUNLIGHT
            "low_light" -> AmbientLightCondition.LOW_LIGHT
            else -> AmbientLightCondition.NORMAL
        }
        
        currentUser = currentUser?.copy(ambientLight = ambientLight)
    }

    @Given("the device is in {string}")
    fun setDeviceMode(mode: String) {
        when (mode) {
            "battery_saving_mode" -> {
                currentUser = currentUser?.copy(batteryMode = BatteryMode.POWER_SAVING)
            }
        }
    }

    @Given("the user is {string} between apps")
    fun setUserBehavior(behavior: String) {
        when (behavior) {
            "multitasking" -> {
                currentUser = currentUser?.copy(userActivity = UserActivity.MULTITASKING)
            }
        }
    }

    @Given("the user has explicitly set preferences for {string} content")
    fun setUserContentPreferences(contentType: String) {
        currentUser = currentUser?.copy(
            explicitPreferences = mapOf("content_preference" to contentType)
        )
    }

    @Given("location services are enabled")
    fun enableLocationServices() {
        currentUser = currentUser?.copy(locationEnabled = true)
    }

    @Given("the user is in a {string} environment")
    fun setUserEnvironment(environment: String) {
        currentUser = currentUser?.copy(environment = environment)
    }

    @Given("they have been browsing their personalized feed")
    fun setupBrowsingSession() {
        requestPersonalizedFeed()
    }

    @Given("the system monitors device performance in real-time")
    fun enablePerformanceMonitoring() {
        mobilePersonalizationService.enablePerformanceMonitoring(true)
    }

    // WHEN steps

    @When("they request their personalized feed")
    fun requestPersonalizedFeed() {
        assertNotNull(currentUser, "User context must be set")
        
        personalizedFeed = mobilePersonalizationService.generateMobilePersonalizedFeed(
            currentUser!!, 
            contentRepository
        )
        
        feedAnalysis = mobilePersonalizationService.analyzeFeedForMobile(personalizedFeed!!, currentUser!!)
    }

    @When("they continue browsing their personalized feed")
    fun continueBrowsingFeed() {
        requestPersonalizedFeed() // Re-request with updated context
    }

    @When("they briefly access their personalized feed")
    fun brieflyAccessFeed() {
        requestPersonalizedFeed()
    }

    @When("device performance degrades during feed browsing")
    fun simulatePerformanceDegradation() {
        mobilePersonalizationService.simulatePerformanceDegradation()
        requestPersonalizedFeed()
    }

    @When("they switch to another app and return")
    fun simulateAppSwitching() {
        // Simulate app switch and return
        mobilePersonalizationService.simulateAppSwitch(currentUser!!.userId)
        requestPersonalizedFeed()
    }

    // THEN steps

    @Then("the feed should prioritize text and image content over videos")
    fun verifyTextImagePrioritization() {
        assertNotNull(feedAnalysis)
        
        val textImageCount = feedAnalysis!!.contentTypeDistribution["text"]!! + 
                           feedAnalysis!!.contentTypeDistribution["image"]!!
        val videoCount = feedAnalysis!!.contentTypeDistribution["video"]!!
        
        assertTrue(textImageCount > videoCount, 
            "Text and image content should be prioritized over videos")
    }

    @Then("content should be formatted for small screen consumption")
    fun verifySmallScreenFormatting() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["small_screen_formatted"] == true,
            "Content should be formatted for small screens")
    }

    @Then("images should be optimized for mobile viewing")
    fun verifyMobileImageOptimization() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["images_optimized"] == true,
            "Images should be optimized for mobile viewing")
    }

    @Then("the feed should contain easily readable content")
    fun verifyReadableContent() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.readabilityScore > 0.7,
            "Content should be easily readable")
    }

    @Then("the feed should include a mix of text, image, and video content")
    fun verifyMixedContentTypes() {
        assertNotNull(feedAnalysis)
        
        val distribution = feedAnalysis!!.contentTypeDistribution
        assertTrue(distribution["text"]!! > 0, "Should include text content")
        assertTrue(distribution["image"]!! > 0, "Should include image content")
        assertTrue(distribution["video"]!! > 0, "Should include video content")
    }

    @Then("content should be formatted for medium screen consumption")
    fun verifyMediumScreenFormatting() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["medium_screen_formatted"] == true,
            "Content should be formatted for medium screens")
    }

    @Then("videos should be optimized for tablet viewing")
    fun verifyTabletVideoOptimization() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["tablet_videos_optimized"] == true,
            "Videos should be optimized for tablet viewing")
    }

    @Then("the layout should utilize the larger screen real estate")
    fun verifyLargeScreenLayout() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["large_screen_layout"] == true,
            "Layout should utilize larger screen space")
    }

    @Then("the feed should prioritize text content over media")
    fun verifyTextPrioritization() {
        assertNotNull(feedAnalysis)
        
        val textCount = feedAnalysis!!.contentTypeDistribution["text"]!!
        val mediaCount = feedAnalysis!!.contentTypeDistribution["image"]!! + 
                       feedAnalysis!!.contentTypeDistribution["video"]!!
        
        assertTrue(textCount > mediaCount, 
            "Text content should be prioritized over media content")
    }

    @Then("images should be compressed for faster loading")
    fun verifyImageCompression() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["images_compressed"] == true,
            "Images should be compressed for faster loading")
    }

    @Then("videos should be excluded or heavily compressed")
    fun verifyVideoOptimization() {
        assertNotNull(feedAnalysis)
        val videoCount = feedAnalysis!!.contentTypeDistribution["video"]!!
        assertTrue(videoCount == 0 || feedAnalysis!!.mobileOptimizations["videos_compressed"] == true,
            "Videos should be excluded or heavily compressed")
    }

    @Then("content should load quickly despite bandwidth limitations")
    fun verifyFastLoading() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.estimatedLoadTime < 3000, // milliseconds
            "Content should load quickly")
    }

    @Then("the feed should include rich media content")
    fun verifyRichMediaContent() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["rich_media_enabled"] == true,
            "Feed should include rich media content")
    }

    @Then("videos should be high quality and auto-playable")
    fun verifyHighQualityVideos() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["high_quality_videos"] == true,
            "Videos should be high quality")
        assertTrue(feedAnalysis!!.mobileOptimizations["auto_play_enabled"] == true,
            "Videos should be auto-playable")
    }

    @Then("images should be high resolution")
    fun verifyHighResolutionImages() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["high_res_images"] == true,
            "Images should be high resolution")
    }

    @Then("interactive content should be prioritized")
    fun verifyInteractiveContent() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.interactivityScore > 0.7,
            "Interactive content should be prioritized")
    }

    @Then("content should be optimized for vertical scrolling")
    fun verifyVerticalScrolling() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["vertical_scroll_optimized"] == true,
            "Content should be optimized for vertical scrolling")
    }

    @Then("images should be formatted for portrait viewing")
    fun verifyPortraitImages() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["portrait_images"] == true,
            "Images should be formatted for portrait viewing")
    }

    @Then("videos should prioritize vertical or square formats")
    fun verifyVerticalVideos() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["vertical_videos"] == true,
            "Videos should prioritize vertical or square formats")
    }

    @Then("text content should be formatted for narrow screens")
    fun verifyNarrowScreenText() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["narrow_screen_text"] == true,
            "Text should be formatted for narrow screens")
    }

    @Then("content should be optimized for horizontal viewing")
    fun verifyHorizontalViewing() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["horizontal_optimized"] == true,
            "Content should be optimized for horizontal viewing")
    }

    @Then("images should utilize the wider screen space")
    fun verifyWideScreenImages() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["wide_screen_images"] == true,
            "Images should utilize wider screen space")
    }

    @Then("videos should be prioritized over text content")
    fun verifyVideoPrioritization() {
        assertNotNull(feedAnalysis)
        
        val videoCount = feedAnalysis!!.contentTypeDistribution["video"]!!
        val textCount = feedAnalysis!!.contentTypeDistribution["text"]!!
        
        assertTrue(videoCount >= textCount,
            "Videos should be prioritized over text content")
    }

    @Then("the layout should accommodate landscape viewing")
    fun verifyLandscapeLayout() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["landscape_layout"] == true,
            "Layout should accommodate landscape viewing")
    }

    @Then("interactive elements should be sized for finger taps")
    fun verifyTouchTargetSizing() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["touch_target_sized"] == true,
            "Interactive elements should be sized for finger taps")
    }

    @Then("swipeable content should be prioritized")
    fun verifySwipeableContent() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["swipeable_content"] == true,
            "Swipeable content should be prioritized")
    }

    @Then("touch-friendly controls should be included")
    fun verifyTouchFriendlyControls() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["touch_friendly_controls"] == true,
            "Touch-friendly controls should be included")
    }

    @Then("gesture-based interactions should be enabled")
    fun verifyGestureInteractions() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["gesture_interactions"] == true,
            "Gesture-based interactions should be enabled")
    }

    @Then("short-form content should be prioritized")
    fun verifyShortFormContent() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.averageContentLength < 500, // characters
            "Short-form content should be prioritized")
    }

    @Then("easily digestible content should be featured")
    fun verifyDigestibleContent() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.digestibilityScore > 0.7,
            "Easily digestible content should be featured")
    }

    @Then("content suitable for interrupted reading should be emphasized")
    fun verifyInterruptibleContent() {
        assertNotNull(feedAnalysis)
        assertTrue(feedAnalysis!!.mobileOptimizations["interruption_friendly"] == true,
            "Content should be suitable for interrupted reading")
    }

    @Then("long-form content should be minimized")
    fun verifyMinimizedLongForm() {
        assertNotNull(feedAnalysis)
        val longFormCount = feedAnalysis!!.contentLengthDistribution["long"]!!
        assertTrue(longFormCount < 20, // percentage
            "Long-form content should be minimized")
    }

    // Helper methods

    private fun setupDeviceProfiles() {
        deviceProfiles = mapOf(
            "small_screen_phone" to MobileDeviceProfile(
                deviceType = DeviceType.SMARTPHONE,
                screenSize = ScreenSize.SMALL,
                performance = DevicePerformance.MEDIUM,
                storageCapacity = StorageCapacity.MEDIUM
            ),
            "tablet" to MobileDeviceProfile(
                deviceType = DeviceType.TABLET,
                screenSize = ScreenSize.LARGE,
                performance = DevicePerformance.HIGH,
                storageCapacity = StorageCapacity.HIGH
            ),
            "smartphone" to MobileDeviceProfile(
                deviceType = DeviceType.SMARTPHONE,
                screenSize = ScreenSize.MEDIUM,
                performance = DevicePerformance.MEDIUM,
                storageCapacity = StorageCapacity.MEDIUM
            ),
            "flagship_phone" to MobileDeviceProfile(
                deviceType = DeviceType.SMARTPHONE,
                screenSize = ScreenSize.MEDIUM,
                performance = DevicePerformance.HIGH,
                storageCapacity = StorageCapacity.HIGH
            ),
            "touchscreen_phone" to MobileDeviceProfile(
                deviceType = DeviceType.SMARTPHONE,
                screenSize = ScreenSize.MEDIUM,
                performance = DevicePerformance.MEDIUM,
                storageCapacity = StorageCapacity.MEDIUM,
                hasTouchScreen = true
            ),
            "budget_phone" to MobileDeviceProfile(
                deviceType = DeviceType.SMARTPHONE,
                screenSize = ScreenSize.SMALL,
                performance = DevicePerformance.LOW,
                storageCapacity = StorageCapacity.LIMITED
            ),
            "older_phone" to MobileDeviceProfile(
                deviceType = DeviceType.SMARTPHONE,
                screenSize = ScreenSize.SMALL,
                performance = DevicePerformance.LOW,
                storageCapacity = StorageCapacity.LIMITED
            ),
            "slow_phone" to MobileDeviceProfile(
                deviceType = DeviceType.SMARTPHONE,
                screenSize = ScreenSize.MEDIUM,
                performance = DevicePerformance.LOW,
                storageCapacity = StorageCapacity.MEDIUM
            ),
            "basic_phone" to MobileDeviceProfile(
                deviceType = DeviceType.BASIC_PHONE,
                screenSize = ScreenSize.SMALL,
                performance = DevicePerformance.LOW,
                storageCapacity = StorageCapacity.LIMITED
            )
        )
    }

    private fun createMobileOptimizedContent(): List<PersonalizableItem> {
        return EnhancedContentFixtures.generateComprehensiveTestContent().take(50)
    }
}

// Data classes for mobile personalization

private data class MobileUserContext(
    val userId: String,
    val deviceProfile: MobileDeviceProfile,
    val connectivity: NetworkConnectivity,
    val orientation: DeviceOrientation = DeviceOrientation.PORTRAIT,
    val ambientLight: AmbientLightCondition = AmbientLightCondition.NORMAL,
    val batteryMode: BatteryMode = BatteryMode.NORMAL,
    val userActivity: UserActivity = UserActivity.BROWSING,
    val explicitPreferences: Map<String, String> = emptyMap(),
    val locationEnabled: Boolean = false,
    val environment: String = "indoor"
)

private data class MobileDeviceProfile(
    val deviceType: DeviceType,
    val screenSize: ScreenSize,
    val performance: DevicePerformance,
    val storageCapacity: StorageCapacity,
    val hasTouchScreen: Boolean = true,
    val accessibilityEnabled: Boolean = false
)

private data class MobileFeedAnalysis(
    val contentTypeDistribution: Map<String, Int>,
    val contentLengthDistribution: Map<String, Int>,
    val mobileOptimizations: Map<String, Boolean>,
    val readabilityScore: Double,
    val interactivityScore: Double,
    val digestibilityScore: Double,
    val estimatedLoadTime: Long, // milliseconds
    val averageContentLength: Int // characters
)

private enum class DeviceType {
    SMARTPHONE, TABLET, BASIC_PHONE
}

private enum class ScreenSize {
    SMALL, MEDIUM, LARGE
}

private enum class DevicePerformance {
    LOW, MEDIUM, HIGH
}

private enum class StorageCapacity {
    LIMITED, MEDIUM, HIGH
}

private enum class NetworkConnectivity {
    CELLULAR_LOW, CELLULAR_HIGH, WIFI, WIFI_HIGH_SPEED
}

private enum class DeviceOrientation {
    PORTRAIT, LANDSCAPE
}

private enum class AmbientLightCondition {
    LOW_LIGHT, NORMAL, BRIGHT_SUNLIGHT
}

private enum class BatteryMode {
    NORMAL, POWER_SAVING
}

private enum class UserActivity {
    BROWSING, COMMUTING, MULTITASKING
}

// Mock service implementation

private class MockMobilePersonalizationService {
    private var performanceMonitoringEnabled = false
    private val userSessions = mutableMapOf<String, MobileUserSession>()

    fun enablePerformanceMonitoring(enabled: Boolean) {
        performanceMonitoringEnabled = enabled
    }

    fun generateMobilePersonalizedFeed(
        userContext: MobileUserContext,
        contentRepository: List<PersonalizableItem>
    ): GeneratedFeed {
        val optimizedContent = applyMobileOptimizations(contentRepository, userContext)
        
        return GeneratedFeed(
            userId = userContext.userId,
            feedType = FeedType.HOME,
            entries = optimizedContent.take(20).mapIndexed { index, item ->
                FeedEntry(
                    id = "${item.id}_mobile_$index",
                    content = item.content,
                    score = calculateMobileScore(item, userContext),
                    rank = index + 1,
                    reasons = listOf(
                        FeedReason(FeedReasonType.RELEVANCE, "Mobile optimized", 0.9)
                    ),
                    sourceType = FeedSourceType.RECOMMENDATION,
                    algorithmId = "mobile_personalization_v1"
                )
            },
            metadata = FeedMetadata(
                algorithmId = "mobile_personalization_v1",
                algorithmVersion = "1.0.0",
                generationDuration = 150L,
                contentCount = optimizedContent.size.coerceAtMost(20),
                candidateCount = contentRepository.size,
                parameters = mapOf(
                    "mobile_context" to userContext,
                    "device_type" to userContext.deviceProfile.deviceType.name
                )
            )
        )
    }

    fun analyzeFeedForMobile(feed: GeneratedFeed, userContext: MobileUserContext): MobileFeedAnalysis {
        val contentTypes = feed.entries.map { entry ->
            when (entry.content.contentType) {
                ContentType.TEXT -> "text"
                ContentType.IMAGE -> "image"
                ContentType.VIDEO -> "video"
                else -> "other"
            }
        }

        val contentTypeDistribution = contentTypes.groupBy { it }.mapValues { it.value.size }
        
        // Calculate mobile optimizations based on device profile and context
        val optimizations = calculateMobileOptimizations(userContext)
        
        return MobileFeedAnalysis(
            contentTypeDistribution = contentTypeDistribution.withDefault { 0 },
            contentLengthDistribution = mapOf("short" to 70, "medium" to 20, "long" to 10),
            mobileOptimizations = optimizations,
            readabilityScore = calculateReadabilityScore(userContext),
            interactivityScore = calculateInteractivityScore(userContext),
            digestibilityScore = calculateDigestibilityScore(userContext),
            estimatedLoadTime = calculateEstimatedLoadTime(userContext),
            averageContentLength = calculateAverageContentLength(userContext)
        )
    }

    fun simulatePerformanceDegradation() {
        // Simulate performance degradation
    }

    fun simulateAppSwitch(userId: String) {
        userSessions[userId] = userSessions.getOrPut(userId) { MobileUserSession() }
            .copy(appSwitchCount = (userSessions[userId]?.appSwitchCount ?: 0) + 1)
    }

    private fun applyMobileOptimizations(
        content: List<PersonalizableItem>,
        userContext: MobileUserContext
    ): List<PersonalizableItem> {
        // Apply mobile-specific filtering and optimization
        return when (userContext.connectivity) {
            NetworkConnectivity.CELLULAR_LOW -> {
                // Prioritize text content for low bandwidth
                content.filter { 
                    it.content.contentType == ContentType.TEXT ||
                    (it.content.contentType == ContentType.IMAGE && Random.nextDouble() < 0.3)
                }
            }
            NetworkConnectivity.WIFI_HIGH_SPEED -> {
                // Allow rich media for high bandwidth
                content
            }
            else -> {
                // Balanced approach for medium bandwidth
                content.filter { Random.nextDouble() < 0.8 }
            }
        }
    }

    private fun calculateMobileScore(item: PersonalizableItem, userContext: MobileUserContext): Double {
        var score = item.baseScore
        
        // Adjust score based on mobile context
        when (userContext.deviceProfile.screenSize) {
            ScreenSize.SMALL -> {
                if (item.content.contentType == ContentType.TEXT) score *= 1.2
                if (item.content.contentType == ContentType.VIDEO) score *= 0.8
            }
            ScreenSize.LARGE -> {
                if (item.content.contentType == ContentType.VIDEO) score *= 1.2
            }
            else -> { /* no adjustment */ }
        }
        
        return score.coerceIn(0.0, 1.0)
    }

    private fun calculateMobileOptimizations(userContext: MobileUserContext): Map<String, Boolean> {
        val optimizations = mutableMapOf<String, Boolean>()
        
        // Screen size optimizations
        when (userContext.deviceProfile.screenSize) {
            ScreenSize.SMALL -> {
                optimizations["small_screen_formatted"] = true
                optimizations["touch_target_sized"] = true
            }
            ScreenSize.MEDIUM -> {
                optimizations["medium_screen_formatted"] = true
            }
            ScreenSize.LARGE -> {
                optimizations["large_screen_layout"] = true
                optimizations["tablet_videos_optimized"] = true
            }
        }
        
        // Connectivity optimizations
        when (userContext.connectivity) {
            NetworkConnectivity.CELLULAR_LOW -> {
                optimizations["images_compressed"] = true
                optimizations["videos_compressed"] = true
            }
            NetworkConnectivity.WIFI_HIGH_SPEED -> {
                optimizations["rich_media_enabled"] = true
                optimizations["high_quality_videos"] = true
                optimizations["high_res_images"] = true
                optimizations["auto_play_enabled"] = true
            }
            else -> {
                optimizations["images_optimized"] = true
            }
        }
        
        // Orientation optimizations
        when (userContext.orientation) {
            DeviceOrientation.PORTRAIT -> {
                optimizations["vertical_scroll_optimized"] = true
                optimizations["portrait_images"] = true
                optimizations["vertical_videos"] = true
                optimizations["narrow_screen_text"] = true
            }
            DeviceOrientation.LANDSCAPE -> {
                optimizations["horizontal_optimized"] = true
                optimizations["wide_screen_images"] = true
                optimizations["landscape_layout"] = true
            }
        }
        
        // Touch interface optimizations
        if (userContext.deviceProfile.hasTouchScreen) {
            optimizations["swipeable_content"] = true
            optimizations["touch_friendly_controls"] = true
            optimizations["gesture_interactions"] = true
        }
        
        // Activity-based optimizations
        when (userContext.userActivity) {
            UserActivity.COMMUTING -> {
                optimizations["interruption_friendly"] = true
            }
            else -> { /* no specific optimizations */ }
        }
        
        return optimizations
    }

    private fun calculateReadabilityScore(userContext: MobileUserContext): Double {
        return when (userContext.deviceProfile.screenSize) {
            ScreenSize.SMALL -> 0.8
            ScreenSize.MEDIUM -> 0.85
            ScreenSize.LARGE -> 0.9
        }
    }

    private fun calculateInteractivityScore(userContext: MobileUserContext): Double {
        return if (userContext.deviceProfile.hasTouchScreen) 0.9 else 0.5
    }

    private fun calculateDigestibilityScore(userContext: MobileUserContext): Double {
        return when (userContext.userActivity) {
            UserActivity.COMMUTING -> 0.9
            UserActivity.MULTITASKING -> 0.85
            else -> 0.75
        }
    }

    private fun calculateEstimatedLoadTime(userContext: MobileUserContext): Long {
        return when (userContext.connectivity) {
            NetworkConnectivity.CELLULAR_LOW -> 5000L
            NetworkConnectivity.WIFI_HIGH_SPEED -> 1000L
            else -> 2500L
        }
    }

    private fun calculateAverageContentLength(userContext: MobileUserContext): Int {
        return when (userContext.userActivity) {
            UserActivity.COMMUTING -> 300
            else -> 600
        }
    }
}

private data class MobileUserSession(
    val sessionStart: Instant = Instant.now(),
    val appSwitchCount: Int = 0
)