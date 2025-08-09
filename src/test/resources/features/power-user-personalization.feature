Feature: Feed Personalization for Power Users
  As a power user with extensive engagement history
  I want to receive highly personalized content based on my established patterns
  So that I can efficiently discover relevant content that matches my refined interests

  Background:
    Given the personalization service is available
    And content fixtures are loaded with diverse topics
    And power user fixtures are available

  Scenario: Power user with strong topic preferences receives focused content
    Given I am a power user with 500+ interactions in "technology" and "programming"
    And I have consistently engaged with "ai" and "machine-learning" content
    When I request my personalized feed
    Then I should receive primarily technology and programming content
    And at least 60% of content should match my established topic preferences
    And the personalization multiplier should be high
    And advanced or expert-level content should be prioritized

  Scenario: Power user receives content from preferred authors and sources
    Given I am a power user who follows 25+ authors
    And I have high engagement rates with content from specific authors
    When I request my personalized feed
    Then at least 40% of content should be from followed authors
    And authors with similar content styles should be recommended
    And high-reputation sources in my interest areas should be prioritized
    And new authors with similar expertise should be suggested

  Scenario: Power user personalization considers engagement patterns
    Given I am a power user with consistent daily engagement for 6+ months
    And I typically engage with "long-form" content during "evening" hours
    And I prefer "technical tutorials" and "in-depth analysis"
    When I request my personalized feed
    Then content should be optimized for my engagement patterns
    And longer, more detailed content should be prioritized
    And content matching my preferred consumption time should be surfaced
    And shallow or basic content should be filtered out

  Scenario: Power user with niche expertise gets specialized recommendations
    Given I am a power user specializing in "blockchain" and "cryptocurrency"
    And I have demonstrated expertise through high-quality interactions
    And I engage with cutting-edge research and technical discussions
    When I request my personalized feed
    Then I should receive highly specialized blockchain content
    And emerging trends and advanced topics should be featured
    And beginner-level content should be minimal
    And research papers and technical whitepapers should be included

  Scenario: Power user receives personalized content across multiple interests
    Given I am a power user with strong interests in "fitness", "nutrition", and "technology"
    And I maintain distinct engagement patterns for each interest area
    And I have 200+ interactions across these topics over 12+ months
    When I request my personalized feed
    Then content should be balanced across my established interests
    And cross-pollination between interests should be considered
    And content quality should match my expertise level in each area
    And temporal preferences should be respected for each topic

  Scenario: Power user personalization adapts to evolving interests
    Given I am a power user with established preferences in "web development"
    And I have recently started engaging with "mobile development" content
    And my engagement patterns show growing interest in mobile topics
    When I request my personalized feed over multiple sessions
    Then the feed should gradually include more mobile development content
    And the transition should be smooth without losing core preferences
    And related content bridging web and mobile should be featured
    And my engagement feedback should influence future recommendations

  Scenario: Power user with high standards gets premium quality content
    Given I am a power user with selective engagement patterns
    And I consistently interact only with high-quality, authoritative content
    And I have low tolerance for clickbait or superficial content
    When I request my personalized feed
    Then all content should meet high quality thresholds
    And clickbait and low-effort content should be filtered out
    And authoritative sources should be heavily weighted
    And content depth and expertise should be prioritized over popularity

  Scenario: Power user receives time-sensitive personalized content
    Given I am a power user in "finance" and "investing" topics
    And I have consistent patterns of engaging with market news and analysis
    And I prefer real-time updates during market hours
    When I request my personalized feed during trading hours
    Then breaking financial news should be prioritized
    And time-sensitive market analysis should be surfaced immediately
    And historical or evergreen content should be deprioritized
    And content should be relevant to current market conditions

  Scenario: Power user personalization handles content saturation gracefully
    Given I am a power user who consumes 50+ content items daily
    And I have seen most content in my primary interest areas
    And I have exhausted obvious recommendations in my topics
    When I request my personalized feed
    Then fresh, previously unseen content should be prioritized
    And tangential topics should be explored to prevent staleness
    And international or alternative perspective sources should be included
    And content freshness should be weighted heavily in ranking

  Scenario: Power user with social influence receives community-relevant content
    Given I am a power user who frequently shares and comments on content
    And I have followers who engage with my shared content
    And I participate actively in topic-specific discussions
    When I request my personalized feed
    Then shareable content with high viral potential should be included
    And content that sparks discussions should be prioritized
    And community trends relevant to my influence should be featured
    And content that aligns with my sharing patterns should be surfaced

  Scenario: Power user personalization maintains serendipity despite strong preferences
    Given I am a power user with very focused interests in "data science"
    And I have highly predictable engagement patterns
    And my preferences are well-established over 18+ months
    When I request my personalized feed
    Then 80% should match my established preferences
    But 20% should introduce controlled serendipity
    And unexpected high-quality content from adjacent fields should appear
    And the system should test new topics while respecting my expertise level
    And feedback loops should refine the balance between focus and discovery