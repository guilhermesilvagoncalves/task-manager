package com.taskmanager.task;

/** Also raised for another user's task, so its existence is never revealed. */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException() {
        super("Task not found");
    }
}
