# Architecture Review Prompt

## Task
Review and assess the architecture of Java LLM Enterprise systems for production readiness.

## Review Dimensions

### 1. Scalability
- Horizontal scaling capability
- Stateless service design
- Load balancing strategy
- Database scaling approach
- Cache layer effectiveness
- Connection pooling configuration

### 2. Reliability
- Error handling and recovery
- Retry strategies
- Circuit breakers
- Fallback mechanisms
- Health checks
- Graceful degradation

### 3. Performance
- API response times
- Database query optimization
- Caching strategy
- Async processing
- Batch operations
- Resource utilization

### 4. Security
- Authentication & authorization
- Data encryption (at rest & in transit)
- Secret management
- Input validation
- Injection prevention
- Audit logging

### 5. Maintainability
- Code organization
- Design patterns usage
- Separation of concerns
- Documentation quality
- Test coverage
- Dependency management

### 6. Observability
- Logging strategy
- Distributed tracing
- Metrics collection
- Alerting rules
- SLA monitoring
- Dashboard coverage

## Architectural Patterns

### Layered Architecture
```
Controller → Service → Repository → Database
```

### Microservices
```
Gateway → Multiple Independent Services → Shared Database/Cache
```

### Event-Driven
```
Producer → Message Broker → Consumer
```

## Assessment Checklist

### Design Quality
- [ ] Clear separation of concerns
- [ ] No circular dependencies
- [ ] Proper abstraction levels
- [ ] Reusable components
- [ ] SOLID principles followed

### Code Quality
- [ ] Consistent naming conventions
- [ ] No code duplication
- [ ] Proper error handling
- [ ] Type-safe code
- [ ] Well-documented

### Testing
- [ ] >90% unit test coverage
- [ ] Integration tests present
- [ ] E2E tests for critical paths
- [ ] Performance tests
- [ ] Load tests for critical services

### Performance
- [ ] API response times < 5s
- [ ] Database queries optimized
- [ ] Caching implemented
- [ ] No N+1 queries
- [ ] Resource limits configured

### Security
- [ ] No hardcoded secrets
- [ ] Input validation
- [ ] Injection prevention
- [ ] Authentication enforced
- [ ] Authorization granular

### Operations
- [ ] Health checks configured
- [ ] Metrics exposed
- [ ] Logs structured
- [ ] Alerts configured
- [ ] Runbooks available

### Deployment
- [ ] CI/CD pipeline automated
- [ ] Containerized applications
- [ ] Configuration externalized
- [ ] Database migrations managed
- [ ] Rollback strategy defined

## Review Output Format

### Executive Summary
- Overall assessment (Green/Yellow/Red)
- Top 3 concerns
- Top 3 strengths
- Estimated effort to fix

### Detailed Findings

#### High Priority
- Critical security issues
- Performance bottlenecks
- Reliability risks
- Scalability limitations

#### Medium Priority
- Code quality issues
- Missing tests
- Documentation gaps
- Operational concerns

#### Low Priority
- Code style issues
- Naming improvements
- Refactoring opportunities
- Documentation enhancements

## Recommendations

### Short Term (1-2 sprints)
- [ ] Fix critical security issues
- [ ] Add missing error handling
- [ ] Improve test coverage
- [ ] Optimize slow queries

### Medium Term (3-6 months)
- [ ] Implement missing patterns
- [ ] Refactor complex services
- [ ] Improve observability
- [ ] Performance optimization

### Long Term (6-12 months)
- [ ] Microservices migration
- [ ] Advanced scaling
- [ ] AI/ML optimization
- [ ] Multi-region deployment

## Example Review Output

```markdown
# Architecture Review: AI Orchestrator Service

## Overall Assessment: YELLOW (Some Concerns)

### Strengths
1. ✅ Clean separation of concerns
2. ✅ Comprehensive test coverage (92%)
3. ✅ Good error handling

### Concerns
1. ⚠️ No rate limiting on LLM calls
2. ⚠️ Synchronous embedding generation
3. ⚠️ Missing observability in RAG pipeline

### Recommendations
1. Implement Kafka-based async embedding
2. Add OpenTelemetry tracing to RAG
3. Configure rate limiters for LLM

## Detailed Assessment
[Details for each dimension]

## Action Items
- [ ] Implement async embedding (Sprint N)
- [ ] Add observability (Sprint N+1)
- [ ] Security audit (Sprint N+2)
```

## Tools & Metrics

### Static Analysis
- SonarQube
- Checkstyle
- SpotBugs
- Dependency Check

### Performance
- JMH (Java Microbenchmark Harness)
- Load testing tools
- Profilers
- APM tools

### Testing Metrics
- Coverage reports
- Test execution time
- Mutation testing
- Regression detection

## Follow-up

### Post-Review
1. Create tickets for findings
2. Prioritize by impact/effort
3. Schedule implementation
4. Set follow-up review date

### Success Metrics
- Issues resolved
- Coverage improved
- Performance improved
- Security posture enhanced

---

**Review Frequency**: Quarterly or before major releases
**Audience**: Architecture team, tech leads, engineers
**Duration**: 1-2 weeks for full architecture assessment
