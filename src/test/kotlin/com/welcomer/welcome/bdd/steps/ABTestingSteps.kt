package com.welcomer.welcome.bdd.steps

import com.welcomer.welcome.bdd.fixtures.*
import com.welcomer.welcome.feed.model.*
import com.welcomer.welcome.feed.service.*
import com.welcomer.welcome.ingestion.model.*
import com.welcomer.welcome.user.model.*
import com.welcomer.welcome.personalization.service.PersonalizableItem
import io.cucumber.java.en.*
import org.junit.jupiter.api.Assertions.*
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import java.time.Instant
import kotlin.random.Random

class ABTestingSteps {

    private lateinit var abTestingService: MockABTestingService
    private var contentRepository: List<PersonalizableItem> = emptyList()
    private var userExperimentAssignments: MutableMap<String, ExperimentConfig> = mutableMapOf()
    private var feedResults: MutableMap<String, GeneratedFeed> = mutableMapOf()
    private var experimentMetrics: MutableList<ExperimentMetricRecord> = mutableListOf()
    private var currentExperiments: List<MockExperiment> = emptyList()
    private var multipleSessionResults: MutableMap<String, List<GeneratedFeed>> = mutableMapOf()
    private var userGroupAssignments: MutableMap<String, String> = mutableMapOf()

    @Given("the A/B testing framework is configured")
    fun configureABTestingFramework() {
        abTestingService = MockABTestingService()
        experimentMetrics.clear()
        userExperimentAssignments.clear()
        feedResults.clear()
        userGroupAssignments.clear()
    }

    @Given("we have a content repository with diverse content")
    fun setupContentRepository() {
        contentRepository = EnhancedContentFixtures.generateComprehensiveTestContent().take(100)
    }

    @Given("we have active experiments for feed personalization")
    fun setupActiveExperiments() {
        currentExperiments = listOf(
            MockExperiment(
                id = "feed_algorithm_test_2024",
                name = "Feed Algorithm Weight Test",
                feedTypes = listOf(FeedType.HOME),
                targetPercentage = 10.0,
                variants = listOf(
                    ExperimentVariant(
                        id = "control",
                        experimentId = "feed_algorithm_test_2024",
                        variantName = "control",
                        allocationPercentage = 50.0,
                        parameters = emptyMap(),
                        isControl = true
                    ),
                    ExperimentVariant(
                        id = "high_recency",
                        experimentId = "feed_algorithm_test_2024", 
                        variantName = "high_recency",
                        allocationPercentage = 50.0,
                        parameters = mapOf(
                            "recency_weight" to 0.7,
                            "popularity_weight" to 0.2,
                            "relevance_weight" to 0.1
                        )
                    )
                )
            )
        )
        abTestingService.setActiveExperiments(currentExperiments)
    }

    @Given("a user {string}")
    fun setupUser(userId: String) {
        // User is implicitly created when referenced
    }

    @Given("a user {string} is assigned to the control group")
    fun assignUserToControlGroup(userId: String) {
        val controlVariant = ExperimentVariant(
            id = "control",
            experimentId = "feed_algorithm_test_2024",
            variantName = "control",
            allocationPercentage = 50.0,
            parameters = emptyMap(),
            isControl = true
        )
        
        abTestingService.forceUserAssignment(userId, "feed_algorithm_test_2024", controlVariant)
        userGroupAssignments[userId] = "control"
    }

    @Given("a user {string} is assigned to the {string} variant")
    fun assignUserToTestVariant(userId: String, variantName: String) {
        val testVariant = currentExperiments.first().variants.find { it.variantName == variantName }
        assertNotNull(testVariant, "Variant $variantName should exist")
        
        abTestingService.forceUserAssignment(userId, "feed_algorithm_test_2024", testVariant!!)
        userGroupAssignments[userId] = variantName
    }

    @Given("an experiment with {int}% control and {int}% test allocation")
    fun setupExperimentAllocation(controlPercent: Int, testPercent: Int) {
        val experiment = MockExperiment(
            id = "allocation_test",
            name = "Allocation Test",
            feedTypes = listOf(FeedType.HOME),
            targetPercentage = 100.0, // Target all users for allocation test
            variants = listOf(
                ExperimentVariant(
                    id = "control",
                    experimentId = "allocation_test",
                    variantName = "control",
                    allocationPercentage = controlPercent.toDouble(),
                    parameters = emptyMap(),
                    isControl = true
                ),
                ExperimentVariant(
                    id = "test",
                    experimentId = "allocation_test",
                    variantName = "test", 
                    allocationPercentage = testPercent.toDouble(),
                    parameters = mapOf("recency_weight" to 0.6)
                )
            )
        )
        
        abTestingService.setActiveExperiments(listOf(experiment))
    }

