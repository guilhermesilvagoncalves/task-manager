package com.taskmanager.task;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final LocalDate DUE = LocalDate.parse("2026-11-01");

    private final TaskRepository repository = mock(TaskRepository.class);
    private final TaskService service = new TaskService(repository);

    @Test
    void createStoresTaskOwnedByTheGivenUser() {
        when(repository.insert(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.create(OWNER, "Title", "Description", TaskStatus.IN_PROGRESS, DUE))
                .assertNext(task -> {
                    assertThat(task.id()).isNotNull();
                    assertThat(task.ownerId()).isEqualTo(OWNER);
                    assertThat(task.title()).isEqualTo("Title");
                    assertThat(task.description()).isEqualTo("Description");
                    assertThat(task.status()).isEqualTo(TaskStatus.IN_PROGRESS);
                    assertThat(task.dueDate()).isEqualTo(DUE);
                    assertThat(task.createdAt()).isNotNull().isEqualTo(task.updatedAt());
                })
                .verifyComplete();
        ArgumentCaptor<Task> stored = ArgumentCaptor.forClass(Task.class);
        verify(repository).insert(stored.capture());
        assertThat(stored.getValue().ownerId()).isEqualTo(OWNER);
    }

    @Test
    void createDefaultsStatusToTodo() {
        when(repository.insert(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.create(OWNER, "Title", "Description", null, DUE))
                .assertNext(task -> assertThat(task.status()).isEqualTo(TaskStatus.TODO))
                .verifyComplete();
    }

    @Test
    void createTruncatesTimestampsToTheDatabasePrecision() {
        when(repository.insert(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.create(OWNER, "Title", "Description", null, DUE))
                .assertNext(task -> assertThat(task.createdAt())
                        .isEqualTo(task.createdAt().truncatedTo(ChronoUnit.MICROS)))
                .verifyComplete();
    }

    @Test
    void getReturnsOwnTask() {
        Task task = task();
        when(repository.findByIdAndOwner(task.id(), OWNER)).thenReturn(Mono.just(task));

        StepVerifier.create(service.get(OWNER, task.id())).expectNext(task).verifyComplete();
    }

    @Test
    void getFailsWhenTaskIsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndOwner(id, OWNER)).thenReturn(Mono.empty());

        StepVerifier.create(service.get(OWNER, id)).verifyError(TaskNotFoundException.class);
    }

    @Test
    void listReturnsFirstPageWithTotals() {
        List<Task> tasks = List.of(task(), task());
        when(repository.findPageByOwner(OWNER, 0, 10)).thenReturn(Flux.fromIterable(tasks));
        when(repository.countByOwner(OWNER)).thenReturn(Mono.just(23L));

        StepVerifier.create(service.list(OWNER, 0))
                .expectNext(new TaskPage(tasks, 0, 10, 23, 3))
                .verifyComplete();
    }

    @Test
    void listUsesOffsetOfTheRequestedPage() {
        when(repository.findPageByOwner(OWNER, 20, 10)).thenReturn(Flux.empty());
        when(repository.countByOwner(OWNER)).thenReturn(Mono.just(20L));

        StepVerifier.create(service.list(OWNER, 2))
                .expectNext(new TaskPage(List.of(), 2, 10, 20, 2))
                .verifyComplete();
    }

    @Test
    void listWithNoTasksHasZeroPages() {
        when(repository.findPageByOwner(OWNER, 0, 10)).thenReturn(Flux.empty());
        when(repository.countByOwner(OWNER)).thenReturn(Mono.just(0L));

        StepVerifier.create(service.list(OWNER, 0))
                .expectNext(new TaskPage(List.of(), 0, 10, 0, 0))
                .verifyComplete();
    }

    @Test
    void listOffsetDoesNotOverflowForHugePages() {
        when(repository.findPageByOwner(OWNER, Integer.MAX_VALUE * 10L, 10)).thenReturn(Flux.empty());
        when(repository.countByOwner(OWNER)).thenReturn(Mono.just(1L));

        StepVerifier.create(service.list(OWNER, Integer.MAX_VALUE))
                .assertNext(page -> assertThat(page.items()).isEmpty())
                .verifyComplete();
    }

    @Test
    void updateReplacesFieldsAndSetsUpdatedAt() {
        UUID id = UUID.randomUUID();
        Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);
        when(repository.update(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.update(OWNER, id, "New", "New description", TaskStatus.DONE, DUE))
                .assertNext(task -> {
                    assertThat(task.id()).isEqualTo(id);
                    assertThat(task.ownerId()).isEqualTo(OWNER);
                    assertThat(task.title()).isEqualTo("New");
                    assertThat(task.description()).isEqualTo("New description");
                    assertThat(task.status()).isEqualTo(TaskStatus.DONE);
                    assertThat(task.dueDate()).isEqualTo(DUE);
                    assertThat(task.updatedAt()).isAfterOrEqualTo(before);
                })
                .verifyComplete();
    }

    @Test
    void updateFailsWhenTaskIsNotFound() {
        when(repository.update(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.update(OWNER, UUID.randomUUID(), "New", "New", TaskStatus.DONE, DUE))
                .verifyError(TaskNotFoundException.class);
    }

    @Test
    void deleteRemovesOwnTask() {
        UUID id = UUID.randomUUID();
        when(repository.deleteByIdAndOwner(id, OWNER)).thenReturn(Mono.just(true));

        StepVerifier.create(service.delete(OWNER, id)).verifyComplete();
    }

    @Test
    void deleteFailsWhenTaskIsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.deleteByIdAndOwner(id, OWNER)).thenReturn(Mono.just(false));

        StepVerifier.create(service.delete(OWNER, id)).verifyError(TaskNotFoundException.class);
    }

    private static Task task() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        return new Task(UUID.randomUUID(), OWNER, "Title", "Description", TaskStatus.TODO, DUE, now, now);
    }
}
