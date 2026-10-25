package com.taskmanager.auth.api;

import com.taskmanager.auth.domain.User;
import com.taskmanager.auth.security.AccessToken;
import com.taskmanager.auth.service.AuthService;
import com.taskmanager.auth.service.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;

@WebFluxTest(AuthController.class)
class AuthControllerTest {

    private static final MediaType PROBLEM = MediaType.APPLICATION_PROBLEM_JSON;

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private AuthService service;

    @Test
    void registerReturnsCreatedUserWithoutPassword() {
        UUID id = UUID.randomUUID();
        when(service.register("john@example.com", "password123"))
                .thenReturn(Mono.just(new User(id, "john@example.com", "hash", Instant.parse("2026-10-24T10:00:00Z"))));

        post("/api/auth/register", "{\"email\":\"john@example.com\",\"password\":\"password123\"}")
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id.toString())
                .jsonPath("$.email").isEqualTo("john@example.com")
                .jsonPath("$.createdAt").isEqualTo("2026-10-24T10:00:00Z")
                .jsonPath("$.password").doesNotExist()
                .jsonPath("$.passwordHash").doesNotExist();
    }

    @Test
    void registerRejectsInvalidFieldsWithErrorPerField() {
        post("/api/auth/register", "{\"email\":\"not-an-email\",\"password\":\"short\"}")
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(PROBLEM)
                .expectBody()
                .jsonPath("$.errors.email").exists()
                .jsonPath("$.errors.password").exists();
    }

    @Test
    void registerRejectsPasswordLongerThan72Characters() {
        post("/api/auth/register", "{\"email\":\"john@example.com\",\"password\":\"" + "a".repeat(73) + "\"}")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors.password").exists();
    }

    @Test
    void registerRejectsPasswordLongerThan72Bytes() {
        post("/api/auth/register", "{\"email\":\"john@example.com\",\"password\":\"" + "é".repeat(72) + "\"}")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors.password").exists();
    }

    @Test
    void registerRejectsMissingFields() {
        post("/api/auth/register", "{}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.email").exists()
                .jsonPath("$.errors.password").exists();
    }

    @Test
    void registerRejectsMalformedJson() {
        post("/api/auth/register", "{\"email\":")
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(PROBLEM);
    }

    @Test
    void registerRejectsMissingBody() {
        client.post().uri("/api/auth/register").contentType(MediaType.APPLICATION_JSON).exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(PROBLEM);
    }

    @Test
    void loginReturnsBearerToken() {
        when(service.login("john@example.com", "password123")).thenReturn(Mono.just(new AccessToken("jwt", 3600)));

        post("/api/auth/login", "{\"email\":\"john@example.com\",\"password\":\"password123\"}")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("jwt")
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.expiresIn").isEqualTo(3600);
    }

    @Test
    void loginWithInvalidCredentialsReturnsUnauthorized() {
        when(service.login("john@example.com", "wrong-password")).thenReturn(Mono.error(new InvalidCredentialsException()));

        post("/api/auth/login", "{\"email\":\"john@example.com\",\"password\":\"wrong-password\"}")
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(PROBLEM)
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.detail").isEqualTo("Invalid email or password");
    }

    @Test
    void loginRejectsBlankFields() {
        post("/api/auth/login", "{\"email\":\"\",\"password\":\"\"}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.email").exists()
                .jsonPath("$.errors.password").exists();
    }

    @Test
    void unsupportedMethodReturnsMethodNotAllowed() {
        client.get().uri("/api/auth/login").exchange()
                .expectStatus().isEqualTo(405)
                .expectHeader().contentTypeCompatibleWith(PROBLEM);
    }

    @Test
    void unknownRouteReturnsNotFound() {
        client.get().uri("/api/auth/unknown").exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(PROBLEM)
                .expectBody().jsonPath("$.status").isEqualTo(404);
    }

    private WebTestClient.ResponseSpec post(String uri, String body) {
        return client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
    }
}
