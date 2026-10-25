@ignore
Feature: Create a task with the given `token` and `title`

  Scenario:
    * url baseUrl
    Given path '/api/me/tasks'
    And header Authorization = token
    And request { title: '#(title)', description: 'Description', dueDate: '2026-11-01' }
    When method post
    Then status 201
