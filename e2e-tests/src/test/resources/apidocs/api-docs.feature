Feature: OpenAPI documents through the gateway

  Background:
    * url baseUrl

  Scenario Outline: <path> is public and documents the service paths
    Given path '<path>'
    When method get
    Then status 200
    And match response.info.title == '<title>'
    And match response.servers[0].url == '/'
    And match response.paths contains { '<resource>': '#object' }

    Examples:
      | path            | title        | resource           |
      | /api-docs/auth  | auth-service | /api/auth/register |
      | /api-docs/tasks | task-service | /api/me/tasks      |
