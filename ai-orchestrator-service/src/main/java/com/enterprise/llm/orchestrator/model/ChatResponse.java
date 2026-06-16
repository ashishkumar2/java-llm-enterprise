package com.enterprise.llm.orchestrator.model;

import java.time.Instant;

public record ChatResponse(
        String sessionId,
        String message,
        String model,
        int tokenCount,
        Instant createdAt
) {
}
