# Unit Test Generation Prompt

## Task
Generate comprehensive unit tests following enterprise testing standards.

## Testing Framework
- **Unit Testing**: JUnit 5 (Jupiter)
- **Mocking**: Mockito
- **Assertions**: AssertJ or JUnit assertions
- **Test Data**: Builders or Faker library

## Test Structure

### Test Class Naming
```
ClassNameTest or ClassNameTests
Example: ChatServiceTest, EmbeddingServiceTests
```

### Test Method Naming Convention
```
methodName_givenCondition_expectedResult
Example: testProcessChat_givenValidRequest_returnsChatResponse
```

### Test Method Structure (AAA Pattern)
```java
@Test
void testProcessChat_givenValidRequest_returnsChatResponse() {
    // GIVEN (Arrange) - set up test data
    ChatRequest request = new ChatRequest("Hello");
    
    // WHEN (Act) - perform the action
    ChatResponse response = chatService.processChat(request);
    
    // THEN (Assert) - verify the result
    assertThat(response).isNotNull();
    assertThat(response.getMessage()).isEqualTo("Hello");
}
```

## Mocking Strategy

### Mock External Dependencies
```java
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    
    @Mock
    private ChatRepository chatRepository;
    
    @Mock
    private OpenAiClient openAiClient;
    
    @Mock
    private EmbeddingService embeddingService;
    
    @InjectMocks
    private ChatService chatService;
}
```

### Configure Mocks
```java
@BeforeEach
void setUp() {
    when(chatRepository.save(any()))
        .thenReturn(new Chat("Hello"));
    
    when(openAiClient.generate("Hello"))
        .thenReturn("Hi there!");
    
    when(embeddingService.embed("Hello"))
        .thenReturn(new Embedding(new double[1536]));
}
```

## Test Coverage

### Minimum Requirements
- **Overall**: 90% line coverage
- **Critical paths**: 100% coverage
- **Exception handling**: Test both success and failure cases

### Coverage Tools
- JaCoCo Maven plugin
- IntelliJ built-in coverage
- SonarQube

## Test Categories

### 1. Happy Path Tests
```java
@Test
void testProcessChat_givenValidRequest_returnsChatResponse() {
    // Standard, expected behavior
}

@Test
void testProcessChat_givenLongMessage_returnsTruncatedResponse() {
    // Edge case but still valid
}
```

### 2. Error Handling Tests
```java
@Test
void testProcessChat_givenEmptyMessage_throwsValidationException() {
    // Invalid input
    assertThrows(ValidationException.class, () -> 
        chatService.processChat(new ChatRequest(""))
    );
}

@Test
void testProcessChat_givenApiError_throwsChatException() {
    // External service failure
    when(openAiClient.generate(any()))
        .thenThrow(new ApiException("Rate limited"));
    
    assertThrows(ChatException.class, () -> 
        chatService.processChat(request)
    );
}
```

### 3. Boundary Tests
```java
@Test
void testProcessChat_givenMaxLengthMessage_succeeds() {
    String maxMessage = "x".repeat(5000);
    ChatRequest request = new ChatRequest(maxMessage);
    
    ChatResponse response = chatService.processChat(request);
    
    assertThat(response).isNotNull();
}

@Test
void testProcessChat_givenTooLongMessage_throwsValidationException() {
    String tooLong = "x".repeat(5001);
    ChatRequest request = new ChatRequest(tooLong);
    
    assertThrows(ValidationException.class, () -> 
        chatService.processChat(request)
    );
}
```

### 4. State Tests
```java
@Test
void testProcessChat_givenSavedChat_updatesRepository() {
    ChatRequest request = new ChatRequest("Hello");
    
    chatService.processChat(request);
    
    verify(chatRepository).save(argThat(chat -> 
        chat.getMessage().equals("Hello")
    ));
}
```

## Test Data Builders

### Using Builder Pattern
```java
public class ChatRequestBuilder {
    private String message = "Default message";
    private Long sessionId = 1L;
    
    public ChatRequestBuilder withMessage(String msg) {
        this.message = msg;
        return this;
    }
    
    public ChatRequest build() {
        return new ChatRequest(message, sessionId);
    }
}

// Usage
ChatRequest request = new ChatRequestBuilder()
    .withMessage("Custom message")
    .build();
```

## Parameterized Tests

```java
@ParameterizedTest
@ValueSource(strings = {"Hello", "Hi", "Hey there"})
void testProcessChat_givenVariousMessages_succeeds(String msg) {
    ChatRequest request = new ChatRequest(msg);
    
    ChatResponse response = chatService.processChat(request);
    
    assertThat(response).isNotNull();
}

@ParameterizedTest
@CsvSource({
    "Hello, 5",
    "Hi there, 8",
    "Very long message here, 24"
})
void testProcessChat_givenMessagesWithLength_countsCorrectly(
    String msg, int expectedLength) {
    
    assertEquals(expectedLength, msg.length());
}
```

## Integration Tests

### With Testcontainers
```java
@SpringBootTest
@Testcontainers
class ChatServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private ChatRepository chatRepository;
    
    @Test
    void testProcessChat_givenValidRequest_persistsAndReturnsResponse() {
        ChatRequest request = new ChatRequest("Test");
        
        ChatResponse response = chatService.processChat(request);
        
        assertThat(response).isNotNull();
        assertThat(chatRepository.count()).isOne();
    }
}
```

## Test Configuration

### Maven Configuration
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
```

## Test Execution

### Run All Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn test -Dtest=ChatServiceTest
```

### Run with Coverage
```bash
mvn jacoco:prepare-agent test jacoco:report
```

## Acceptance Criteria
- [ ] All tests follow naming convention
- [ ] AAA pattern implemented consistently
- [ ] >90% code coverage achieved
- [ ] Error cases tested
- [ ] Boundary conditions tested
- [ ] Mocks configured appropriately
- [ ] No hard-coded values
- [ ] All tests pass
- [ ] Integration tests use containers
- [ ] Test data builders used
