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

  Scenario: Swagger UI is served by the gateway and lists both documents
    Given path '/swagger-ui.html'
    When method get
    Then status 200
    And match response contains '/api-docs/auth'
    And match response contains '/api-docs/tasks'
    Given path '/webjars/swagger-ui/swagger-ui-bundle.js'
    When method head
    Then status 200
