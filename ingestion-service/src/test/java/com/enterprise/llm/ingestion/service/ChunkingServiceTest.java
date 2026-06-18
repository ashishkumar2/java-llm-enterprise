package com.enterprise.llm.ingestion.service;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkingServiceTest {

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingService(800, 200, 100);
    }

    @Test
    void chunk_nullInput_returnsEmpty() {
        assertThat(chunkingService.chunk(null)).isEmpty();
    }

    @Test
    void chunk_blankInput_returnsEmpty() {
        assertThat(chunkingService.chunk("   ")).isEmpty();
    }

    @Test
    void chunk_shortText_returnsSingleChunk() {
        String text = "Hello world. This is a short document.";
        List<String> chunks = chunkingService.chunk(text);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("Hello world");
    }

    @Test
    void chunk_textShorterThanMinSize_returnsEmpty() {
        // min chunk size is 100; give it 50 chars
        String text = "A".repeat(50);
        List<String> chunks = chunkingService.chunk(text);
        assertThat(chunks).isEmpty();
    }

    @Test
    void chunk_longText_producesMultipleChunks() {
        // 3000 chars >> 800 chunk size → must produce multiple chunks
        String text = "This is a sentence. ".repeat(150);
        List<String> chunks = chunkingService.chunk(text);
        assertThat(chunks.size()).isGreaterThan(1);
    }

    @Test
    void chunk_overlapEnsuresContentRepetition() {
        // Build text exactly 1200 chars; with 800 chunk / 200 overlap we expect the
        // end of chunk 1 to appear at the start of chunk 2.
        String sentence = "The quick brown fox jumps over the lazy dog. ";
        String text = sentence.repeat(30); // ~1350 chars

        List<String> chunks = chunkingService.chunk(text);
        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);

        // The tail of chunk 0 should overlap with the head of chunk 1
        String tail = chunks.get(0).substring(Math.max(0, chunks.get(0).length() - 200));
        assertThat(chunks.get(1)).startsWith(tail.substring(0, Math.min(50, tail.length())));
    }

    @Test
    void chunk_breaksAtSentenceBoundary() {
        // 900-char block with a sentence boundary at position ~810
        StringBuilder sb = new StringBuilder("A".repeat(810));
        sb.append(". ");
        sb.append("B".repeat(300));
        String text = sb.toString();

        List<String> chunks = chunkingService.chunk(text);
        assertThat(chunks).isNotEmpty();
        // First chunk should end at the sentence boundary, not mid-word
        assertThat(chunks.get(0)).endsWith(".");
    }

    @Test
    void chunk_normalisesWindowsLineEndings() {
        String text = "Line one.\r\nLine two.\r\nLine three.";
        List<String> chunks = chunkingService.chunk(text);
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0)).doesNotContain("\r");
    }

    @Test
    void chunk_breaksAtParagraphBoundaryPreferably() {
        // Two paragraphs separated by double newline near the chunk boundary
        String para1 = "X".repeat(700) + "\n\n";
        String para2 = "Y".repeat(600);
        List<String> chunks = chunkingService.chunk(para1 + para2);

        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);
        // First chunk should not contain Y characters (split happened at paragraph)
        assertThat(chunks.get(0)).doesNotContain("Y");
    }

    @Test
    void chunk_customChunkSize_respectsConfiguration() {
        ChunkingService small = new ChunkingService(200, 50, 20);
        String text = "W".repeat(1000);
        List<String> chunks = small.chunk(text);
        // With 200-char chunks and 50 overlap, expect roughly 1000/(200-50) ≈ 7 chunks
        assertThat(chunks.size()).isGreaterThan(4);
        chunks.forEach(c -> assertThat(c.length()).isLessThanOrEqualTo(200));
    }
}
