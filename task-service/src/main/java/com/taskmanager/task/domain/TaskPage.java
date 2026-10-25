package com.taskmanager.task.domain;

import java.util.List;

public record TaskPage(List<Task> items, int page, int size, long totalItems, long totalPages) {
}
