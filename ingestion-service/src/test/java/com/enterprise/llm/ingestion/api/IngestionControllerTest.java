package com.enterprise.llm.ingestion.api;

import com.enterprise.llm.common.exception.ResourceNotFoundException;
import com.enterprise.llm.ingestion.model.Document;
import com.enterprise.llm.ingestion.model.DocumentStatus;
import com.enterprise.llm.ingestion.model.IngestionResponse;
import com.enterprise.llm.ingestion.repository.DocumentRepository;
import com.enterprise.llm.ingestion.service.DocumentIngestionService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionControllerTest {

    @Mock private DocumentRepository       documentRepository;
    @Mock private DocumentIngestionService ingestionService;

    private IngestionController controller;

    @BeforeEach
    void setUp() {
        controller = new IngestionController(documentRepository, ingestionService);
    }

    @Test
    void ingest_validFile_returns202AndTriggersAsync() throws Exception {
        Document saved = new Document("test.txt", "text/plain", 100L, "user-1");
        when(documentRepository.save(any(Document.class))).thenReturn(saved);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "hello world".getBytes());

        var response = controller.ingest(file, "user-1");

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("PROCESSING");
        verify(ingestionService).ingest(any(), any(byte[].class));
    }

    @Test
    void ingest_emptyFile_returns400() throws Exception {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        var response = controller.ingest(empty, "user-1");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(ingestionService, never()).ingest(any(), any());
    }

    @Test
    void ingest_disallowedExtension_returns400() throws Exception {
        MockMultipartFile exe = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", "data".getBytes());

        var response = controller.ingest(exe, "user-1");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(ingestionService, never()).ingest(any(), any());
    }

    @Test
    void status_existingDocument_returnsCurrentStatus() {
        UUID id = UUID.randomUUID();
        Document doc = new Document("report.pdf", "application/pdf", 512L, "user-1");
        when(documentRepository.findById(id)).thenReturn(Optional.of(doc));

        var response = controller.status(id);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        IngestionResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.filename()).isEqualTo("report.pdf");
        assertThat(body.status()).isEqualTo(DocumentStatus.PROCESSING.name());
    }

    @Test
    void status_unknownDocument_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.status(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
