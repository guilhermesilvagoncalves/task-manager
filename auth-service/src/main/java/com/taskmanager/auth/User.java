package com.taskmanager.auth;

import java.time.Instant;
import java.util.UUID;

public record User(UUID id, String email, String passwordHash, Instant createdAt) {
}
