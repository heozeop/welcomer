Feature: Mobile-Specific Personalization for Feed Content

  Background:
    Given the mobile personalization system is configured
    And we have a diverse content repository with mobile-optimized content
    And we have various mobile device profiles

  Scenario: Small screen mobile device receives optimized content layout
    Given a user with a "small_screen_phone" device profile
    And the user has normal bandwidth connectivity
    When they request their personalized feed
    Then the feed should prioritize text and image content over videos
    And content should be formatted for small screen consumption
    And images should be optimized for mobile viewing
    And the feed should contain easily readable content

  Scenario: Tablet device receives medium-format content
    Given a user with a "tablet" device profile  
    And the user has wifi connectivity
    When they request their personalized feed
    Then the feed should include a mix of text, image, and video content
    And content should be formatted for medium screen consumption
    And videos should be optimized for tablet viewing
    And the layout should utilize the larger screen real estate

  Scenario: Low bandwidth mobile user receives data-optimized content
    Given a user with a "smartphone" device profile
    And the user has "low_bandwidth" connectivity
    When they request their personalized feed
    Then the feed should prioritize text content over media
    And images should be compressed for faster loading
    And videos should be excluded or heavily compressed
    And content should load quickly despite bandwidth limitations

  Scenario: High bandwidth mobile user receives rich media content
    Given a user with a "flagship_phone" device profile
    And the user has "high_bandwidth" connectivity
    When they request their personalized feed
    Then the feed should include rich media content
    And videos should be high quality and auto-playable
    And images should be high resolution
    And interactive content should be prioritized

  Scenario: Mobile user in portrait orientation gets vertical content
    Given a user with a "smartphone" device profile
    And the device is in "portrait" orientation
    When they request their personalized feed
    Then content should be optimized for vertical scrolling
    And images should be formatted for portrait viewing
    And videos should prioritize vertical or square formats
    And text content should be formatted for narrow screens

  Scenario: Mobile user in landscape orientation gets horizontal content
    Given a user with a "smartphone" device profile
    And the device is in "landscape" orientation
    When they request their personalized feed
    Then content should be optimized for horizontal viewing
    And images should utilize the wider screen space
    And videos should be prioritized over text content
    And the layout should accommodate landscape viewing

  Scenario: Mobile user with touch interface gets interaction-friendly content
    Given a user with a "touchscreen_phone" device profile
    When they request their personalized feed
    Then interactive elements should be sized for finger taps
    And swipeable content should be prioritized
    And touch-friendly controls should be included
    And gesture-based interactions should be enabled

  Scenario: Mobile user during commute gets quick-consumption content
    Given a user with a "smartphone" device profile
    And the user context indicates "commuting" activity
    When they request their personalized feed
    Then short-form content should be prioritized
    And easily digestible content should be featured
    And content suitable for interrupted reading should be emphasized
    And long-form content should be minimized

  Scenario: Mobile user with limited storage gets lightweight content
    Given a user with a "budget_phone" device profile
    And the device has "limited_storage" available
    When they request their personalized feed
    Then content should minimize storage requirements
    And cached content should be limited
    And lightweight content formats should be prioritized
    And storage-intensive content should be avoided

  Scenario: Mobile user with older device gets performance-optimized content
    Given a user with an "older_phone" device profile
    And the device has "limited_performance" capabilities
    When they request their personalized feed
    Then content should be optimized for slower processors
    And complex interactive elements should be simplified
    And resource-intensive content should be avoided
    And performance should be prioritized over richness

  Scenario: Mobile user switches between WiFi and cellular data
    Given a user with a "smartphone" device profile
    And the user's connectivity changes from "wifi" to "cellular"
    When they continue browsing their personalized feed
    Then content delivery should adapt to the new bandwidth
    And data usage should be optimized for cellular connection
    And content quality should adjust automatically
    And the user should receive appropriate notifications about data usage

  Scenario: Mobile user with accessibility needs gets mobile-accessible content
    Given a user with a "smartphone" device profile
    And the user has "accessibility_needs" enabled
    When they request their personalized feed
    Then content should be screen reader compatible
    And touch targets should be larger for motor accessibility
    And high contrast options should be available
    And audio descriptions should be provided for visual content

  Scenario: Mobile user in bright sunlight gets high contrast content
    Given a user with a "smartphone" device profile
    And the ambient light condition is "bright_sunlight"
    When they request their personalized feed
    Then content should use high contrast colors
    And text should be bold and easily readable
    And bright backgrounds should be avoided
    And content should be optimized for outdoor viewing

  Scenario: Mobile user with battery saving mode gets energy-efficient content
    Given a user with a "smartphone" device profile
    And the device is in "battery_saving_mode"
    When they request their personalized feed
    Then power-intensive content should be minimized
    And dark mode should be enabled when possible
    And auto-playing videos should be disabled
    And refresh rates should be optimized for battery conservation

  Scenario: Mobile user multitasking gets quick-access content
    Given a user with a "smartphone" device profile
    And the user is "multitasking" between apps
    When they briefly access their personalized feed
    Then the most relevant content should load first
    And quick preview options should be available
    And content should be easily dismissible
    And session resumption should be seamless

  Scenario: Mobile user with slow device gets progressive content loading
    Given a user with a "slow_phone" device profile
    When they request their personalized feed
    Then content should load progressively
    And essential content should appear first
    And images should load after text content
    And the user should see immediate feedback during loading

  Scenario: Mobile user preferences override device capabilities
    Given a user with a "basic_phone" device profile
    But the user has explicitly set preferences for "rich_media" content
    When they request their personalized feed
    Then user preferences should be respected within device limitations
    And appropriate warnings about performance impact should be shown
    And fallback options should be provided
    And the user should maintain control over their experience

  Scenario: Mobile user gets location-aware content on mobile
    Given a user with a "smartphone" device profile
    And location services are enabled
    And the user is in a "urban" environment
    When they request their personalized feed
    Then location-relevant content should be prioritized
    And local events and news should be featured
    And content should respect location privacy settings
    And urban-relevant content should be emphasized

  Scenario: Mobile user app switching maintains personalization state
    Given a user with a "smartphone" device profile
    And they have been browsing their personalized feed
    When they switch to another app and return
    Then their personalization state should be maintained
    And their reading position should be preserved
    And new content should be appropriately integrated
    And the user experience should be seamless

  Scenario: Mobile device performance monitoring affects content delivery
    Given a user with a "smartphone" device profile
    And the system monitors device performance in real-time
    When device performance degrades during feed browsing
    Then content complexity should be automatically reduced
    And the user should be notified of the adaptation
    And performance should be continuously monitored
    And content should adapt dynamically to performance changes