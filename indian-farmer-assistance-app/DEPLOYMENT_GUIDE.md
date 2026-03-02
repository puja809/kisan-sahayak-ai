# AWS ECS Deployment Guide (Zero-to-Hero)

This guide details the complete process for deploying the Indian Farmer Assistance App microservices architecture to AWS ECS Fargate from scratch. 

We have created 4 custom PowerShell scripts to automate and secure this process:
1. `build_and_push.ps1`
2. `deploy_ecs.ps1` 
3. `restart_services.ps1`
4. `scrub_template.ps1`

---

## Part 1: Starting from Scratch (Prerequisites)

If you are deploying this application to a completely new AWS Account or a new AWS Region, you must follow these static setup steps **before** running any scripts.

### 1a. Configuration Variables (`.ps1` scripts & `.env`)
You must update the hardcoded variables at the top of the PowerShell scripts to match your new AWS environment:
- **`$REGISTRY_ID`**: Your 12-digit AWS Account ID (Found in the top right of the AWS Console).
- **`$REGION`**: Your target AWS region (e.g., `us-east-1`, `ap-south-1`).

You must update these variables in:
- `build_and_push.ps1` (Lines 3 & 4)
- `restart_services.ps1` (Line 4)
- `.env` (Update `USER_DB_URL`, etc., to point to your new RDS instance).

### 1b. Create the Amazon ECR Repositories (One-Time Setup)
Before you can build and push Docker images, the target repositories must exist in Amazon Elastic Container Registry (ECR). 

Run these exact AWS CLI commands once in your terminal to create all 12 required repositories in your chosen region:

```powershell
aws ecr create-repository --repository-name farmer-api-gateway --region us-east-1
aws ecr create-repository --repository-name farmer-user-service --region us-east-1
aws ecr create-repository --repository-name farmer-crop-service --region us-east-1
aws ecr create-repository --repository-name farmer-location-service --region us-east-1
aws ecr create-repository --repository-name farmer-mandi-service --region us-east-1
aws ecr create-repository --repository-name farmer-scheme-service --region us-east-1
aws ecr create-repository --repository-name farmer-admin-service --region us-east-1
aws ecr create-repository --repository-name farmer-iot-service --region us-east-1
aws ecr create-repository --repository-name farmer-weather-service --region us-east-1
aws ecr create-repository --repository-name farmer-yield-service --region us-east-1
aws ecr create-repository --repository-name farmer-ml-service --region us-east-1
aws ecr create-repository --repository-name farmer-frontend --region us-east-1
```
*(Note: Change `--region us-east-1` to your target region if you are migrating).*

---

## Part 2: The Deployment Pipeline (Daily Usage)

Once your ECR repositories exist, follow this exact sequence to deploy your code to the cloud.

### Step 1: Build & Push Docker Images (`build_and_push.ps1`)
**Purpose**: Compiles your local Java and Angular code into Docker images and uploads them to AWS ECR.
**When to execute**:  Every time you change `.java` or `.ts` source code and want the changes shipped.
**Command**:
```powershell
.\build_and_push.ps1
```

### Step 2: Deploy Infrastructure (`deploy_ecs.ps1`)
**Purpose**: Safely deploys AWS CloudFormation infrastructure variations without exposing raw secrets to the source-controlled `aws-ecs-template.yml`.
**How it works**: It dynamically parses your local `.env` file, injects your real Database Passwords and API Keys in-memory, updates the CloudFormation Stack (`farmer-ecs-stack`), and wipes the temporary file. 
**When to execute**: 
- **The very first time you deploy** (To create the ECS Cluster, ALB, and Fargate Services).
- If you add a brand new microservice.
- If you change CPU/Memory limits, Load Balancer rules, or Security Groups in `aws-ecs-template.yml`.
**Command**:
```powershell
.\deploy_ecs.ps1
```

### Step 3: Graceful Cluster Restart (`restart_services.ps1`)
**Purpose**: Triggers a `force-new-deployment` rolling pipeline update to refresh instances on the ECS cluster.
**How it works**: AWS ECS does not automatically pull new images when ECR changes. This script queries the 12 active services in the cluster and forces them to pull the newly built images from Step 1. It waits until the cluster stabilizes, then performs an HTTP Health Check on the API Gateway.
**When to execute**: 
- **IMMEDIATELY AFTER** running `build_and_push.ps1`. 
**Command**:
```powershell
.\restart_services.ps1
```

---

## Part 3: Security Utilities

### Emergency Blueprint Wiper (`scrub_template.ps1`)
**Purpose**: Erases any and all AWS passwords mistakenly hardcoded over the `aws-ecs-template.yml`.
**When to execute**: You want to commit `aws-ecs-template.yml` but realize someone accidentally pasted the literal `farmer_password` or `JWT_SECRET` directly onto the document.
**How it works**: It parses secret values from `.env` and searches the local YAML for identical matches. If found, it permanently replaces them with the safe `.env` wildcard aliases (e.g. `<YOUR_DB_PASSWORD>`).
**Command**:
```powershell
.\scrub_template.ps1
```

---

## Part 4: API Gateway Architecture

