package com.taskmanager.e2e;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Karate has no simple way to fire two requests at once, so this flow uses the JDK client. */
class ConcurrentRegisterTest {

    private final String baseUrl = System.getProperty("gatewayUrl", "http://localhost:8080");
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void simultaneousRegistrationsBothReturnCreatedAndStoreOneUser() {
        String email = "concurrent-" + UUID.randomUUID() + "@example.com";

        List<Integer> statuses = List.of("password-one", "password-two").stream()
                .map(password -> post("/api/auth/register", email, password))
                .toList().stream()
                .map(CompletableFuture::join)
                .map(HttpResponse::statusCode)
                .toList();

        assertEquals(List.of(201, 201), statuses);
        // Exactly one of the two passwords was stored.
        long loggedIn = List.of("password-one", "password-two").stream()
                .map(password -> post("/api/auth/login", email, password).join().statusCode())
                .filter(status -> status == 200)
                .count();
        assertEquals(1, loggedIn);
    }

    private CompletableFuture<HttpResponse<String>> post(String path, String email, String password) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
