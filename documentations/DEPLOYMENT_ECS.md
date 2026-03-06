# AWS ECS Deployment Architecture

**Infrastructure**: AWS Elastic Container Service (ECS)  
**Orchestration**: Fargate (Serverless)  
**Service Discovery**: AWS Cloud Map  
**Load Balancing**: Application Load Balancer (ALB)  
**Database**: Amazon RDS PostgreSQL  
**Caching**: Amazon ElastiCache Redis  
**Logging**: CloudWatch Logs

## Overview

The Indian Farmer Assistance App is deployed on AWS ECS Fargate, providing a scalable, serverless container orchestration platform. The deployment uses Infrastructure as Code (CloudFormation) for reproducible deployments.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Internet                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Application Load Balancer                     │
│  - Port 80 (HTTP) → Redirect to HTTPS                           │
│  - Port 443 (HTTPS) → Route to services                         │
│  - Health checks on /actuator/health                            │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      ECS Cluster                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Fargate Tasks (Serverless Containers)                  │   │
│  │                                                          │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │ API Gateway Service (Port 8080)                │    │   │
│  │  │ - CPU: 512, Memory: 1024 MB                    │    │   │
│  │  │ - Desired Count: 1                             │    │   │
│  │  │ - Auto-scaling: 1-3 instances                  │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                                                          │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │ User Service (Port 8099)                       │    │   │
│  │  │ - CPU: 512, Memory: 1024 MB                    │    │   │
│  │  │ - Desired Count: 1                             │    │   │
│  │  │ - Auto-scaling: 1-2 instances                  │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                                                          │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │ Crop Service (Port 8093)                       │    │   │
│  │  │ - CPU: 512, Memory: 1024 MB                    │    │   │
│  │  │ - Desired Count: 1                             │    │   │
│  │  │ - Auto-scaling: 1-2 instances                  │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                                                          │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │ Mandi Service (Port 8096)                      │    │   │
│  │  │ - CPU: 512, Memory: 1024 MB                    │    │   │
│  │  │ - Desired Count: 1                             │    │   │
│  │  │ - Auto-scaling: 1-2 instances                  │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                                                          │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │ Weather Service (Port 8100)                    │    │   │
│  │  │ - CPU: 512, Memory: 1024 MB                    │    │   │
│  │  │ - Desired Count: 1                             │    │   │
│  │  │ - Auto-scaling: 1-2 instances                  │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                                                          │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │ Scheme Service (Port 8097)                     │    │   │
│  │  │ - CPU: 512, Memory: 1024 MB                    │    │   │
│  │  │ - Desired Count: 1                             │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                                                          │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │ Location Service (Port 8095)                   │    │   │
│  │  │ - CPU: 512, Memory: 1024 MB                    │    │   │
│  │  │ - Desired Count: 1                             │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                                                          │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │ Yield Service (Port 8101)                      │    │   │
│  │  │ - CPU: 512, Memory: 1024 MB                    │    │   │
│  │  │ - Desired Count: 1                             │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                                                          │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │ Admin Service (Port 8091)                      │    │   │
│  │  │ - CPU: 512, Memory: 1024 MB                    │    │   │
│  │  │ - Desired Count: 1                             │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                                                          │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │ ML Service (Port 8001)                         │    │   │
│  │  │ - CPU: 512, Memory: 1024 MB                    │    │   │
│  │  │ - Desired Count: 1                             │    │   │
│  │  │ - Task Role: Bedrock permissions               │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                                                          │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │ Frontend (Port 4200)                           │    │   │
│  │  │ - CPU: 256, Memory: 512 MB                     │    │   │
│  │  │ - Desired Count: 1                             │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                                                          │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Cloud Map (Service Discovery)                          │   │
│  │  - Namespace: farmer-network.local                      │   │
│  │  - DNS-based service discovery                          │   │
│  │  - Auto-registration of services                        │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Data Layer                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Amazon RDS PostgreSQL                                  │   │
│  │  - Multi-AZ deployment                                  │   │
│  │  - Automated backups                                    │   │
│  │  - Read replicas for scaling                            │   │
│  │  - Database: indian_farmer_db                           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Amazon ElastiCache Redis                               │   │
│  │  - Cache layer for performance                          │   │
│  │  - Session management                                   │   │
│  │  - Rate limiting counters                               │   │
│  │  - Recommendation caching                               │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Amazon S3                                              │   │
│  │  - Document storage (Admin Service)                     │   │
│  │  - Bucket: farmer-documents                             │   │
│  │  - Server-side encryption enabled                       │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## ECS Cluster Configuration

