package com.taskmanager.auth;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashIsNotThePlainPassword() {
        String hash = hasher.hash("password123").block();

        assertThat(hash).isNotEqualTo("password123").startsWith("$2a$");
    }

    @Test
    void hashingRunsOffTheCallerThread() {
        StepVerifier.create(hasher.hash("password123").map(hash -> Thread.currentThread().getName()))
                .assertNext(thread -> assertThat(thread).startsWith("boundedElastic"))
                .verifyComplete();
    }
}
