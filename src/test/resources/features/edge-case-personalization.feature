Feature: Edge Case Feed Personalization
  As a user with complex or challenging personalization requirements
  I want the feed system to handle edge cases gracefully
  So that I receive reasonable content even with conflicting or rapidly changing preferences

  Background:
    Given the personalization service is available
    And content fixtures are loaded with diverse topics
    And edge case fixtures are available

  Scenario: User with conflicting topic preferences receives balanced content
    Given I am a user who loves both "minimalism" and "maximalism"
    And my engagement history shows equal interest in both contradictory topics
    When I request my personalized feed
    Then I should receive content from both minimalism and maximalism topics
    And the system should not favor one contradictory preference over the other
    And bridging content that explores both concepts should be prioritized
    And the feed should acknowledge the complexity of my preferences

  Scenario: User with professional vs personal interest conflicts
    Given I am a user whose work interest is "corporate finance"
    And my personal interest is "anti-corporate activism"
    And I engage with both types of content regularly
    When I request my personalized feed during business hours
    Then I should receive primarily corporate finance content
    When I request my personalized feed during evening hours
    Then I should receive more anti-corporate activism content
    And the system should context-switch appropriately

  Scenario: User whose interests change rapidly over short periods
    Given I am a user who was heavily interested in "photography" last month
    And I have recently shifted to intense interest in "music production"
    And my engagement patterns show this rapid transition
    When I request my personalized feed
    Then I should receive primarily music production content
    And photography content should be reduced but not eliminated
    And transitional content connecting photography and music should appear
    And the system should adapt quickly to the interest shift

  Scenario: User with seasonal interest volatility
    Given I am a user whose interests change dramatically with seasons
    And I was interested in "winter sports" 3 months ago
    But now I'm interested in "summer travel"
    When I request my personalized feed in summer
    Then I should receive primarily summer travel content
    And winter sports content should be minimized
    And the system should recognize seasonal interest patterns
    And year-round bridging topics should be maintained

  Scenario: User with contradictory content type preferences
    Given I am a user who prefers "long-form detailed articles"
    But I also prefer "quick visual content"
    And my engagement shows I consume both extensively but at different times
    When I request my personalized feed on mobile during lunch
    Then I should receive more quick visual content
    When I request my personalized feed on desktop during evening
    Then I should receive more long-form detailed articles
    And the system should optimize for context-appropriate content types

  Scenario: Cold start with new content type introduction
    Given I am an existing user with established preferences in "text" and "image" content
    And the platform introduces a new "podcast" content type
    When new podcast content becomes available in my interest areas
    Then I should gradually see podcast content introduced to my feed
    And the podcasts should align with my existing topic interests
    And the introduction should be gentle, not overwhelming
    And I should be able to express preferences about the new content type

  Scenario: User with conflicting engagement patterns and stated preferences
    Given I have explicitly stated I dislike "sports" content
    But my engagement history shows I frequently interact with sports content
    When I request my personalized feed
    Then the system should prioritize my engagement behavior over stated preferences
    And sports content should appear in my feed despite stated dislike
    But the system should provide options to reconcile this conflict
    And I should be asked to clarify my preferences

  Scenario: User experiencing interest fatigue in main topics
    Given I am a user heavily engaged with "technology" content for 2 years
    And my recent engagement with technology shows declining interest patterns
    But I haven't explicitly changed my preferences
    When I request my personalized feed
    Then the system should detect the engagement fatigue
    And technology content should be reduced gradually
    And adjacent topics like "innovation" and "science" should be increased
    And the system should proactively suggest interest diversification

  Scenario: User with extreme niche interests and limited content
    Given I am a user interested in "18th century clockmaking"
    And there is very limited content available for this topic
    But I consistently engage with any clockmaking content
    When I request my personalized feed
    Then I should receive all available clockmaking content
    And related topics like "craftsmanship" and "history" should be included
    And the system should explain the scarcity and suggest broadening
    And quality should be prioritized over quantity for niche content

  Scenario: User with privacy-conflicting personalization needs
    Given I am a user who wants personalized content
    But I have disabled behavioral tracking
    And I have minimal explicit preferences set
    When I request my personalized feed
    Then the system should provide reasonable content based on limited data
    And popular high-quality content should be prioritized
    And basic demographic-based recommendations should be used
    And the limitations should be transparently communicated

  Scenario: User with multiple personas or account sharing
    Given I am a user account that shows engagement patterns of multiple people
    And the patterns include "children's content" and "investment advice"
    And "cooking" content and "gaming" content at different times
    When I request my personalized feed
    Then the system should recognize multiple usage patterns
    And content should vary appropriately by time of access
    And family-friendly content should be balanced with adult content
    And the system should offer persona separation options

  Scenario: User returning after long absence with stale preferences
    Given I am a user who was very active 1 year ago
    And my last recorded interests were "travel" and "photography"
    But I've been inactive and interests may have changed
    When I return and request my personalized feed
    Then the system should treat me as a semi-cold start case
    And recent popular content should be weighted higher than stale preferences
    And the system should gently reintroduce personalization
    And I should be prompted to update my interests

  Scenario: User with conflicting social signals and personal preferences
    Given I personally love "quiet contemplative content"
    But my social network heavily shares "viral trending content"
    And I sometimes engage with shared viral content
    When I request my personalized feed
    Then quiet contemplative content should be prioritized
    And viral content should appear but not dominate
    And the system should balance personal vs social influences appropriately
    And I should have control over social signal weighting

  Scenario: User whose preferences conflict with platform safety guidelines
    Given I am a user interested in "controversial political topics"
    But some content in this area violates platform guidelines
    When I request my personalized feed
    Then I should receive policy-compliant political content
    And the system should not compromise safety for personalization
    And alternative perspective sources should be provided
    And content moderation should be transparent

  Scenario: User experiencing algorithmic filter bubble detection
    Given I am a user who has been receiving very similar content
    And the system detects I'm in a potential filter bubble
    But my engagement with the similar content remains high
    When I request my personalized feed
    Then the system should introduce controlled diversity
    And filter bubble breaking content should be gradually introduced
    And my engagement with diverse content should be monitored
    And I should be informed about diversity initiatives

  Scenario: User with accessibility needs conflicting with content preferences
    Given I am a user who needs "screen reader compatible" content
    But my interests are in "visual art" and "photography"
    When I request my personalized feed
    Then visual content should include detailed descriptions
    And alternative accessible content about art should be provided
    And the system should not exclude visual content entirely
    And accessibility should enhance rather than limit personalization

  Scenario: User with temporary vs permanent interest changes
    Given I am a user with normally stable interests in "science"
    But I'm currently planning a wedding and engaging heavily with "wedding" content
    When I request my personalized feed during wedding planning period
    Then wedding content should be prominently featured
    But science content should be maintained as a baseline
    When I request my feed 6 months after the wedding
    Then wedding content should be reduced back to minimal levels
    And the system should distinguish temporary from permanent interest shifts

  Scenario: User with contradictory quality vs quantity preferences
    Given I am a user who engages with both "high-quality in-depth" content
    And "quick low-effort entertainment" content
    When I request my personalized feed
    Then both content types should be represented appropriately
    And quality content should appear during focused browsing times
    And quick content should appear during casual browsing times
    And the system should learn my quality preferences by context

  Scenario: User experiencing platform algorithm changes
    Given I am a user with well-established feed preferences
    And the platform updates its personalization algorithm
    When the new algorithm affects my feed significantly
    Then I should be notified about the algorithm change
    And I should have options to adjust to the new personalization
    And feedback mechanisms should be available for the new algorithm
    And gradual transition should be preferred over sudden changes

  Scenario: User with multilingual content preferences and conflicts
    Given I am a user who consumes content in "English" and "Spanish"
    But some topics are only available in one language
    And translation quality varies significantly
    When I request my personalized feed
    Then content should be provided in my preferred languages when available
    And high-quality translated content should be included when necessary
    And language preferences should not limit topic diversity
    And original language content should be marked clearly