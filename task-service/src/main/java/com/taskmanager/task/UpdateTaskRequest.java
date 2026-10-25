package com.taskmanager.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Full replacement: every field is required. Title and description are stripped before validation. */
public record UpdateTaskRequest(
        @NotBlank @MaxCodePoints(100) String title,
        @NotBlank @MaxCodePoints(500) String description,
        @NotNull TaskStatus status,
        @NotNull LocalDate dueDate) {

    public UpdateTaskRequest {
        title = title == null ? null : title.strip();
        description = description == null ? null : description.strip();
    }
}
