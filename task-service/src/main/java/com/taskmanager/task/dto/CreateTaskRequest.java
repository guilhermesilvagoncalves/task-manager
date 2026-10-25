package com.taskmanager.task.dto;

import com.taskmanager.task.domain.TaskStatus;
import com.taskmanager.task.validation.MaxCodePoints;
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
