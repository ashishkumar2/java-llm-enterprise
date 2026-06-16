# Coding Standards

## Overview

This document defines the coding standards for the Java LLM Enterprise platform. All code contributions must comply with these standards.

## Java Language Features

### Java Version
- **Target**: Java 25 LTS
- **Preview Features**: Use sparingly and only when mature
- **Record Classes**: Prefer for immutable data carriers (Java 14+)
- **Sealed Classes**: Use for restricted inheritance hierarchies
- **Pattern Matching**: Leverage for cleaner switch/instanceof statements

### Naming Conventions

**Classes**: PascalCase
```java
public class ChatService { }
public class UserRepository { }
```

**Methods & Variables**: camelCase
```java
public void processChat(ChatRequest request) { }
private String extractContext(String query) { }
```

**Constants**: UPPER_SNAKE_CASE
```java
private static final int MAX_RETRIES = 3;
private static final String API_KEY_ENV = "OPENAI_API_KEY";
```

**Package Names**: lowercase, reverse domain notation
```
com.enterprise.ai.orchestrator
com.enterprise.ai.rag
com.enterprise.ai.agents
```

**Test Classes**: ClassNameTest or ClassNameTests
```java
class ChatServiceTest { }
class EmbeddingServiceTests { }
```

## Code Organization

### Package Structure

```
src/main/java/com/enterprise/ai/
├── controller/      # HTTP endpoints
├── service/         # Business logic
├── orchestrator/    # AI orchestration logic
├── repository/      # Data access
├── entity/          # JPA entities
├── dto/            # Data transfer objects
├── config/         # Configuration classes
├── exception/      # Custom exceptions
├── util/           # Utility classes
├── aspect/         # AOP aspects
├── filter/         # Request/Response filters
└── security/       # Security-related classes
```

### Class Organization

```java
public class ChatService {
    
    // Constants
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    
    // Fields (inject via constructor)
    private final ChatRepository repository;
    private final OpenAiClient openAi;
    
    // Constructor
    public ChatService(ChatRepository repository, OpenAiClient openAi) {
        this.repository = repository;
        this.openAi = openAi;
    }
    
    // Public methods
    public ChatResponse processChat(ChatRequest request) { }
    
    // Private methods
    private String generateResponse(String prompt) { }
}
```

## Code Style

### Formatting

- **Indentation**: 4 spaces (never tabs)
- **Line Length**: Maximum 120 characters (soft), 150 (hard limit)
- **Blank Lines**: 2 between class members, 1 between methods
- **Braces**: Opening brace on same line (Java style)

```java
if (condition) {
    doSomething();
}

for (Item item : items) {
    process(item);
}
```

### Imports

- Organize imports by: static, then alphabetically
- Remove unused imports
- No wildcard imports (`import java.util.*;` ❌)

```java
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.enterprise.ai.dto.ChatRequest;
import com.enterprise.ai.service.ChatService;
import org.springframework.stereotype.Service;
```

## Spring Framework Conventions

### Dependency Injection

**Use constructor injection ONLY** (no field injection):

```java
@Service
public class ChatService {
    private final ChatRepository repository;
    private final OpenAiClient openAi;
    private final CacheManager cache;
    
    public ChatService(ChatRepository repository, 
                      OpenAiClient openAi, 
                      CacheManager cache) {
        this.repository = repository;
        this.openAi = openAi;
        this.cache = cache;
    }
}
```

### Annotations

**Controllers**:
```java
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService service;
    
    @PostMapping
    public ResponseEntity<ChatResponse> createChat(@Valid @RequestBody ChatRequest req) {
        return ResponseEntity.ok(service.processChat(req));
    }
}
```

**Services**:
```java
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {
    // Only @Transactional at service level
}
```

**Repositories**:
```java
@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    Optional<Chat> findBySessionId(Long sessionId);
    
    @Query("SELECT c FROM Chat c WHERE c.userId = :userId")
    List<Chat> findByUser(@Param("userId") Long userId);
}
```

## Exception Handling

### Custom Exceptions

