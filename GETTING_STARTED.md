# Getting Started with Java LLM Enterprise

Welcome to the Java LLM Enterprise Repository! This guide will help you get started with development.

## Prerequisites

- **Java 25 LTS**: Download from the official JDK 25 release page or use [SDKMAN](https://sdkman.io/)
- **Maven 3.8+**: Download from [Apache Maven](https://maven.apache.org/download.cgi)
- **Docker & Docker Compose**: [Installation Guide](https://docs.docker.com/get-docker/)
- **Git**: [Installation Guide](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd java-llm-enterprise
```

### 2. Start Infrastructure

```bash
# Start all services (PostgreSQL, Redis, Kafka, Prometheus, Grafana, Jaeger)
docker-compose up -d

# Verify services are running
docker-compose ps
```

### 3. Set Environment Variables

```bash
# Copy example env file
cp .env.example .env

# Edit .env with your configuration
# - OPENAI_API_KEY: Get from https://platform.openai.com
# - Database credentials
# - AWS credentials (if deploying to AWS)
```

### 4. Build All Services

```bash
# From root directory
mvn clean install

# This will build:
# - common-library
# - gateway-service
# - ai-orchestrator-service
# - ingestion-service
```

### 5. Run Services Locally

#### Terminal 1: Gateway Service
```bash
cd gateway-service
mvn spring-boot:run
# Runs on http://localhost:8080
```

#### Terminal 2: AI Orchestrator Service
```bash
cd ai-orchestrator-service
mvn spring-boot:run
# Runs on http://localhost:8081
```

#### Terminal 3: Ingestion Service
```bash
cd ingestion-service
mvn spring-boot:run
# Runs on http://localhost:8082
```

### 6. Verify Setup

```bash
# Check gateway health
curl http://localhost:8080/actuator/health

# Check AI orchestrator health
curl http://localhost:8081/actuator/health

# Check ingestion service health
curl http://localhost:8082/actuator/health

# View logs
curl http://localhost:8080/actuator/metrics
```

## Access Services

- **API Gateway**: http://localhost:8080
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Jaeger UI**: http://localhost:16686
- **pgAdmin**: http://localhost:5050 (admin@example.com/admin)

## IDE Setup (IntelliJ)

1. **Open Project**
   - File → Open → Select project root

2. **Configure Java**
   - Settings → Project Structure → SDK
   - Select Java 25 LTS

3. **Configure Maven**
   - Settings → Build → Maven
   - Maven home path: `/path/to/maven`

4. **Enable Annotation Processing**
   - Settings → Build → Annotation Processors
   - Check "Enable annotation processing"

5. **Code Style**
   - Settings → Editor → Code Style → Java
   - Import: See `.editorconfig`

## Development Workflow

### 1. Create Feature Branch

```bash
git checkout -b feature/new-feature
```

### 2. Make Changes

Follow [CODING_STANDARDS.md](./CODING_STANDARDS.md) guidelines

### 3. Run Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Check coverage
mvn jacoco:report
# View report: target/site/jacoco/index.html
```

### 4. Commit & Push

```bash
git add .
git commit -m "feat: description of change"
git push origin feature/new-feature
```

### 5. Create Pull Request

- Title: `[FEATURE|BUG|REFACTOR] Short description`
- Description: What changed and why
- Link related issues

### 6. Code Review

- At least 2 approvals required
- All tests must pass
- Coverage maintained > 90%

## Common Tasks

### Run a Single Test

```bash
mvn test -Dtest=ChatServiceTest
```

### Run Tests with Coverage

```bash
mvn clean test jacoco:report
```

### Build Docker Image

```bash
cd ai-orchestrator-service
docker build -t ai-orchestrator:latest .
```

### Generate API Documentation

Services auto-generate OpenAPI docs:
- Gateway: http://localhost:8080/v3/api-docs
- Swagger UI: http://localhost:8080/swagger-ui.html

### Check for Vulnerabilities

```bash
mvn org.owasp:dependency-check-maven:check
```

### Format Code

```bash
mvn spotless:apply
```

## Debugging

### Enable Debug Logging

Edit `application.yml`:
```yaml
logging:
  level:
    com.enterprise: DEBUG
```

### Set Breakpoints

1. Click line number in IDE
2. Debug → Debug 'ServiceName'
3. Step through code

### View Traces

1. Open Jaeger UI: http://localhost:16686
2. Select service from dropdown
3. View trace timeline

## Troubleshooting

### Port Already in Use

```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>
```

### Database Connection Issues

```bash
# Check PostgreSQL is running
docker-compose logs postgres

# Verify connection
psql -h localhost -U postgres -d llm_db
```

### Maven Build Failure

```bash
# Clean and rebuild
mvn clean install -U

# Skip tests (not recommended)
mvn clean install -DskipTests
```

### Services Won't Start

1. Check environment variables in `.env`
2. Verify Docker services are running: `docker-compose ps`
3. Check logs: `docker-compose logs <service-name>`
4. Restart services: `docker-compose restart`

## Documentation

- [README.md](./README.md) - Overview
- [ARCHITECTURE.md](./ARCHITECTURE.md) - System design
- [AGENTS.md](./AGENTS.md) - AI coding rules
- [CODING_STANDARDS.md](./CODING_STANDARDS.md) - Code style
- [SECURITY_GUIDELINES.md](./SECURITY_GUIDELINES.md) - Security practices
- [BUSINESS_RULES.md](./BUSINESS_RULES.md) - Domain rules
- [docs/runbooks/deployment.md](./docs/runbooks/deployment.md) - Deployment guide
- [docs/runbooks/incident-response.md](./docs/runbooks/incident-response.md) - Incident procedures

## Useful Commands

```bash
# Build all services
mvn clean install

# Run specific service
cd <service-name> && mvn spring-boot:run

# Run tests
mvn test

# Skip tests during build
mvn install -DskipTests

# Run integration tests
mvn verify

# Check code quality
mvn sonar:sonar

# Format code
mvn spotless:apply

# Check formatting
mvn spotless:check

# Generate coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Next Steps

1. Read [ARCHITECTURE.md](./ARCHITECTURE.md) to understand system design
2. Review [AGENTS.md](./AGENTS.md) for AI coding patterns
3. Check [CODING_STANDARDS.md](./CODING_STANDARDS.md) for style guidelines
4. Explore the prompts in `prompts/` for AI-assisted development
5. Run the example tests to understand testing patterns

## Support

- **Documentation**: See `docs/` directory
- **Runbooks**: See `docs/runbooks/` for operational procedures
- **Issues**: Create GitHub issues for bugs/feature requests
- **Questions**: Ask in team Slack channel

---

**Good luck, and happy coding!** 🚀
