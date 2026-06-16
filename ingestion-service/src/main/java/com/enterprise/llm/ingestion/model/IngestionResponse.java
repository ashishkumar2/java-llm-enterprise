package com.enterprise.llm.ingestion.model;

import java.util.UUID;

public record IngestionResponse(
        UUID   documentId,
        String filename,
        String status,
        String message
) {
    public static IngestionResponse accepted(UUID id, String filename) {
        return new IngestionResponse(id, filename, "PROCESSING",
                "Document accepted and queued for ingestion");
    }
}
