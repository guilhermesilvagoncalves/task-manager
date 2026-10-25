package com.taskmanager.auth.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Callable;

/** BCrypt is CPU-heavy and blocking, so it runs on boundedElastic, never on the request thread. */
@Component
public class PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    // Computed at startup so an unknown email costs the same as a wrong password.
    private final String dummyHash = encoder.encode("dummy-password");

    public Mono<String> hash(String password) {
        return offload(() -> encoder.encode(password));
    }

    public Mono<Boolean> matches(String password, String hash) {
        return offload(() -> encoder.matches(password, hash));
    }

    public Mono<Void> compareWithDummy(String password) {
        return matches(password, dummyHash).then();
    }

    private static <T> Mono<T> offload(Callable<T> work) {
        return Mono.fromCallable(work).subscribeOn(Schedulers.boundedElastic());
    }
}