    @Given("an experiment targeting only {int}% of users")
    fun setupLimitedTargetingExperiment(targetPercent: Int) {
        val experiment = MockExperiment(
            id = "limited_experiment", 
            name = "Limited Targeting Experiment",
            feedTypes = listOf(FeedType.HOME),
            targetPercentage = targetPercent.toDouble(),
            variants = listOf(
                ExperimentVariant(
                    id = "control",
                    experimentId = "limited_experiment",
                    variantName = "control",
                    allocationPercentage = 50.0,
                    parameters = emptyMap(),
                    isControl = true
                ),
                ExperimentVariant(
                    id = "test",
                    experimentId = "limited_experiment",
                    variantName = "test",
                    allocationPercentage = 50.0,
                    parameters = mapOf("recency_weight" to 0.6)
                )
            )
        )
        
        abTestingService.setActiveExperiments(listOf(experiment))
    }

    @Given("a user {string} is not selected for the experiment")
    fun forceUserExclusion(userId: String) {
        abTestingService.forceUserExclusion(userId)
    }

    @Given("a user {string} is in the {string} test group")
    fun assignUserToSpecificTestGroup(userId: String, variantName: String) {
        assignUserToTestVariant(userId, variantName)
    }

    @Given("multiple active experiments for different feed aspects")
    fun setupMultipleExperiments() {
        val experiments = listOf(
            MockExperiment(
                id = "algorithm_weights",
                name = "Algorithm Weights Test",
                feedTypes = listOf(FeedType.HOME),
                targetPercentage = 20.0,
                variants = listOf(
                    ExperimentVariant("control_algo", "algorithm_weights", "control", 50.0, emptyMap(), true),
                    ExperimentVariant("test_algo", "algorithm_weights", "high_recency", 50.0, mapOf("recency_weight" to 0.7))
                )
            ),
            MockExperiment(
                id = "diversity_settings", 
                name = "Diversity Settings Test",
                feedTypes = listOf(FeedType.HOME),
                targetPercentage = 15.0,
                variants = listOf(
                    ExperimentVariant("control_div", "diversity_settings", "control", 50.0, emptyMap(), true),
                    ExperimentVariant("test_div", "diversity_settings", "high_diversity", 50.0, mapOf("max_same_author" to 2))
                )
            )
        )
        
        abTestingService.setActiveExperiments(experiments)
    }

    @Given("user {string} is eligible for multiple experiments")
    fun makeUserEligibleForMultipleExperiments(userId: String) {
        abTestingService.makeUserEligibleForAll(userId)
    }

    @Given("an experiment variant with custom scoring weights")
    fun setupCustomScoringWeights() {
        // Already set up in previous steps
    }

    @Given("the variant specifies recency_weight: {double}, popularity_weight: {double}, relevance_weight: {double}")
    fun verifyVariantParameters(recencyWeight: Double, popularityWeight: Double, relevanceWeight: Double) {
        val variant = currentExperiments.first().variants.find { !it.isControl }
        assertNotNull(variant)
        assertEquals(recencyWeight, variant!!.parameters["recency_weight"])
        assertEquals(popularityWeight, variant.parameters["popularity_weight"]) 
        assertEquals(relevanceWeight, variant.parameters["relevance_weight"])
    }

    @Given("experiments configured for different feed types")
    fun setupFeedTypeSpecificExperiments() {
        val experiments = listOf(
            MockExperiment(
                id = "home_feed_exp",
                name = "Home Feed Experiment", 
                feedTypes = listOf(FeedType.HOME),
                targetPercentage = 10.0,
                variants = listOf(
                    ExperimentVariant("home_control", "home_feed_exp", "control", 50.0, emptyMap(), true),
                    ExperimentVariant("home_test", "home_feed_exp", "test", 50.0, mapOf("recency_weight" to 0.6))
                )
            ),
            MockExperiment(
                id = "explore_feed_exp",
                name = "Explore Feed Experiment",
                feedTypes = listOf(FeedType.EXPLORE),
                targetPercentage = 15.0,
                variants = listOf(
                    ExperimentVariant("explore_control", "explore_feed_exp", "control", 50.0, emptyMap(), true),
                    ExperimentVariant("explore_test", "explore_feed_exp", "test", 50.0, mapOf("diversity_weight" to 0.8))
                )
            )
        )
        
        abTestingService.setActiveExperiments(experiments)
    }

    @Given("user {string} requests different types of feeds")
    fun prepareUserForMultipleFeedTypes(userId: String) {
        // User setup complete
    }