### Cluster Details

```yaml
Cluster Name: farmer-ecs-cluster
Launch Type: FARGATE
Capacity Providers: FARGATE, FARGATE_SPOT
VPC: User-provided VPC
Subnets: At least 2 subnets (for high availability)
```

### Task Definitions

Each service has a task definition specifying:

| Service | CPU | Memory | Port | Image |
|---------|-----|--------|------|-------|
| API Gateway | 512 | 1024 | 8080 | farmer-api-gateway:latest |
| User Service | 512 | 1024 | 8099 | farmer-user-service:latest |
| Crop Service | 512 | 1024 | 8093 | farmer-crop-service:latest |
| Mandi Service | 512 | 1024 | 8096 | farmer-mandi-service:latest |
| Weather Service | 512 | 1024 | 8100 | farmer-weather-service:latest |
| Scheme Service | 512 | 1024 | 8097 | farmer-scheme-service:latest |
| Location Service | 512 | 1024 | 8095 | farmer-location-service:latest |
| Yield Service | 512 | 1024 | 8101 | farmer-yield-service:latest |
| Admin Service | 512 | 1024 | 8091 | farmer-admin-service:latest |
| ML Service | 512 | 1024 | 8001 | farmer-ml-service:latest |
| Frontend | 256 | 512 | 4200 | farmer-frontend:latest |

### ECS Services

Each service is deployed as an ECS Service with:

```yaml
Launch Type: FARGATE
Desired Count: 1 (can be scaled)
Network Mode: awsvpc
Deployment Configuration:
  - Circuit Breaker: Enabled
  - Rollback: Enabled
  - Min Healthy Percent: 100%
  - Max Percent: 200%
```

## Load Balancing

### Application Load Balancer (ALB)

```
ALB Configuration:
├─ Name: farmer-alb
├─ Scheme: Internet-facing
├─ Subnets: User-provided subnets
├─ Security Groups: farmer-security-group
│
├─ Listeners:
│  ├─ Port 80 (HTTP)
│  │  └─ Action: Redirect to HTTPS (301)
│  │
│  └─ Port 443 (HTTPS)
│     ├─ Certificate: ACM certificate
│     └─ Rules:
│        ├─ Path /api/* → API Gateway Target Group
│        ├─ Path /actuator/* → API Gateway Target Group
│        └─ Default → Frontend Target Group
│
└─ Target Groups:
   ├─ API Gateway Targets
   │  ├─ Port: 8080
   │  ├─ Protocol: HTTP
   │  ├─ Health Check: /actuator/health
   │  └─ Matcher: 200
   │
   └─ Frontend Targets
      ├─ Port: 4200
      ├─ Protocol: HTTP
      └─ Health Check: /
```

### Health Checks

```
API Gateway:
  Path: /actuator/health
  Interval: 30 seconds
  Timeout: 5 seconds
  Healthy Threshold: 2
  Unhealthy Threshold: 3

Frontend:
  Path: /
  Interval: 30 seconds
  Timeout: 5 seconds
  Healthy Threshold: 2
  Unhealthy Threshold: 3
```

## Service Discovery (Cloud Map)

### Private DNS Namespace

```
Namespace: farmer-network.local
VPC: User-provided VPC
Type: Private DNS

Services:
├─ api-gateway.farmer-network.local:8080
├─ user-service.farmer-network.local:8099
├─ crop-service.farmer-network.local:8093
├─ mandi-service.farmer-network.local:8096
├─ weather-service.farmer-network.local:8100
├─ scheme-service.farmer-network.local:8097
├─ location-service.farmer-network.local:8095
├─ yield-service.farmer-network.local:8101
├─ admin-service.farmer-network.local:8091
└─ ml-service.farmer-network.local:8001
```

