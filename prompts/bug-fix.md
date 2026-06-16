# Bug Fix Prompt

## Task
Systematically identify, analyze, and fix bugs in the Java LLM Enterprise codebase.

## Approach

### 1. Problem Statement
- What is the bug? (symptoms)
- Where does it occur? (stack trace, component)
- How often? (reproducibility)
- Impact? (severity)

### 2. Root Cause Analysis
- Check logs and monitoring
- Reproduce the issue locally
- Isolate the failing component
- Identify the root cause

### 3. Solution Design
- Minimal change principle (fix the root cause, not symptoms)
- No breaking changes
- Performance impact analysis
- Security implications

### 4. Implementation
- Write failing unit test first (TDD)
- Implement fix
- Run full test suite
- Check code coverage impact

### 5. Testing
- Unit tests for the fix
- Integration tests (if applicable)
- Edge case testing
- Regression testing

### 6. Documentation
- Update CHANGELOG
- Add comments if non-obvious
- Update relevant docs
- Communicate to team

## Common Bug Categories

### Logic Errors
- Null pointer exceptions
- Off-by-one errors
- Incorrect conditionals
- Type casting issues

### Concurrency Issues
- Race conditions
- Deadlocks
- Memory visibility
- Synchronization problems

### Resource Leaks
- Unclosed connections
- Memory leaks
- Thread leaks
- File handle leaks

### Configuration Issues
- Missing environment variables
- Incorrect database configuration
- Cache misses
- Timeout values

### Integration Issues
- API failures
- Network timeouts
- Incompatible library versions
- Serialization issues

## Debugging Tools & Techniques

### Logging
```java
// Structured logging
logger.error("Operation failed", Map.of(
    "userId", userId,
    "operation", "embeddings",
    "errorCode", e.getErrorCode()
));
```

### Debugging
```
1. Set breakpoint at suspicious code
2. Step through execution
3. Inspect variables at each step
4. Check stack trace
5. Verify assumptions
```

### Profiling
```
- JVM profiler (CPU, memory)
- Database query profiling
- HTTP request timing
- Thread dumps
```

## Testing Strategy

### Write Failing Test First
```java
@Test
void testBugFix_givenCondition_expectedBehavior() {
    // This test fails before the fix
    // It passes after the fix
}
```

### Verify Fix
- Run specific failing test
- Run full test suite
- Check code coverage
- Manual verification in staging

## Documentation

### Commit Message Format
```
fix: brief description of bug

Detailed explanation of:
- What was wrong
- Why it was wrong
- How it's fixed
- Testing approach

Fixes #ISSUE_NUMBER
```

### Changelog Entry
```
## Bug Fixes
- [CRITICAL] Fixed NullPointerException in RAG retrieval
- [MAJOR] Fixed race condition in embedding cache
- [MINOR] Fixed typo in error message
```

## Acceptance Criteria
- [ ] Bug reproduced and root cause identified
- [ ] Failing unit test written
- [ ] Fix implemented (minimal changes)
- [ ] Test passes
- [ ] Full test suite passes
- [ ] Code coverage maintained (>90%)
- [ ] No new warnings/errors
- [ ] Documentation updated
- [ ] Verified in staging environment
- [ ] Ready for production deployment
