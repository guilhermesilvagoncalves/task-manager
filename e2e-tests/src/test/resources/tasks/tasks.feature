Feature: Tasks of the authenticated user through the gateway

  Background:
    * url baseUrl
    * def alice = call read('classpath:tasks/new-user.feature')
    * def task = { title: 'Buy milk', description: 'Two litres', dueDate: '2026-11-01' }
    * def repeat = function(s, n) { var r = ''; for (var i = 0; i < n; i++) r += s; return r }
    * def emoji = '😀'
    * def taskShape = { id: '#uuid', title: '#string', description: '#string', status: '#string', dueDate: '#string', createdAt: '#string', updatedAt: '#string' }

  Scenario: no token or a malformed token returns 401 problem
    Given path '/api/me/tasks'
    When method get
    Then status 401
    And match responseHeaders['Content-Type'][0] contains 'application/problem+json'
    And match response.status == 401

    Given path '/api/me/tasks'
    And header Authorization = 'Bearer not-a-jwt'
    When method get
    Then status 401

  Scenario: create, read, update and delete a task
    Given path '/api/me/tasks'
    And header Authorization = alice.token
    And request task
    When method post
    Then status 201
    And match response == taskShape
    And match response.status == 'TODO'
    * def created = response
    And match responseHeaders['Location'][0] == '/api/me/tasks/' + created.id

    Given path '/api/me/tasks', created.id
    And header Authorization = alice.token
    When method get
    Then status 200
    And match response == created

    Given path '/api/me/tasks', created.id
    And header Authorization = alice.token
    And request { title: 'Buy oat milk', description: 'One litre', status: 'DONE', dueDate: '2020-01-01' }
    When method put
    Then status 200
    And match response contains { id: '#(created.id)', title: 'Buy oat milk', status: 'DONE', dueDate: '2020-01-01' }
    And match response.createdAt == created.createdAt
    And match response.updatedAt != created.updatedAt
    * def updated = response

    # A past due date can be kept on update.
    Given path '/api/me/tasks', created.id
    And header Authorization = alice.token
    And request { title: 'Buy oat milk', description: 'One litre', status: 'IN_PROGRESS', dueDate: '2020-01-01' }
    When method put
    Then status 200

    Given path '/api/me/tasks', created.id
    And header Authorization = alice.token
    When method delete
    Then status 204

    Given path '/api/me/tasks', created.id
    And header Authorization = alice.token
    When method get
    Then status 404
    And match response.status == 404

  Scenario: invalid payloads return 400 with errors per field
    Given path '/api/me/tasks'
    And header Authorization = alice.token
    And request { title: '  ', owner: 'someone' }
    When method post
    Then status 400
    And match responseHeaders['Content-Type'][0] contains 'application/problem+json'
    And match response.errors == { title: '#string', description: '#string', dueDate: '#string' }

    Given path '/api/me/tasks'
    And header Authorization = alice.token
    And request { title: '#(repeat("a", 101))', description: '#(repeat("b", 501))', dueDate: '2026-11-01' }
    When method post
    Then status 400
    And match response.errors == { title: '#string', description: '#string' }

    Given path '/api/me/tasks'
    And header Authorization = alice.token
    And request { title: 'Title', description: 'Description', status: 'OPEN', dueDate: '2026-11-01' }
    When method post
    Then status 400

    Given path '/api/me/tasks'
    And header Authorization = alice.token
    And request { title: 'Title', description: 'Description', dueDate: '2026-02-30' }
    When method post
    Then status 400

  Scenario: values at the limit (in code points) are persisted unchanged
    * def title = repeat(emoji, 100)
    * def description = repeat(emoji, 500)
    Given path '/api/me/tasks'
    And header Authorization = alice.token
    And request { title: '#(title)', description: '#(description)', dueDate: '2026-11-01' }
    When method post
    Then status 201
    * def id = response.id

    Given path '/api/me/tasks', id
    And header Authorization = alice.token
    When method get
    Then status 200
    And match response.title == title
    And match response.description == description

    Given path '/api/me/tasks'
    And header Authorization = alice.token
    And request { title: '#(repeat(emoji, 101))', description: 'Description', dueDate: '2026-11-01' }
    When method post
    Then status 400

  Scenario: list is paginated newest first, 10 per page, without duplicates
    Given path '/api/me/tasks'
    And header Authorization = alice.token
    When method get
    Then status 200
    And match response == { items: [], page: 0, size: 10, totalItems: 0, totalPages: 0 }

    * def titles = []
    * eval for (var i = 1; i <= 12; i++) titles.push('Task ' + i)
    * def create = function(title) { return karate.call('classpath:tasks/create-task.feature', { token: alice.token, title: title }).response.id }
    * def ids = karate.map(titles, create)

    Given path '/api/me/tasks'
    And header Authorization = alice.token
    When method get
    Then status 200
    And match response contains { page: 0, size: 10, totalItems: 12, totalPages: 2 }
    And match response.items == '#[10]'
    And match response.items[0].title == 'Task 12'
    * def first = response.items

    Given path '/api/me/tasks'
    And header Authorization = alice.token
    And param page = 1
    When method get
    Then status 200
    And match response.items == '#[2]'
    And match response.items[1].title == 'Task 1'
    * def seen = karate.map(first.concat(response.items), function(t) { return t.id })
    And match seen contains only ids

    Given path '/api/me/tasks'
    And header Authorization = alice.token
    And param page = 5
    When method get
    Then status 200
    And match response.items == []

    Given path '/api/me/tasks'
    And header Authorization = alice.token
    And param page = -1
    When method get
    Then status 400

  Scenario: another user can neither see nor change the task
    * def bob = call read('classpath:tasks/new-user.feature')
    Given path '/api/me/tasks'
    And header Authorization = alice.token
    And request task
    When method post
    Then status 201
    * def created = response

    Given path '/api/me/tasks', created.id
    And header Authorization = bob.token
    When method get
    Then status 404

    Given path '/api/me/tasks', created.id
    And header Authorization = bob.token
    And request { title: 'Hacked', description: 'Hacked', status: 'DONE', dueDate: '2026-11-01' }
    When method put
    Then status 404

    Given path '/api/me/tasks', created.id
    And header Authorization = bob.token
    When method delete
    Then status 404

    Given path '/api/me/tasks'
    And header Authorization = bob.token
    When method get
    Then status 200
    And match response.totalItems == 0

    Given path '/api/me/tasks', created.id
    And header Authorization = alice.token
    When method get
    Then status 200
    And match response == created

  Scenario: unknown routes and unsupported methods
    Given path '/api/tasks'
    And header Authorization = alice.token
    When method get
    Then status 404
    And match responseHeaders['Content-Type'][0] contains 'application/problem+json'

    Given path '/api/me/tasks/' + java.util.UUID.randomUUID() + '/unknown'
    And header Authorization = alice.token
    When method get
    Then status 404

    Given path '/api/me/tasks', java.util.UUID.randomUUID()
    And header Authorization = alice.token
    And request {}
    When method patch
    Then status 405

    Given path '/api/me/tasks/not-a-uuid'
    And header Authorization = alice.token
    When method get
    Then status 400
