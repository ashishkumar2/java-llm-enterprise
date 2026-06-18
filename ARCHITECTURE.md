# Architecture & System Design

## System Overview

```
  Client (REST / curl)
          │
          │  JWT Bearer token
          ▼
┌─────────────────────────────────────────────────────────────┐
│                   Gateway Service :8080                      │
│                                                             │
│  RequestIdFilter  — injects X-Request-ID on every request  │
│  AuthFilter       — validates JWT, propagates X-User-ID    │
│  RateLimitFilter  — Redis sliding-window counter per user  │
│                                                             │
│  Routes:                                                    │
│    POST /api/ai/**   → AI Orchestrator :8081               │
│    GET  /api/ai/**   → AI Orchestrator :8081               │
│    POST /api/ingest  → Ingestion Service :8082             │
│    GET  /api/ingest/**→ Ingestion Service :8082            │
│                                                             │
│  Stack: Spring Cloud Gateway, Spring WebFlux (reactive)    │
└──────────────┬──────────────────────────┬───────────────────┘
               │                          │
               ▼                          ▼
┌──────────────────────────┐  ┌───────────────────────────────┐
│  AI Orchestrator :8081   │  │   Ingestion Service :8082     │
│                          │  │                               │
│  ChatController          │  │  IngestionController          │
│    POST /api/chat        │  │    POST /api/ingest           │
│    GET  /api/chat/       │  │    GET  /api/ingest/{id}      │
│         history/{id}     │  │                               │
│                          │  │  Async pipeline (@Async):     │
│  DefaultChatService      │  │    Tika text extraction       │
│    1. Redis history      │  │    ChunkingService (800/200)  │
│    2. Embed query        │  │    EmbeddingService (batch)   │
│    3. pgvector top-K     │  │    pgvector INSERT            │
│    4. Build prompt       │  │    Kafka event publish        │
│    5. OpenAI call        │  │                               │
│    6. Save to Redis      │  │  Stack: Servlet, JPA, Tika,  │
│                          │  │  Kafka, Spring AI Embedding   │
│  Stack: Servlet, JPA,    │  └───────────┬───────────────────┘
│  Spring AI, Redis        │              │
└──────────┬───────────────┘              │
           │                              │
           └──────────────┬───────────────┘
                          │
         ┌────────────────┼────────────────┐
         ▼                ▼                ▼
  ┌─────────────┐  ┌──────────┐  ┌───────────────┐
  │ PostgreSQL  │  │  Redis 7 │  │  Kafka        │
  │ + pgvector  │  │          │  │               │
  │             │  │ Chat     │  │ document-     │
  │ documents   │  │ session  │  │ ingested      │
  │ document_   │  │ history  │  │               │
  │   chunks    │  │ Rate     │  │ ingestion-    │
  │ chat_       │  │ limit    │  │ failed        │
  │   sessions  │  │ counters │  │               │
  │ chat_       │  │          │  │               │
  │   messages  │  └──────────┘  └───────────────┘
  └─────────────┘
```

---

## RAG Chat Flow

```
POST /api/ai/chat
  {"message": "...", "sessionId": "...", "userId": "..."}

Gateway (:8080)
  1. Validate JWT (Spring Security OAuth2 Resource Server)
  2. Increment Redis rate-limit counter; reject if over limit
  3. Inject X-User-ID header from JWT subject claim
  4. Rewrite /api/ai/chat → /api/chat; forward to :8081

AI Orchestrator (:8081)  —  DefaultChatService
  5. Load conversation history from Redis
       key = "chat:history:{sessionId}"
       LRANGE 0 -1 → List<StoredMessage{role, content}>
  6. Embed user message
       OpenAI text-embedding-3-small → List<Double> (1536 dims)
  7. pgvector cosine similarity search (top-K)
       SELECT content FROM document_chunks
       ORDER BY embedding <=> ?::vector LIMIT 5
  8. Build system prompt = enterprise persona
       + "Relevant context:\n{ragChunks}" (if RAG enabled)
  9. Assemble messages: [SystemMessage, ...history, UserMessage]
  10. ChatClient.call(Prompt) → OpenAI gpt-4
  11. RPUSH both turns to Redis; LTRIM to last 20; expire 24 h
  12. Return ChatResponse{sessionId, message, model, tokenCount}

GET /api/ai/chat/history/{sessionId}
  Returns List<{role, content}> from Redis (no LLM call)
```

---

## Document Ingestion Flow

```
POST /api/ingest  (multipart, max 50 MB)

Gateway (:8080)
  1. Validate JWT, rate limit
  2. Forward to :8082 (path unchanged)

Ingestion Service (:8082)  —  IngestionController
  3. Validate file extension (pdf, docx, txt, md, html, csv)
  4. Persist Document entity {filename, contentType, fileSize,
     status=PROCESSING, uploadedBy}
  5. Return 202 Accepted {documentId, status="PROCESSING"}

  @Async thread pool (5 core / 10 max)  — DocumentIngestionService
  6. Load Document from JPA
  7. Apache Tika AutoDetectParser → plain text
  8. ChunkingService.chunk(text)
       800-char windows, 200-char overlap
       Breaks at paragraph boundary → sentence boundary → hard cut
  9. EmbeddingService.embedAll(chunks)
       Batches of 100 chunks → OpenAI text-embedding-3-small
       Retries up to 3× with exponential backoff
  10. INSERT INTO document_chunks (document_id, chunk_index,
      content, embedding::vector) — upsert on conflict
  11. doc.markCompleted() → documentRepository.save(doc)
  12. Kafka publish: topic=document-ingested
      {documentId, filename, chunkCount}

  On failure:
  11. doc.markFailed(reason) → documentRepository.save(doc)
  12. Kafka publish: topic=ingestion-failed
      {documentId, filename, reason}

GET /api/ingest/{documentId}
  Returns current {documentId, filename, status, errorMessage}
  Poll until status = COMPLETED or FAILED
```