    @Given("an experiment variant with invalid parameters")
    fun setupInvalidParameters() {
        val experiment = MockExperiment(
            id = "invalid_params_exp",
            name = "Invalid Parameters Test",
            feedTypes = listOf(FeedType.HOME),
            targetPercentage = 10.0,
            variants = listOf(
                ExperimentVariant(
                    id = "invalid_variant",
                    experimentId = "invalid_params_exp",
                    variantName = "invalid_test",
                    allocationPercentage = 100.0,
                    parameters = mapOf(
                        "recency_weight" to "invalid_string", // Invalid type
                        "unknown_param" to 0.5,              // Unknown parameter
                        "negative_weight" to -0.1             // Invalid value
                    )
                )
            )
        )
        
        abTestingService.setActiveExperiments(listOf(experiment))
    }

    @Given("a user is actively participating in an experiment")
    fun setupActiveExperimentParticipation() {
        setupActiveExperiments()
        assignUserToTestVariant("active_user", "high_recency")
    }

    @Given("an experiment initially targeting {int}% of users")
    fun setupInitialExperimentTargeting(initialPercent: Int) {
        val experiment = MockExperiment(
            id = "rollout_exp",
            name = "Gradual Rollout Test",
            feedTypes = listOf(FeedType.HOME),
            targetPercentage = initialPercent.toDouble(),
            variants = listOf(
                ExperimentVariant("rollout_control", "rollout_exp", "control", 50.0, emptyMap(), true),
                ExperimentVariant("rollout_test", "rollout_exp", "test", 50.0, mapOf("recency_weight" to 0.6))
            )
        )
        
        abTestingService.setActiveExperiments(listOf(experiment))
    }

    @Given("users in both control and test groups")
    fun setupUsersInBothGroups() {
        assignUserToControlGroup("control_user_1")
        assignUserToControlGroup("control_user_2") 
        assignUserToTestVariant("test_user_1", "high_recency")
        assignUserToTestVariant("test_user_2", "high_recency")
    }

    @Given("multiple users participating in various experiments")
    fun setupMultipleUsersInExperiments() {
        setupMultipleExperiments()
        abTestingService.forceUserAssignment("user_1", "algorithm_weights", currentExperiments[0].variants[0])
        abTestingService.forceUserAssignment("user_2", "algorithm_weights", currentExperiments[0].variants[1])  
        abTestingService.forceUserAssignment("user_3", "diversity_settings", currentExperiments[1].variants[0])
        abTestingService.forceUserAssignment("user_4", "diversity_settings", currentExperiments[1].variants[1])
    }

    @Given("an active experiment is causing performance issues")
    fun setupProblematicExperiment() {
        val experiment = MockExperiment(
            id = "performance_issue_exp",
            name = "Performance Issue Experiment", 
            feedTypes = listOf(FeedType.HOME),
            targetPercentage = 50.0,
            variants = listOf(
                ExperimentVariant("perf_control", "performance_issue_exp", "control", 50.0, emptyMap(), true),
                ExperimentVariant("perf_test", "performance_issue_exp", "slow_variant", 50.0, 
                    mapOf("slow_processing" to true))
            )
        )
        
        abTestingService.setActiveExperiments(listOf(experiment))
        assignUserToTestVariant("perf_user", "slow_variant")
    }

    // WHEN steps

    @When("they request their personalized feed multiple times")
    fun requestFeedMultipleTimes() {
        val userId = "user123"
        val feeds = mutableListOf<GeneratedFeed>()
        
        repeat(5) {
            val feed = generatePersonalizedFeed(userId, FeedType.HOME)
            feeds.add(feed)
        }
        
        multipleSessionResults[userId] = feeds
    }

    @When("they request their personalized feed")
    fun requestPersonalizedFeed() {
        val userId = determineCurrentUser()
        val feed = generatePersonalizedFeed(userId, FeedType.HOME)
        feedResults[userId] = feed
    }

    @When("{int} users request their personalized feeds") 
    fun multipleUsersRequestFeeds(userCount: Int) {
        repeat(userCount) { i ->
            val userId = "batch_user_$i"
            val feed = generatePersonalizedFeed(userId, FeedType.HOME)
            feedResults[userId] = feed
        }
    }

    @When("they interact with their personalized feed")
    fun interactWithFeed() {
        val userId = determineCurrentUser()
        // Simulate interactions
        abTestingService.trackEvent(userId, "feed_algorithm_test_2024", ExperimentEventType.FEED_GENERATED)
        abTestingService.trackEvent(userId, "feed_algorithm_test_2024", ExperimentEventType.CONTENT_VIEWED)
    }

