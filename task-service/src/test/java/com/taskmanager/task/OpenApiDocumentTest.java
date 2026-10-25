package com.taskmanager.task;

import com.taskmanager.task.api.TaskController;
import com.taskmanager.task.config.SecurityConfig;
import com.taskmanager.task.security.ProblemDetailsAuthenticationEntryPoint;
import com.taskmanager.task.service.TaskService;
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

@WebFluxTest(TaskController.class)
@Import({SecurityConfig.class, ProblemDetailsAuthenticationEntryPoint.class, TaskServiceApplication.class})
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocConfigProperties.class,
        SpringDocWebFluxConfiguration.class})
class OpenApiDocumentTest {

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private TaskService service;

    @ParameterizedTest
    @CsvSource({
            "/api/me/tasks, post, 201, TaskResponse, '400,401'",
            "/api/me/tasks, get, 200, TaskListResponse, '400,401'",
            "/api/me/tasks/{id}, get, 200, TaskResponse, '400,401,404'",
            "/api/me/tasks/{id}, put, 200, TaskResponse, '400,401,404'"})
    void operationDocumentsSuccessAndProblemResponses(String path, String method, String success, String schema,
                                                     String errors) {
        String operation = "$.paths['" + path + "']." + method;
        WebTestClient.BodyContentSpec body = apiDocs();
        body.jsonPath(operation + ".responses['" + success + "'].content['application/json'].schema.$ref")
                .isEqualTo("#/components/schemas/" + schema);
        for (String error : errors.split(",")) {
            body.jsonPath(operation + ".responses['" + error + "'].content['application/problem+json'].schema.$ref")
                    .isEqualTo("#/components/schemas/Problem");
        }
        body.jsonPath(operation + ".responses.length()").isEqualTo(1 + errors.split(",").length);
        body.jsonPath(operation + ".security[0].bearer").exists();
    }

    @Test
    void deleteDocumentsNoContentAndProblemResponses() {
        String operation = "$.paths['/api/me/tasks/{id}'].delete";
        WebTestClient.BodyContentSpec body = apiDocs();
        body.jsonPath(operation + ".responses['204']").exists();
        body.jsonPath(operation + ".responses.length()").isEqualTo(4);
        for (String error : new String[]{"400", "401", "404"}) {
            body.jsonPath(operation + ".responses['" + error + "'].content['application/problem+json'].schema.$ref")
                    .isEqualTo("#/components/schemas/Problem");
        }
        body.jsonPath(operation + ".security[0].bearer").exists();
    }

    @Test
    void bearerSchemeIsDeclared() {
        apiDocs()
                .jsonPath("$.components.securitySchemes.bearer.type").isEqualTo("http")
                .jsonPath("$.components.securitySchemes.bearer.scheme").isEqualTo("bearer")
                .jsonPath("$.components.securitySchemes.bearer.bearerFormat").isEqualTo("JWT");
    }

    @Test
    void schemasMatchSpecResponseShapes() {
        apiDocs()
                .jsonPath("$.components.schemas.TaskResponse.properties.length()").isEqualTo(7)
                .jsonPath("$.components.schemas.TaskResponse.properties.status.enum.length()").isEqualTo(3)
                .jsonPath("$.components.schemas.TaskResponse.properties.dueDate.format").isEqualTo("date")
                .jsonPath("$.components.schemas.TaskResponse.properties.createdAt.format").isEqualTo("date-time")
                .jsonPath("$.components.schemas.TaskListResponse.properties.items.items.$ref")
                .isEqualTo("#/components/schemas/TaskResponse")
                .jsonPath("$.components.schemas.TaskListResponse.properties.totalItems").exists()
                .jsonPath("$.components.schemas.TaskListResponse.properties.totalPages").exists()
                .jsonPath("$.components.schemas.Problem.properties.type").exists()
                .jsonPath("$.components.schemas.Problem.properties.title").exists()
                .jsonPath("$.components.schemas.Problem.properties.status").exists()
                .jsonPath("$.components.schemas.Problem.properties.detail").exists()
                .jsonPath("$.components.schemas.Problem.properties.instance").exists()
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