```java
public abstract class ApplicationException extends RuntimeException {
    private final String errorCode;
    
    public ApplicationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String resource, Long id) {
        super(
            String.format("%s not found: %d", resource, id),
            "RESOURCE_NOT_FOUND"
        );
    }
}
```

### Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
        ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "An error occurred"));
    }
}
```

## Logging Standards

### Structured Logging

```java
private static final Logger log = LoggerFactory.getLogger(ChatService.class);

public ChatResponse processChat(ChatRequest request) {
    String traceId = UUID.randomUUID().toString();
    MDC.put("traceId", traceId);
    MDC.put("userId", request.getUserId());
    
    try {
        log.info("Processing chat request", Map.of(
            "sessionId", request.getSessionId(),
            "messageLength", request.getMessage().length()
        ));
        
        ChatResponse response = doProcess(request);
        
        log.info("Chat processed successfully", Map.of(
            "responseLength", response.getText().length(),
            "tokensUsed", response.getTokens()
        ));
        
        return response;
    } catch (Exception e) {
        log.error("Chat processing failed", Map.of(
            "errorCode", "CHAT_ERROR",
            "errorMessage", e.getMessage()
        ), e);
        throw e;
    } finally {
        MDC.clear();
    }
}
```

### Log Levels

- **ERROR**: System errors, exceptions that need attention
- **WARN**: Degraded functionality, recoverable errors
- **INFO**: Important state changes, deployment events
- **DEBUG**: Detailed flow information (development only)
- **TRACE**: Very detailed information (rarely used)

Never log:
- PII (email, phone, SSN)
- API keys or credentials
- Passwords or tokens
- Sensitive business data

## Testing Standards

### Test Structure

```java
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    
    @Mock
    private ChatRepository repository;
    
    @InjectMocks
    private ChatService service;
    
    @Test
    void testProcessChat_givenValidRequest_returnsChatResponse() {
        // GIVEN
        ChatRequest request = new ChatRequest("Hello");
        when(repository.save(any())).thenReturn(new Chat());
        
        // WHEN
        ChatResponse response = service.processChat(request);
        
        // THEN
        assertThat(response).isNotNull();
        verify(repository).save(any());
    }
}
```

### Test Coverage

- **Minimum**: 90% line coverage
- **Critical paths**: 100% coverage
- **Tool**: JaCoCo

## Code Review Checklist

- [ ] Follows naming conventions
- [ ] Proper package organization
- [ ] Constructor injection used
- [ ] No business logic in controllers
- [ ] Input validation present
- [ ] Error handling comprehensive
- [ ] Logging is structured
- [ ] Tests present and passing
- [ ] Coverage > 90%
- [ ] No PII in logs
- [ ] No hardcoded secrets
- [ ] Documentation updated

## Tools & Automation

### Maven
```bash
# Check formatting
mvn spotless:check

# Format code
mvn spotless:apply

# Run tests with coverage
mvn clean verify

# Check for vulnerabilities
mvn org.owasp:dependency-check-maven:check
```

### IntelliJ IDEA
- Code Style: Editor → Code Style → Java
- Inspections: Analyze → Run Inspections

## Common Anti-Patterns

❌ **Field Injection**
```java
@Autowired
private ChatService service;
```

✅ **Constructor Injection**
```java
public ChatController(ChatService service) {
    this.service = service;
}
```

---

❌ **Business Logic in Controller**
```java
@PostMapping
public void chat(@RequestBody ChatRequest req) {
    Chat chat = repository.save(new Chat(req));
    String response = openAi.generate(req.getMessage());
}
```

✅ **Service Layer**
```java
@PostMapping
public ChatResponse chat(@RequestBody ChatRequest req) {
    return service.processChat(req);
}
```

---

❌ **Exposed Entities**
```java
@GetMapping("/{id}")
public Chat getChat(@PathVariable Long id) {
    return repository.findById(id).orElse(null);
}
```

✅ **DTOs**
```java
@GetMapping("/{id}")
public ChatResponse getChat(@PathVariable Long id) {
    return repository.findById(id)
        .map(this::toDto)
        .orElseThrow();
}
```

---

**Last Updated**: June 2026
**Version**: 1.0