    @When("they view content items")
    fun viewContentItems() {
        val userId = determineCurrentUser()
        repeat(3) {
            abTestingService.trackEvent(userId, "feed_algorithm_test_2024", ExperimentEventType.CONTENT_VIEWED)
        }
    }

    @When("they engage with content \\(likes, shares, comments)")
    fun engageWithContent() {
        val userId = determineCurrentUser()
        repeat(2) {
            abTestingService.trackEvent(userId, "feed_algorithm_test_2024", ExperimentEventType.CONTENT_ENGAGED)
        }
    }

    @When("a user in this variant requests their feed")
    fun userInVariantRequestsFeed() {
        val userId = "variant_user"
        assignUserToTestVariant(userId, "high_recency")
        val feed = generatePersonalizedFeed(userId, FeedType.HOME)
        feedResults[userId] = feed
    }

    @When("they start multiple sessions throughout the day")
    fun startMultipleSessions() {
        val userId = "session_user"
        val sessions = mutableListOf<GeneratedFeed>()
        
        repeat(4) { sessionIndex ->
            abTestingService.trackEvent(userId, "feed_algorithm_test_2024", ExperimentEventType.SESSION_STARTED)
            val feed = generatePersonalizedFeed(userId, FeedType.HOME)
            sessions.add(feed)
            abTestingService.trackEvent(userId, "feed_algorithm_test_2024", ExperimentEventType.SESSION_ENDED)
        }
        
        multipleSessionResults[userId] = sessions
    }

    @When("they request a HOME feed")
    fun requestHomeFeed() {
        val userId = "feed_type_user"
        val feed = generatePersonalizedFeed(userId, FeedType.HOME)
        feedResults["${userId}_HOME"] = feed
    }

    @When("they request an EXPLORE feed")
    fun requestExploreFeed() {
        val userId = "feed_type_user"
        val feed = generatePersonalizedFeed(userId, FeedType.EXPLORE) 
        feedResults["${userId}_EXPLORE"] = feed
    }

    @When("the personalization system processes the experiment")
    fun processExperimentWithInvalidParams() {
        val userId = "invalid_params_user"
        abTestingService.forceUserAssignment(userId, "invalid_params_exp", 
            currentExperiments.first().variants.first())
        val feed = generatePersonalizedFeed(userId, FeedType.HOME)
        feedResults[userId] = feed
    }

    @When("they perform various actions on personalized content")
    fun performVariousActions() {
        val userId = "active_user"
        val actions = listOf(
            ExperimentEventType.FEED_GENERATED,
            ExperimentEventType.CONTENT_VIEWED,
            ExperimentEventType.CONTENT_ENGAGED,
            ExperimentEventType.FEED_REFRESHED
        )
        
        actions.forEach { action ->
            abTestingService.trackEvent(userId, "feed_algorithm_test_2024", action)
        }
    }

    @When("the experiment target percentage is increased to {int}%")
    fun increaseExperimentTargeting(newPercent: Int) {
        val updatedExperiment = MockExperiment(
            currentExperiments.first().id,
            currentExperiments.first().name,
            currentExperiments.first().feedTypes,
            newPercent.toDouble(),
            currentExperiments.first().variants
        )
        abTestingService.updateExperiment(updatedExperiment)
    }

    @When("they receive personalized content over time")
    fun receiveContentOverTime() {
        listOf("control_user_1", "control_user_2", "test_user_1", "test_user_2").forEach { userId ->
            repeat(5) { 
                val feed = generatePersonalizedFeed(userId, FeedType.HOME)
                feedResults["${userId}_session_$it"] = feed
            }
        }
    }

    @When("experiment data is collected over time")
    fun collectExperimentDataOverTime() {
        // Data collection happens automatically through tracking
    }

    @When("an emergency override is triggered")
    fun triggerEmergencyOverride() {
        abTestingService.enableEmergencyOverride()
    }

    // THEN steps

    @Then("they should consistently receive the same experiment variant")
    fun verifyConsistentVariantAssignment() {
        val userId = "user123"
        val feeds = multipleSessionResults[userId]!!
        
        val experimentConfigs = feeds.mapNotNull { it.metadata.parameters["experiment_config"] as? ExperimentConfig }
        assertTrue(experimentConfigs.isNotEmpty(), "Should have experiment configs")
        
        val firstVariant = experimentConfigs.first().variantId
        assertTrue(experimentConfigs.all { it.variantId == firstVariant }, 
            "All requests should return same variant: $firstVariant")
    }

