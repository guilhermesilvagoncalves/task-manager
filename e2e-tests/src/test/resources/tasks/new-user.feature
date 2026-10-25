@ignore
Feature: Register and log in a fresh user; returns its Authorization header value as `token`

  Scenario:
    * url baseUrl
    * def email = 'user-' + java.util.UUID.randomUUID() + '@example.com'

    Given path '/api/auth/register'
    And request { email: '#(email)', password: 'password123' }
    When method post
    Then status 201

    Given path '/api/auth/login'
    And request { email: '#(email)', password: 'password123' }
    When method post
    Then status 200
    * def token = 'Bearer ' + response.accessToken
