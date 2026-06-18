# CLAUDE.md — Java LLM Enterprise

## Project overview

Production-grade enterprise LLM platform built with Java 21 / Spring Boot 3. Three microservices behind a reactive API gateway expose RAG-powered chat and async document ingestion backed by PostgreSQL + pgvector, Redis, and Kafka. This is a portfolio project demonstrating real-world AI engineering patterns.

## Build environment

All tooling is bundled in `.tools/` — no system Java or Maven required.

```bash
# Set in every terminal session (or source .env first)
export JAVA_HOME="$(pwd)/.tools/jdk-25.0.2"
export PATH="$JAVA_HOME/bin:$(pwd)/.tools/apache-maven-3.9.16/bin:$PATH"
```

Java target: **21** (source/target in pom.xml). The bundled JDK is 25.0.2, which compiles to 21 bytecode.

## Key commands

```bash
# Build everything (skip tests)
mvn clean package -DskipTests

# Run all unit tests
mvn test

# Run integration tests (needs Docker)
mvn verify

# Start infrastructure
docker-compose up -d

# Run a single service (run from project root)
SPRING_PROFILES_ACTIVE=local mvn -pl gateway-service spring-boot:run
SPRING_PROFILES_ACTIVE=local mvn -pl ai-orchestrator-service spring-boot:run
SPRING_PROFILES_ACTIVE=local mvn -pl ingestion-service spring-boot:run
```

## Services & ports

| Service | Port | Purpose |
|---------|------|---------|
| `gateway-service` | 8080 | API Gateway — JWT auth, rate limiting, routing |
| `ai-orchestrator-service` | 8081 | RAG chat, conversation memory, OpenAI calls |
| `ingestion-service` | 8082 | Document upload, chunking, embedding, pgvector |

Start order: infrastructure → orchestrator + ingestion (parallel) → gateway.

## Infrastructure (docker-compose)

```bash
docker-compose up -d          # start all (infra + 3 app services)
docker-compose up -d postgres redis zookeeper kafka  # infra only for local dev
docker-compose logs -f kafka  # tail a specific service
```

Services: `postgres` (pgvector), `redis`, `zookeeper`, `kafka`, `prometheus`, `grafana` (:3000), `otel-collector`, `jaeger` (:16686), `pgadmin` (:5050), `ai-orchestrator`, `ingestion`, `gateway`.

**Docker builds** — all 3 app services have multi-stage Dockerfiles (`eclipse-temurin:21-jdk-alpine` build → `eclipse-temurin:21-jre-alpine` runtime). Build context is the project root. Gateway needs `AI_ORCHESTRATOR_URL` and `INGESTION_URL` env vars (set automatically in docker-compose).

## Environment configuration

```bash
cp .env.example .env
# Required vars:
# OPENAI_API_KEY=sk-...
# POSTGRES_URL=jdbc:postgresql://localhost:5432/llm_db
# REDIS_HOST=localhost
# KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

## Module structure

```
common-library/          Shared DTOs, exceptions, filters (not a Spring Boot app)
gateway-service/         WebFlux + Spring Cloud Gateway (reactive, no JPA)
ai-orchestrator-service/ Servlet stack, JPA, Spring AI, LangChain4j
ingestion-service/       Servlet stack, JPA, Apache Tika, async Kafka producer
```

## Key source files

| File | What it does |
|------|-------------|
| `ai-orchestrator-service/.../service/DefaultChatService.java` | Core RAG loop: Redis history → pgvector context → OpenAI → persist |
| `ai-orchestrator-service/.../service/RagContextService.java` | pgvector cosine similarity search (configurable top-K) |
| `ai-orchestrator-service/.../service/ConversationMemoryService.java` | Redis session storage + `getStoredHistory()` for API |
| `ai-orchestrator-service/.../api/ChatController.java` | `POST /api/chat` and `GET /api/chat/history/{sessionId}` |
| `ingestion-service/.../service/DocumentIngestionService.java` | Async pipeline: Tika → chunk → embed → pgvector → Kafka |
| `ingestion-service/.../service/ChunkingService.java` | 800/200 sliding window with sentence/paragraph boundary detection |
| `ingestion-service/.../service/EmbeddingService.java` | OpenAI text-embedding-3-small, batch 100, retry 3× |
| `ingestion-service/.../api/IngestionController.java` | `POST /api/ingest` and `GET /api/ingest/{documentId}` |
| `gateway-service/.../filter/AuthFilterGatewayFilterFactory.java` | JWT validation, X-User-ID propagation |
| `gateway-service/.../filter/RateLimitFilterGatewayFilterFactory.java` | Redis sliding-window rate limiting |

## Architecture patterns

- **Controllers** — HTTP only. No business logic. Return DTOs.
- **Services** — Business logic and orchestration. Constructor injection only.
- **Repositories** — JPA only. No business logic.
- **No `@Autowired` field injection** — constructor injection everywhere.
- **Records** for immutable DTOs (`ChatRequest`, `ChatResponse`, `ErrorResponse`).
- **GlobalExceptionHandler** in common-library handles all error responses.
- **MDC logging** with request-id, trace-id on every log line.

## Testing conventions

```bash
mvn test                              # unit tests only
mvn verify -Pit                       # integration tests (Testcontainers)
mvn test -pl ai-orchestrator-service  # single module
```

- Naming: `methodName_givenCondition_expectedResult`
- Unit: Mockito mocks for all external dependencies
- Integration: Testcontainers (PostgreSQL, Kafka — no manual setup needed)
- Coverage target: 90% line coverage via JaCoCo

## Common tasks

**Add a new chat endpoint:**
1. Add method to `ChatService` interface
2. Implement in `DefaultChatService`
3. Add route in `ChatController`
4. Add gateway route in `gateway-service/src/main/resources/application.yml`

**Add a new document format:**
1. Register extension in `IngestionController` allowed-types list
2. Apache Tika handles parsing automatically for most formats

**Poll document ingestion status:**
```bash
curl http://localhost:8080/api/ingest/{documentId} -H "Authorization: Bearer $TOKEN"
# returns {status: "PROCESSING"|"COMPLETED"|"FAILED"}
```

**Retrieve conversation history:**
```bash
curl http://localhost:8080/api/ai/chat/history/{sessionId} -H "Authorization: Bearer $TOKEN"
# returns [{role, content}, ...]
```

**Extend conversation memory:**
- `ConversationMemoryService` uses Redis LRANGE/RPUSH; TTL configurable via `app.memory.ttl-hours` (default 24h); sliding window configurable via `app.memory.max-messages` (default 20)

**Change LLM model:**
- Set `OPENAI_MODEL` in `.env` (default: `gpt-4`)
- Embedding model: `OPENAI_EMBEDDING_MODEL` (default: `text-embedding-3-small`, 1536 dims)

## Flyway migrations

Schema managed by Flyway. Migration files in:
- `ai-orchestrator-service/src/main/resources/db/migration/`
- `ingestion-service/src/main/resources/db/migration/`

Never edit existing `V__*.sql` files — add new `V{n+1}__*.sql` instead.

## Observability

- Metrics: `http://localhost:9090` (Prometheus) and `http://localhost:3000` (Grafana, admin/admin)
- Traces: `http://localhost:16686` (Jaeger)
- Health endpoints: `http://localhost:{port}/actuator/health`

## Feature flags (in .env)

```
FEATURE_RAG_ENABLED=true           # pgvector context injection
FEATURE_AGENTS_ENABLED=false       # autonomous agents (not implemented)
FEATURE_MULTI_LLM_ROUTING=false    # multi-provider routing (not implemented)
```