    @Then("their experience should remain consistent across sessions")
    fun verifyConsistentExperience() {
        val userId = "user123"
        val feeds = multipleSessionResults[userId]!!
        
        // Verify consistent algorithm parameters
        val algorithms = feeds.map { it.metadata.algorithmId }.toSet()
        assertTrue(algorithms.size <= 1, "Should use consistent algorithm across sessions")
    }

    @Then("the feed should be generated using default algorithm parameters")
    fun verifyDefaultAlgorithmParameters() {
        val userId = "control_user"
        val feed = feedResults[userId]!!
        
        val experimentConfig = feed.metadata.parameters["experiment_config"] as? ExperimentConfig
        assertNotNull(experimentConfig, "Should have experiment config")
        assertTrue(experimentConfig!!.isControl, "Should be in control group")
    }

    @Then("the experiment metadata should indicate control group assignment")
    fun verifyControlGroupMetadata() {
        val userId = "control_user"
        val feed = feedResults[userId]!!
        
        val experimentConfig = feed.metadata.parameters["experiment_config"] as? ExperimentConfig
        assertNotNull(experimentConfig)
        assertTrue(experimentConfig!!.isControl)
        assertEquals("control", experimentConfig.variantId)
    }

    @Then("the feed generation metrics should be tracked for the control group")
    fun verifyControlGroupMetricsTracking() {
        val userId = "control_user"
        val events = abTestingService.getTrackedEvents(userId)
        
        assertTrue(events.any { it.eventType == ExperimentEventType.FEED_GENERATED })
        assertTrue(events.all { it.experimentId == "feed_algorithm_test_2024" })
    }

    @Then("the feed should be generated with modified recency weights")
    fun verifyModifiedRecencyWeights() {
        val userId = "test_user"
        val feed = feedResults[userId]!!
        
        val experimentConfig = feed.metadata.parameters["experiment_config"] as? ExperimentConfig
        assertNotNull(experimentConfig)
        assertEquals(0.7, experimentConfig!!.parameters["recency_weight"])
    }

    @Then("the content should prioritize more recent posts")
    fun verifyRecentContentPrioritization() {
        val userId = "test_user"
        val feed = feedResults[userId]!!
        
        // Verify that content is ordered by recency more heavily
        val timestamps = feed.entries.map { it.content.publishedAt ?: Instant.EPOCH }
        val isRecentFirst = timestamps.zipWithNext().all { (a, b) -> a >= b }
        assertTrue(isRecentFirst || timestamps.size <= 1, "Content should be ordered by recency")
    }

    @Then("the experiment metadata should indicate test group assignment")
    fun verifyTestGroupMetadata() {
        val userId = "test_user"
        val feed = feedResults[userId]!!
        
        val experimentConfig = feed.metadata.parameters["experiment_config"] as? ExperimentConfig
        assertNotNull(experimentConfig)
        assertFalse(experimentConfig!!.isControl)
        assertEquals("high_recency", experimentConfig.variantId)
    }

    @Then("the feed generation metrics should be tracked for the test group")
    fun verifyTestGroupMetricsTracking() {
        val userId = "test_user"  
        val events = abTestingService.getTrackedEvents(userId)
        
        assertTrue(events.any { it.eventType == ExperimentEventType.FEED_GENERATED })
        assertTrue(events.all { it.experimentId == "feed_algorithm_test_2024" })
    }

    @Then("approximately {int}% should be assigned to control group")
    fun verifyControlGroupAssignment(expectedPercent: Int) {
        val controlCount = feedResults.values.count { feed ->
            val experimentConfig = feed.metadata.parameters["experiment_config"] as? ExperimentConfig
            experimentConfig?.isControl == true
        }
        
        val totalCount = feedResults.size
        val actualPercent = (controlCount * 100.0 / totalCount)
        
        // Allow for 10% variance in assignment 
        assertTrue(actualPercent >= expectedPercent - 10 && actualPercent <= expectedPercent + 10,
            "Control group should be ~$expectedPercent%, was ${actualPercent.toInt()}%")
    }

    @Then("approximately {int}% should be assigned to test group") 
    fun verifyTestGroupAssignment(expectedPercent: Int) {
        val testCount = feedResults.values.count { feed ->
            val experimentConfig = feed.metadata.parameters["experiment_config"] as? ExperimentConfig
            experimentConfig?.isControl == false
        }
        
        val totalCount = feedResults.size
        val actualPercent = (testCount * 100.0 / totalCount)
        
        // Allow for 10% variance in assignment
        assertTrue(actualPercent >= expectedPercent - 10 && actualPercent <= expectedPercent + 10,
            "Test group should be ~$expectedPercent%, was ${actualPercent.toInt()}%")
    }

