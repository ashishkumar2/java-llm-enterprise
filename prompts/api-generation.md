# API Generation Prompt

## Task
Generate a production-ready Spring Boot REST API endpoint following enterprise architecture patterns.

## Context
Tech Stack:
- Java 25 LTS
- Spring Boot 3.x
- PostgreSQL database
- JPA/Hibernate
- OpenAPI/Swagger
- Structured logging
- OpenTelemetry tracing

## Requirements

### API Design
- RESTful endpoint design (proper HTTP methods)
- Request/Response DTOs (never expose entities)
- Comprehensive input validation
- OpenAPI annotations for documentation
- Error handling with meaningful messages
- CORS and security headers

### Code Structure
- Controller layer (request handling only)
- Service layer (business logic)
- Repository layer (data access)
- DTO layer (data transfer)
- Entity layer (JPA models)
- Exception handling

### Non-Functional Requirements
- Unit tests (minimum 90% coverage)
- Integration tests with Testcontainers
- Structured logging with SLF4J
- OpenTelemetry tracing
- Constructor injection only
- Transaction management (@Transactional at service level)
- Input validation at controller level
- Never expose stack traces

## Output Format

### 1. API Specification
- HTTP method and path
- Request body schema
- Response schema
- Error responses
- Success response

### 2. Controller
```java
@RestController
@RequestMapping("/api/v1/resource")
public class ResourceController {
    // Implementation
}
```

### 3. Service
```java
@Service
public class ResourceService {
    // Business logic
}
```

### 4. DTOs
- CreateResourceRequest
- UpdateResourceRequest
- ResourceResponse

### 5. Entity
```java
@Entity
@Table(name = "resources")
public class Resource {
    // JPA mapping
}
```

### 6. Repository
```java
@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    // Query methods
}
```

### 7. Unit Tests
- Service layer tests with Mockito
- Repository tests
- Controller tests
- Integration tests with Testcontainers

## Coding Standards
- No business logic in controllers
- Constructor injection for all dependencies
- @Transactional only at service level
- Parameterized queries (prevent SQL injection)
- Mask PII in logs
- Use UUID for distributed systems
- Idempotent operations where possible
- Soft delete pattern for data integrity

## Example API

**Request**:
```json
POST /api/v1/chats
{
  "sessionId": 123,
  "message": "Hello, how are you?"
}
```

**Response**:
```json
{
  "id": 456,
  "sessionId": 123,
  "message": "Hello, how are you?",
  "response": "I'm doing great! How can I help?",
  "createdAt": "2026-06-01T10:00:00Z"
}
```

## Acceptance Criteria
- [ ] API follows REST conventions
- [ ] Request/Response DTOs defined
- [ ] Input validation implemented
- [ ] Unit tests pass (>90% coverage)
- [ ] Integration tests pass
- [ ] Structured logging configured
- [ ] OpenTelemetry tracing added
- [ ] Error handling comprehensive
- [ ] Documentation complete
- [ ] Security best practices followed
