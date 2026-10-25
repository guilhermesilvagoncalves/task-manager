package com.taskmanager.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Task(UUID id, UUID ownerId, String title, String description, TaskStatus status,
                   LocalDate dueDate, Instant createdAt, Instant updatedAt) {
}
