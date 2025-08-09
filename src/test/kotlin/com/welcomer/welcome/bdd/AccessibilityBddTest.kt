package com.welcomer.welcome.bdd

import io.cucumber.junit.platform.engine.Cucumber
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite
import io.cucumber.junit.platform.engine.Constants

/**
 * BDD test suite runner for accessibility personalization scenarios
 * 
 * This test runner executes Cucumber scenarios defined in the accessibility-personalization.feature file.
 * It validates that the feed personalization system properly handles accessibility requirements and
 * inclusive design principles.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/accessibility-personalization.feature")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.welcomer.welcome.bdd.steps")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty")
class AccessibilityBddTest