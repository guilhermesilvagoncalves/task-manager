package com.taskmanager.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(TaskController.class)
@Import({SecurityConfig.class, ProblemDetailsAuthenticationEntryPoint.class})
class TaskControllerTest {

    private static final MediaType PROBLEM = MediaType.APPLICATION_PROBLEM_JSON;
    private static final UUID OWNER = UUID.randomUUID();
    private static final LocalDate DUE = LocalDate.parse("2026-11-01");
    private static final String VALID_BODY =
            "{\"title\":\"Title\",\"description\":\"Description\",\"status\":\"TODO\",\"dueDate\":\"2026-11-01\"}";
    private static final String EMOJI = "😀";

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private TaskService service;

    // Authentication

    @ParameterizedTest
    @CsvSource({"GET,/api/me/tasks", "POST,/api/me/tasks", "GET,/api/me/tasks/00000000-0000-0000-0000-000000000001",
            "PUT,/api/me/tasks/00000000-0000-0000-0000-000000000001",
            "DELETE,/api/me/tasks/00000000-0000-0000-0000-000000000001"})
    void missingTokenReturnsUnauthorizedProblem(String method, String uri) {
        client.method(HttpMethod.valueOf(method)).uri(uri).exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(PROBLEM)
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.title").isEqualTo("Unauthorized")
                .jsonPath("$.instance").isEqualTo(uri);
        verifyNoInteractions(service);
    }

    @Test
    void malformedTokenReturnsUnauthorizedProblem() {
        client.get().uri("/api/me/tasks").header("Authorization", "Bearer not-a-jwt").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(PROBLEM)
                .expectBody().jsonPath("$.status").isEqualTo(401);
        verifyNoInteractions(service);
    }

    // Create

    @Test
    void createReturnsCreatedTaskWithLocation() {
        Task task = task("Title", "Description");
        when(service.create(OWNER, "Title", "Description", TaskStatus.TODO, DUE)).thenReturn(Mono.just(task));

        post(VALID_BODY)
                .expectStatus().isCreated()
                .expectHeader().location("/api/me/tasks/" + task.id())
                .expectBody()
                .jsonPath("$.id").isEqualTo(task.id().toString())
                .jsonPath("$.title").isEqualTo("Title")
                .jsonPath("$.description").isEqualTo("Description")
                .jsonPath("$.status").isEqualTo("TODO")
                .jsonPath("$.dueDate").isEqualTo("2026-11-01")
                .jsonPath("$.createdAt").isEqualTo("2026-10-25T10:00:00Z")
                .jsonPath("$.updatedAt").isEqualTo("2026-10-25T10:00:00Z")
                .jsonPath("$.ownerId").doesNotExist();
    }

    @Test
    void createWithoutStatusPassesNoStatus() {
        when(service.create(eq(OWNER), any(), any(), isNull(), any())).thenReturn(Mono.just(task("Title", "Description")));

        post("{\"title\":\"Title\",\"description\":\"Description\",\"dueDate\":\"2026-11-01\"}")
                .expectStatus().isCreated();
        verify(service).create(OWNER, "Title", "Description", null, DUE);
    }

    @Test
    void createIgnoresOwnerInBody() {
        when(service.create(any(), any(), any(), any(), any())).thenReturn(Mono.just(task("Title", "Description")));

        post("{\"owner\":\"" + UUID.randomUUID() + "\",\"ownerId\":\"" + UUID.randomUUID()
                + "\",\"title\":\"Title\",\"description\":\"Description\",\"dueDate\":\"2026-11-01\"}")
                .expectStatus().isCreated();
        verify(service).create(OWNER, "Title", "Description", null, DUE);
    }

    @Test
    void createTrimsTitleAndDescription() {
        when(service.create(any(), any(), any(), any(), any())).thenReturn(Mono.just(task("Title", "Description")));

        post("{\"title\":\"  Title \",\"description\":\"\\tDescription\\n\",\"dueDate\":\"2026-11-01\"}")
                .expectStatus().isCreated();
        verify(service).create(OWNER, "Title", "Description", null, DUE);
    }

