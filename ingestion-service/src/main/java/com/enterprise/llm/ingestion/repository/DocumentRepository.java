package com.enterprise.llm.ingestion.repository;

import com.enterprise.llm.ingestion.model.Document;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {}
