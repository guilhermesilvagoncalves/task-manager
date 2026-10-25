package com.taskmanager.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.util.Map;

/** OpenAPI schema of the RFC 7807 error body written by {@link ProblemDetailsHandler}; documentation only. */
@Schema(description = "RFC 7807 problem detail")
public record Problem(URI type, String title, int status, String detail, URI instance,
                      @Schema(description = "Error per field, only on validation errors") Map<String, String> errors) {
}
