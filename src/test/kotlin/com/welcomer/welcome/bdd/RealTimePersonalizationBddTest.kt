package com.welcomer.welcome.bdd

import io.cucumber.junit.platform.engine.Constants
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * BDD test runner for real-time personalization scenarios
 * 
 * This test verifies that the personalization system can adapt quickly
 * and accurately to real-time user behaviors, preference changes, and
 * contextual updates during active browsing sessions.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/real-time-personalization.feature")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.welcomer.welcome.bdd,com.welcomer.welcome.bdd.steps")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty,html:build/reports/cucumber/real-time.html,json:build/reports/cucumber/real-time.json")
@ConfigurationParameter(key = Constants.SNIPPET_TYPE_PROPERTY_NAME, value = "camelcase")
class RealTimePersonalizationBddTest