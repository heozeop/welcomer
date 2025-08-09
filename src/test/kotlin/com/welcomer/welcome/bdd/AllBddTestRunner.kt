package com.welcomer.welcome.bdd

import io.cucumber.junit.platform.engine.Constants
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Main BDD test suite runner for all core personalization scenarios
 * 
 * This test runner executes all Cucumber scenarios from the feature files.
 * It serves as the main entry point for BDD testing of the feed personalization system.
 * 
 * Features covered:
 * - New user personalization (cold-start scenarios)
 * - Power user personalization (heavy users with rich history)
 * - Contextual personalization (time, location, device)
 * - Edge case handling
 * - Error scenarios
 * - API testing
 * - Performance testing
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.welcomer.welcome.bdd,com.welcomer.welcome.bdd.steps")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty,html:build/reports/cucumber/all.html,json:build/reports/cucumber/all.json,junit:build/reports/cucumber/all.xml")
@ConfigurationParameter(key = Constants.SNIPPET_TYPE_PROPERTY_NAME, value = "camelcase")
class AllBddTestRunner