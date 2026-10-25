package com.taskmanager.task;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class TaskService {

    static final int PAGE_SIZE = 10;

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public Mono<Task> create(UUID ownerId, String title, String description, TaskStatus status, LocalDate dueDate) {
        Instant now = now();
        Task task = new Task(UUID.randomUUID(), ownerId, title, description,
                status == null ? TaskStatus.TODO : status, dueDate, now, now);
        return repository.insert(task).thenReturn(task);
    }

    public Mono<Task> get(UUID ownerId, UUID id) {
        return repository.findByIdAndOwner(id, ownerId)
                .switchIfEmpty(Mono.error(TaskNotFoundException::new));
    }

    public Mono<TaskPage> list(UUID ownerId, int page) {
        return Mono.zip(
                        repository.findPageByOwner(ownerId, (long) page * PAGE_SIZE, PAGE_SIZE).collectList(),
                        repository.countByOwner(ownerId))
                .map(result -> {
                    long total = result.getT2();
                    long totalPages = (total + PAGE_SIZE - 1) / PAGE_SIZE;
                    return new TaskPage(result.getT1(), page, PAGE_SIZE, total, totalPages);
                });
    }

    public Mono<Task> update(UUID ownerId, UUID id, String title, String description, TaskStatus status, LocalDate dueDate) {
        return repository.update(new Task(id, ownerId, title, description, status, dueDate, null, now()))
                .switchIfEmpty(Mono.error(TaskNotFoundException::new));
    }

    public Mono<Void> delete(UUID ownerId, UUID id) {
        return repository.deleteByIdAndOwner(id, ownerId)
                .flatMap(deleted -> deleted ? Mono.<Void>empty() : Mono.error(new TaskNotFoundException()));
    }

    /** Postgres stores microseconds; truncating keeps responses equal to what a later read returns. */
    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