    @Then("the assignment should be based on consistent hashing")
    fun verifyConsistentHashing() {
        // Request same users again and verify same assignments
        val originalAssignments = feedResults.keys.associateWith { userId ->
            val feed = feedResults[userId]!!
            (feed.metadata.parameters["experiment_config"] as? ExperimentConfig)?.variantId
        }
        
        // Clear and regenerate
        feedResults.clear()
        originalAssignments.keys.forEach { userId ->
            val feed = generatePersonalizedFeed(userId, FeedType.HOME)
            feedResults[userId] = feed
        }
        
        // Verify assignments remain the same
        originalAssignments.forEach { (userId, originalVariant) ->
            val newFeed = feedResults[userId]!!
            val newVariant = (newFeed.metadata.parameters["experiment_config"] as? ExperimentConfig)?.variantId
            assertEquals(originalVariant, newVariant, "User $userId should maintain same assignment")
        }
    }

    @Then("they should receive the default personalization experience")
    fun verifyDefaultPersonalizationExperience() {
        val userId = "excluded_user"
        val feed = feedResults[userId]!!
        
        val experimentConfig = feed.metadata.parameters["experiment_config"] as? ExperimentConfig
        assertNull(experimentConfig, "Excluded user should not have experiment config")
    }

    @Then("no experiment metrics should be collected for them")
    fun verifyNoExperimentMetrics() {
        val userId = "excluded_user"
        val events = abTestingService.getTrackedEvents(userId)
        assertTrue(events.isEmpty() || events.all { it.eventType == ExperimentEventType.EXPERIMENT_EXCLUDED })
    }

    @Then("an exclusion event should be tracked")
    fun verifyExclusionEventTracking() {
        val userId = "excluded_user"
        val events = abTestingService.getTrackedEvents(userId)
        assertTrue(events.any { it.eventType == ExperimentEventType.EXPERIMENT_EXCLUDED })
    }

    @Then("all experiment events should be tracked")
    fun verifyAllExperimentEventsTracked() {
        val userId = "metrics_user"
        val events = abTestingService.getTrackedEvents(userId)
        
        val expectedEvents = setOf(
            ExperimentEventType.FEED_GENERATED,
            ExperimentEventType.CONTENT_VIEWED, 
            ExperimentEventType.CONTENT_ENGAGED
        )
        
        expectedEvents.forEach { expectedEvent ->
            assertTrue(events.any { it.eventType == expectedEvent }, 
                "Should track $expectedEvent events")
        }
    }

    @Then("the metrics should include experiment ID and variant ID")
    fun verifyMetricsIncludeIds() {
        val userId = "metrics_user"
        val events = abTestingService.getTrackedEvents(userId)
        
        events.forEach { event ->
            assertNotNull(event.experimentId, "Event should have experiment ID")
            assertNotNull(event.variantId, "Event should have variant ID")
        }
    }

    @Then("user interaction data should be associated with the experiment")
    fun verifyUserInteractionAssociation() {
        val userId = "metrics_user"
        val events = abTestingService.getTrackedEvents(userId)
        
        val engagementEvents = events.filter { it.eventType == ExperimentEventType.CONTENT_ENGAGED }
        assertTrue(engagementEvents.isNotEmpty(), "Should have engagement events")
        
        engagementEvents.forEach { event ->
            assertEquals("feed_algorithm_test_2024", event.experimentId)
        }
    }

    @Then("feed generation performance should be recorded")
    fun verifyFeedGenerationPerformanceRecorded() {
        val userId = "metrics_user"
        val feed = feedResults[userId]!!
        
        assertTrue(feed.metadata.generationDuration > 0, "Should record generation duration")
        assertTrue(feed.metadata.contentCount > 0, "Should record content count")
    }

    // Helper methods

    private fun determineCurrentUser(): String {
        return when {
            userGroupAssignments.containsKey("control_user") -> "control_user"
            userGroupAssignments.containsKey("test_user") -> "test_user"
            userGroupAssignments.containsKey("metrics_user") -> "metrics_user"
            userGroupAssignments.containsKey("excluded_user") -> "excluded_user"
            userGroupAssignments.containsKey("variant_user") -> "variant_user"
            userGroupAssignments.containsKey("session_user") -> "session_user"
            userGroupAssignments.containsKey("active_user") -> "active_user"
            userGroupAssignments.containsKey("invalid_params_user") -> "invalid_params_user"
            else -> "default_user"
        }
    }

