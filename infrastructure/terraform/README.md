# Terraform Configuration

This directory contains Infrastructure as Code (IaC) for deploying the Java LLM Enterprise platform to AWS.

## Structure

```
terraform/
├── main.tf          # Main resources
├── vpc.tf           # VPC and networking
├── rds.tf           # PostgreSQL database
├── elasticache.tf   # Redis cache
├── msk.tf           # Kafka cluster
├── ecs.tf           # Container orchestration
├── variables.tf     # Input variables
├── outputs.tf       # Output values
├── terraform.tfvars # Variable values
└── README.md        # This file
```

## Prerequisites

- Terraform 1.0+
- AWS CLI configured with credentials
- AWS account with appropriate permissions

## Usage

### Initialize Terraform

```bash
cd infrastructure/terraform
terraform init
```

### Plan Deployment

```bash
terraform plan -out=tfplan
```

### Apply Changes

```bash
terraform apply tfplan
```

### Destroy Infrastructure

```bash
terraform destroy
```

## Configuration

Edit `terraform.tfvars` to configure:
- AWS region
- VPC CIDR
- Database instance type
- Cache instance type
- Number of Kafka brokers
- ECS task definitions

## Outputs

After deployment, Terraform outputs:
- RDS endpoint
- Redis endpoint
- Kafka bootstrap servers
- ECS cluster name
- Load balancer DNS

```bash
terraform output
```

## State Management

- State stored locally (development) or in S3 (production)
- Use DynamoDB for state locking (production)

```bash
# Configure S3 backend
terraform init -backend-config="bucket=tfstate" \
  -backend-config="key=llm/terraform.tfstate"
```

## Scaling

### Database

```bash
# Edit terraform.tfvars
allocated_storage = 200

# Apply
terraform apply
```

### Cache

```bash
# Edit terraform.tfvars
cache_node_type = "cache.r6g.2xlarge"

# Apply
terraform apply
```

### Kafka

```bash
# Edit terraform.tfvars
kafka_broker_nodes = 5

# Apply
terraform apply
```

## Cost Estimation

```bash
terraform plan -out=tfplan
terraform show -json tfplan | jq '.resource_changes[] | select(.change.actions[]=="create") | .address'
```

## Troubleshooting

### Terraform Plan Shows Errors

```bash
terraform validate
terraform fmt -recursive
terraform plan
```

### AWS Permissions Issues

Verify AWS credentials and permissions for:
- EC2
- RDS
- ElastiCache
- MSK
- ECS
- VPC
- IAM

### State Corruption

```bash
# Backup state
cp terraform.tfstate terraform.tfstate.backup

# Refresh state
terraform refresh
```

## Next Steps

1. Apply Terraform configuration
2. Deploy Docker images to ECR
3. Update ECS services with new images
4. Configure monitoring and alarms
5. Test connectivity
6. Configure auto-scaling policies

---

**Last Updated**: June 2026
**Version**: 1.0
