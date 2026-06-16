package com.enterprise.llm.orchestrator.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Message cannot be blank")
        @Size(max = 5000, message = "Message cannot exceed 5000 characters")
        String message,

        @NotBlank(message = "Session ID is required")
        String sessionId,

        @NotBlank(message = "User ID is required")
        String userId,

        @Size(max = 5000, message = "Context cannot exceed 5000 characters")
        String context
) {
}
