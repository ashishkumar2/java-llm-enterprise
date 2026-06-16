# AI Coding Rules & Patterns

This document defines the AI-assisted coding standards for the Java LLM Enterprise platform.

## Tech Stack

- **JDK**: Java 25 LTS
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL 15+ with pgvector
- **Messaging**: Kafka 3.5+
- **Cache**: Redis 7+
- **AI**: Spring AI + LangChain4j
- **Build**: Maven 3.8+

## Architecture Patterns

### Layered Architecture

```
┌─────────────────────────────────────┐
│  Controller Layer (HTTP)            │
│  - Handle requests/responses        │
│  - Input validation                 │
│  - Call services                    │
└─────────────────────────────────────┘
            ↓
┌─────────────────────────────────────┐
│  Service Layer (Business Logic)     │
│  - Orchestrate business flows       │
│  - Call repositories/external APIs  │
│  - Transaction management           │
└─────────────────────────────────────┘
            ↓
┌─────────────────────────────────────┐
│  Repository Layer (Data Access)     │
│  - JPA/Hibernate queries            │
│  - Cache management                 │
│  - Entity transformations           │
└─────────────────────────────────────┘
            ↓
┌─────────────────────────────────────┐
│  External Services & Databases      │
│  - PostgreSQL / pgvector            │
│  - Redis                            │
│  - OpenAI / Azure OpenAI            │
└─────────────────────────────────────┘
```

### AI Orchestrator Pattern

```
Request → Controller → Service → Orchestrator → Prompt Builder
                                     ↓
                              LLM Provider
                                     ↓
                          Response Evaluator
                                     ↓
                          Memory Manager (Redis)
```

## Coding Standards

### 1. No Business Logic in Controllers

❌ **Bad**:
```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest req) {
        // Business logic here - WRONG!
        String result = processWithLLM(req.getPrompt());
        return new ChatResponse(result);
    }
}
```

✅ **Good**:
```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest req) {
        return chatService.processChat(req);
    }
}
```

### 2. Constructor Injection Only

❌ **Bad**:
```java
@Service
public class ChatService {
    @Autowired
    private ChatRepository repo;
    
    @Autowired
    private OpenAiClient openAi;
}
```

✅ **Good**:
```java
@Service
public class ChatService {
    private final ChatRepository repo;
    private final OpenAiClient openAi;
    private final CacheManager cache;
    
    public ChatService(ChatRepository repo, OpenAiClient openAi, CacheManager cache) {
        this.repo = repo;
        this.openAi = openAi;
        this.cache = cache;
    }
}
```

### 3. Data Transfer Objects (DTOs)

Always use DTOs for API responses. Never expose JPA entities directly.

❌ **Bad**:
```java
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).orElse(null);
}
```

✅ **Good**:
```java
@GetMapping("/{id}")
public UserResponse getUser(@PathVariable Long id) {
    return userRepository.findById(id)
        .map(this::toDto)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
}

private UserResponse toDto(User user) {
    return new UserResponse(user.getId(), user.getName(), user.getEmail());
}
```

### 4. Structured Logging

Always use SLF4J with structured logging (JSON format preferred).

```java
@Service
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    public ChatResponse processChat(ChatRequest request) {
        logger.info("Processing chat request", Map.of(
            "userId", request.getUserId(),
            "sessionId", request.getSessionId(),
            "timestamp", System.currentTimeMillis()
        ));
        
        try {
            ChatResponse response = orchestrator.orchestrate(request);
            logger.info("Chat processing completed", Map.of(
                "userId", request.getUserId(),
                "responseTokens", response.getTokenCount()
            ));
            return response;
        } catch (Exception e) {
            logger.error("Chat processing failed", Map.of(
                "userId", request.getUserId(),
                "errorMessage", e.getMessage()
            ), e);
            throw new ChatProcessingException("Failed to process chat");
        }
    }
}
```

### 5. OpenTelemetry Tracing

Trace all critical AI workflows.

