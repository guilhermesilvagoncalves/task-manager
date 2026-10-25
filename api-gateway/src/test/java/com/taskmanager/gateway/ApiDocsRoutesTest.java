package com.taskmanager.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ApiDocsRoutesTest {

    private static final HttpServer AUTH = stub("auth");
    private static final HttpServer TASKS = stub("tasks");

    @Autowired
    private WebTestClient client;

    @DynamicPropertySource
    static void serviceUrls(DynamicPropertyRegistry registry) {
        registry.add("AUTH_SERVICE_URL", () -> "http://localhost:" + AUTH.getAddress().getPort());
        registry.add("TASK_SERVICE_URL", () -> "http://localhost:" + TASKS.getAddress().getPort());
    }

    @AfterAll
    static void stopStubs() {
        AUTH.stop(0);
        TASKS.stop(0);
    }

    @ParameterizedTest
    @CsvSource({"/api-docs/auth, auth", "/api-docs/tasks, tasks"})
    void routesToTheServiceOpenApiDocument(String path, String service) {
        client.get().uri(path).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo(service + " /v3/api-docs");
    }

    /** Answers every request with its own name and the path it received. */
    private static HttpServer stub(String name) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                byte[] body = (name + " " + exchange.getRequestURI().getPath()).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
