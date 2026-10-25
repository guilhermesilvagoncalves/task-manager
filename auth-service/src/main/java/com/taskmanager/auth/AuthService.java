package com.taskmanager.auth;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository repository;
    private final PasswordHasher passwordHasher;

    public AuthService(UserRepository repository, PasswordHasher passwordHasher) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Always returns a user, even when the email is already taken (then nothing is stored),
     * so the response never reveals whether an email exists.
     */
    public Mono<User> register(String email, String password) {
        return passwordHasher.hash(password)
                .map(hash -> new User(UUID.randomUUID(), normalize(email), hash, Instant.now()))
                .flatMap(user -> repository.insertIfAbsent(user).thenReturn(user));
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
