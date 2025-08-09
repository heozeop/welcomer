Feature: Feed Personalization for New Users
  As a new user with minimal history
  I want to receive diverse and discoverable content
  So that I can explore different topics and find my interests

  Background:
    Given the personalization service is available
    And content fixtures are loaded with diverse topics

  Scenario: First-time user receives diverse content
    Given I am a new user with no engagement history
    When I request my personalized feed
    Then I should receive content from at least 5 different topics
    And no single topic should represent more than 30% of the content
    And I should receive content from multiple authors
    And the personalization multiplier should be minimal

  Scenario: New user with basic interests gets exploration content
    Given I am a new user who has specified interest in "technology"
    When I request my personalized feed
    Then I should receive some technology content
    But I should also receive diverse content from other topics
    And trending content should be included for discovery
    And the content should have high quality scores

  Scenario: New user with minimal interactions gets balanced recommendations
    Given I am a new user with 2 previous interactions on "fitness" content
    When I request my personalized feed
    Then I should receive some fitness-related content
    But the majority should be diverse content for exploration
    And I should receive content from both followed and unfollowed authors
    And fresh content should be prioritized

  Scenario: New user personalization respects content type preferences
    Given I am a new mobile-first user
    When I request my personalized feed
    Then I should receive more visual content (images and videos)
    And text content should be optimized for mobile consumption
    And the content should have high shareability scores
    And session duration should be considered for content length

  Scenario: New user with accessibility needs gets appropriate content
    Given I am a new user with accessibility requirements
    When I request my personalized feed
    Then I should receive primarily text-based content
    And all content should have proper descriptions
    And visual content should have alt-text considerations
    And the content should be screen reader friendly

  Scenario: New user discovers trending content for engagement
    Given I am a new user during peak hours
    When I request my personalized feed
    Then I should receive trending content from the last 24 hours
    And high-engagement content should be prioritized
    And viral content should be included for discovery
    And the trending score should influence content ranking

  Scenario: New user personalization avoids filter bubbles
    Given I am a new user with expressed interest in "cooking"
    When I request my personalized feed over multiple sessions
    Then I should receive cooking content but not exclusively
    And serendipitous content from unexpected topics should appear
    And the diversity should increase over subsequent requests
    And I should discover content from new authors

  Scenario: New user gets quality-assured content for good first impression
    Given I am a new user requesting content for the first time
    When I request my personalized feed
    Then all content should have quality scores above 0.7
    And content from verified or high-reputation sources should be prioritized
    And spam or low-quality content should be filtered out
    And the content should be recent (within the last week)

  Scenario: New user personalization handles no available content gracefully
    Given I am a new user with very specific niche interests
    And there is limited content matching my profile
    When I request my personalized feed
    Then I should receive the best available diverse content
    And fallback content should be high-quality general interest items
    And an explanation should be provided about content discovery
    And suggestions for broadening interests should be included

  Scenario: New user receives content appropriate for their context
    Given I am a new user accessing the platform at "morning" time
    And I am located in "US" region
    When I request my personalized feed
    Then I should receive content appropriate for morning consumption
    And regional preferences should be considered
    And time-sensitive content should be prioritized
    And the content mix should suit the time of day