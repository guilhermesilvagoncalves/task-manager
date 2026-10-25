package com.taskmanager.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webflux.core.configuration.SpringDocWebFluxConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest({AuthController.class, JwksController.class})
@Import({SigningKeyConfig.class, AuthServiceApplication.class})
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocConfigProperties.class,
        SpringDocWebFluxConfiguration.class})
class OpenApiDocumentTest {

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private AuthService service;

    @ParameterizedTest
    @CsvSource({
            "/api/auth/register, 201, UserResponse, '400'",
            "/api/auth/login, 200, TokenResponse, '400,401'"})
    void operationDocumentsSuccessAndProblemResponses(String path, String success, String schema, String errors) {
        String operation = "$.paths['" + path + "'].post";
        WebTestClient.BodyContentSpec body = apiDocs();
        body.jsonPath(operation + ".responses['" + success + "'].content['application/json'].schema.$ref")
                .isEqualTo("#/components/schemas/" + schema);
        for (String error : errors.split(",")) {
            body.jsonPath(operation + ".responses['" + error + "'].content['application/problem+json'].schema.$ref")
                    .isEqualTo("#/components/schemas/Problem");
        }
        body.jsonPath(operation + ".responses.length()").isEqualTo(1 + errors.split(",").length);
    }

    @Test
    void internalJwksEndpointIsNotDocumented() {
        apiDocs().jsonPath("$.paths['/.well-known/jwks.json']").doesNotExist();
    }

    @Test
    void schemasMatchSpecResponseShapes() {
        apiDocs()
                .jsonPath("$.components.schemas.UserResponse.properties.length()").isEqualTo(3)
                .jsonPath("$.components.schemas.TokenResponse.properties.accessToken").exists()
                .jsonPath("$.components.schemas.TokenResponse.properties.tokenType").exists()
                .jsonPath("$.components.schemas.TokenResponse.properties.expiresIn").exists()
                .jsonPath("$.components.schemas.Problem.properties.status").exists()
                .jsonPath("$.components.schemas.Problem.properties.errors.additionalProperties.type")
                .isEqualTo("string");
    }

    @Test
    void serverIsRelativeSoPathsWorkThroughTheGateway() {
        apiDocs().jsonPath("$.servers[0].url").isEqualTo("/");
    }

    private WebTestClient.BodyContentSpec apiDocs() {
        return client.get().uri("/v3/api-docs").exchange()
                .expectStatus().isOk()
                .expectBody();
    }
}
