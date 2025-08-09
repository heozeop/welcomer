Feature: Contextual Feed Personalization
  As a user accessing the platform at different times and locations
  I want to receive content that is contextually relevant to my current situation
  So that the feed adapts to my temporal and geographic context

  Background:
    Given the personalization service is available
    And content fixtures are loaded with diverse topics
    And contextual content fixtures are available

  Scenario: Morning feed prioritizes energizing and informative content
    Given I am a user accessing the platform at "6:30 AM"
    And I am located in "US/Pacific" timezone
    When I request my personalized feed
    Then I should receive morning-appropriate content
    And news and current events should be prioritized
    And motivational and educational content should be included
    And entertainment content should be minimal
    And the content should have an energizing tone

  Scenario: Evening feed emphasizes relaxation and entertainment
    Given I am a user accessing the platform at "8:00 PM"
    And I am located in "US/Eastern" timezone
    When I request my personalized feed
    Then I should receive evening-appropriate content
    And entertainment and lifestyle content should be prioritized
    And relaxing content should be featured
    And work-related content should be minimal
    And the content should have a wind-down tone

  Scenario: Lunch-time feed provides quick consumption content
    Given I am a user accessing the platform at "12:30 PM"
    And I am located in "Europe/London" timezone
    And I have a "mobile" device
    When I request my personalized feed
    Then I should receive lunch-break appropriate content
    And quick-read articles should be prioritized
    And bite-sized content should be featured
    And long-form content should be minimal
    And the content should be easily consumable in 5-10 minutes

  Scenario: Weekend morning feed differs from weekday morning
    Given I am a user accessing the platform on "Saturday at 9:00 AM"
    And I am located in "US/Central" timezone
    When I request my personalized feed
    Then I should receive weekend morning content
    And leisure and hobby content should be prioritized
    And work-related content should be reduced
    And lifestyle and personal interest content should be featured
    And the pace should be more relaxed than weekday content

  Scenario: Location-based content personalization for local relevance
    Given I am a user located in "New York City, NY"
    And I have location-based preferences enabled
    When I request my personalized feed
    Then I should receive location-relevant content
    And local news and events should be included
    And content from local sources should be prioritized
    And time zone appropriate content should be delivered
    And weather-relevant content should be considered

  Scenario: Cultural context adaptation for different regions
    Given I am a user located in "Tokyo, Japan"
    And my language preference is set to "English"
    When I request my personalized feed
    Then I should receive culturally appropriate content
    And international perspective content should be prioritized
    And Asia-Pacific relevant topics should be featured
    And time zone appropriate delivery should be respected
    And cultural sensitivity should be maintained

  Scenario: Business hours vs after-hours content adaptation
    Given I am a user accessing during "business hours" at "2:00 PM"
    And I am located in "US/Pacific" timezone
    And I have indicated I am "at work"
    When I request my personalized feed
    Then I should receive work-appropriate content
    And professional development content should be included
    And industry news should be prioritized
    And personal entertainment should be minimal
    And content should be suitable for workplace consumption

  Scenario: Commute time content optimization
    Given I am a user accessing during "morning commute" at "7:45 AM"
    And I am located in a "metropolitan area"
    And I have a "mobile" device
    When I request my personalized feed
    Then I should receive commute-friendly content
    And audio and video content should be prioritized
    And traffic and transit updates should be included
    And hands-free consumable content should be featured
    And content duration should match typical commute length

  Scenario: Time zone switching maintains content relevance
    Given I am a user who has traveled from "US/Pacific" to "Europe/Berlin"
    And I accessed the platform 2 hours ago in the previous timezone
    When I request my personalized feed
    Then the content should adapt to the new timezone
    And duplicate content from previous session should be minimized
    And local time-appropriate content should be prioritized
    And the feed should recognize the timezone change

  Scenario: Late night content curation for night owls
    Given I am a user accessing the platform at "1:30 AM"
    And I am located in "US/Mountain" timezone
    And I have a history of late-night usage
    When I request my personalized feed
    Then I should receive late-night appropriate content
    And global content from different timezones should be included
    And thought-provoking or creative content should be featured
    And high-energy content should be avoided
    And content should acknowledge the late hour context

  Scenario: Weather-influenced content personalization
    Given I am a user located in "Seattle, WA"
    And the current weather is "rainy"
    And the season is "winter"
    When I request my personalized feed
    Then weather-appropriate content should be included
    And indoor activity content should be prioritized
    And seasonal content should be featured
    And weather-relevant topics should be surfaced
    And mood-appropriate content should be considered

  Scenario: Holiday and special event contextual adaptation
    Given I am a user accessing on "Thanksgiving Day"
    And I am located in "United States"
    When I request my personalized feed
    Then holiday-appropriate content should be featured
    And family and gratitude themed content should be prioritized
    And cooking and recipe content should be included
    And work-related content should be minimal
    And the content should reflect the holiday spirit

  Scenario: Multi-location user with travel context
    Given I am a frequent traveler currently in "London, UK"
    And my home location is "San Francisco, CA"
    And I have been in London for "3 days"
    When I request my personalized feed
    Then I should receive travel-contextualized content
    And London-specific content should be included
    And travel tips and local recommendations should be featured
    And content bridging my home and current location should appear
    And timezone-appropriate delivery should be maintained

  Scenario: Workplace vs home location context switching
    Given I am a user at my "workplace" location during "business hours"
    And my workplace is in "downtown business district"
    When I request my personalized feed
    Then workplace-appropriate content should be delivered
    And professional content should be prioritized
    And lunch spot recommendations should be included
    And content should be suitable for professional environment
    And personal content should be appropriately filtered

  Scenario: Event-based temporal personalization
    Given I am a user accessing during a "major sporting event"
    And the event is "World Cup Final"
    And I am located in a country "participating in the event"
    When I request my personalized feed
    Then event-related content should be prominently featured
    And real-time updates should be prioritized
    And celebratory or commentary content should be included
    And the content should match the excitement level of the event
    And local perspective on the event should be emphasized