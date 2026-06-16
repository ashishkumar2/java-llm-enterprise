# RAG Generation Prompt

## Task
Build a production-ready Retrieval Augmented Generation (RAG) implementation for enterprise systems.

## Context
Tech Stack:
- Java 25 LTS, Spring Boot 3
- Spring AI framework
- PostgreSQL with pgvector extension
- OpenAI embeddings (text-embedding-3-small)
- Redis for caching
- Kafka for async processing

## Architecture

```
Document Upload
    ↓
Chunking (semantic)
    ↓
Embedding Generation (OpenAI)
    ↓
Vector Storage (pgvector)
    ↓
Retrieval (semantic search)
    ↓
Context Injection
    ↓
LLM (GPT-4)
    ↓
Response Evaluation
```

## Components to Generate

### 1. Vector Store Configuration
- PostgreSQL pgvector setup
- Embedding dimension: 1536 (text-embedding-3-small)
- Index strategy (HNSW)
- Connection pooling

### 2. Chunking Service
- Semantic chunking strategy
- Chunk size: 500-1000 tokens
- Overlap: 200 tokens
- Metadata extraction (title, section, page number)

### 3. Embedding Service
- OpenAI integration
- Batch processing (reduce API calls)
- Caching strategy (Redis)
- Error handling & retry logic

### 4. Retrieval Service
- Vector similarity search
- Top-K results (default: 5)
- Metadata filtering
- Re-ranking strategy

### 5. RAG Pipeline
- End-to-end orchestration
- Conversation memory
- Context window management
- Structured logging & tracing

### 6. Controllers
- Document upload endpoint
- Query endpoint
- Search endpoint
- Document management endpoints

### 7. Tests
- Chunking strategy tests
- Embedding generation tests
- Vector search tests
- E2E RAG pipeline tests
- Integration tests with test PostgreSQL + pgvector

## Database Schema

```sql
-- Vector store table
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(500),
    source_url VARCHAR(1000),
    content_hash VARCHAR(64),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Document chunks with embeddings
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    chunk_number INT,
    content TEXT,
    metadata JSONB,
    embedding vector(1536),
    embedding_model VARCHAR(100),
    created_at TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents(id),
    CONSTRAINT document_chunks_embedding_idx UNIQUE (embedding)
);

CREATE INDEX ON document_chunks USING hnsw (embedding vector_cosine_ops);
```

## Configuration Example

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      model: text-embedding-3-small
      embedding-dimensions: 1536
    
    embedding:
      batch-size: 100
      cache-ttl: 86400  # 24 hours
      
    rag:
      chunk-size: 800
      chunk-overlap: 200
      retrieval-k: 5
      re-ranking-enabled: true
```

## Security Considerations
- Validate document uploads (virus scan)
- User isolation (only see own documents)
- Encryption at rest for sensitive data
- Audit logging for all RAG operations
- PII masking in search logs
- Rate limiting on retrieval endpoints

## Performance Targets
- Embedding generation: < 1 second per chunk (batch)
- Vector search: < 100ms for K=5
- E2E RAG latency: < 5 seconds
- Memory usage: Configurable based on chunk size

## Evaluation Metrics
- Retrieval precision (relevant documents)
- Retrieval recall (coverage)
- Embedding quality
- Response faithfulness (using RAGAS)
- Latency and throughput

## Acceptance Criteria
- [ ] Vector store initialized with pgvector
- [ ] Chunking strategy implemented
- [ ] Embeddings generated and cached
- [ ] Semantic search functional
- [ ] RAG pipeline working E2E
- [ ] Conversation memory working
- [ ] Unit tests > 90% coverage
- [ ] Integration tests passing
- [ ] Performance targets met
- [ ] Security controls in place
- [ ] Structured logging configured
- [ ] OpenTelemetry tracing added
