Feature: Cross-Device User Journey Testing for Feed Personalization

  Background:
    Given the cross-device synchronization system is configured
    And we have a diverse content repository with multi-device optimized content
    And we have various device profiles available

  Scenario: User preferences sync across devices after login
    Given a user "alice_123" with established preferences on their smartphone
    And their preferences include high interest in "technology" and "science"
    And they prefer "long-form" content over "short-form" content
    When they log in to their tablet device
    And they request their personalized feed
    Then the feed should reflect their technology and science preferences
    And the content should be formatted appropriately for tablet viewing
    And their preference weights should be synchronized from their smartphone

  Scenario: Reading position is preserved when switching devices
    Given a user "bob_456" is reading an article on their laptop
    And they have read 60% of a long-form article about "artificial intelligence"
    When they switch to their mobile phone
    And they open the same article
    Then their reading position should be preserved at 60%
    And the article should be formatted for mobile viewing
    And continuation prompts should be available

  Scenario: Feed personalization adapts to new device capabilities
    Given a user "carol_789" has personalization settings from a basic phone
    And their current preferences prioritize "text-only" content
    When they upgrade to a high-end tablet
    And they request their personalized feed
    Then the system should gradually introduce rich media content
    And their preferences should evolve to include "images" and "videos"
    And the adaptation should be seamless and user-controlled

  Scenario: Cross-device content recommendation consistency
    Given a user "dave_101" interacts with content on multiple devices
    And they engage with "sports" content on their smartphone
    And they watch "basketball" videos on their smart TV
    When they use their work laptop
    And they request their personalized feed
    Then sports content should be prioritized across all devices
    And basketball content should receive higher relevance scores
    And the recommendations should maintain consistency across platforms

  Scenario: Device-specific personalization overlays work correctly
    Given a user "eve_202" has global preferences for "news" and "politics"
    And they have device-specific overlay preferences for work laptop
    And their work overlay emphasizes "business" and "technology" content
    When they use their work laptop during business hours
    And they request their personalized feed
    Then business and technology content should be prioritized
    And news and politics content should be secondary
    And personal preferences should still influence ranking

  Scenario: Offline content synchronization across devices
    Given a user "frank_303" downloads content for offline reading on their tablet
    And they mark several articles as "saved for later"
    And they start reading one article offline
    When they connect to WiFi and sync with their smartphone
    Then their saved articles should be available on smartphone
    And their reading progress should be synchronized
    And offline content should remain accessible on both devices

  Scenario: Cross-device engagement tracking improves personalization
    Given a user "grace_404" has varied engagement patterns across devices
    And they prefer short sessions on mobile during commute
    And they prefer long reading sessions on tablet at home
    When the system analyzes their cross-device behavior
    And they request personalized feed on any device
    Then content length should adapt to current device and context
    And engagement history should inform content selection
    And device-appropriate content should be prioritized

  Scenario: Family account with multiple devices maintains individual preferences
    Given a family account with users "parent_505" and "child_505"
    And they share a tablet but have individual smartphones
    And parent prefers "finance" and "health" content
    And child prefers "gaming" and "entertainment" content
    When parent uses the shared tablet
    Then their personalized feed should show finance and health content
    When child uses the same shared tablet
    Then their personalized feed should show gaming and entertainment content

  Scenario: Device handoff maintains personalization context
    Given a user "helen_606" is browsing their feed on desktop
    And they have viewed several articles about "climate change"
    And they have spent 5 minutes on environmental content
    When they pick up their mobile phone within 10 minutes
    And they open the feed app
    Then related climate change content should be prioritized
    And their browsing session context should be maintained
    And content should be formatted for mobile consumption

  Scenario: Cross-device A/B testing maintains experiment consistency
    Given a user "ian_707" is part of an A/B test for feed algorithms
    And they are in the "experimental_group_b" on their laptop
    When they access their feed from their smartphone
    Then they should remain in "experimental_group_b"
    And the same experimental parameters should apply
    And their experiment participation should be device-agnostic

  Scenario: Privacy settings sync appropriately across devices
    Given a user "jane_808" has privacy settings configured on their smartphone
    And they have disabled location-based personalization
    And they have opted out of behavioral tracking
    When they log in on their tablet
    Then their privacy settings should be respected
    And location-based content should not be used for personalization
    And behavioral tracking should remain disabled across devices

  Scenario: Device performance affects cross-device content delivery
    Given a user "kevin_909" has content preferences synced across devices
    And they have a high-end smartphone and low-end tablet
    When they request the same personalized feed on both devices
    Then content should be optimized per device capabilities
    And high-end smartphone should receive rich media content
    And low-end tablet should receive performance-optimized content
    And core personalization should remain consistent

  Scenario: Cross-device notification preferences work correctly  
    Given a user "linda_010" has notification preferences set on their primary device
    And they want "breaking news" notifications on mobile
    And they want "daily digest" notifications on desktop
    When important news breaks
    Then mobile device should receive immediate notifications
    When it's their configured digest time
    Then desktop should receive daily digest notification
    And notification content should be personalized per device

  Scenario: Seamless device switching during content consumption
    Given a user "mike_111" is watching a video on their tablet
    And they have watched 3 minutes of a 10-minute video
    When they need to leave and switch to their smartphone
    Then they should be able to continue from where they left off
    And the video should adapt to mobile viewing format
    And their watching history should be updated across devices

  Scenario: Cross-device content caching optimizes user experience
    Given a user "nina_212" frequently switches between laptop and mobile
    And they have predictable usage patterns throughout the day
    When the system analyzes their device switching patterns
    Then content should be pre-cached on likely next device
    And switching between devices should be near-instantaneous
    And cached content should be personalized and relevant

  Scenario: Device-specific personalization conflicts are resolved gracefully
    Given a user "oscar_313" has conflicting preferences across devices
    And their mobile preferences favor "quick news updates"
    And their desktop preferences favor "in-depth analysis"
    When they use a new device for the first time
    Then the system should prompt for preference clarification
    And a balanced approach should be used initially
    And user should have control over preference resolution

  Scenario: Cross-device accessibility preferences are maintained
    Given a user "paula_414" has accessibility needs configured on their smartphone
    And they require "high contrast mode" and "large text"
    When they access their account from any other device
    Then accessibility settings should be applied automatically
    And content should be formatted for their accessibility needs
    And the experience should be consistent across all devices

  Scenario: Multi-device analytics provide comprehensive user insights
    Given a user "quinn_515" uses multiple devices throughout the day
    And their engagement patterns vary by device and time
    When the personalization system analyzes their behavior
    Then insights should combine data from all devices
    And personalization should improve based on holistic user behavior
    And device-specific optimizations should be data-driven

  Scenario: Emergency device replacement maintains personalization continuity
    Given a user "rachel_616" loses their primary smartphone
    And all their personalization data was synced to the cloud
    When they set up a new device and log in
    Then all their preferences should be restored immediately
    And their personalized feed should be identical to their lost device
    And no personalization history should be lost

  Scenario: Cross-device experiment graduation affects all devices simultaneously
    Given a user "steve_717" is participating in a personalization experiment
    And the experiment is concluded with positive results
    When the experimental feature graduates to production
    Then all their devices should receive the improved personalization
    And the transition should be seamless across devices
    And their experience should improve consistently everywhere