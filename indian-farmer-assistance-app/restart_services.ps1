$ErrorActionPreference = "Continue"

$CLUSTER_NAME = "farmer-ecs-cluster"
$REGION = "us-east-1"

Write-Host "Fetching list of services in ECS cluster: $CLUSTER_NAME..."
$servicesRaw = aws ecs list-services --cluster $CLUSTER_NAME --query "serviceArns" --output text --region $REGION

if (-not $servicesRaw -or $servicesRaw.Trim() -eq "") {
    Write-Host "No services found in cluster $CLUSTER_NAME."
    exit
}

$services = $servicesRaw -split "`t" | Where-Object { $_ -ne "" }

Write-Host "`nFound $($services.Count) services. Triggering force-new-deployment for all..."
Write-Host "================================================="

foreach ($serviceArn in $services) {
    # Extract just the service name from the ARN for cleaner logging
    $serviceName = ($serviceArn -split "/")[-1]
    
    Write-Host "Restarting service: $serviceName..."
    aws ecs update-service --cluster $CLUSTER_NAME --service $serviceArn --force-new-deployment --region $REGION | Out-Null
    
    if ($?) {
        Write-Host "  -> Successfully triggered deployment for $serviceName" -ForegroundColor Green
    }
    else {
        Write-Host "  -> Failed to trigger deployment for $serviceName" -ForegroundColor Red
    }
}

Write-Host "`n================================================="
Write-Host "All services triggered. Waiting for deployments to stabilize..."
Write-Host "This process can take between 3 to 10 minutes..."

# AWS CLI wait services-stable only supports max 10 services per call. So we chunk it.
$chunkSize = 10
for ($i = 0; $i -lt $services.Count; $i += $chunkSize) {
    $chunk = $services | Select-Object -Skip $i -First $chunkSize
    $chunkNames = $chunk | ForEach-Object { ($_ -split "/")[-1] }
    
    Write-Host "Waiting for batch: $($chunkNames -join ', ')"
    # Re-assemble the names to pass as arguments to aws CLI
    # The command requires space separated service names.
    $argsList = @("ecs", "wait", "services-stable", "--cluster", $CLUSTER_NAME, "--region", $REGION, "--services") + $chunkNames
    & aws $argsList
    
    if ($?) {
        Write-Host "  -> Batch stabilized!" -ForegroundColor Green
    }
    else {
        Write-Host "  -> Error waiting for batch. Check AWS Console." -ForegroundColor Red
        exit 1
    }
}

Write-Host "`nAll ECS services successfully reached a STABLE state!"

Write-Host "`nPerforming Health Check on ALB API Gateway..."

$TEST_URL = "https://farmer-alb-1165490536.us-east-1.elb.amazonaws.com/api/v1/schemes/state/Bihar"
try {
    $response = Invoke-WebRequest -Uri $TEST_URL -UseBasicParsing -TimeoutSec 15
    if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
        Write-Host "`n[SUCCESS] Systems loaded perfectly! The API returned HTTP $($response.StatusCode)" -ForegroundColor Green
    }
    else {
        Write-Host "`n[WARNING] Systems stabilized, but the Health Check returned HTTP $($response.StatusCode)" -ForegroundColor Yellow
    }
}
catch {
    Write-Host "`n[ERROR] Systems stabilized, but Health Check failed: $($_.Exception.Message)" -ForegroundColor Red
}
