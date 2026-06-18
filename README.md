# Enterprise LLM Platform

> Production-grade AI backend platform — RAG chat, async document ingestion, and enterprise security — built with Java 21, Spring Boot 3, and OpenAI.

[![CI](https://github.com/ashishkumar2/enterprise-rag-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/ashishkumar2/enterprise-rag-platform/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring_AI-0.8-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-316192?logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)
[![Kafka](https://img.shields.io/badge/Apache_Kafka-3.5-231F20?logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## What this project demonstrates

This is a portfolio project showing how to build a **production-ready LLM backend** from scratch — the kind of platform a team at a startup or enterprise would actually ship. It covers the full stack of concerns that appear in real AI engineering work:

- Retrieval-Augmented Generation (RAG) with vector similarity search
- Async document ingestion pipeline with event-driven architecture
- Enterprise security: JWT auth, rate limiting, reactive API gateway
- Distributed systems: Kafka events, Redis sessions, pgvector embeddings
- Observability: OpenTelemetry traces, Prometheus metrics, Grafana dashboards
- Infrastructure as code: Docker Compose, Kubernetes manifests, Terraform (AWS)

---

## Architecture

```
                          ┌──────────────────────────────────┐
  Client Request          │        Gateway Service :8080      │
  ──────────────────────► │  JWT Auth · Rate Limit · Routing  │
                          │  Spring Cloud Gateway + WebFlux   │
                          └──────────┬───────────────┬────────┘
                                     │               │
                          ┌──────────▼──────┐ ┌──────▼──────────────┐
                          │ AI Orchestrator  │ │  Ingestion Service   │
                          │    :8081         │ │      :8082           │
                          │                 │ │                      │
                          │ RAG Chat Loop   │ │ Upload → Tika parse  │
                          │ Prompt Assembly │ │ Chunk → Embed        │
                          │ OpenAI Calls    │ │ pgvector store       │
                          │ Token counting  │ │ Kafka event publish  │
                          └──┬──────────┬──┘ └────────────┬─────────┘
                             │          │                  │
               ┌─────────────▼─┐  ┌────▼──────┐  ┌──────▼────────┐
               │  PostgreSQL   │  │   Redis    │  │    Kafka      │
               │  + pgvector   │  │  Sessions  │  │  document-    │
               │  Documents    │  │  Cache     │  │  ingested     │
               │  Embeddings   │  │            │  │  events       │
               └───────────────┘  └────────────┘  └───────────────┘
```

### RAG Chat flow

```
POST /api/ai/chat
  → Gateway: validate JWT, check rate limit
  → AI Orchestrator:
      1. Load conversation history from Redis (last N turns)
      2. Embed user query (text-embedding-3-small)
      3. Search pgvector for top-5 relevant document chunks
      4. Build system prompt = context chunks + chat history + user message
      5. Call OpenAI gpt-4, stream response
      6. Persist assistant turn to Redis (1 hr TTL)
      7. Return ChatResponse with token count
```

### Document ingestion flow

```
POST /api/ingest  (multipart file, up to 50 MB)
  → Gateway: validate JWT
  → Ingestion Service:
      1. Persist Document entity (status=PROCESSING) → 202 Accepted
      2. Async thread pool (5 core / 10 max):
           a. Apache Tika: extract text from PDF/DOCX/TXT/HTML/CSV
           b. Chunk: 800-char windows, 200-char overlap
           c. Embed batches of 100 chunks via OpenAI
           d. Persist to document_chunks with pgvector column
           e. Status → COMPLETED
           f. Publish document-ingested event to Kafka
```

---

## Tech stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.2.12 |
| AI Frameworks | Spring AI + LangChain4j | 0.8.0 / 0.24.0 |
| API Gateway | Spring Cloud Gateway | 2023.0.3 |
| Reactive | Spring WebFlux + Project Reactor | — |
| Vector DB | PostgreSQL + pgvector | 15 / 0.5 |
| Embeddings | OpenAI text-embedding-3-small | 1536 dims |
| LLM | OpenAI gpt-4 (Azure optional) | — |
| Cache / Sessions | Redis 7 | Lettuce client |
| Messaging | Apache Kafka | 7.5.0 (Confluent) |
| Security | Spring Security + OAuth2 Resource Server + JJWT | 0.11.5 |
| File Parsing | Apache Tika | 2.8.0 |
| ORM / Migrations | JPA + Hibernate + Flyway | Spring Boot defaults |
| Testing | JUnit 5, Mockito 5, Testcontainers | — |
| Observability | OpenTelemetry + Prometheus + Grafana + Jaeger | — |
| Containers | Docker Compose | — |
| Cloud / IaC | Kubernetes + Terraform (AWS ECS, RDS, ElastiCache, MSK) | — |
| Build | Maven 3.9 | bundled in `.tools/` |

---

## Project structure

```
enterprise-rag-platform/
├── common-library/                # Shared DTOs, exceptions, MDC logging filter
├── gateway-service/               # Reactive API gateway (auth, rate limiting, routing)
│   └── Dockerfile                 # Multi-stage JDK21→JRE21 image
├── ai-orchestrator-service/       # RAG chat engine (Spring AI, pgvector, Redis)
│   └── Dockerfile
├── ingestion-service/             # Document pipeline (Tika, chunking, embeddings, Kafka)
│   └── Dockerfile
├── infrastructure/
│   ├── kubernetes/                # K8s deployment notes
│   ├── terraform/                 # AWS IaC notes (ECS, RDS, ElastiCache, MSK)
│   └── monitoring/                # Prometheus + OTel Collector configs
├── docs/runbooks/                 # Deployment and incident-response runbooks
├── prompts/                       # Reusable AI prompt templates for dev workflows
├── docker-compose.yml             # Full local stack (12 services)
├── .dockerignore
├── start.sh / start.bat           # One-command local startup scripts
├── CLAUDE.md                      # Claude Code project intelligence
└── ARCHITECTURE.md                # System design, data flows, design decisions
```

---

## Quick start

### Prerequisites

- Docker Desktop
- OpenAI API key

### 1. Clone and configure

```bash
git clone https://github.com/ashishkumar2/enterprise-rag-platform.git
cd enterprise-rag-platform

cp .env.example .env
# Edit .env — set OPENAI_API_KEY at minimum
```

### 2. Start infrastructure

```bash
docker-compose up -d postgres redis zookeeper kafka
# Wait ~30 seconds for Kafka to be ready
docker-compose ps   # verify all healthy
```

### 3. Build services

```bash
# Uses bundled JDK 25 + Maven in .tools/
export JAVA_HOME="$(pwd)/.tools/jdk-25.0.2"
export PATH="$JAVA_HOME/bin:$(pwd)/.tools/apache-maven-3.9.16/bin:$PATH"

mvn clean package -DskipTests
```

### 4. Run services

Open three terminals:

```bash
# Terminal 1 — AI Orchestrator
SPRING_PROFILES_ACTIVE=local mvn -pl ai-orchestrator-service spring-boot:run

# Terminal 2 — Ingestion Service
SPRING_PROFILES_ACTIVE=local mvn -pl ingestion-service spring-boot:run

# Terminal 3 — Gateway (start last)
SPRING_PROFILES_ACTIVE=local mvn -pl gateway-service spring-boot:run
```

### 5. Test the API

```bash
# Chat (RAG)
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT" \
  -d '{"message": "What documents have been ingested?", "sessionId": "demo-1"}'

# Ingest a document
curl -X POST http://localhost:8080/api/ingest \
  -H "Authorization: Bearer YOUR_JWT" \
  -F "file=@/path/to/document.pdf"

# Health checks
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

### 6. Observability dashboards

| Dashboard | URL | Credentials |
|-----------|-----|-------------|
| Grafana | http://localhost:3000 | admin / admin |
| Jaeger tracing | http://localhost:16686 | — |
| Prometheus | http://localhost:9090 | — |
| pgAdmin | http://localhost:5050 | admin@example.com / admin |

---

## API reference

All requests go through the gateway at `:8080` with `Authorization: Bearer <JWT>`.

### Chat API

```
POST /api/ai/chat
Content-Type: application/json

{
  "message":   "string — user message (max 5000 chars)",
  "sessionId": "string — conversation session ID",
  "userId":    "string — caller identity",
  "context":   "string — optional extra context (max 5000 chars)"
}

Response 200:
{
  "sessionId":  "string",
  "message":    "string — assistant reply",
  "model":      "gpt-4",
  "tokenCount": 342,
  "createdAt":  "2026-06-16T10:30:00Z"
}

GET /api/ai/chat/history/{sessionId}

Response 200:
[
  {"role": "user",      "content": "What is pgvector?"},
  {"role": "assistant", "content": "pgvector is a PostgreSQL extension..."}
]
```

### Ingestion API

```
POST /api/ingest
Content-Type: multipart/form-data

file: <binary>   — PDF, DOCX, TXT, MD, HTML, CSV (max 50 MB)

Response 202 Accepted:
{
  "documentId": "uuid",
  "status":     "PROCESSING",
  "filename":   "document.pdf",
  "message":    "Document accepted and queued for ingestion"
}

GET /api/ingest/{documentId}     — poll async status

Response 200:
{
  "documentId":   "uuid",
  "filename":     "document.pdf",
  "status":       "COMPLETED",   -- PROCESSING | COMPLETED | FAILED
  "message":      null           -- error detail if FAILED
}
```

---

## Running tests

```bash
# Unit tests (fast, no Docker needed)
mvn test

# Integration tests (requires Docker for Testcontainers)
mvn verify

# Single module
mvn test -pl ai-orchestrator-service

# Coverage report (opens in browser)
mvn verify && open ai-orchestrator-service/target/site/jacoco/index.html
```

Tests use Testcontainers — real PostgreSQL and Kafka spin up automatically, no manual setup.

---

## Configuration reference

Key environment variables (full list in [`.env.example`](.env.example)):

| Variable | Default | Description |
|----------|---------|-------------|
| `OPENAI_API_KEY` | — | **Required.** OpenAI API key |
| `OPENAI_MODEL` | `gpt-4` | Chat model |
| `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` | Embedding model |
| `POSTGRES_URL` | `jdbc:postgresql://localhost:5432/llm_db` | Database URL |
| `REDIS_HOST` | `localhost` | Redis host |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `JWT_SECRET` | — | JWT signing secret (min 256-bit) |
| `FEATURE_RAG_ENABLED` | `true` | Enable pgvector context retrieval |
| `RATE_LIMIT_PER_MINUTE` | `60` | Per-user rate limit |

---

## Security model

- **JWT auth** enforced at the gateway for all `/api/**` routes
- **Rate limiting** via Redis token bucket (configurable per minute/hour)
- **PII masking** in MDC-structured logs
- **Parameterized queries** via JPA — no raw SQL concatenation
- **File validation** — extension allowlist + Apache Tika MIME detection
- **Secrets** designed for AWS Secrets Manager / environment injection (never hardcoded)
- **CORS** configurable per environment

---

## Deployment

### Kubernetes

```bash
kubectl apply -f infrastructure/kubernetes/
```

Manifests include resource limits, liveness/readiness probes, ConfigMaps, and Secrets references.

### Terraform (AWS)

Provisions: ECS Fargate (services), RDS PostgreSQL (pgvector), ElastiCache Redis, MSK Kafka, VPC, ALB, Security Groups.

```bash
cd infrastructure/terraform
terraform init
terraform plan -var-file=environments/prod.tfvars
terraform apply
```

---

## Design decisions

**Why Spring Cloud Gateway (reactive) instead of a servlet gateway?**
Gateway sits on the hot path of every request. WebFlux non-blocking I/O means a single gateway pod can handle thousands of concurrent connections without thread-per-request overhead.

**Why pgvector instead of a dedicated vector database?**
For most enterprise RAG workloads, pgvector on PostgreSQL eliminates one more service to operate and keeps document metadata and embeddings transactionally consistent in one store. Switch to Pinecone or Weaviate when scale demands it.

**Why Kafka for ingestion events instead of direct calls?**
Ingestion is slow (Tika parse + batch embedding calls). Kafka decouples the HTTP response (202 Accepted) from the processing pipeline, enables replay on failure, and lets downstream consumers (evaluation, audit) subscribe independently.

**Why constructor injection everywhere?**
Field injection hides dependencies and makes unit testing harder (you'd need Spring context or reflection hacks). Constructor injection makes dependencies explicit and lets Mockito inject mocks directly.

---

## Documentation

| File | Contents |
|------|---------|
| [`CLAUDE.md`](CLAUDE.md) | Claude Code project intelligence — commands, patterns, key files |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | System design, data flows, ADRs |
| [`CODING_STANDARDS.md`](CODING_STANDARDS.md) | Java patterns, Spring conventions, naming |
| [`AGENTS.md`](AGENTS.md) | AI-assisted coding rules for LLM pair-programming |
| [`SECURITY_GUIDELINES.md`](SECURITY_GUIDELINES.md) | Auth, encryption, audit practices |
| [`GETTING_STARTED.md`](GETTING_STARTED.md) | Detailed developer onboarding |
| [`docs/`](docs/) | API contracts, sequence diagrams, runbooks |
| [`prompts/`](prompts/) | Reusable prompt templates for AI-assisted development |

---

## Skills demonstrated

This project was built to showcase:

| Domain | Skills |
|--------|--------|
| **AI / LLM Engineering** | RAG pipeline design, embedding generation, pgvector similarity search, prompt engineering, conversation memory, token management |
| **Java / Spring** | Spring Boot 3, Spring AI, Spring Cloud Gateway, WebFlux (reactive), JPA, Flyway, async processing, constructor DI |
| **Distributed Systems** | Kafka event-driven architecture, Redis caching/sessions, microservice decomposition, API gateway pattern |
| **Security** | JWT auth, OAuth2 Resource Server, rate limiting, PII-safe logging |
| **Observability** | OpenTelemetry distributed tracing, Prometheus metrics, Grafana dashboards, structured MDC logging |
| **Testing** | Unit tests (Mockito), integration tests (Testcontainers), JaCoCo coverage |
| **Infrastructure / Cloud** | Docker Compose, Kubernetes manifests, Terraform AWS (ECS, RDS, ElastiCache, MSK) |
| **Developer Experience** | CLAUDE.md for AI-assisted coding, prompt templates, runbooks, architecture docs |

---

## License

MIT — see [LICENSE](LICENSE). Built as a portfolio project by [Ashish Basani](https://github.com/ashishkumar2).
