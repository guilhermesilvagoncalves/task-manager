package com.taskmanager.task.dto;

import com.taskmanager.task.domain.Task;
import com.taskmanager.task.domain.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(UUID id, String title, String description, TaskStatus status, LocalDate dueDate,
                           Instant createdAt, Instant updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(task.id(), task.title(), task.description(), task.status(), task.dueDate(),
                task.createdAt(), task.updatedAt());
    }
}
