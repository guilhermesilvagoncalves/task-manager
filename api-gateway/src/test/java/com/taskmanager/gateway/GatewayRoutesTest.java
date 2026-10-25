package com.taskmanager.gateway;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayRoutesTest {

    @Autowired
    private WebTestClient client;

    @ParameterizedTest
    @ValueSource(strings = {"/api/tasks", "/api/me", "/api/me/taskss", "/api/ping", "/.well-known/jwks.json",
            "/swagger-ui/index.html", "/webjars/swagger-ui/missing.js"})
    void pathWithoutRouteReturnsNotFoundProblem(String path) {
        client.get().uri(path).exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.title").isEqualTo("Not Found")
                .jsonPath("$.instance").isEqualTo(path);
    }
}
