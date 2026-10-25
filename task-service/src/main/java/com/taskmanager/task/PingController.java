package com.taskmanager.task;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class PingController {

    private final DatabaseVersionRepository repository;

    public PingController(DatabaseVersionRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/ping")
    public Mono<PingResponse> ping() {
        return repository.version().map(version -> new PingResponse("ok", version));
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public void databaseUnavailable() {
    }

    public record PingResponse(String status, String db) {
    }
}
