package com.taskmanager.auth.security;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashIsNotThePlainPasswordAndMatchesIt() {
        String hash = hasher.hash("password123").block();

        assertThat(hash).isNotEqualTo("password123");
        StepVerifier.create(hasher.matches("password123", hash)).expectNext(true).verifyComplete();
        StepVerifier.create(hasher.matches("wrong-password", hash)).expectNext(false).verifyComplete();
    }

    @Test
    void passwordOverTheByteLimitDoesNotMatchInsteadOfFailing() {
        String hash = hasher.hash("password123").block();

        StepVerifier.create(hasher.matches("é".repeat(72), hash)).expectNext(false).verifyComplete();
    }

    @Test
    void hashingRunsOffTheCallerThread() {
        StepVerifier.create(hasher.hash("password123").map(hash -> Thread.currentThread().getName()))
                .assertNext(thread -> assertThat(thread).startsWith("boundedElastic"))
                .verifyComplete();
        StepVerifier.create(hasher.matches("password123", "$2a$10$invalid").map(ok -> Thread.currentThread().getName()))
                .assertNext(thread -> assertThat(thread).startsWith("boundedElastic"))
                .verifyComplete();
    }
}
