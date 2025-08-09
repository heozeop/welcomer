Feature: API Endpoint Testing for Feed Personalization
  As an API consumer
  I want to interact with the feed personalization endpoints correctly
  So that I can retrieve personalized content via REST API calls

  Background:
    Given the API is available and accessible
    And I have a valid authentication token
    And test data is available for API testing

  Scenario: Successfully retrieve personalized home feed
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/feed/home"
    Then the response status should be 200
    And the response should have content type "application/json"
    And the response body should contain a "items" array
    And the response body should contain "meta" information
    And the response body should contain "pagination" information
    And each item in the feed should have required fields
    And the feed should be personalized for the user

  Scenario: Retrieve home feed with pagination parameters
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/feed/home" with parameters:
      | limit | 25  |
    Then the response status should be 200
    And the response body should contain at most 25 items
    And the response should include pagination metadata
    And the response should include a "nextCursor" if more items exist

  Scenario: Retrieve feed with content type filtering
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/feed/home" with parameters:
      | contentType | video |
    Then the response status should be 200
    And all returned items should have content type "VIDEO"
    And the response should indicate content filtering was applied

  Scenario: Retrieve feed with topic filtering
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/feed/home" with parameters:
      | topic | technology |
    Then the response status should be 200
    And all returned items should be tagged with topic "technology"
    And the response should indicate topic filtering was applied

  Scenario: Retrieve feed with multiple filters
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/feed/home" with parameters:
      | contentType | article    |
      | topic       | science    |
      | sort        | latest     |
    Then the response status should be 200
    And all returned items should have content type "TEXT"
    And all returned items should be tagged with topic "science"
    And items should be sorted by latest creation time
    And the response should indicate multiple filters were applied

  Scenario: Retrieve feed with cursor-based pagination
    Given I am authenticated as user "user123"
    And I have obtained a cursor from a previous feed request
    When I send a GET request to "/api/v1/feed/home" with parameters:
      | cursor | previous_cursor_token |
      | limit  | 10                   |
    Then the response status should be 200
    And the response should contain the next page of results
    And the response should include a new cursor for further pagination
    And items should not overlap with the previous page

  Scenario: Force refresh cached feed
    Given I am authenticated as user "user123"
    And there is cached feed data for the user
    When I send a GET request to "/api/v1/feed/home" with parameters:
      | refresh | true |
    Then the response status should be 200
    And the response should contain fresh feed data
    And the response metadata should indicate cache was bypassed
    And the response should include updated timestamps

  Scenario: Test different feed types
    Given I am authenticated as user "user123"
    When I send GET requests to all feed type endpoints:
      | /api/v1/feed/home        |
      | /api/v1/feed/following   |
      | /api/v1/feed/explore     |
      | /api/v1/feed/trending    |
      | /api/v1/feed/personalized |
    Then all responses should have status 200
    And each feed should have content appropriate to its type
    And following feed should only show content from followed users
    And trending feed should show currently popular content
    And explore feed should show discovery content

  Scenario: Retrieve available feed types
    When I send a GET request to "/api/v1/feed/types"
    Then the response status should be 200
    And the response should contain a list of available feed types
    And each feed type should have a name, display name, and description
    And the list should include HOME, FOLLOWING, EXPLORE, TRENDING, and PERSONALIZED

  Scenario: Get feed refresh since specific timestamp
    Given I am authenticated as user "user123"
    And there is existing feed data with timestamps
    When I send a GET request to "/api/v1/feed/home/refresh" with parameters:
      | since | 2024-01-01T10:00:00Z |
      | limit | 15                   |
    Then the response status should be 200
    And the response should only contain content published after the timestamp
    And the response should have at most 15 items
    And all items should have timestamps after the specified time

  Scenario: Invalidate feed cache via POST
    Given I am authenticated as user "user123"
    When I send a POST request to "/api/v1/feed/home/refresh"
    Then the response status should be 200
    And the response should indicate successful cache invalidation
    And the response should include a success message
    And subsequent feed requests should generate fresh content

  Scenario: Handle invalid feed type parameter
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/feed/invalid_feed_type"
    Then the response status should be 400
    And the response should contain an error message about invalid feed type
    And the response should list valid feed types
    And the response should have proper error structure

  Scenario: Handle excessive limit parameter
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/feed/home" with parameters:
      | limit | 500 |
    Then the response status should be 200
    And the response should contain at most 100 items
    And the response should indicate limit was capped at maximum

  Scenario: Handle invalid cursor parameter
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/feed/home" with parameters:
      | cursor | invalid_cursor_token |
    Then the response status should be 400
    And the response should contain an error message about invalid cursor
    And the response should suggest using a valid cursor or omitting the parameter

  Scenario: Handle unauthenticated request
    Given I am not authenticated
    When I send a GET request to "/api/v1/feed/home"
    Then the response status should be 401
    And the response should contain an authentication error message
    And the response should indicate user authentication is required

  Scenario: Test API performance under normal load
    Given I am authenticated as user "user123"
    When I send 10 concurrent GET requests to "/api/v1/feed/home"
    Then all responses should have status 200
    And all response times should be under 500 milliseconds
    And all responses should contain valid feed data
    And no requests should fail due to concurrency

  Scenario: Test user preferences API endpoint
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/users/user123/preferences"
    Then the response status should be 200
    And the response should contain user preference data
    And the response should include topic interests
    And the response should include content type preferences
    And the response should include confidence scores

  Scenario: Test user preferences with type parameter
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/users/user123/preferences" with parameters:
      | type | explicit |
    Then the response status should be 200
    And the response should contain only explicit preferences
    And the response should not contain implicit or derived preferences

  Scenario: Test user preferences access control
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/users/user456/preferences"
    Then the response status should be 403
    And the response should contain an access denied error message
    And the response should not reveal other user's preference data

  Scenario: Test feed endpoint with algorithm selection
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/feed/personalized" with parameters:
      | algorithmId | collaborative_filtering |
    Then the response status should be 200
    And the response metadata should indicate the algorithm used
    And the feed should be generated using collaborative filtering
    And the response should include algorithm confidence scores

  Scenario: Test feed endpoint response structure validation
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/feed/home"
    Then the response status should be 200
    And the response should have valid JSON structure
    And the response should contain required top-level fields:
      | items      |
      | meta       |
      | pagination |
    And each feed item should contain required fields:
      | id          |
      | authorId    |
      | contentType |
      | createdAt   |
      | topics      |
    And all timestamps should be in ISO format
    And all IDs should be non-empty strings

  Scenario: Test feed endpoint response headers
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/feed/home"
    Then the response status should be 200
    And the response should have required headers:
      | Content-Type     | application/json |
      | Cache-Control    | present          |
      | X-RateLimit-*    | present          |
    And the response should include CORS headers if needed
    And the response should include appropriate cache directives

  Scenario: Test feed endpoint with invalid timestamp format
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/feed/home/refresh" with parameters:
      | since | invalid-timestamp |
    Then the response status should be 400
    And the response should contain an error about invalid timestamp format
    And the response should suggest using ISO timestamp format

  Scenario: Test feed endpoint error handling and recovery
    Given I am authenticated as user "user123"
    And the personalization service is temporarily unavailable
    When I send a GET request to "/api/v1/feed/home"
    Then the response status should be 500
    And the response should contain a generic error message
    And the response should not expose internal system details
    And the error should be properly logged for debugging

  Scenario: Test feed endpoint with empty results
    Given I am authenticated as user "new_user_with_no_content"
    When I send a GET request to "/api/v1/feed/home"
    Then the response status should be 200
    And the response should contain an empty items array
    And the response should include appropriate metadata for empty results
    And the response should suggest actions for new users

  Scenario: Test feed endpoint rate limiting
    Given I am authenticated as user "user123"
    When I send 100 requests rapidly to "/api/v1/feed/home"
    Then most responses should have status 200
    And some responses should have status 429 when rate limit is exceeded
    And rate limited responses should include Retry-After header
    And rate limited responses should explain the rate limit policy

  Scenario: Test feed endpoint with special characters in parameters
    Given I am authenticated as user "user123"
    When I send a GET request to "/api/v1/feed/home" with parameters:
      | topic | "technology & programming" |
    Then the response status should be 200
    And the topic parameter should be properly URL decoded
    And the response should handle special characters correctly
    And no security vulnerabilities should be exposed

  Scenario: Test feed endpoint CORS support
    Given I am making a request from a web browser
    When I send an OPTIONS request to "/api/v1/feed/home"
    Then the response should include CORS headers
    And the response should allow GET method
    And the response should handle preflight requests correctly
    And cross-origin requests should be properly supported