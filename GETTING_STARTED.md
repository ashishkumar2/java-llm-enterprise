# Getting Started

## Prerequisites

You only need two things:

- **Docker Desktop** — [install](https://docs.docker.com/get-docker/)
- **OpenAI API key** — [get one](https://platform.openai.com/api-keys)

Java 21 and Maven are **bundled** in `.tools/` — no system installation required.

---

## Quick start (3 steps)

### 1. Configure environment

```bash
cp .env.example .env
# Open .env and set:  OPENAI_API_KEY=sk-...
```

### 2. Start everything

**Linux / macOS:**
```bash
chmod +x start.sh
./start.sh all
```

**Windows:**
```bat
start.bat all
```

This will:
- Start Docker infrastructure (PostgreSQL + pgvector, Redis, Kafka, Zookeeper)
- Build all Maven modules
- Launch all three services in the background
- Print health check URLs when ready

### 3. Verify

```bash
curl http://localhost:8080/actuator/health   # Gateway
curl http://localhost:8081/actuator/health   # AI Orchestrator
curl http://localhost:8082/actuator/health   # Ingestion
```

All three should return `{"status":"UP"}`.

---

## Manual startup (optional)

If you prefer to run services individually, set the build environment first:

**Linux / macOS:**
```bash
export JAVA_HOME="$(pwd)/.tools/jdk-25.0.2"
export PATH="$JAVA_HOME/bin:$(pwd)/.tools/apache-maven-3.9.16/bin:$PATH"
```

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME = "$PWD\.tools\jdk-25.0.2"
$env:PATH = "$env:JAVA_HOME\bin;$PWD\.tools\apache-maven-3.9.16\bin;$env:PATH"
```

Then start services in order (orchestrator + ingestion first, gateway last):

```bash
# Terminal 1
docker-compose up -d postgres redis zookeeper kafka

# Terminal 2
SPRING_PROFILES_ACTIVE=local mvn -pl ai-orchestrator-service spring-boot:run

# Terminal 3
SPRING_PROFILES_ACTIVE=local mvn -pl ingestion-service spring-boot:run

# Terminal 4 (after terminals 2 & 3 are up)
SPRING_PROFILES_ACTIVE=local mvn -pl gateway-service spring-boot:run
```

---

## Try the RAG pipeline

### 1. Generate a JWT (dev shortcut)

For local testing, generate a HS256 JWT signed with the `JWT_SECRET` from your `.env`:

```bash
# Install jwt-cli:  npm install -g jwt-cli
jwt sign --secret "$JWT_SECRET" '{"sub":"dev-user","roles":["USER"]}'
# Copy the token
export TOKEN="<paste token here>"
```

### 2. Ingest a document

```bash
echo "pgvector enables vector similarity search in PostgreSQL." > /tmp/test.txt

curl -X POST http://localhost:8080/api/ingest \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/tmp/test.txt"
# Response: {"documentId":"...","status":"PROCESSING"}
```

Wait ~5 seconds for async processing to complete.

### 3. Chat with RAG context

```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"message":"What does pgvector do?","sessionId":"demo-1"}'
```

The response will include context retrieved from the ingested document.

---

## Observability

| Dashboard | URL | Login |
|-----------|-----|-------|
| Grafana | http://localhost:3000 | admin / admin |
| Jaeger (traces) | http://localhost:16686 | — |
| Prometheus | http://localhost:9090 | — |
| pgAdmin | http://localhost:5050 | admin@example.com / admin |

Start the full monitoring stack:
```bash
docker-compose up -d
```

---

## Running tests

```bash
# Unit tests only (fast, no Docker)
mvn test

# Integration tests (Testcontainers — needs Docker)
mvn verify

# Single module
mvn test -pl ai-orchestrator-service

# Coverage report (opens at target/site/jacoco/index.html)
mvn clean verify
```

---

## IDE setup (IntelliJ IDEA)

1. **Open** the project root as a Maven project
2. **Project SDK**: point to `.tools/jdk-25.0.2` in this directory
3. **Maven**: Settings → Build → Maven → Maven home path → `.tools/apache-maven-3.9.16`
4. **Annotation processing**: Settings → Build → Compiler → Annotation Processors → Enable
5. Run any `*Application.java` with `SPRING_PROFILES_ACTIVE=local` in the run config's env vars

---

## Stop everything

```bash
./start.sh stop        # Linux/macOS
start.bat stop         # Windows
```

Or manually:
```bash
docker-compose down
```

---

## Troubleshooting

**Kafka not ready / services won't connect**
```bash
docker-compose logs kafka | tail -20
# If Kafka is restarting, wait another 15-20s and retry
```

**Port already in use**
```bash
# Linux/macOS
lsof -i :8080 | awk 'NR>1{print $2}' | xargs kill -9
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**PostgreSQL connection refused**
```bash
docker-compose logs postgres | tail -10
# Ensure POSTGRES_URL in .env matches docker-compose service name/port
```

**`mvn: command not found`**
You forgot to set `JAVA_HOME` / `PATH`. Re-run the export commands above or use `./start.sh`.

**OpenAI 401 errors**
Check `OPENAI_API_KEY` in `.env` — it must start with `sk-`.

---

## Next steps

- Read [ARCHITECTURE.md](ARCHITECTURE.md) — system design and data flows
- Read [CODING_STANDARDS.md](CODING_STANDARDS.md) — patterns used in this codebase
- Read [CONTRIBUTING.md](CONTRIBUTING.md) — how to add features or fix bugs
- Browse `prompts/` — reusable AI prompt templates for common development tasks
