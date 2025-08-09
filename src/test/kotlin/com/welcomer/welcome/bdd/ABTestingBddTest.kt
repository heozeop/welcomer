package com.welcomer.welcome.bdd

import io.cucumber.junit.platform.engine.Constants
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * BDD Test Runner for A/B Testing Framework Integration Scenarios
 * 
 * This test suite validates that the feed personalization system correctly 
 * integrates with A/B testing frameworks and delivers consistent experiences 
 * within test groups.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/ab-testing.feature")
@ConfigurationParameter(
    key = Constants.GLUE_PROPERTY_NAME,
    value = "com.welcomer.welcome.bdd.steps"
)
@ConfigurationParameter(
    key = Constants.EXECUTION_DRY_RUN_PROPERTY_NAME,
    value = "false"
)
@ConfigurationParameter(
    key = Constants.PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber/ab-testing.html, json:build/reports/cucumber/ab-testing.json"
)
class ABTestingBddTest