package com.enterprise.llm.ingestion.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChunkingService {

    private final int chunkSize;
    private final int chunkOverlap;
    private final int minChunkSize;

    public ChunkingService(
            @Value("${app.chunking.chunk-size:800}")    int chunkSize,
            @Value("${app.chunking.chunk-overlap:200}") int chunkOverlap,
            @Value("${app.chunking.min-chunk-size:100}") int minChunkSize
    ) {
        this.chunkSize    = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.minChunkSize = minChunkSize;
    }

    /**
     * Splits text into overlapping chunks, breaking on sentence or paragraph
     * boundaries where possible to avoid cutting mid-sentence.
     */
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalised = text.replaceAll("\\r\\n|\\r", "\n").trim();
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < normalised.length()) {
            int end = Math.min(start + chunkSize, normalised.length());

            // Try to break at a paragraph boundary first, then sentence boundary
            if (end < normalised.length()) {
                int paragraphBreak = normalised.lastIndexOf("\n\n", end);
                int sentenceBreak  = lastSentenceBoundary(normalised, start, end);

                if (paragraphBreak > start + minChunkSize) {
                    end = paragraphBreak;
                } else if (sentenceBreak > start + minChunkSize) {
                    end = sentenceBreak;
                }
            }

            String chunk = normalised.substring(start, end).trim();
            if (chunk.length() >= minChunkSize) {
                chunks.add(chunk);
            }

            // Advance by chunkSize minus overlap
            start = end - chunkOverlap;
            if (start <= 0 || start >= normalised.length()) break;
        }
        return chunks;
    }

    private int lastSentenceBoundary(String text, int from, int to) {
        for (int i = to - 1; i > from; i--) {
            char c = text.charAt(i);
            if ((c == '.' || c == '!' || c == '?') &&
                    i + 1 < text.length() && text.charAt(i + 1) == ' ') {
                return i + 1;
            }
        }
        return to;
    }
}
