package com.enterprise.llm.ingestion.service;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingClient embeddingClient;
    private final int             batchSize;
    private final int             maxRetries;
    private final long            retryDelayMs;

    public EmbeddingService(
            EmbeddingClient embeddingClient,
            @Value("${app.embedding.batch-size:100}")          int  batchSize,
            @Value("${app.embedding.retry-max-attempts:3}")    int  maxRetries,
            @Value("${app.embedding.retry-delay-millis:1000}") long retryDelayMs
    ) {
        this.embeddingClient = embeddingClient;
        this.batchSize       = batchSize;
        this.maxRetries      = maxRetries;
        this.retryDelayMs    = retryDelayMs;
    }

    /**
     * Embeds a list of text chunks in batches, returning one embedding per chunk.
     * Retries transiently-failing batches up to maxRetries times.
     */
    public List<List<Double>> embedAll(List<String> texts) {
        List<List<Double>> results = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            results.addAll(embedBatchWithRetry(batch));
        }
        return results;
    }

    private List<List<Double>> embedBatchWithRetry(List<String> batch) {
        Exception last = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return embeddingClient.embed(batch);
            } catch (Exception e) {
                last = e;
                logger.warn("Embedding batch failed (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    sleep(retryDelayMs * attempt);
                }
            }
        }
        throw new RuntimeException("Embedding failed after " + maxRetries + " attempts", last);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
