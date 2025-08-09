Feature: A/B Testing Framework Integration for Feed Personalization

  Background:
    Given the A/B testing framework is configured
    And we have a content repository with diverse content
    And we have active experiments for feed personalization

  Scenario: User is consistently assigned to the same test group
    Given a user "user123" 
    When they request their personalized feed multiple times
    Then they should consistently receive the same experiment variant
    And their experience should remain consistent across sessions

  Scenario: Control group receives default personalization algorithm
    Given a user "control_user" is assigned to the control group
    When they request their personalized feed  
    Then the feed should be generated using default algorithm parameters
    And the experiment metadata should indicate control group assignment
    And the feed generation metrics should be tracked for the control group

  Scenario: Test group receives modified personalization algorithm
    Given a user "test_user" is assigned to the "high_recency" variant
    When they request their personalized feed
    Then the feed should be generated with modified recency weights
    And the content should prioritize more recent posts
    And the experiment metadata should indicate test group assignment
    And the feed generation metrics should be tracked for the test group

  Scenario: User assignment follows percentage distribution
    Given an experiment with 50% control and 50% test allocation
    When 100 users request their personalized feeds
    Then approximately 50% should be assigned to control group  
    And approximately 50% should be assigned to test group
    And the assignment should be based on consistent hashing

  Scenario: Users excluded from experiment receive default experience
    Given an experiment targeting only 10% of users
    When a user "excluded_user" is not selected for the experiment
    Then they should receive the default personalization experience
    And no experiment metrics should be collected for them
    And an exclusion event should be tracked

  Scenario: Experiment metrics are properly collected
    Given a user "metrics_user" is in the "high_recency" test group
    When they interact with their personalized feed
    And they view content items
    And they engage with content (likes, shares, comments)
    Then all experiment events should be tracked
    And the metrics should include experiment ID and variant ID
    And user interaction data should be associated with the experiment
    And feed generation performance should be recorded

  Scenario: Multiple concurrent experiments assignment
    Given multiple active experiments for different feed aspects
    And user "multi_test_user" is eligible for multiple experiments  
    When they request their personalized feed
    Then they should be assigned to compatible experiment variants
    And all experiment parameters should be properly combined
    And metrics should be tracked for all active experiments
    And experiment conflicts should be handled gracefully

  Scenario: Experiment variant parameters are correctly applied
    Given an experiment variant with custom scoring weights
    And the variant specifies recency_weight: 0.7, popularity_weight: 0.2, relevance_weight: 0.1
    When a user in this variant requests their feed
    Then the personalization algorithm should use the modified weights
    And content scoring should reflect the adjusted parameters
    And recent content should be prioritized more heavily
    And the feed composition should differ from control group

  Scenario: Session consistency within experiment groups  
    Given a user "session_user" is assigned to a specific experiment variant
    When they start multiple sessions throughout the day
    Then they should maintain the same experiment assignment
    And their personalization should remain consistent
    And session-based experiment events should be properly tracked
    And variant parameters should persist across sessions

  Scenario: Feed type specific experiment assignment
    Given experiments configured for different feed types
    And user "feed_type_user" requests different types of feeds
    When they request a HOME feed
    Then they should get experiment assignment for HOME feed type
    When they request an EXPLORE feed  
    Then they should get appropriate experiment assignment for EXPLORE feed type
    And experiments should be independent per feed type
    And metrics should be tracked separately by feed type

  Scenario: Experiment parameter edge cases and validation
    Given an experiment variant with invalid parameters
    When the personalization system processes the experiment
    Then invalid parameters should be ignored or defaulted
    And valid parameters should be applied correctly
    And the system should continue to function normally
    And parameter validation errors should be logged

  Scenario: Real-time experiment metrics collection
    Given a user is actively participating in an experiment
    When they perform various actions on personalized content
    Then feed generation metrics should be collected immediately
    And user engagement metrics should be tracked in real-time
    And experiment performance data should be available for analysis
    And metrics should include timestamps and user context

  Scenario: Experiment gradual rollout and ramping
    Given an experiment initially targeting 5% of users
    When the experiment target percentage is increased to 15%
    Then newly eligible users should be assigned to experiment variants
    And existing assignments should remain stable
    And the new distribution should reflect the updated percentages
    And rollout metrics should be tracked

  Scenario: A/B test impact on personalization quality
    Given users in both control and test groups
    When they receive personalized content over time
    Then content relevance should be maintained or improved in test groups
    And user engagement metrics should be comparable or better
    And personalization effectiveness should not degrade
    And quality metrics should be tracked for comparison

  Scenario: Experiment data isolation and analysis preparation
    Given multiple users participating in various experiments
    When experiment data is collected over time
    Then control group data should be clearly isolated
    And test group data should be properly segmented by variant
    And cross-contamination between experiments should be prevented
    And data should be structured for statistical analysis

  Scenario: Emergency experiment override and fallback
    Given an active experiment is causing performance issues
    When an emergency override is triggered
    Then all users should fall back to the control experience
    And experiment assignments should be temporarily suspended  
    And fallback events should be tracked for analysis
    And the system should operate normally during the override