package com.enterprise.llm.ingestion.service;

import com.enterprise.llm.ingestion.model.Document;
import com.enterprise.llm.ingestion.repository.DocumentRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

@Service
public class DocumentIngestionService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final DocumentRepository documentRepository;
    private final ChunkingService    chunkingService;
    private final EmbeddingService   embeddingService;
    private final JdbcTemplate       jdbcTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DocumentIngestionService(
            DocumentRepository documentRepository,
            ChunkingService chunkingService,
            EmbeddingService embeddingService,
            JdbcTemplate jdbcTemplate,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.documentRepository = documentRepository;
        this.chunkingService    = chunkingService;
        this.embeddingService   = embeddingService;
        this.jdbcTemplate       = jdbcTemplate;
        this.kafkaTemplate      = kafkaTemplate;
    }

    @Async
    public void ingest(UUID documentId, byte[] fileBytes) {
        Document doc = documentRepository.findById(documentId).orElseThrow();
        try {
            logger.info("Starting ingestion: documentId={}, filename={}", documentId, doc.getFilename());

            // 1. Extract text from any supported format via Tika
            String text = extractText(fileBytes);

            // 2. Split into overlapping chunks
            List<String> chunks = chunkingService.chunk(text);
            logger.info("Chunked document: documentId={}, chunks={}", documentId, chunks.size());

            // 3. Generate embeddings in batches
            List<List<Double>> embeddings = embeddingService.embedAll(chunks);

            // 4. Persist chunks + embeddings to pgvector
            persistChunks(documentId, chunks, embeddings);

            // 5. Mark document as completed
            updateStatus(documentId, "COMPLETED", null);

            // 6. Publish Kafka event for downstream consumers
            kafkaTemplate.send("document-ingested", documentId.toString(),
                    new DocumentIngestedEvent(documentId, doc.getFilename(), chunks.size()));

            logger.info("Ingestion complete: documentId={}, chunks={}", documentId, chunks.size());

        } catch (Exception e) {
            logger.error("Ingestion failed: documentId={}", documentId, e);
            updateStatus(documentId, "FAILED", e.getMessage());
            kafkaTemplate.send("ingestion-failed", documentId.toString(),
                    new IngestionFailedEvent(documentId, doc.getFilename(), e.getMessage()));
        }
    }

    private String extractText(byte[] fileBytes) throws IOException, SAXException, TikaException {
        BodyContentHandler handler = new BodyContentHandler(-1);
        AutoDetectParser   parser  = new AutoDetectParser();
        Metadata           meta    = new Metadata();
        parser.parse(new ByteArrayInputStream(fileBytes), handler, meta, new ParseContext());
        return handler.toString();
    }

    private void persistChunks(UUID documentId, List<String> chunks, List<List<Double>> embeddings) {
        for (int i = 0; i < chunks.size(); i++) {
            String vectorLiteral = toVectorLiteral(embeddings.get(i));
            jdbcTemplate.update(
                    "INSERT INTO document_chunks (document_id, chunk_index, content, embedding) " +
                    "VALUES (?, ?, ?, ?::vector) " +
                    "ON CONFLICT (document_id, chunk_index) DO UPDATE " +
                    "SET content = EXCLUDED.content, embedding = EXCLUDED.embedding",
                    documentId, i, chunks.get(i), vectorLiteral
            );
        }
    }

    @Transactional
    void updateStatus(UUID documentId, String status, String error) {
        jdbcTemplate.update(
                "UPDATE documents SET status = ?, error_message = ?, updated_at = NOW() WHERE id = ?",
                status, error, documentId
        );
    }

    private String toVectorLiteral(List<Double> embedding) {
        return "[" + embedding.stream().map(Object::toString).collect(Collectors.joining(",")) + "]";
    }

    public record DocumentIngestedEvent(UUID documentId, String filename, int chunkCount) {}
    public record IngestionFailedEvent(UUID documentId, String filename, String reason) {}
}
