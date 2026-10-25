package com.taskmanager.auth.api;

import com.taskmanager.auth.config.SigningKeyConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(JwksController.class)
@Import(SigningKeyConfig.class)
class JwksControllerTest {

    @Autowired
    private WebTestClient client;

    @Test
    void returnsPublicKeyOnly() {
        client.get().uri("/.well-known/jwks.json").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.keys.length()").isEqualTo(1)
                .jsonPath("$.keys[0].kty").isEqualTo("RSA")
                .jsonPath("$.keys[0].alg").isEqualTo("RS256")
                .jsonPath("$.keys[0].kid").exists()
                .jsonPath("$.keys[0].n").exists()
                .jsonPath("$.keys[0].e").exists()
                // Private RSA parameters must never be published.
                .jsonPath("$.keys[0].d").doesNotExist()
                .jsonPath("$.keys[0].p").doesNotExist()
                .jsonPath("$.keys[0].q").doesNotExist()
                .jsonPath("$.keys[0].dp").doesNotExist()
                .jsonPath("$.keys[0].dq").doesNotExist()
                .jsonPath("$.keys[0].qi").doesNotExist();
    }

    @Test
    void rejectsPost() {
        client.post().uri("/.well-known/jwks.json").exchange()
                .expectStatus().isEqualTo(405);
    }
}
