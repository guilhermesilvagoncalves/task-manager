package com.taskmanager.task;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@WebFluxTest(PingController.class)
class PingControllerTest {

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private DatabaseVersionRepository repository;

    @Test
    void returnsStatusAndDatabaseVersion() {
        when(repository.version()).thenReturn(Mono.just("PostgreSQL 18.6"));

        client.get().uri("/api/ping").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok")
                .jsonPath("$.db").isEqualTo("PostgreSQL 18.6");
    }

    @Test
    void rejectsPost() {
        client.post().uri("/api/ping").exchange()
                .expectStatus().isEqualTo(405);
    }

    @Test
    void returnsServiceUnavailableWhenDatabaseFails() {
        when(repository.version()).thenReturn(Mono.error(new DataAccessResourceFailureException("down")));

        client.get().uri("/api/ping").exchange()
                .expectStatus().isEqualTo(503);
    }
}
