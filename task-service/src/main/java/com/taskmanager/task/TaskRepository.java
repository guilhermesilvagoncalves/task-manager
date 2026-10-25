package com.taskmanager.task;

import io.r2dbc.spi.Readable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** Every query is scoped by owner: a user can never read or change another user's task. */
@Repository
public class TaskRepository {

    private static final String COLUMNS = "id, owner_id, title, description, status, due_date, created_at, updated_at";

    private final DatabaseClient client;

    public TaskRepository(DatabaseClient client) {
        this.client = client;
    }

    public Mono<Void> insert(Task task) {
        return client.sql("INSERT INTO tasks (" + COLUMNS + """
                        ) VALUES (:id, :ownerId, :title, :description, :status, :dueDate, :createdAt, :updatedAt)""")
                .bind("id", task.id())
                .bind("ownerId", task.ownerId())
                .bind("title", task.title())
                .bind("description", task.description())
                .bind("status", task.status().name())
                .bind("dueDate", task.dueDate())
                .bind("createdAt", task.createdAt().atOffset(ZoneOffset.UTC))
                .bind("updatedAt", task.updatedAt().atOffset(ZoneOffset.UTC))
                .then();
    }

    public Mono<Task> findByIdAndOwner(UUID id, UUID ownerId) {
        return client.sql("SELECT " + COLUMNS + " FROM tasks WHERE id = :id AND owner_id = :ownerId")
                .bind("id", id)
                .bind("ownerId", ownerId)
                .map(TaskRepository::toTask)
                .one();
    }

    /** Newest first; the id breaks ties so pages never overlap or skip a task. */
    public Flux<Task> findPageByOwner(UUID ownerId, long offset, int limit) {
        return client.sql("SELECT " + COLUMNS + """
                         FROM tasks WHERE owner_id = :ownerId
                        ORDER BY created_at DESC, id DESC
                        LIMIT :limit OFFSET :offset""")
                .bind("ownerId", ownerId)
                .bind("limit", limit)
                .bind("offset", offset)
                .map(TaskRepository::toTask)
                .all();
    }

    public Mono<Long> countByOwner(UUID ownerId) {
        return client.sql("SELECT count(*) FROM tasks WHERE owner_id = :ownerId")
                .bind("ownerId", ownerId)
                .map(row -> row.get(0, Long.class))
                .one();
    }

    /** Replaces the editable fields and updated_at; created_at is kept. Empty when not found. */
    public Mono<Task> update(Task task) {
        return client.sql("""
                        UPDATE tasks SET title = :title, description = :description, status = :status,
                            due_date = :dueDate, updated_at = :updatedAt
                        WHERE id = :id AND owner_id = :ownerId
                        RETURNING\s""" + COLUMNS)
                .bind("id", task.id())
                .bind("ownerId", task.ownerId())
                .bind("title", task.title())
                .bind("description", task.description())
                .bind("status", task.status().name())
                .bind("dueDate", task.dueDate())
                .bind("updatedAt", task.updatedAt().atOffset(ZoneOffset.UTC))
                .map(TaskRepository::toTask)
                .one();
    }

    public Mono<Boolean> deleteByIdAndOwner(UUID id, UUID ownerId) {
        return client.sql("DELETE FROM tasks WHERE id = :id AND owner_id = :ownerId")
                .bind("id", id)
                .bind("ownerId", ownerId)
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1);
    }

    private static Task toTask(Readable row) {
        return new Task(
                row.get("id", UUID.class),
                row.get("owner_id", UUID.class),
                row.get("title", String.class),
                row.get("description", String.class),
                TaskStatus.valueOf(row.get("status", String.class)),
                row.get("due_date", LocalDate.class),
                row.get("created_at", OffsetDateTime.class).toInstant(),
                row.get("updated_at", OffsetDateTime.class).toInstant());
    }
}
