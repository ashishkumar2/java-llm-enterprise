package com.enterprise.llm.common.api;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path,
        List<String> details
) {
    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(code, message, Instant.now(), path, List.of());
    }

    public static ErrorResponse of(String code, String message, String path, List<String> details) {
        return new ErrorResponse(code, message, Instant.now(), path, details);
    }
}
