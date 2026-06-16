# Kubernetes Deployment - AI Orchestrator Service

## Prerequisites
- Kubernetes cluster (EKS, AKS, or GKE)
- Helm 3+
- kubectl configured

## Deployment Steps

### 1. Create Namespace
```bash
kubectl create namespace llm
kubectl label namespace llm istio-injection=enabled
```

### 2. Create Secrets
```bash
kubectl create secret generic ai-orchestrator-secrets \
  --from-literal=openai-api-key=sk-... \
  --from-literal=db-password=password \
  -n llm
```

### 3. Deploy Services
```bash
# PostgreSQL
helm install postgres bitnami/postgresql \
  --namespace llm \
  --values infrastructure/helm/postgres-values.yaml

# Redis
helm install redis bitnami/redis \
  --namespace llm \
  --values infrastructure/helm/redis-values.yaml

# Kafka
helm install kafka bitnami/kafka \
  --namespace llm \
  --values infrastructure/helm/kafka-values.yaml

# Prometheus
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace llm

# Grafana
helm install grafana grafana/grafana \
  --namespace llm
```

### 4. Deploy Applications
```bash
# Gateway Service
kubectl apply -f infrastructure/kubernetes/gateway-service.yaml -n llm

# AI Orchestrator Service
kubectl apply -f infrastructure/kubernetes/ai-orchestrator-service.yaml -n llm

# Ingestion Service
kubectl apply -f infrastructure/kubernetes/ingestion-service.yaml -n llm
```

### 5. Verify Deployment
```bash
# Check pods
kubectl get pods -n llm

# Check services
kubectl get svc -n llm

# Check logs
kubectl logs -f deployment/ai-orchestrator-service -n llm
```

## Scaling

```bash
# Scale up
kubectl scale deployment ai-orchestrator-service --replicas=5 -n llm

# Auto-scaling
kubectl autoscale deployment ai-orchestrator-service \
  --min=2 --max=10 --cpu-percent=80 -n llm
```

## Updating

```bash
# Roll out new version
kubectl set image deployment/ai-orchestrator-service \
  ai-orchestrator=myregistry/ai-orchestrator:v2.0.0 -n llm

# Check rollout status
kubectl rollout status deployment/ai-orchestrator-service -n llm

# Rollback if needed
kubectl rollout undo deployment/ai-orchestrator-service -n llm
```

## Monitoring

```bash
# Port forward to Grafana
kubectl port-forward -n llm svc/grafana 3000:80

# Port forward to Prometheus
kubectl port-forward -n llm svc/prometheus 9090:9090

# View logs
kubectl logs -f deployment/ai-orchestrator-service -n llm
```

## Cleanup

```bash
# Delete application
kubectl delete deployment -n llm --all

# Delete namespace
kubectl delete namespace llm
```