```java
@Service
public class RagService {
    private final Tracer tracer;
    
    public RagService(Tracer tracer) {
        this.tracer = tracer;
    }
    
    public RagResponse retrieve(RagQuery query) {
        Span span = tracer.spanBuilder("rag.retrieve")
            .setAttribute("query", query.getText())
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // Embedding phase
            Span embeddingSpan = tracer.spanBuilder("rag.embedding")
                .setParent(Context.current().with(span))
                .startSpan();
            try {
                Embedding embedding = embeddingService.embed(query.getText());
                embeddingSpan.addEvent("embedding.success");
            } finally {
                embeddingSpan.end();
            }
            
            // Vector search phase
            Span searchSpan = tracer.spanBuilder("rag.vectorSearch")
                .setParent(Context.current().with(span))
                .startSpan();
            try {
                List<Document> docs = vectorStore.search(embedding, 5);
                searchSpan.setAttribute("results.count", docs.size());
            } finally {
                searchSpan.end();
            }
            
            return new RagResponse(/* ... */);
        } finally {
            span.end();
        }
    }
}
```

### 6. Input Validation

Always validate inputs at the controller level.

```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest req) {
        return chatService.processChat(req);
    }
}

@Data
public class ChatRequest {
    @NotBlank(message = "Message cannot be blank")
    private String message;
    
    @NotNull(message = "Session ID is required")
    @Positive
    private Long sessionId;
    
    @Size(min = 1, max = 5000)
    private String context;
}
```

### 7. Exception Handling

Never expose stack traces. Return meaningful error responses.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("RESOURCE_NOT_FOUND", e.getMessage()));
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        logger.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "An error occurred"));
    }
}
```

### 8. Transaction Management

Use `@Transactional` at the service layer only.

```java
@Service
public class ChatService {
    
    @Transactional
    public ChatResponse processChat(ChatRequest request) {
        Chat chat = new Chat();
        chat.setMessage(request.getMessage());
        chat.setUserId(request.getUserId());
        chat.setCreatedAt(LocalDateTime.now());
        
        chatRepository.save(chat);
        
        // Call LLM
        String response = llmClient.generate(request.getMessage());
        
        ChatMessage message = new ChatMessage();
        message.setContent(response);
        message.setChat(chat);
        
        messageRepository.save(message);
        
        return toDto(chat);
    }
}
```

## Testing Standards

### 1. Unit Tests

Use Mockito for all external dependencies.

```java
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    
    @Mock
    private ChatRepository chatRepository;
    
    @Mock
    private OpenAiClient openAiClient;
    
    @InjectMocks
    private ChatService chatService;
    
    @Test
    void testProcessChat_givenValidRequest_returnsChatResponse() {
        // Given
        ChatRequest request = new ChatRequest("Hello");
        Chat expectedChat = new Chat("Hello");
        when(chatRepository.save(any())).thenReturn(expectedChat);
        when(openAiClient.generate("Hello")).thenReturn("Hi there!");
        
        // When
        ChatResponse response = chatService.processChat(request);
        
        // Then
        assertNotNull(response);
        verify(chatRepository).save(any());
        verify(openAiClient).generate("Hello");
    }
    
    @Test
    void testProcessChat_givenEmptyMessage_throwsValidationException() {
        // Given
        ChatRequest request = new ChatRequest("");
        
        // When & Then
        assertThrows(ValidationException.class, () -> chatService.processChat(request));
    }
}
```

### 2. Integration Tests

Use Testcontainers for database/external services.

```java
@SpringBootTest
@Testcontainers
class ChatServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private ChatRepository chatRepository;
    
    @Test
    void testProcessChat_givenValidRequest_persistsChatAndReturnsResponse() {
        // Given
        ChatRequest request = new ChatRequest("Test message");
        
        // When
        ChatResponse response = chatService.processChat(request);
        
        // Then
        assertNotNull(response);
        assertEquals(1, chatRepository.count());
    }
}
```

### 3. Test Coverage

- **Minimum**: 90% coverage
- **Target**: 95% for critical paths
- **Exclude**: Auto-generated code, DTOs without logic

## Security Standards

### 1. PII Masking in Logs

Never log personally identifiable information directly.

```java
@Aspect
@Component
public class PiiMaskingAspect {
    
