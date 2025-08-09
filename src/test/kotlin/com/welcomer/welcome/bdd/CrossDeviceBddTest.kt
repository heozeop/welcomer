package com.welcomer.welcome.bdd

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * BDD test runner for cross-device user journey scenarios
 * 
 * This test verifies that personalization consistency and synchronization
 * work correctly across multiple devices for the same user, including
 * preference sync, reading position preservation, and device-specific
 * optimizations.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["src/test/resources/features/cross-device.feature"],
    glue = ["com.welcomer.welcome.bdd", "com.welcomer.welcome.bdd.steps"],
    plugin = [
        "pretty",
        "html:build/reports/cucumber/cross-device.html",
        "json:build/reports/cucumber/cross-device.json"
    ],
    snippets = CucumberOptions.SnippetType.CAMELCASE
)
class CrossDeviceBddTest