    private fun generatePersonalizedFeed(userId: String, feedType: FeedType): GeneratedFeed {
        // Simulate feed generation with A/B testing
        val experimentConfig = kotlinx.coroutines.runBlocking {
            abTestingService.getUserExperiment(userId, feedType)
        }
        
        val baseWeights = ScoringWeights(
            recency = 0.5,
            popularity = 0.3,
            relevance = 0.2
        )
        
        val finalWeights = if (experimentConfig != null) {
            abTestingService.applyExperimentParameters(baseWeights, experimentConfig)
        } else {
            baseWeights
        }
        
        // Apply scoring weights to select and rank content
        val rankedContent = contentRepository
            .map { item ->
                val score = calculateContentScore(item, finalWeights)
                Pair(item, score)
            }
            .sortedByDescending { it.second }
            .take(20)
        
        val feedEntries = rankedContent.mapIndexed { index, (item, score) ->
            FeedEntry(
                id = "${item.id}_entry_$index",
                content = convertToStoredContent(item),
                score = score,
                rank = index + 1,
                reasons = listOf(
                    FeedReason(FeedReasonType.RELEVANCE, "Personalized for user", 0.8)
                ),
                sourceType = FeedSourceType.RECOMMENDATION,
                algorithmId = if (experimentConfig?.isControl == false) "experiment_${experimentConfig.variantId}" else "default"
            )
        }
        
        val metadata = FeedMetadata(
            algorithmId = if (experimentConfig?.isControl == false) "experiment_${experimentConfig.variantId}" else "default",
            algorithmVersion = "1.0.0", 
            generationDuration = Random.nextLong(50, 200),
            contentCount = feedEntries.size,
            candidateCount = contentRepository.size,
            parameters = if (experimentConfig != null) {
                mapOf("experiment_config" to experimentConfig, "weights" to finalWeights)
            } else {
                mapOf("weights" to finalWeights)
            }
        )
        
        // Track experiment event if participating
        if (experimentConfig != null) {
            kotlinx.coroutines.runBlocking {
                abTestingService.logExperimentMetrics(userId, experimentConfig, metadata)
            }
        }
        
        return GeneratedFeed(
            userId = userId,
            feedType = feedType,
            entries = feedEntries,
            metadata = metadata
        )
    }

    private fun calculateContentScore(item: PersonalizableItem, weights: ScoringWeights): Double {
        val recencyScore = calculateRecencyScore(item.createdAt)
        val popularityScore = item.baseScore
        val relevanceScore = item.baseScore * 0.8 // Simulate topic relevance
        
        return (recencyScore * weights.recency) + 
               (popularityScore * weights.popularity) +
               (relevanceScore * weights.relevance)
    }

    private fun calculateRecencyScore(publishedAt: Instant): Double {
        val hoursAgo = java.time.Duration.between(publishedAt, Instant.now()).toHours()
        return when {
            hoursAgo <= 1 -> 1.0
            hoursAgo <= 6 -> 0.8
            hoursAgo <= 24 -> 0.6
            hoursAgo <= 72 -> 0.4
            else -> 0.2
        }
    }

    private fun convertToStoredContent(item: PersonalizableItem): StoredContent {
        return item.content
    }
}

// Mock classes and data structures

private class MockABTestingService : ABTestingService {
    private var activeExperiments: List<MockExperiment> = emptyList()
    private val userAssignments = mutableMapOf<String, Map<String, ExperimentVariant>>()
    private val trackedEvents = mutableMapOf<String, MutableList<ExperimentEventRecord>>()
    private val forcedAssignments = mutableMapOf<String, Map<String, ExperimentVariant>>()
    private val excludedUsers = mutableSetOf<String>()
    private val eligibleUsers = mutableSetOf<String>()
    private var emergencyOverride = false

    fun setActiveExperiments(experiments: List<MockExperiment>) {
        this.activeExperiments = experiments
    }
    
    fun forceUserAssignment(userId: String, experimentId: String, variant: ExperimentVariant) {
        val userExperiments = forcedAssignments.getOrPut(userId) { mutableMapOf() }.toMutableMap()
        userExperiments[experimentId] = variant
        forcedAssignments[userId] = userExperiments
    }
    
    fun forceUserExclusion(userId: String) {
        excludedUsers.add(userId)
    }
    
    fun makeUserEligibleForAll(userId: String) {
        eligibleUsers.add(userId)
    }
    
    fun trackEvent(userId: String, experimentId: String, eventType: ExperimentEventType) {
        val variant = getUserAssignment(userId, experimentId)
        val event = ExperimentEventRecord(
            userId = userId,
            experimentId = experimentId,
            variantId = variant?.id,
            eventType = eventType,
            timestamp = Instant.now()
        )
        
        trackedEvents.getOrPut(userId) { mutableListOf() }.add(event)
    }
    
