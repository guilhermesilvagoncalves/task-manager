package com.taskmanager.task;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

/** Missing or invalid token: 401 as an RFC 7807 problem detail, written by the same codecs as other errors. */
@Component
public class ProblemDetailsAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private static final ResolvableType PROBLEM_TYPE = ResolvableType.forClass(ProblemDetail.class);
    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    private final HttpMessageWriter<?> writer;

    public ProblemDetailsAuthenticationEntryPoint(ServerCodecConfigurer codecs) {
        this.writer = codecs.getWriters().stream()
                .filter(w -> w.canWrite(PROBLEM_TYPE, PROBLEM_JSON))
                .findFirst()
                .orElseThrow();
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        problem.setInstance(URI.create(exchange.getRequest().getPath().value()));
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return write(problem, response);
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> write(ProblemDetail problem, ServerHttpResponse response) {
        return ((HttpMessageWriter<ProblemDetail>) writer)
                .write(Mono.just(problem), PROBLEM_TYPE, PROBLEM_JSON, response, Map.of());
    }
}
