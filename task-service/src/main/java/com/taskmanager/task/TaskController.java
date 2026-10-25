package com.taskmanager.task;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

/** Tasks of the authenticated user; the owner always comes from the token's subject. */
@RestController
@RequestMapping("/api/me/tasks")
@SecurityRequirement(name = "bearer")
@ApiResponse(responseCode = "401", description = "Missing, invalid or expired token",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Task created; `Location` points to it")
    @ApiResponse(responseCode = "400", description = "Invalid fields, error per field",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    public Mono<ResponseEntity<TaskResponse>> create(@AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody CreateTaskRequest request) {
        return service.create(owner(jwt), request.title(), request.description(), request.status(), request.dueDate())
                .map(task -> ResponseEntity.created(URI.create("/api/me/tasks/" + task.id()))
                        .body(TaskResponse.from(task)));
    }

    @GetMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "The task")
    @ApiResponse(responseCode = "400", description = "Malformed id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    @ApiResponse(responseCode = "404", description = "No task with this id for the caller",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    public Mono<TaskResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.get(owner(jwt), id).map(TaskResponse::from);
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "Page of the caller's tasks, newest first, 10 per page")
    @ApiResponse(responseCode = "400", description = "Negative or non-numeric page",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    public Mono<TaskListResponse> list(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(defaultValue = "0") @Min(0) int page) {
        return service.list(owner(jwt), page).map(TaskListResponse::from);
    }

    @PutMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "The updated task")
    @ApiResponse(responseCode = "400", description = "Malformed id or invalid fields, error per field",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    @ApiResponse(responseCode = "404", description = "No task with this id for the caller",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    public Mono<TaskResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                     @Valid @RequestBody UpdateTaskRequest request) {
        return service.update(owner(jwt), id, request.title(), request.description(), request.status(), request.dueDate())
                .map(TaskResponse::from);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "Task deleted")
    @ApiResponse(responseCode = "400", description = "Malformed id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    @ApiResponse(responseCode = "404", description = "No task with this id for the caller",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.delete(owner(jwt), id);
    }

    private static UUID owner(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
