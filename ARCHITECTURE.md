# Architecture & System Design

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (React/Next.js)                  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                    (HTTPS, JWT Token)
                             │
┌────────────────────────────┴────────────────────────────────────┐
│                      API Gateway Service                         │
│  - Authentication (JWT validation)                              │
│  - Rate limiting (Redis)                                        │
│  - Request routing & aggregation                                │
│  - Request/Response transformation                              │
│  - OpenTelemetry tracing                                        │
└────────────────────────────┬────────────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
    ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────┐
    │ Chat APIs       │  │ RAG APIs         │  │ Admin APIs       │
    │ - POST /chat    │  │ - POST /search   │  │ - GET /config    │
    │ - GET /history  │  │ - POST /ingest   │  │ - POST /settings │
    └────────┬────────┘  └────────┬─────────┘  └────────┬─────────┘
             │                    │                     │
             └────────────────────┼─────────────────────┘
                                  │
┌─────────────────────────────────┴────────────────────────────────┐
│           AI Orchestrator Service (Spring AI + LangChain4j)       │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Prompt Orchestrator                                         │ │
│  │ - Template selection & rendering                           │ │
│  │ - LLM provider routing                                     │ │
│  │ - Fallback strategies                                      │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ RAG Engine                                                  │ │
│  │ - Vector embedding (OpenAI text-embedding-3-small)        │ │
│  │ - Semantic similarity search (pgvector)                    │ │
│  │ - Result re-ranking                                        │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Agent Framework                                             │ │
│  │ - Tool definitions & execution                             │ │
│  │ - LLM as controller                                        │ │
│  │ - Conversation memory (Redis)                              │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Memory Manager                                              │ │
│  │ - Session cache (Redis)                                    │ │
│  │ - Conversation history                                     │ │
│  │ - Context window management                                │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ LLM Providers                                               │ │
│  │ - OpenAI GPT-4 (primary)                                   │ │
│  │ - Azure OpenAI (backup)                                    │ │
│  │ - Cost tracking per request                                │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Response Evaluation                                         │ │
│  │ - Hallucination detection                                  │ │
│  │ - Relevance scoring                                        │ │
│  │ - Audit logging                                            │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  Messaging: Kafka (async AI operations)                          │
│  Cache: Redis (embeddings, sessions)                             │
│  Database: PostgreSQL (chat history, logs)                       │
└─────────────────────────────────────────────────────────────────┘
                             │
              ┌──────────────┘
              │
┌─────────────┴──────────────────────────────────────────────────┐
│         Ingestion Service (Apache Tika + Spring Batch)           │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Document Processor                                       │  │
│  │ - PDF/DOCX/TXT extraction (Apache Tika)                 │  │
│  │ - Content parsing                                        │  │
│  │ - Metadata extraction                                    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Chunking Engine                                          │  │
│  │ - Semantic chunking                                      │  │
│  │ - Sliding window chunks                                  │  │
│  │ - Metadata preservation                                  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Batch Processor (Spring Batch)                           │  │
│  │ - Read (documents)                                       │  │
│  │ - Process (embedding)                                    │  │
│  │ - Write (vector DB)                                      │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  Cache: Redis (processing queue)                                │
│  Database: PostgreSQL + pgvector                                │
│  Messaging: Kafka (document events)                             │
└──────────────────────────────────────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
    ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────┐
    │   PostgreSQL    │  │      Redis       │  │      Kafka       │
    │   + pgvector    │  │   (Cache)        │  │   (Messaging)    │
    │   (Vectors)     │  │   (Sessions)     │  │   (Events)       │
    │   (Chat Hist.)  │  │                  │  │                  │
    └─────────────────┘  └──────────────────┘  └──────────────────┘

                   Observability Layer
    ┌────────────────────────────────────────────────────┐
    │  OpenTelemetry (Traces) → Jaeger/Tempo            │
    │  Prometheus (Metrics) → Grafana                   │
    │  Structured Logs (JSON) → ElasticSearch/CloudWatch│
    │  Audit Logs → PostgreSQL + S3 Archive             │
    └────────────────────────────────────────────────────┘
