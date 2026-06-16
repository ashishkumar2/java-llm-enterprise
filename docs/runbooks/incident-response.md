# Incident Response Runbook

## Severity Classification

### CRITICAL (P1)
- System is down
- Data loss risk
- Security breach
- Multiple services affected
- Response time: Immediate
- Escalation: VP Engineering

### MAJOR (P2)
- Service degraded
- Feature unavailable
- High error rate
- Performance impact
- Response time: 30 minutes
- Escalation: Engineering Lead

### MINOR (P3)
- Non-critical bug
- Low impact
- Single user affected
- Workaround available
- Response time: 4 hours
- Escalation: Team Lead

## Incident Response Process

### Phase 1: Detection & Alert
1. PagerDuty alert triggered
2. Check monitoring dashboard
3. Verify alert is legitimate (not false positive)
4. Document incident in #incidents Slack channel

### Phase 2: Initial Response
1. Acknowledge alert/ticket
2. Gather initial information
3. Determine severity
4. Assemble incident commander & team
5. Set up war room (Zoom call)

### Phase 3: Investigation
1. Check logs (CloudWatch, application logs)
2. Review recent deployments
3. Check external service status
4. Verify database connectivity
5. Review error patterns

### Phase 4: Mitigation
1. **Immediate actions**
   - Scale up services
   - Clear cache
   - Failover to backup

2. **Short-term fix**
   - Rollback to previous version
   - Apply hotfix
   - Restart services

3. **Communication**
   - Update status page
   - Notify stakeholders
   - Post updates every 15 min (critical)

### Phase 5: Resolution
1. Implement permanent fix
2. Deploy to staging
3. Test thoroughly
4. Deploy to production
5. Verify metrics return to normal

### Phase 6: Post-Incident
1. Document incident (what happened, impact, cause)
2. Create action items for prevention
3. Schedule post-mortem (24-48 hours)
4. Update runbooks/procedures
5. Share learnings with team

## Incident Commands (Slack)

```
/incident declare   # Start incident tracking
/incident update    # Provide status update
/incident resolve   # Mark incident as resolved
/incident postmortem # Schedule postmortem
```

## Common Issues & Solutions

### High CPU Usage
```bash
# Check for long-running operations
kubectl top pods -n production

# Check logs for loops/deadlocks
kubectl logs deployment/ai-orchestrator -n production

# Scale up if needed
kubectl scale deployment ai-orchestrator --replicas=5 -n production
```

### High Memory Usage
```bash
# Check for memory leaks
jmap -histo <pid>

# Review cache size
redis-cli info memory

# Increase container limits if needed
```

### Database Connection Pool Exhausted
```bash
# Check connections
psql -c "SELECT count(*) FROM pg_stat_activity;"

# Restart connection pool
kubectl rollout restart deployment/ai-orchestrator

# Increase pool size if needed
```

### API Response Timeout
1. Check database query performance
2. Check for N+1 query problems
3. Review Redis cache hit rates
4. Check external API latency
5. Scale services

### Message Queue Backlog
```bash
# Check queue size
kafka-consumer-groups --bootstrap-server kafka:9092 \
  --group my-consumer-group --describe

# Scale consumer replicas
kubectl scale deployment consumer --replicas=10
```

## Communication Template

### Initial Alert (within 15 min)
```
We're investigating reports of [SERVICE] being [ISSUE]. 
More updates to follow every 15 minutes.
Slack: #incidents
Status Page: status.example.com
```

### Update (every 15 min)
```
Status: [INVESTIGATING/IDENTIFIED/MITIGATING]
Root Cause: [What we've found so far]
ETA: [When we expect to have it fixed]
Impact: [What users are experiencing]
```

### Resolution
```
We've resolved the [SERVICE] issue. 
Root Cause: [Final cause]
Duration: [Total time]
Steps Taken: [What we did to fix it]
```

### Post-Mortem (48 hours)
```
[Link to Confluence page with details]
Action Items:
1. [Prevention measure 1]
2. [Prevention measure 2]
3. [Code improvement]
```

## War Room Structure

- **Incident Commander**: Leads investigation, coordinates team
- **Technical Lead**: Hands-on debugging
- **Database Team**: Database-specific issues
- **DevOps**: Infrastructure & deployment
- **Communication Lead**: Status updates

## Tools & Access

- **Monitoring**: Grafana (link)
- **Logs**: CloudWatch (link)
- **Tracing**: Jaeger (link)
- **On-call**: PagerDuty (link)
- **War Room**: Zoom (link)
- **Metrics**: Prometheus (link)

## Prevention Checklist

- [ ] Change management in place
- [ ] Automated tests >90% coverage
- [ ] Staging environment testing
- [ ] Canary deployment
- [ ] Feature flags for rollback
- [ ] Monitoring & alerting configured
- [ ] Runbooks documented
- [ ] On-call rotation established
- [ ] Regular incident drills
- [ ] Post-mortems completed

---

**Last Updated**: June 2026
**Version**: 1.0