    @Test
    void createRejectsMissingFieldsWithErrorPerField() {
        post("{}")
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(PROBLEM)
                .expectBody()
                .jsonPath("$.errors.title").exists()
                .jsonPath("$.errors.description").exists()
                .jsonPath("$.errors.dueDate").exists()
                .jsonPath("$.errors.status").doesNotExist();
        verifyNoInteractions(service);
    }

    @Test
    void createRejectsBlankTitleAndDescription() {
        post("{\"title\":\"   \",\"description\":\"\",\"dueDate\":\"2026-11-01\"}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.title").exists()
                .jsonPath("$.errors.description").exists();
    }

    @Test
    void createRejectsUnknownStatus() {
        post("{\"title\":\"Title\",\"description\":\"Description\",\"status\":\"OPEN\",\"dueDate\":\"2026-11-01\"}")
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(PROBLEM);
    }

    @ParameterizedTest
    @CsvSource({"2026-13-01", "2026-02-30", "01/11/2026", "2026-1-1"})
    void createRejectsMalformedDate(String date) {
        post("{\"title\":\"Title\",\"description\":\"Description\",\"dueDate\":\"" + date + "\"}")
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(PROBLEM);
    }

    @Test
    void createRejectsMalformedJson() {
        post("{\"title\":")
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(PROBLEM);
    }

    @Test
    void createAcceptsMaximumLengths() {
        String title = "a".repeat(100);
        String description = "b".repeat(500);
        when(service.create(OWNER, title, description, null, DUE)).thenReturn(Mono.just(task(title, description)));

        post(body(title, description)).expectStatus().isCreated();
    }

    @Test
    void createRejectsLengthsOverTheLimitWithErrorPerField() {
        post(body("a".repeat(101), "b".repeat(501)))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.title").exists()
                .jsonPath("$.errors.description").exists();
        verifyNoInteractions(service);
    }

    @Test
    void createCountsCodePointsNotUtf16Units() {
        String title = EMOJI.repeat(100);
        String description = EMOJI.repeat(500);
        when(service.create(OWNER, title, description, null, DUE)).thenReturn(Mono.just(task(title, description)));

        post(body(title, description)).expectStatus().isCreated();
    }

    @Test
    void createRejectsTooManyCodePoints() {
        post(body(EMOJI.repeat(101), EMOJI.repeat(501)))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.title").exists()
                .jsonPath("$.errors.description").exists();
    }

    @Test
    void createMeasuresLengthAfterTrimming() {
        String title = "a".repeat(100);
        when(service.create(OWNER, title, "Description", null, DUE)).thenReturn(Mono.just(task(title, "Description")));

        post(body("  " + title + "  ", "Description")).expectStatus().isCreated();
    }

    // Read

    @Test
    void getReturnsOwnTask() {
        Task task = task("Title", "Description");
        when(service.get(OWNER, task.id())).thenReturn(Mono.just(task));

        authenticated().get().uri("/api/me/tasks/{id}", task.id()).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(task.id().toString())
                .jsonPath("$.title").isEqualTo("Title");
    }

    @Test
    void getReturnsNotFoundProblem() {
        UUID id = UUID.randomUUID();
        when(service.get(OWNER, id)).thenReturn(Mono.error(new TaskNotFoundException()));

        authenticated().get().uri("/api/me/tasks/{id}", id).exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(PROBLEM)
                .expectBody().jsonPath("$.status").isEqualTo(404);
    }

    @Test
    void getRejectsMalformedId() {
        authenticated().get().uri("/api/me/tasks/not-a-uuid").exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(PROBLEM);
        verifyNoInteractions(service);
    }

    // List

    @Test
    void listReturnsPageWithTotals() {
        Task task = task("Title", "Description");
        when(service.list(OWNER, 1)).thenReturn(Mono.just(new TaskPage(List.of(task), 1, 10, 11, 2)));

        authenticated().get().uri("/api/me/tasks?page=1").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.items[0].id").isEqualTo(task.id().toString())
                .jsonPath("$.items[0].ownerId").doesNotExist()
                .jsonPath("$.page").isEqualTo(1)
                .jsonPath("$.size").isEqualTo(10)
                .jsonPath("$.totalItems").isEqualTo(11)
                .jsonPath("$.totalPages").isEqualTo(2);
    }

