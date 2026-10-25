package com.taskmanager.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Callable;

/** BCrypt is CPU-heavy and blocking, so it runs on boundedElastic, never on the request thread. */
@Component
public class PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public Mono<String> hash(String password) {
        return offload(() -> encoder.encode(password));
    }

    private static <T> Mono<T> offload(Callable<T> work) {
        return Mono.fromCallable(work).subscribeOn(Schedulers.boundedElastic());
    }
}