    fun getTrackedEvents(userId: String): List<ExperimentEventRecord> {
        return trackedEvents[userId] ?: emptyList()
    }
    
    fun updateExperiment(experiment: MockExperiment) {
        activeExperiments = activeExperiments.map { 
            if (it.id == experiment.id) experiment else it 
        }
    }
    
    fun enableEmergencyOverride() {
        emergencyOverride = true
    }

    private fun getUserAssignment(userId: String, experimentId: String): ExperimentVariant? {
        return forcedAssignments[userId]?.get(experimentId) 
            ?: userAssignments[userId]?.get(experimentId)
    }

    override suspend fun getUserExperiment(userId: String, feedType: FeedType): ExperimentConfig? {
        if (emergencyOverride) return null
        
        val experiment = activeExperiments.find { it.feedTypes.contains(feedType) } ?: return null
        val variant = getExperimentVariant(userId, experiment.id) ?: return null
        
        return ExperimentConfig(
            experimentId = experiment.id,
            variantId = variant.id,
            parameters = variant.parameters,
            isControl = variant.isControl
        )
    }

    override fun applyExperimentParameters(baseWeights: ScoringWeights, experimentConfig: ExperimentConfig): ScoringWeights {
        var modifiedWeights = baseWeights
        
        experimentConfig.parameters.forEach { (key, value) ->
            when (key) {
                "recency_weight" -> {
                    val weight = (value as? Number)?.toDouble() ?: baseWeights.recency
                    modifiedWeights = modifiedWeights.copy(recency = weight)
                }
                "popularity_weight" -> {
                    val weight = (value as? Number)?.toDouble() ?: baseWeights.popularity
                    modifiedWeights = modifiedWeights.copy(popularity = weight)
                }
                "relevance_weight" -> {
                    val weight = (value as? Number)?.toDouble() ?: baseWeights.relevance
                    modifiedWeights = modifiedWeights.copy(relevance = weight)
                }
            }
        }
        
        return modifiedWeights
    }

    override suspend fun logExperimentMetrics(
        userId: String,
        experimentConfig: ExperimentConfig,
        feedMetadata: FeedMetadata,
        userInteractions: Map<String, Any>
    ) {
        trackEvent(userId, experimentConfig.experimentId, ExperimentEventType.FEED_GENERATED)
    }

    override fun shouldIncludeInExperiment(userId: String, experimentId: String, targetPercentage: Double): Boolean {
        if (excludedUsers.contains(userId)) return false
        if (eligibleUsers.contains(userId)) return true
        
        val hash = "$userId:$experimentId".hashCode()
        val bucket = (Math.abs(hash) % 100).toDouble()
        return bucket < targetPercentage
    }

    override suspend fun getExperimentVariant(userId: String, experimentId: String): ExperimentVariant? {
        // Check forced assignments first
        forcedAssignments[userId]?.get(experimentId)?.let { return it }
        
        val experiment = activeExperiments.find { it.id == experimentId } ?: return null
        
        if (!shouldIncludeInExperiment(userId, experimentId, experiment.targetPercentage)) {
            trackEvent(userId, experimentId, ExperimentEventType.EXPERIMENT_EXCLUDED)
            return null
        }
        
        // Use consistent hashing for variant assignment
        val hash = "$userId:$experimentId:variant".hashCode()
        val bucket = Math.abs(hash) % 100.0
        
        var cumulativePercentage = 0.0
        for (variant in experiment.variants) {
            cumulativePercentage += variant.allocationPercentage
            if (bucket < cumulativePercentage) {
                // Store assignment
                val userExperiments = userAssignments.getOrPut(userId) { mutableMapOf() }.toMutableMap()
                userExperiments[experimentId] = variant
                userAssignments[userId] = userExperiments
                return variant
            }
        }
        
        return experiment.variants.lastOrNull()
    }

    override suspend fun trackExperimentEvent(
        userId: String,
        experimentId: String,
        eventType: ExperimentEventType,
        metadata: Map<String, Any>
    ) {
        trackEvent(userId, experimentId, eventType)
    }
}

private data class MockExperiment(
    val id: String,
    val name: String,
    val feedTypes: List<FeedType>,
    val targetPercentage: Double,
    val variants: List<ExperimentVariant>
)

private data class ExperimentEventRecord(
    val userId: String,
    val experimentId: String,
    val variantId: String?,
    val eventType: ExperimentEventType,
    val timestamp: Instant,
    val metadata: Map<String, Any> = emptyMap()
)

private data class ExperimentMetricRecord(
    val userId: String,
    val experimentId: String,
    val variantId: String,
    val metricName: String,
    val metricValue: Double,
    val timestamp: Instant
)