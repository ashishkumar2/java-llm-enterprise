package com.enterprise.llm.orchestrator.service;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RagContextService {
    private static final Logger logger = LoggerFactory.getLogger(RagContextService.class);

    private final EmbeddingClient embeddingClient;
    private final JdbcTemplate    jdbcTemplate;
    private final int             topK;
    private final boolean         ragEnabled;

    public RagContextService(
            EmbeddingClient embeddingClient,
            JdbcTemplate jdbcTemplate,
            @Value("${app.rag.top-k:5}") int topK,
            @Value("${app.rag.enabled:true}") boolean ragEnabled
    ) {
        this.embeddingClient = embeddingClient;
        this.jdbcTemplate    = jdbcTemplate;
        this.topK            = topK;
        this.ragEnabled      = ragEnabled;
    }

    /**
     * Returns top-K relevant document chunks for the given query, or an empty
     * string if RAG is disabled or no chunks match.
     */
    public String retrieveContext(String query) {
        if (!ragEnabled || query == null || query.isBlank()) {
            return "";
        }
        try {
            List<Double> embedding = embeddingClient.embed(query);
            String vectorLiteral  = toVectorLiteral(embedding);

            List<String> chunks = jdbcTemplate.queryForList(
                    "SELECT content FROM document_chunks " +
                    "WHERE embedding IS NOT NULL " +
                    "ORDER BY embedding <=> ?::vector " +
                    "LIMIT ?",
                    String.class,
                    vectorLiteral, topK
            );

            if (chunks.isEmpty()) {
                return "";
            }
            logger.debug("RAG retrieved {} chunks for query length={}", chunks.size(), query.length());
            return String.join("\n\n---\n\n", chunks);

        } catch (Exception e) {
            // RAG failure must never block the chat response
            logger.warn("RAG context retrieval failed, proceeding without context: {}", e.getMessage());
            return "";
        }
    }

    private String toVectorLiteral(List<Double> embedding) {
        return "[" + embedding.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")) + "]";
    }
}
