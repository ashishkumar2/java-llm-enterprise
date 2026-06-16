package com.enterprise.llm.ingestion.api;

import com.enterprise.llm.ingestion.model.Document;
import com.enterprise.llm.ingestion.model.IngestionResponse;
import com.enterprise.llm.ingestion.repository.DocumentRepository;
import com.enterprise.llm.ingestion.service.DocumentIngestionService;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ingest")
public class IngestionController {
    private static final Logger logger = LoggerFactory.getLogger(IngestionController.class);
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "docx", "txt", "md", "html", "csv");

    private final DocumentRepository       documentRepository;
    private final DocumentIngestionService ingestionService;

    public IngestionController(
            DocumentRepository documentRepository,
            DocumentIngestionService ingestionService
    ) {
        this.documentRepository = documentRepository;
        this.ingestionService   = ingestionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IngestionResponse> ingest(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-ID", defaultValue = "anonymous") String userId
    ) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest().build();
        }

        // Persist document record — status starts as PROCESSING
        Document doc = documentRepository.save(
                new Document(file.getOriginalFilename(), file.getContentType(),
                        file.getSize(), userId));

        logger.info("Document accepted: documentId={}, filename={}, uploadedBy={}",
                doc.getId(), doc.getFilename(), userId);

        // Trigger async ingestion (returns immediately)
        ingestionService.ingest(doc.getId(), file.getBytes());

        return ResponseEntity.accepted().body(IngestionResponse.accepted(doc.getId(), doc.getFilename()));
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
