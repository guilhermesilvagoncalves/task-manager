package com.taskmanager.gateway;

import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

/** Errors raised by the gateway itself (e.g. no matching route) are RFC 7807 problem details, like the services'. */
@Component
@Order(-2) // Before Spring Boot's default handler (-1).
public class ProblemDetailsErrorHandler implements ErrorWebExceptionHandler {

    private static final ResolvableType PROBLEM_TYPE = ResolvableType.forClass(ProblemDetail.class);
    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    private final HttpMessageWriter<?> writer;

    public ProblemDetailsErrorHandler(ServerCodecConfigurer codecs) {
        this.writer = codecs.getWriters().stream()
                .filter(w -> w.canWrite(PROBLEM_TYPE, PROBLEM_JSON))
                .findFirst()
                .orElseThrow();
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }
        ProblemDetail problem = ex instanceof ErrorResponse error
                ? error.getBody()
                : ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setInstance(URI.create(exchange.getRequest().getPath().value()));
        if (ex instanceof ErrorResponse error) {
            response.getHeaders().addAll(error.getHeaders());
        }
        response.setStatusCode(HttpStatus.valueOf(problem.getStatus()));
        return write(problem, response);
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> write(ProblemDetail problem, ServerHttpResponse response) {
        return ((HttpMessageWriter<ProblemDetail>) writer)
                .write(Mono.just(problem), PROBLEM_TYPE, PROBLEM_JSON, response, Map.of());
    }
}
