package com.enterprise.llm.ingestion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String filename;

    @Column(name = "content_type", length = 200)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentStatus status = DocumentStatus.PROCESSING;

    @Column(name = "uploaded_by", nullable = false, length = 255)
    private String uploadedBy;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Document() {}

    public Document(String filename, String contentType, Long fileSizeBytes, String uploadedBy) {
        this.filename      = filename;
        this.contentType   = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.uploadedBy    = uploadedBy;
    }

    public UUID getId()                    { return id; }
    public String getFilename()            { return filename; }
    public String getContentType()         { return contentType; }
    public Long getFileSizeBytes()         { return fileSizeBytes; }
    public DocumentStatus getStatus()      { return status; }
    public String getUploadedBy()          { return uploadedBy; }
    public String getErrorMessage()        { return errorMessage; }
    public Instant getCreatedAt()          { return createdAt; }
    public Instant getUpdatedAt()          { return updatedAt; }

    public void markCompleted() {
        this.status    = DocumentStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status       = DocumentStatus.FAILED;
        this.errorMessage = reason;
        this.updatedAt    = Instant.now();
    }
}