    @Test
    void listWithoutPageReturnsFirstPage() {
        when(service.list(OWNER, 0)).thenReturn(Mono.just(new TaskPage(List.of(), 0, 10, 0, 0)));

        authenticated().get().uri("/api/me/tasks").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(0)
                .jsonPath("$.page").isEqualTo(0);
    }

    @ParameterizedTest
    @CsvSource({"-1", "abc", "1.5"})
    void listRejectsInvalidPage(String page) {
        authenticated().get().uri("/api/me/tasks?page=" + page).exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(PROBLEM);
        verifyNoInteractions(service);
    }

    // Update

    @Test
    void updateReturnsUpdatedTask() {
        Task task = task("New", "New description");
        when(service.update(OWNER, task.id(), "New", "New description", TaskStatus.DONE, DUE)).thenReturn(Mono.just(task));

        put(task.id(), "{\"title\":\"New\",\"description\":\"New description\",\"status\":\"DONE\",\"dueDate\":\"2026-11-01\"}")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(task.id().toString())
                .jsonPath("$.title").isEqualTo("New");
    }

    @Test
    void updateRejectsMissingFieldsIncludingStatus() {
        put(UUID.randomUUID(), "{}")
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(PROBLEM)
                .expectBody()
                .jsonPath("$.errors.title").exists()
                .jsonPath("$.errors.description").exists()
                .jsonPath("$.errors.status").exists()
                .jsonPath("$.errors.dueDate").exists();
        verifyNoInteractions(service);
    }

    @Test
    void updateRejectsLengthsOverTheLimit() {
        put(UUID.randomUUID(), "{\"title\":\"" + EMOJI.repeat(101) + "\",\"description\":\"" + "b".repeat(501)
                + "\",\"status\":\"DONE\",\"dueDate\":\"2026-11-01\"}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.title").exists()
                .jsonPath("$.errors.description").exists();
    }

    @Test
    void updateReturnsNotFoundProblem() {
        UUID id = UUID.randomUUID();
        when(service.update(any(), any(), any(), any(), any(), any())).thenReturn(Mono.error(new TaskNotFoundException()));

        put(id, VALID_BODY)
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(PROBLEM);
    }

    // Delete

    @Test
    void deleteReturnsNoContent() {
        UUID id = UUID.randomUUID();
        when(service.delete(OWNER, id)).thenReturn(Mono.empty());

        authenticated().delete().uri("/api/me/tasks/{id}", id).exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    @Test
    void deleteReturnsNotFoundProblem() {
        UUID id = UUID.randomUUID();
        when(service.delete(OWNER, id)).thenReturn(Mono.error(new TaskNotFoundException()));

        authenticated().delete().uri("/api/me/tasks/{id}", id).exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(PROBLEM);
    }

    // General

    @ParameterizedTest
    @CsvSource({"PATCH,/api/me/tasks/00000000-0000-0000-0000-000000000001", "DELETE,/api/me/tasks", "PUT,/api/me/tasks"})
    void unsupportedMethodReturnsMethodNotAllowed(String method, String uri) {
        authenticated().method(HttpMethod.valueOf(method)).uri(uri).exchange()
                .expectStatus().isEqualTo(405)
                .expectHeader().contentTypeCompatibleWith(PROBLEM);
    }

    @ParameterizedTest
    @CsvSource({"/api/tasks", "/api/me/tasks/00000000-0000-0000-0000-000000000001/unknown"})
    void unknownRouteReturnsNotFound(String uri) {
        authenticated().get().uri(uri).exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(PROBLEM);
    }

    private WebTestClient authenticated() {
        return client.mutateWith(mockJwt().jwt(jwt -> jwt.subject(OWNER.toString())));
    }

    private WebTestClient.ResponseSpec post(String body) {
        return authenticated().post().uri("/api/me/tasks")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
    }

    private WebTestClient.ResponseSpec put(UUID id, String body) {
        return authenticated().put().uri("/api/me/tasks/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
    }

    private static String body(String title, String description) {
        return "{\"title\":\"" + title + "\",\"description\":\"" + description + "\",\"dueDate\":\"2026-11-01\"}";
    }

    private static Task task(String title, String description) {
        Instant at = Instant.parse("2026-10-25T10:00:00Z");
        return new Task(UUID.randomUUID(), OWNER, title, description, TaskStatus.TODO, DUE, at, at);
    }
}
