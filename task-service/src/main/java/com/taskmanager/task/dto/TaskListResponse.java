package com.taskmanager.task.dto;

import com.taskmanager.task.domain.TaskPage;

import java.util.List;

public record TaskListResponse(List<TaskResponse> items, int page, int size, long totalItems, long totalPages) {

    public static TaskListResponse from(TaskPage page) {
        return new TaskListResponse(page.items().stream().map(TaskResponse::from).toList(),
                page.page(), page.size(), page.totalItems(), page.totalPages());
    }
}
