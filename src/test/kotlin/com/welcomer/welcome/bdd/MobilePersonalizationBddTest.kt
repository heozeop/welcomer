package com.welcomer.welcome.bdd

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * BDD test runner for mobile-specific personalization scenarios
 * 
 * This test validates that the feed personalization system adapts correctly
 * to mobile devices with their unique constraints and capabilities, including
 * smaller screens, touch interactions, and variable connectivity.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["src/test/resources/features/mobile-personalization.feature"],
    glue = ["com.welcomer.welcome.bdd", "com.welcomer.welcome.bdd.steps"],
    plugin = [
        "pretty",
        "html:build/reports/cucumber/mobile.html",
        "json:build/reports/cucumber/mobile.json"
    ],
    snippets = CucumberOptions.SnippetType.CAMELCASE
)
class MobilePersonalizationBddTest