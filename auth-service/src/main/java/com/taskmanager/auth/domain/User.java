package com.taskmanager.auth.domain;

import java.time.Instant;
import java.util.UUID;

public record User(UUID id, String email, String passwordHash, Instant createdAt) {
}