All external traffic flows through a single entry point — the **Application Load Balancer (ALB)** — which fans out to the **Spring Cloud API Gateway** (`farmer-api-gateway`) running on port `8080`.

### How Routing Works

```
Browser → ALB (port 80) → API Gateway (port 8080) → Backend Services (internal ports)
```

1. **ALB Listener Rule**: Any request matching `/api/*` is forwarded to the `api-gateway-targets` Target Group (port `8080`). Everything else (e.g. `/`, `/schemes`, `/crops`) is forwarded to the `frontend-targets` Target Group (port `4200`).

2. **Spring Cloud Gateway** (`api-gateway/src/main/resources/application.yml`): The API Gateway uses route predicates to match URL paths and forward them to the correct internal service via **AWS Cloud Map DNS** (`*.farmer-network.local`).

### Service Route Table

| Route Pattern | Target Service | Internal DNS | Port |
|---|---|---|---|
| `/api/v1/auth/**`, `/api/v1/users/**` | user-service | `user-service.farmer-network.local` | 8099 |
| `/api/v1/crops/yield/**` | yield-service | `yield-service.farmer-network.local` | 8101 |
| `/api/v1/crops/**` | crop-service | `crop-service.farmer-network.local` | 8096 |
| `/api/v1/location/government-bodies/**` | location-service | `location-service.farmer-network.local` | 8095 |
| `/api/v1/mandi/**`, `/api/v1/fertilizer/**` | mandi-service | `mandi-service.farmer-network.local` | 8093 |
| `/api/v1/schemes/**` | scheme-service | `scheme-service.farmer-network.local` | 8097 |
| `/api/v1/admin/documents/**` | admin-service | `admin-service.farmer-network.local` | 8091 |
| `/api/v1/iot/**` | iot-service | `iot-service.farmer-network.local` | 8094 |
| `/api/v1/weather/**` | weather-service | `weather-service.farmer-network.local` | 8100 |
| `/api/ml/**` | ml-service | `ml-service.farmer-network.local` | 8001 |

> **IMPORTANT**: The `yield-service` route (`/api/v1/crops/yield/**`) MUST appear **before** the `crop-service` route (`/api/v1/crops/**`) in the gateway config. Spring Cloud Gateway evaluates routes top-to-bottom — if `crop-service` is listed first, it will swallow yield requests and return 403/404 errors.

### Internal DNS (AWS Cloud Map)

Services discover each other via a **Private DNS Namespace** called `farmer-network.local`, created by the CloudFormation template. Each ECS service registers itself automatically under this namespace (e.g., `location-service.farmer-network.local`). The API Gateway resolves these DNS names internally within the VPC — no public internet is involved.

### Adding a New Route

To expose a new backend service through the gateway:
1. Add the service's ECS Task Definition, Discovery Service, and ECS Service to `aws-ecs-template.yml`.
2. Add a new route entry in `api-gateway/src/main/resources/application.yml`:
   ```yaml
   - id: my-new-service
     uri: http://my-new-service.farmer-network.local:<PORT>
     predicates:
     - Path=/api/v1/my-new-service/**
   ```
3. Run `.\build_and_push.ps1` → `.\restart_services.ps1`.

---

## Part 5: RDS Database Setup (One-Time)

If starting from scratch, you need to create a PostgreSQL RDS instance. Use the AWS Console or CLI:

```powershell
aws rds create-db-instance `
  --db-instance-identifier farmer-db-free `
  --db-instance-class db.t3.micro `
  --engine postgres `
  --engine-version 15 `
  --master-username postgres `
  --master-user-password <YOUR_DB_PASSWORD> `
  --allocated-storage 20 `
  --publicly-accessible `
  --region us-east-1
```

> **Note**: The default database name in RDS Free Tier is `postgres` (not `farmer_db`). All services connect to the `postgres` database. Update your `.env` JDBC URLs accordingly:
> ```
> USER_DB_URL=jdbc:postgresql://<YOUR_RDS_ENDPOINT>:5432/postgres
> ```

After the RDS instance is available, update the Security Group to allow inbound connections on port `5432` from your VPC CIDR (`172.31.0.0/16`).

---

## Part 6: Migrating to a Different Region

To redeploy the entire application in a new AWS region (e.g., from `us-east-1` to `ap-south-1`):

### Files to Update

| File | What to Change |
|---|---|
| `build_and_push.ps1` | `$REGION` on line 4 |
| `restart_services.ps1` | `$REGION` on line 4 |
| `aws-ecs-template.yml` | ECR image URIs (replace `us-east-1` in all `Image:` fields) |
| `.env` | All `*_DB_URL` values (new RDS endpoint) |

### Steps

1. Create an RDS instance in the new region (see Part 5).
2. Create ECR repositories in the new region (see Part 1b).
3. Update the files listed above with the new region and endpoints.
4. Log into the new ECR:
   ```powershell
   aws ecr get-login-password --region ap-south-1 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.ap-south-1.amazonaws.com
   ```
5. Run the full pipeline:
   ```powershell
   .\build_and_push.ps1
   .\deploy_ecs.ps1
   .\restart_services.ps1
   ```

---

## Quick Reference Cheat Sheet

| Task | Command |
|---|---|
| **Build & push all Docker images** | `.\build_and_push.ps1` |
| **Deploy/update CloudFormation stack** | `.\deploy_ecs.ps1` |
| **Restart all ECS services & health check** | `.\restart_services.ps1` |
| **Scrub secrets before committing** | `.\scrub_template.ps1` |
| **Check ECS service status** | `aws ecs describe-services --cluster farmer-ecs-cluster --services <name> --query "services[0].events[0:3].message" --output json` |
| **View service logs** | `aws logs tail /ecs/farmer-<service-name> --follow --region us-east-1` |
| **Force restart single service** | `aws ecs update-service --cluster farmer-ecs-cluster --service <name> --force-new-deployment` |
| **Check ALB health** | `curl http://farmer-alb-<ID>.us-east-1.elb.amazonaws.com/actuator/health` |