### Service Registration

Each service automatically registers with Cloud Map:

```
Service Name: {service-name}
DNS Records: A record with TTL 60
Health Check: Custom config with failure threshold 1
Auto-deregistration: On unhealthy
```

## Security Configuration

### Security Group

```
Inbound Rules:
├─ Port 8080 (HTTP) from 0.0.0.0/0 (API Gateway)
├─ Port 80 (HTTP) from 0.0.0.0/0 (ALB)
├─ Port 443 (HTTPS) from 0.0.0.0/0 (ALB)
└─ All ports from VPC CIDR (172.31.0.0/16) (Inter-service)

Outbound Rules:
└─ All traffic to 0.0.0.0/0
```

### IAM Roles

#### ECS Task Execution Role

```json
{
  "AssumeRolePolicyDocument": {
    "Effect": "Allow",
    "Principal": {"Service": "ecs-tasks.amazonaws.com"},
    "Action": "sts:AssumeRole"
  },
  "ManagedPolicies": [
    "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
  ],
  "Policies": [
    {
      "PolicyName": "EcsLogsPolicy",
      "Statement": [
        {
          "Effect": "Allow",
          "Action": [
            "logs:CreateLogGroup",
            "logs:CreateLogStream",
            "logs:PutLogEvents"
          ],
          "Resource": "arn:aws:logs:*:*:*"
        }
      ]
    }
  ]
}
```

#### ML Service Task Role

```json
{
  "AssumeRolePolicyDocument": {
    "Effect": "Allow",
    "Principal": {"Service": "ecs-tasks.amazonaws.com"},
    "Action": "sts:AssumeRole"
  },
  "Policies": [
    {
      "PolicyName": "BedrockInvokePolicy",
      "Statement": [
        {
          "Effect": "Allow",
          "Action": [
            "bedrock:InvokeModel",
            "bedrock:InvokeModelWithResponseStream"
          ],
          "Resource": "*"
        }
      ]
    }
  ]
}
```

## Environment Configuration

### Environment Variables

Each service receives environment variables via task definition:

```yaml
Common Variables:
  SPRING_PROFILES_ACTIVE: prod
  SPRING_DATASOURCE_URL: jdbc:postgresql://<RDS_ENDPOINT>:5432/postgres
  SPRING_DATASOURCE_USERNAME: postgres
  SPRING_DATASOURCE_PASSWORD: <DB_PASSWORD>
  JWT_SECRET: <JWT_SECRET>

Service-Specific Variables:
  ML_SERVICE_URL: http://ml-service.farmer-network.local:8001
  WEATHER_API_KEY: <API_KEY>
  WEATHER_API_URL: https://api.weatherapi.com/v1
  MANDI_DATAGOV_PRICE_API_KEY: <API_KEY>
  MANDI_DATAGOV_FERTILIZER_API_KEY: <API_KEY>
```

### Secrets Management

Sensitive data should be stored in AWS Secrets Manager:

```
Secrets:
├─ /farmer/db/password
├─ /farmer/jwt/secret
├─ /farmer/weather/api-key
├─ /farmer/datagov/api-key
└─ /farmer/aws/credentials
```

## Logging & Monitoring

### CloudWatch Logs

```
Log Groups:
├─ /ecs/farmer-api-gateway
├─ /ecs/farmer-user-service
├─ /ecs/farmer-crop-service
├─ /ecs/farmer-mandi-service
├─ /ecs/farmer-weather-service
├─ /ecs/farmer-scheme-service
├─ /ecs/farmer-location-service
├─ /ecs/farmer-yield-service
├─ /ecs/farmer-admin-service
├─ /ecs/farmer-ml-service
└─ /ecs/farmer-frontend

Log Retention: 30 days
Log Driver: awslogs
```

### CloudWatch Metrics

```
Metrics:
├─ CPU Utilization
├─ Memory Utilization
├─ Network In/Out
├─ Task Count
├─ Service Count
└─ ALB Target Health
```

### CloudWatch Alarms

