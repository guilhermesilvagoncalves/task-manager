package com.taskmanager.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Status is optional (defaults to TODO). Title and description are stripped before validation. */
public record CreateTaskRequest(
        @NotBlank @MaxCodePoints(100) String title,
        @NotBlank @MaxCodePoints(500) String description,
        TaskStatus status,
        @NotNull LocalDate dueDate) {

    public CreateTaskRequest {
        title = title == null ? null : title.strip();
        description = description == null ? null : description.strip();
    }
}