```

## Data Flow - Chat with RAG

```
1. User submits chat query
   │
   ├─→ Gateway: Validate JWT, rate limit check
   │
   ├─→ AI Orchestrator: Create chat session
   │
   ├─→ RAG Engine:
   │   ├─ Embed user query (OpenAI)
   │   ├─ Search vectors in PostgreSQL
   │   └─ Retrieve top-5 similar documents
   │
   ├─→ Prompt Builder:
   │   ├─ Select prompt template
   │   ├─ Inject context from RAG
   │   └─ Inject conversation history
   │
   ├─→ LLM Provider:
   │   ├─ Send to OpenAI GPT-4
   │   ├─ Track tokens & cost
   │   └─ Handle streaming (if needed)
   │
   ├─→ Response Evaluator:
   │   ├─ Check for hallucinations
   │   ├─ Verify factual correctness
   │   └─ Score relevance
   │
   ├─→ Memory Manager:
   │   ├─ Cache embedding
   │   ├─ Store in Redis (session)
   │   └─ Persist to PostgreSQL
   │
   ├─→ Audit Service:
   │   └─ Log request/response/metrics
   │
   └─→ Return response to frontend
```

## Technology Choices & Rationale

| Component | Technology | Why |
|-----------|-----------|-----|
| Backend | Java 25 LTS + Spring Boot 3 | Enterprise standard, type-safe, excellent ecosystem |
| AI Framework | Spring AI + LangChain4j | Native Spring integration, multi-LLM support |
| LLM | OpenAI GPT-4 | Best accuracy, multi-modal, reasonable costs |
| Vector DB | PostgreSQL + pgvector | SQL + vectors, no separate infra, ACID |
| Cache | Redis | Sub-millisecond, high throughput, session mgmt |
| Messaging | Kafka | Durability, replay-ability, event sourcing |
| Cloud | AWS | Proven enterprise infrastructure |
| Orchestration | Kubernetes | Industry standard, portable |
| Monitoring | OpenTelemetry + Prometheus + Grafana | Open standard, vendor-agnostic |

## Deployment Architecture

### Development (Docker Compose)
- Single machine setup
- All services in containers
- PostgreSQL, Redis, Kafka local
- Suitable for testing

### Production (Kubernetes on AWS)
- High availability across AZs
- Auto-scaling for workloads
- Managed databases (RDS, ElastiCache, MSK)
- Load balancing
- Private VPC networking
- CDN for frontend

### Infrastructure as Code
- Terraform for AWS resources
- Helm charts for Kubernetes
- GitOps deployment pipeline

## Security Architecture

```
├── Network Security
│   ├─ Private VPC
│   ├─ Security groups (port whitelisting)
│   ├─ NACLs for subnet isolation
│   └─ API Gateway (WAF rules)
│
├── Application Security
│   ├─ JWT token validation
│   ├─ Role-based access control (RBAC)
│   ├─ API key management
│   └─ Rate limiting per user/API key
│
├── Data Security
│   ├─ Encryption at rest (AWS KMS)
│   ├─ Encryption in transit (TLS 1.3)
│   ├─ PII masking in logs
│   ├─ Secrets in AWS Secrets Manager
│   └─ Database column encryption
│
├── Audit & Compliance
│   ├─ All AI requests logged with trace ID
│   ├─ User action audit trail
│   ├─ Cost tracking per user/API
│   ├─ Data retention policies
│   └─ GDPR data export/deletion
│
└── Secrets Management
    ├─ No secrets in code
    ├─ AWS Secrets Manager rotation
    ├─ Environment-specific configs
    └─ Emergency break-glass access
```

## Scalability Considerations

1. **Horizontal Scaling**
   - Stateless service design
   - Load balanced across multiple instances
   - Database connection pooling

2. **Caching Strategy**
   - Embeddings cached in Redis (24h TTL)
   - Session state in Redis
   - Database query results cached

3. **Batch Processing**
   - Ingestion via Spring Batch (parallel chunks)
   - Async embedding generation
   - Kafka event streaming for loose coupling

4. **Database Optimization**
   - Index on vector_embedding column
   - Partitioning chat history by date
   - Archive old records to S3

## Disaster Recovery

- **Backup**: Daily snapshots of PostgreSQL to S3
- **Replication**: Multi-AZ RDS deployment
- **Failover**: Automatic RDS failover
- **Recovery Time Objective (RTO)**: < 5 minutes
- **Recovery Point Objective (RPO)**: < 1 minute

---

**Version**: 1.0
**Last Updated**: June 2026
