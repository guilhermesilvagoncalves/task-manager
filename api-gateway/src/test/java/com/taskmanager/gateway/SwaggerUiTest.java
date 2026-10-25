package com.taskmanager.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SwaggerUiTest {

    private static final Pattern ASSET = Pattern.compile("(?:src|href)=\"(/webjars/[^\"]+)\"");

    @Autowired
    private WebTestClient client;

    @Test
    void pageListsBothServiceDocuments() {
        assertThat(page())
                .contains("{name: \"auth\", url: \"/api-docs/auth\"}")
                .contains("{name: \"tasks\", url: \"/api-docs/tasks\"}");
    }

    /** Guards webjar upgrades: every asset the page references must be served. */
    @Test
    void everyAssetThePageReferencesIsServed() {
        List<String> assets = ASSET.matcher(page()).results().map(m -> m.group(1)).toList();

        assertThat(assets).isNotEmpty();
        assets.forEach(asset -> client.head().uri(asset).exchange().expectStatus().isOk());
    }

    @Test
    void gatewayPublishesNoDocumentOfItsOwn() {
        client.get().uri("/v3/api-docs").exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    private String page() {
        return client.get().uri("/swagger-ui.html").exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class).returnResult().getResponseBody();
    }
}
