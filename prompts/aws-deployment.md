# AWS Deployment Prompt

## Task
Deploy Java LLM Enterprise application to AWS with production-grade infrastructure.

## Architecture Overview

```
Route 53 (DNS)
    ↓
CloudFront (CDN)
    ↓
Application Load Balancer
    ↓
ECS Fargate Cluster (Services)
    ↓
RDS PostgreSQL (Database)
ElastiCache Redis (Cache)
MSK Kafka (Messaging)
S3 (Document Storage)
```

## AWS Services

### Compute
- **ECS Fargate**: Containerized services (no server management)
- **Auto Scaling**: Horizontal scaling based on metrics
- **Lambda**: Async tasks (optional)

### Database
- **RDS PostgreSQL**: Relational database with pgvector
- **ElastiCache Redis**: In-memory cache
- **S3**: Document storage

### Messaging
- **MSK (Managed Streaming for Kafka)**: Event streaming
- **SQS**: Simple Queue Service (optional)

### Networking
- **VPC**: Isolated network
- **ALB**: Application Load Balancer
- **CloudFront**: CDN
- **Route 53**: DNS

### Security
- **IAM**: Identity and access management
- **KMS**: Key management service
- **Secrets Manager**: Secrets storage
- **VPC Security Groups**: Network firewall
- **WAF**: Web application firewall

### Monitoring
- **CloudWatch**: Logs and metrics
- **X-Ray**: Distributed tracing
- **SNS**: Notifications
- **CloudTrail**: Audit logging

## Deployment Process

### 1. Infrastructure Setup (Terraform)

```hcl
# VPC
resource "aws_vpc" "llm_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
}

# RDS PostgreSQL
resource "aws_db_instance" "postgres" {
  engine               = "postgres"
  engine_version       = "15.3"
  instance_class       = "db.r6g.xlarge"
  allocated_storage    = 100
  multi_az             = true
  storage_encrypted    = true
  backup_retention_days = 30
}

# ElastiCache Redis
resource "aws_elasticache_cluster" "redis" {
  cluster_id    = "llm-redis"
  engine        = "redis"
  node_type     = "cache.r6g.xlarge"
  num_cache_nodes = 3
  parameter_group_name = "default.redis7"
  engine_version = "7.0"
}

# MSK Kafka
resource "aws_msk_cluster" "kafka" {
  cluster_name           = "llm-kafka"
  kafka_version          = "3.5.0"
  number_of_broker_nodes = 3
  broker_node_group_info { ... }
}
```

### 2. ECS Task Definition

```json
{
  "family": "ai-orchestrator-service",
  "containerDefinitions": [
    {
      "name": "ai-orchestrator",
      "image": "123456789.dkr.ecr.us-east-1.amazonaws.com/ai-orchestrator:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "hostPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        }
      ],
      "secrets": [
        {
          "name": "OPENAI_API_KEY",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:123456:secret:openai-key"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/ai-orchestrator",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "memory": 2048,
      "cpu": 1024
    }
  ],
  "requiresCompatibilities": ["FARGATE"],
  "networkMode": "awsvpc"
}
```

### 3. Docker Image Build & Push

```bash
# Build image
docker build -t ai-orchestrator:latest .

# Tag for ECR
docker tag ai-orchestrator:latest \
  123456789.dkr.ecr.us-east-1.amazonaws.com/ai-orchestrator:latest

# Push to ECR
aws ecr get-login-password --region us-east-1 | docker login \
  --username AWS --password-stdin \
  123456789.dkr.ecr.us-east-1.amazonaws.com

docker push \
  123456789.dkr.ecr.us-east-1.amazonaws.com/ai-orchestrator:latest
```

### 4. CloudFormation/Terraform Deployment

```bash
# Deploy infrastructure
terraform init
terraform plan
terraform apply

# Verify deployment
aws ecs list-services --cluster llm-cluster
aws rds describe-db-instances --db-instance-identifier llm-postgres
aws elasticache describe-cache-clusters --cache-cluster-id llm-redis
```

## Configuration Management

### Environment Variables
```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://rds-endpoint:5432/llm_db
SPRING_DATASOURCE_USERNAME: ${DB_USER}
SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
SPRING_REDIS_HOST: elasticache-endpoint
SPRING_KAFKA_BOOTSTRAP_SERVERS: msk-broker-1,msk-broker-2
OPENAI_API_KEY: ${OPENAI_API_KEY}
```

### Secrets Management
```bash
# Store in AWS Secrets Manager
aws secretsmanager create-secret \
  --name openai-api-key \
  --secret-string "sk-..."

# Reference in ECS Task Definition
"valueFrom": "arn:aws:secretsmanager:region:account:secret:name"
```

## Monitoring & Logging

### CloudWatch Logs
```bash
# Create log group
aws logs create-log-group --log-group-name /ecs/ai-orchestrator

# Stream logs
aws logs tail /ecs/ai-orchestrator --follow
```

### Metrics & Alarms
```bash
# CPU utilization alarm
aws cloudwatch put-metric-alarm \
  --alarm-name high-cpu \
  --alarm-actions arn:aws:sns:region:account:topic

# Memory utilization
aws cloudwatch put-metric-alarm \
  --alarm-name high-memory \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold
```

## Auto-Scaling Configuration

```bash
# Target tracking scaling policy
aws application-autoscaling put-scaling-policy \
  --policy-name cpu-scaling \
  --service-namespace ecs \
  --target-tracking-scaling-policy-configuration \
    "TargetValue=70.0,PredefinedMetricSpecification={PredefinedMetricType=ECSServiceAverageCPUUtilization}"
```

## Security Best Practices

1. **Network Security**
   - Private subnets for databases
   - Security groups restrict access
   - VPC endpoints for AWS services

2. **Data Security**
   - Encryption at rest (RDS, S3)
   - Encryption in transit (TLS)
   - Secrets in Secrets Manager

3. **Access Control**
   - IAM roles per service
   - Least privilege principle
   - MFA for admin access

4. **Audit & Compliance**
   - CloudTrail logging
   - VPC Flow Logs
   - Config Rules for compliance

## Cost Optimization

### Reserved Instances
- Database: 1-year term
- Cache: 1-year term
- Compute: On-demand for flexibility

### Auto-Scaling
- Scale down during low traffic
- Scheduled scaling for predictable patterns
- Cost anomaly detection

## Disaster Recovery

### Backup Strategy
- RDS automated backups (30 days)
- Cross-region replication
- S3 versioning enabled

### RTO/RPO
- RTO: < 5 minutes
- RPO: < 1 minute

## Deployment Checklist
- [ ] VPC and networking configured
- [ ] RDS PostgreSQL with pgvector
- [ ] ElastiCache Redis cluster
- [ ] MSK Kafka cluster
- [ ] ECS cluster created
- [ ] Docker image built and pushed
- [ ] Task definitions configured
- [ ] Services deployed
- [ ] Load balancer configured
- [ ] DNS records updated
- [ ] Monitoring and alarms set
- [ ] Logging configured
- [ ] Security groups verified
- [ ] Backups configured
- [ ] Tested from staging environment