    @Around("@annotation(com.enterprise.security.MaskPii)")
    public Object maskPii(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        return maskSensitiveData(result);
    }
    
    private Object maskSensitiveData(Object obj) {
        // Mask email, phone, SSN, etc.
        String str = obj.toString();
        str = str.replaceAll("\\b[\\w.-]+@[\\w.-]+\\.\\w+\\b", "***@***.***");
        str = str.replaceAll("\\d{3}-\\d{2}-\\d{4}", "***-**-****");
        return str;
    }
}
```

### 2. Secrets Management

Use AWS Secrets Manager or Spring Cloud Config.

```java
@Configuration
public class SecretConfig {
    
    @Bean
    public SecretsManager secretsManager(SecretsManagerClient client) {
        return new SecretsManager(client);
    }
}
```

Store in `.env`:
```
OPENAI_API_KEY=sk-...
DB_PASSWORD=...
```

### 3. Audit Logging

Log all AI requests and responses.

```java
@Component
public class AuditService {
    
    public void auditAiRequest(ChatRequest request, String userId) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction("AI_REQUEST");
        log.setResource("CHAT");
        log.setTimestamp(LocalDateTime.now());
        log.setDetails(Map.of("messageLength", request.getMessage().length()));
        
        auditRepository.save(log);
    }
}
```

## Performance Guidelines

### 1. Caching Strategy

```java
@Service
@CacheConfig(cacheNames = "embeddings")
public class EmbeddingService {
    
    @Cacheable(key = "#text")
    public Embedding embed(String text) {
        // Expensive embedding operation
        return openAiClient.createEmbedding(text);
    }
}
```

### 2. Batch Processing

Use Spring Batch for large-scale processing.

```java
@Configuration
public class IngestionBatchConfig {
    
    @Bean
    public Job ingestionJob(JobRepository jobRepository) {
        return new JobBuilder("ingestionJob", jobRepository)
            .start(readStep())
            .next(embeddingStep())
            .next(storageStep())
            .build();
    }
    
    @Bean
    public Step readStep() {
        return new StepBuilder("readStep", jobRepository)
            .<Document, Document>chunk(100)
            .reader(documentReader())
            .processor(chunkProcessor())
            .writer(embeddingWriter())
            .build();
    }
}
```

### 3. Async Processing with Kafka

```java
@Service
public class ChatService {
    
    private final KafkaTemplate<String, ChatEvent> kafkaTemplate;
    
    @Transactional
    public void processChatAsync(ChatRequest request) {
        Chat chat = chatRepository.save(createChat(request));
        
        ChatEvent event = new ChatEvent(chat.getId(), request.getMessage());
        kafkaTemplate.send("chat-events", event);
    }
}

@Component
public class ChatEventConsumer {
    
    @KafkaListener(topics = "chat-events")
    public void onChatEvent(ChatEvent event) {
        // Process asynchronously
        String response = generateResponse(event.getMessage());
        saveResponse(event.getChatId(), response);
    }
}
```

## Common Mistakes to Avoid

1. ❌ Calling external APIs in controllers
2. ❌ Field injection instead of constructor injection
3. ❌ Logging sensitive data
4. ❌ Exposing stack traces to clients
5. ❌ No input validation
6. ❌ No transaction management
7. ❌ Hard-coded secrets
8. ❌ No error handling
9. ❌ Low test coverage
10. ❌ No monitoring/tracing

## Code Review Checklist

- [ ] No business logic in controllers
- [ ] All dependencies constructor-injected
- [ ] DTOs used for all API responses
- [ ] Inputs validated at controller level
- [ ] Structured logging implemented
- [ ] OpenTelemetry tracing added
- [ ] Exception handling with no stack traces
- [ ] Unit tests with mocks (>90% coverage)
- [ ] Integration tests with containers
- [ ] No PII in logs
- [ ] No hard-coded secrets
- [ ] Async operations use Kafka/Tasks
- [ ] Database queries are parameterized
- [ ] Cache strategy defined
- [ ] Documentation updated

---

**Last Updated**: June 2026
**Version**: 1.0
