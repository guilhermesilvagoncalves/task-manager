Feature: Authentication through the gateway

  Background:
    * url baseUrl
    * def email = 'user-' + java.util.UUID.randomUUID() + '@example.com'
    * def password = 'password123'

  Scenario: register, then log in with any letter case
    Given path '/api/auth/register'
    And request { email: '#(email)', password: '#(password)' }
    When method post
    Then status 201
    And match response == { id: '#uuid', email: '#(email)', createdAt: '#string' }

    Given path '/api/auth/login'
    And request { email: '#(email.toUpperCase())', password: '#(password)' }
    When method post
    Then status 200
    And match response == { accessToken: '#string', tokenType: 'Bearer', expiresIn: 3600 }

  Scenario: registering an existing email (other case) returns 201 and keeps the original password
    Given path '/api/auth/register'
    And request { email: '#(email)', password: '#(password)' }
    When method post
    Then status 201

    Given path '/api/auth/register'
    And request { email: '#(email.toUpperCase())', password: 'another-password' }
    When method post
    Then status 201
    And match response.id == '#uuid'

    Given path '/api/auth/login'
    And request { email: '#(email)', password: 'another-password' }
    When method post
    Then status 401

  Scenario: invalid register payload returns 400 with errors per field
    Given path '/api/auth/register'
    And request { email: 'not-an-email', password: 'short' }
    When method post
    Then status 400
    And match responseHeaders['Content-Type'][0] contains 'application/problem+json'
    And match response.errors == { email: '#string', password: '#string' }

  Scenario: wrong password and unknown email get the same 401 body
    Given path '/api/auth/register'
    And request { email: '#(email)', password: '#(password)' }
    When method post
    Then status 201

    Given path '/api/auth/login'
    And request { email: '#(email)', password: 'wrong-password' }
    When method post
    Then status 401
    * def wrongPassword = response

    Given path '/api/auth/login'
    And request { email: 'unknown-' + email, password: 'wrong-password' }
    When method post
    Then status 401
    * def unknownEmail = response
    And match unknownEmail == wrongPassword

  Scenario: unsupported method and unknown route
    Given path '/api/auth/login'
    When method get
    Then status 405

    Given path '/api/auth/unknown'
    When method get
    Then status 404

  Scenario: JWKS is not exposed through the gateway
    Given path '/.well-known/jwks.json'
    When method get
    Then status 404
