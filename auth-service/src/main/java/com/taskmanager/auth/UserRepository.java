package com.taskmanager.auth;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Repository
public class UserRepository {

    private final DatabaseClient client;

    public UserRepository(DatabaseClient client) {
        this.client = client;
    }

    /** Returns false when the email already exists; the unique index makes this safe under concurrency. */
    public Mono<Boolean> insertIfAbsent(User user) {
        return client.sql("""
                        INSERT INTO users (id, email, password_hash, created_at)
                        VALUES (:id, :email, :passwordHash, :createdAt)
                        ON CONFLICT (email) DO NOTHING""")
                .bind("id", user.id())
                .bind("email", user.email())
                .bind("passwordHash", user.passwordHash())
                .bind("createdAt", user.createdAt().atOffset(ZoneOffset.UTC))
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1);
    }

    public Mono<User> findByEmail(String email) {
        return client.sql("SELECT id, email, password_hash, created_at FROM users WHERE email = :email")
                .bind("email", email)
                .map(row -> new User(
                        row.get("id", UUID.class),
                        row.get("email", String.class),
                        row.get("password_hash", String.class),
                        row.get("created_at", OffsetDateTime.class).toInstant()))
                .one();
    }
}