```
Alarms:
├─ High CPU Utilization (> 80%)
├─ High Memory Utilization (> 80%)
├─ Task Failures
├─ Service Unhealthy
├─ ALB Target Unhealthy
└─ Database Connection Errors
```

## Auto-Scaling

### Target Tracking Scaling

```yaml
API Gateway Service:
  Target Metric: CPU Utilization
  Target Value: 70%
  Min Capacity: 1
  Max Capacity: 3
  Scale-out Cooldown: 60 seconds
  Scale-in Cooldown: 300 seconds

Other Services:
  Target Metric: CPU Utilization
  Target Value: 75%
  Min Capacity: 1
  Max Capacity: 2
```

## Deployment Process

### CloudFormation Stack Creation

```bash
# Create stack
aws cloudformation create-stack \
  --stack-name farmer-ecs-stack \
  --template-body file://aws-ecs-template.yml \
  --parameters \
    ParameterKey=VpcId,ParameterValue=vpc-xxxxx \
    ParameterKey=Subnets,ParameterValue="subnet-xxxxx,subnet-yyyyy" \
  --capabilities CAPABILITY_NAMED_IAM

# Monitor stack creation
aws cloudformation describe-stacks \
  --stack-name farmer-ecs-stack \
  --query 'Stacks[0].StackStatus'
```

### Service Deployment

```bash
# Update service with new image
aws ecs update-service \
  --cluster farmer-ecs-cluster \
  --service api-gateway \
  --force-new-deployment

# Monitor deployment
aws ecs describe-services \
  --cluster farmer-ecs-cluster \
  --services api-gateway \
  --query 'services[0].deployments'
```

## Cost Optimization

### Fargate Pricing

```
Compute: $0.04664 per vCPU-hour
Memory: $0.00511 per GB-hour

Example (512 CPU, 1024 MB):
  Monthly Cost ≈ $35-50 per task
  Total (10 tasks): $350-500/month
```

### Cost Reduction Strategies

1. **Use Fargate Spot**: 70% discount on Fargate pricing
2. **Right-size tasks**: Use appropriate CPU/memory
3. **Consolidate services**: Combine small services
4. **Reserved Capacity**: For predictable workloads
5. **Auto-scaling**: Scale down during off-peak hours

## Disaster Recovery

### Backup Strategy

```
Database:
  - Automated daily backups (7-day retention)
  - Multi-AZ deployment
  - Read replicas in different AZ

S3:
  - Versioning enabled
  - Cross-region replication
  - Lifecycle policies for old versions
```

### Recovery Procedures

```
Service Failure:
  1. ECS detects unhealthy task
  2. Task is automatically replaced
  3. New task starts in different AZ
  4. ALB routes traffic to new task

Database Failure:
  1. RDS failover to standby instance
  2. Automatic DNS update
  3. Services reconnect automatically

Complete Region Failure:
  1. Restore from backup in different region
  2. Update DNS to new region
  3. Restore S3 data from replication
```

## Monitoring Dashboard

### Key Metrics to Monitor

```
Application Health:
  - API Gateway response time
  - Service error rate
  - Database connection pool usage
  - Cache hit rate

Infrastructure Health:
  - ECS task CPU/memory utilization
  - ALB target health
  - RDS CPU/memory utilization
  - Network throughput

Business Metrics:
  - Active users
  - API requests per minute
  - Recommendation accuracy
  - Disease detection accuracy
```

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Task fails to start | Insufficient resources | Increase task CPU/memory or scale cluster |
| Service unhealthy | Health check failing | Check service logs, verify endpoints |
| High latency | Database bottleneck | Add read replicas, increase cache |
| Out of memory | Memory leak | Restart task, review logs |
| Network timeout | Security group issue | Verify security group rules |

## Future Enhancements

1. **Kubernetes Migration**: Move to EKS for better control
2. **Multi-region Deployment**: Active-active setup
3. **Service Mesh**: Istio for advanced traffic management
4. **GitOps**: Automated deployments via Git
5. **Canary Deployments**: Gradual rollout of new versions
6. **Blue-Green Deployments**: Zero-downtime updates
