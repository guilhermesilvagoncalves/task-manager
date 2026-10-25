package com.taskmanager.task;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the SQL against a real Postgres: ownership scoping, column limits and pagination order. */
@SpringBootTest
@Testcontainers
class TaskRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine");

    private static final String EMOJI = "😀";
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private TaskRepository repository;

    @Test
    void persistsValuesAtTheLimitUnchanged() {
        Task task = task(UUID.randomUUID(), EMOJI.repeat(100), EMOJI.repeat(500), NOW);

        StepVerifier.create(repository.insert(task).then(repository.findByIdAndOwner(task.id(), task.ownerId())))
                .expectNext(task)
                .verifyComplete();
    }

    @Test
    void rejectsTitleOverTheLimit() {
        Task task = task(UUID.randomUUID(), EMOJI.repeat(101), "Description", NOW);

        StepVerifier.create(repository.insert(task)).verifyError(DataAccessException.class);
    }

    @Test
    void rejectsDescriptionOverTheLimit() {
        Task task = task(UUID.randomUUID(), "Title", EMOJI.repeat(501), NOW);

        StepVerifier.create(repository.insert(task)).verifyError(DataAccessException.class);
    }

    @Test
    void findDoesNotReturnAnotherUsersTask() {
        Task task = insert(task(UUID.randomUUID(), "Title", "Description", NOW));

        StepVerifier.create(repository.findByIdAndOwner(task.id(), UUID.randomUUID())).verifyComplete();
    }

    @Test
    void pageContainsOnlyOwnTasksNewestFirst() {
        UUID owner = UUID.randomUUID();
        Task older = insert(task(owner, "Older", "Description", NOW.minusSeconds(10)));
        Task newer = insert(task(owner, "Newer", "Description", NOW));
        insert(task(UUID.randomUUID(), "Other", "Description", NOW));

        StepVerifier.create(repository.findPageByOwner(owner, 0, 10))
                .expectNext(newer, older)
                .verifyComplete();
        StepVerifier.create(repository.countByOwner(owner)).expectNext(2L).verifyComplete();
    }

    @Test
    void paginationIsStableWhenCreationDatesTie() {
        UUID owner = UUID.randomUUID();
        Set<UUID> inserted = new HashSet<>();
        for (int i = 0; i < 25; i++) {
            // Groups of five share the same creation date.
            inserted.add(insert(task(owner, "Task " + i, "Description", NOW.minusSeconds(i / 5))).id());
        }

        List<UUID> seen = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            seen.addAll(repository.findPageByOwner(owner, page * 10L, 10).map(Task::id).collectList().block());
        }

        assertThat(seen).hasSize(25).doesNotHaveDuplicates();
        assertThat(new HashSet<>(seen)).isEqualTo(inserted);
    }

    @Test
    void pageBeyondTheLastIsEmpty() {
        UUID owner = UUID.randomUUID();
        insert(task(owner, "Title", "Description", NOW));

        StepVerifier.create(repository.findPageByOwner(owner, 10, 10)).verifyComplete();
    }

    @Test
    void updateReplacesFieldsAndKeepsCreatedAt() {
        Task task = insert(task(UUID.randomUUID(), "Title", "Description", NOW));
        Instant later = NOW.plusSeconds(60);
        Task replacement = new Task(task.id(), task.ownerId(), "New", "New description", TaskStatus.DONE,
                LocalDate.parse("2020-01-01"), null, later);

        StepVerifier.create(repository.update(replacement))
                .expectNext(new Task(task.id(), task.ownerId(), "New", "New description", TaskStatus.DONE,
                        LocalDate.parse("2020-01-01"), NOW, later))
                .verifyComplete();
    }

    @Test
    void updateOfAnotherUsersTaskChangesNothing() {
        Task task = insert(task(UUID.randomUUID(), "Title", "Description", NOW));
        Task replacement = new Task(task.id(), UUID.randomUUID(), "Hacked", "Hacked", TaskStatus.DONE,
                task.dueDate(), null, NOW.plusSeconds(60));

        StepVerifier.create(repository.update(replacement)).verifyComplete();
        StepVerifier.create(repository.findByIdAndOwner(task.id(), task.ownerId())).expectNext(task).verifyComplete();
    }

    @Test
    void deleteOfAnotherUsersTaskChangesNothing() {
        Task task = insert(task(UUID.randomUUID(), "Title", "Description", NOW));

        StepVerifier.create(repository.deleteByIdAndOwner(task.id(), UUID.randomUUID())).expectNext(false).verifyComplete();
        StepVerifier.create(repository.findByIdAndOwner(task.id(), task.ownerId())).expectNext(task).verifyComplete();
    }

    @Test
    void deleteRemovesOwnTask() {
        Task task = insert(task(UUID.randomUUID(), "Title", "Description", NOW));

        StepVerifier.create(repository.deleteByIdAndOwner(task.id(), task.ownerId())).expectNext(true).verifyComplete();
        StepVerifier.create(repository.findByIdAndOwner(task.id(), task.ownerId())).verifyComplete();
    }

    private Task insert(Task task) {
        repository.insert(task).block();
        return task;
    }

    private static Task task(UUID owner, String title, String description, Instant createdAt) {
        return new Task(UUID.randomUUID(), owner, title, description, TaskStatus.TODO,
                LocalDate.parse("2026-11-01"), createdAt, createdAt);
    }
}
