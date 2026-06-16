# Refactoring Prompt

## Task
Improve code quality through systematic refactoring while maintaining functionality.

## Refactoring Goals
1. Improve readability and maintainability
2. Reduce code duplication
3. Simplify complex logic
4. Improve performance
5. Strengthen type safety
6. Better separation of concerns

## Common Refactoring Patterns

### Extract Method
```java
// Before
public void processChat(ChatRequest request) {
    logger.info("Processing chat");
    Chat chat = chatRepository.save(new Chat(request));
    String response = openAi.generate(request.getMessage());
    ChatMessage message = new ChatMessage(response);
    messageRepository.save(message);
    logger.info("Chat processed");
}

// After
public void processChat(ChatRequest request) {
    Chat chat = createChat(request);
    String response = generateResponse(request);
    saveResponse(chat, response);
}

private Chat createChat(ChatRequest request) { ... }
private String generateResponse(ChatRequest request) { ... }
private void saveResponse(Chat chat, String response) { ... }
```

### Extract Class
```java
// Before - mixed concerns
class ChatService {
    public void processChat(...) { ... }
    public void validateInput(...) { ... }
    public void logMetrics(...) { ... }
}

// After - separation of concerns
class ChatService { /* core logic */ }
class ChatValidator { /* validation */ }
class ChatMetrics { /* metrics */ }
```

### Replace Conditional with Polymorphism
```java
// Before
if (provider.equals("openai")) {
    return openAiClient.generate(prompt);
} else if (provider.equals("azure")) {
    return azureClient.generate(prompt);
}

// After
LlmProvider provider = providerFactory.create(providerType);
return provider.generate(prompt);
```

### Extract Constants
```java
// Before
if (retries < 3) { ... }

// After
private static final int MAX_RETRIES = 3;
if (retries < MAX_RETRIES) { ... }
```

### Extract Interface
```java
// Before - tight coupling
public ChatService(OpenAiClient client) { ... }

// After - loose coupling
public ChatService(LlmProvider provider) { ... }

interface LlmProvider {
    String generate(String prompt);
}
```

## Refactoring Rules

### Safety First
- All tests must pass before and after
- No change in public API signatures
- Backward compatibility maintained
- No performance regression

### Incremental Changes
- One refactoring at a time
- Small, reviewable PRs
- Clear commit messages
- Explain intent

### Code Reviews
- Another engineer reviews
- Verify tests still pass
- Check for performance impact
- Ensure clarity improved

## Specific Areas to Refactor

### 1. Eliminate Duplication
- Duplicate code in services
- Similar entity mappings
- Repeated configuration
- Common validation logic

### 2. Simplify Complex Methods
- Methods > 20 lines
- Cyclomatic complexity > 5
- Too many parameters
- Nested conditionals

### 3. Improve Naming
- Unclear variable names
- Misleading method names
- Inconsistent naming conventions
- Acronyms that aren't self-explanatory

### 4. Better Error Handling
- Generic catch(Exception e)
- Swallowed exceptions
- Unhelpful error messages
- Missing null checks

### 5. Type Safety
- Raw types instead of generics
- Object casting
- Type-safe collections
- Proper use of Optional

## Testing During Refactoring

### Green Bar First
1. All tests passing before changes
2. Make small refactoring
3. Verify tests still pass
4. Repeat until done

### Test Coverage Targets
- Minimum 90% coverage
- 100% for refactored code
- Integration tests for complex paths

## Performance Considerations

### Before Refactoring
- Measure baseline performance
- Identify bottlenecks
- Profile critical paths

### After Refactoring
- Re-measure performance
- Compare to baseline
- Verify no regressions
- Monitor in staging

## Refactoring Checklist
- [ ] All tests pass before refactoring
- [ ] Refactoring goal clearly defined
- [ ] Code reviewed by team member
- [ ] All tests pass after refactoring
- [ ] Code coverage maintained (>90%)
- [ ] No performance regression
- [ ] Documentation updated
- [ ] Commit message explains intent

## Anti-Patterns to Avoid
- ❌ Refactoring while adding features
- ❌ Large refactoring in single PR
- ❌ Refactoring without tests
- ❌ Breaking public APIs
- ❌ Ignoring performance impact
- ❌ Over-engineering simple code

## Tools & IDE Features
- IntelliJ: Refactoring menu
- Eclipse: Source menu
- VS Code: Extensions for refactoring
- SonarQube: Code quality metrics
- Checkstyle: Code style enforcement
