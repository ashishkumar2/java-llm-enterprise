# Deployment Runbook

## Pre-Deployment Checklist

- [ ] All tests passing (unit + integration)
- [ ] Code review approved
- [ ] Deployment plan documented
- [ ] Stakeholders notified
- [ ] Rollback plan ready
- [ ] Monitoring configured
- [ ] Runbook reviewed

## Deployment Steps

### 1. Staging Environment

```bash
# Build
mvn clean package -P staging

# Run tests
mvn verify

# Deploy to staging
docker build -t ai-orchestrator:staging .
docker push myregistry.azurecr.io/ai-orchestrator:staging

# Verify deployment
kubectl apply -f k8s/staging.yaml
kubectl rollout status deployment/ai-orchestrator -n staging
```

### 2. Smoke Tests

```bash
# Check service health
curl https://staging-api.example.com/health

# Verify database connection
curl https://staging-api.example.com/health/db

# Test core functionality
curl -X POST https://staging-api.example.com/api/v1/chat \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"message": "test"}'
```

### 3. Production Deployment

```bash
# Tag and push production image
docker build -t ai-orchestrator:v1.0.0 .
docker push myregistry.azurecr.io/ai-orchestrator:v1.0.0

# Deploy with rolling update
kubectl set image deployment/ai-orchestrator \
  ai-orchestrator=myregistry.azurecr.io/ai-orchestrator:v1.0.0 \
  -n production

# Monitor rollout
kubectl rollout status deployment/ai-orchestrator -n production
kubectl logs -f deployment/ai-orchestrator -n production
```

### 4. Verification

- [ ] Service responding to requests
- [ ] Error rates < 0.1%
- [ ] Response times normal
- [ ] Database replication working
- [ ] Cache hit rates normal
- [ ] Message queue processing

## Rollback Procedure

If issues detected, rollback immediately:

```bash
# Rollback to previous version
kubectl rollout undo deployment/ai-orchestrator -n production

# Verify rollback
kubectl rollout status deployment/ai-orchestrator -n production

# Check logs
kubectl logs -f deployment/ai-orchestrator -n production
```

## Post-Deployment

1. Monitor for 24 hours
2. Check for any errors or warnings
3. Verify metrics and traces
4. Confirm with product team
5. Document deployment in changelog

## Troubleshooting

### Service won't start
```bash
# Check logs
kubectl logs deployment/ai-orchestrator -n production

# Check status
kubectl describe pod <pod-name> -n production

# Check environment variables
kubectl exec pod/<pod-name> -n production -- env
```

### High error rate
1. Check CloudWatch logs
2. Review recent code changes
3. Check database connections
4. Verify external API access
5. Rollback if necessary

### Performance degradation
1. Check CPU/memory metrics
2. Review query logs
3. Check cache hit rates
4. Verify network connectivity
5. Scale up if needed

---

**Last Updated**: June 2026
**Version**: 1.0