---

## Part 7: Running Services Locally

There are 3 ways to run the app locally for development and testing.

### Option A: Docker Compose (Recommended — Runs Everything)

This spins up all 12 services plus the frontend in Docker containers, reading credentials from `.env`.

```powershell
docker-compose up --build
```

The services will be available at:
- **Frontend**: http://localhost:4200
- **API Gateway**: http://localhost:8080
- **Individual services**: See port table below

To stop everything:
```powershell
docker-compose down
```

### Option B: Run a Single Service via Maven (For Debugging)

If you need to debug a specific service with breakpoints, run it directly with Maven. You must set the environment variables first so it knows which database to connect to.

```powershell
# Step 1: Set environment variables (reads from .env manually)
$env:LOCATION_DB_URL = "jdbc:postgresql://farmer-db-free.c6988uycio77.us-east-1.rds.amazonaws.com:5432/postgres"
$env:LOCATION_DB_USERNAME = "postgres"
$env:LOCATION_DB_PASSWORD = "farmer_password"
$env:LOCATION_SERVICE_PORT = "8095"

# Step 2: Run the service
cd backend
mvn spring-boot:run -pl location-service
```

Replace `location-service` with whichever service you want to run (e.g., `crop-service`, `scheme-service`).

### Option C: Run a Pre-Built JAR Directly

If you've already built the project with `mvn clean package -DskipTests`, you can run the JAR directly:

```powershell
# Set env vars (same as Option B)
$env:LOCATION_DB_URL = "jdbc:postgresql://farmer-db-free.c6988uycio77.us-east-1.rds.amazonaws.com:5432/postgres"
$env:LOCATION_DB_USERNAME = "postgres"
$env:LOCATION_DB_PASSWORD = "farmer_password"

# Run the JAR
java -jar backend\location-service\target\location-service-1.0.0-SNAPSHOT.jar
```

### Service Port Reference

| Service | Local Port | Test URL |
|---|---|---|
| api-gateway | 8080 | http://localhost:8080/actuator/health |
| user-service | 8099 | http://localhost:8099/api/v1/users |
| crop-service | 8096 | http://localhost:8096/api/v1/crops |
| location-service | 8095 | http://localhost:8095/api/v1/government-bodies/state/Bihar |
| mandi-service | 8093 | http://localhost:8093/api/v1/mandi |
| scheme-service | 8097 | http://localhost:8097/api/v1/schemes/state/Bihar |
| admin-service | 8091 | http://localhost:8091/api/v1/admin |
| iot-service | 8094 | http://localhost:8094/api/v1/iot |
| weather-service | 8100 | http://localhost:8100/api/v1/weather |
| yield-service | 8101 | http://localhost:8101/api/v1/crops/yield/commodities |
| ml-service | 8001 | http://localhost:8001/api/ml/health |
| frontend | 4200 | http://localhost:4200 |

### Running the Frontend Separately (Angular Dev Server)

For live-reload Angular development:
```powershell
cd frontend
npm install
ng serve
```
The frontend will be at http://localhost:4200 and will auto-reload on code changes.

---

## Part 8: CI/CD with GitHub Actions

A GitHub Actions workflow is available at `.github/workflows/deploy-ecs.yml`. It automatically builds all Docker images and deploys them to ECS whenever code is pushed to the `main` branch.

### Required GitHub Secrets

You must add these secrets in your GitHub repository settings (**Settings → Secrets and Variables → Actions**):

| Secret Name | Value |
|---|---|
| `AWS_ACCESS_KEY_ID` | Your IAM user's Access Key ID |
| `AWS_SECRET_ACCESS_KEY` | Your IAM user's Secret Access Key |

### How It Works

1. Triggers on every `push` to the `main` branch.
2. Checks out your code and sets up JDK 17.
3. Builds all backend services with `mvn clean package -DskipTests`.
4. Builds Docker images for all 12 services and pushes them to ECR.
5. Triggers a `force-new-deployment` on each ECS service to pick up the new images.

### Trigger Branch

The workflow currently triggers on `main`. To change this (e.g., to `aws-ecs`), edit `.github/workflows/deploy-ecs.yml`:
```yaml
on:
  push:
    branches:
      - aws-ecs  # Change this to your target branch
```
