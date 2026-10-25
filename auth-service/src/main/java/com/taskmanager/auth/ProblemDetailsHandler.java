package com.taskmanager.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.TreeMap;

/** Every error response is an RFC 7807 problem detail. */
@RestControllerAdvice
public class ProblemDetailsHandler extends ResponseEntityExceptionHandler {

    @Override
    protected Mono<ResponseEntity<Object>> handleWebExchangeBindException(
            WebExchangeBindException e, HttpHeaders headers, HttpStatusCode status, ServerWebExchange exchange) {
        Map<String, String> errors = new TreeMap<>();
        for (FieldError error : e.getFieldErrors()) {
            errors.merge(error.getField(), error.getDefaultMessage(), (a, b) -> a + "; " + b);
        }
        ProblemDetail problem = e.getBody();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(e, problem, headers, status, exchange);
    }
}