---

## Gateway Security Model

```
Inbound request
    │
    ▼
Spring Security OAuth2 Resource Server
    │  validates JWT signature against jwk-set-uri
    │  populates ReactiveSecurityContextHolder
    ▼
AuthFilterGatewayFilterFactory
    │  ctx.getAuthentication().getPrincipal() → Jwt
    │  injects X-User-ID: {jwt.subject} header
    ▼
RateLimitFilterGatewayFilterFactory
    │  key = "rate:{X-User-ID}" (or client IP)
    │  INCR key → count
    │  if count == 1: EXPIRE key 60s
    │  if count > limit: 429 + Retry-After: 60
    │  else: add X-RateLimit-Remaining header
    ▼
Downstream service
```

---

## Data Model

### `ingestion-service` (PostgreSQL)

```sql
documents
  id              UUID PRIMARY KEY
  filename        VARCHAR(500) NOT NULL
  content_type    VARCHAR(200)
  file_size_bytes BIGINT
  status          VARCHAR(50)  -- PROCESSING | COMPLETED | FAILED
  uploaded_by     VARCHAR(255)
  error_message   TEXT
  created_at      TIMESTAMPTZ
  updated_at      TIMESTAMPTZ

document_chunks
  id          BIGSERIAL PRIMARY KEY
  document_id UUID → documents(id) CASCADE DELETE
  chunk_index INT
  content     TEXT
  embedding   vector(1536)   -- IVFFlat cosine index (lists=100)
  created_at  TIMESTAMPTZ
  UNIQUE (document_id, chunk_index)
```

### `ai-orchestrator-service` (PostgreSQL)

```sql
chat_sessions
  id             VARCHAR(255) PRIMARY KEY
  user_id        VARCHAR(255)
  created_at     TIMESTAMPTZ
  last_active_at TIMESTAMPTZ

chat_messages
  id          BIGSERIAL PRIMARY KEY
  session_id  VARCHAR(255) → chat_sessions(id)
  role        VARCHAR(50)   -- user | assistant | system
  content     TEXT
  model       VARCHAR(100)
  token_count INT
  created_at  TIMESTAMPTZ
```

### Redis keys

| Key pattern | Type | Purpose |
|---|---|---|
| `chat:history:{sessionId}` | List | Conversation turns (JSON `{role, content}`) |
| `rate:{userId\|ip}` | String | Request count in current 60-second window |

---

## Key Design Decisions

**Reactive gateway, servlet backends** — The gateway is on the hot path of every request; WebFlux non-blocking I/O lets a single pod handle thousands of concurrent connections. The AI and ingestion services do blocking I/O (JPA, synchronous OpenAI calls) where the servlet thread model is simpler.

**pgvector instead of a dedicated vector database** — Eliminates an extra service to operate. Document metadata, embeddings, and chat history are all in one store with ACID guarantees. The IVFFlat index handles millions of vectors; switch to Pinecone or Weaviate when horizontal scale requires it.

**Kafka for ingestion events** — Ingestion is slow (Tika + batch OpenAI calls). Kafka decouples the HTTP 202 response from the processing pipeline, enables replay on failure, and lets downstream consumers (evaluation, audit, notifications) subscribe without modifying the ingestion service.

**Constructor injection everywhere** — Field injection hides dependencies and makes unit testing require Spring context or reflection. Constructor injection makes dependencies explicit; Mockito can inject mocks directly without any container.

**Redis sliding-window rate limiting** — Stateless gateway pods share a single Redis counter per user. The `INCR`+`EXPIRE` pattern is atomic and O(1) per request with no Lua script required.

---

## Module Map

| Module | Stack | Key classes |
|--------|-------|-------------|
| `common-library` | Plain Java | `GlobalExceptionHandler`, `MdcLoggingFilter`, `ErrorResponse`, `ResourceNotFoundException`, `AiProcessingException` |
| `gateway-service` | WebFlux, Spring Cloud Gateway | `AuthFilterGatewayFilterFactory`, `RateLimitFilterGatewayFilterFactory`, `RequestIdFilter` |
| `ai-orchestrator-service` | Servlet, Spring AI, JPA | `DefaultChatService`, `RagContextService`, `ConversationMemoryService`, `ChatController` |
| `ingestion-service` | Servlet, JPA, Tika, Kafka | `DocumentIngestionService`, `ChunkingService`, `EmbeddingService`, `IngestionController` |

---

## Observability

All services export:

- **Traces** — OpenTelemetry → OTel Collector → Jaeger (`http://localhost:16686`)
- **Metrics** — Micrometer Prometheus endpoint (`/actuator/prometheus`) → Prometheus → Grafana (`http://localhost:3000`)
- **Logs** — SLF4J + Logback with MDC fields: `request-id`, `trace-id`, `user-id`
- **Health** — Spring Actuator `/actuator/health` with readiness/liveness probes

Kafka events (`document-ingested`, `ingestion-failed`) can be consumed by external evaluation or audit services.
