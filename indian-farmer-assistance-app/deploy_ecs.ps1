$ErrorActionPreference = "Stop"

Write-Host "Reading local .env file..."
$envFile = ".\.env"
if (-not (Test-Path $envFile)) {
    Write-Host "[ERROR] .env file not found in the current directory!" -ForegroundColor Red
    exit 1
}

# Parse .env into a dictionary
$envVars = @{}
foreach ($line in Get-Content $envFile) {
    if (-not [string]::IsNullOrWhiteSpace($line) -and -not $line.StartsWith("#")) {
        $parts = $line -split '=', 2
        if ($parts.Length -eq 2) {
            $envVars[$parts[0].Trim()] = $parts[1].Trim()
        }
    }
}

Write-Host "Extracting variables from .env..."
$dbPass = $envVars["USER_DB_PASSWORD"]
$jwtSecret = $envVars["JWT_SECRET"]
$weatherKey = $envVars["WEATHER_API_KEY"]
$dataGovKey = $envVars["MANDI_DATAGOV_PRICE_API_KEY"]

# Extract RDS endpoint from JDBC URL: jdbc:postgresql://farmer-db-free...:5432/farmer_db
$dbUrl = $envVars["USER_DB_URL"]
if ($dbUrl -match "//([^:]+):") {
    $rdsEndpoint = $matches[1]
}
else {
    Write-Host "[ERROR] Could not parse RDS endpoint from USER_DB_URL: $dbUrl" -ForegroundColor Red
    exit 1
}

Write-Host "Found credentials successfully. Generating deployment template..."

# Load the secure template from Git
$templateContent = Get-Content "aws-ecs-template.yml" -Raw

# In-memory replacement
$templateContent = $templateContent -replace '<YOUR_DB_PASSWORD>', $dbPass
$templateContent = $templateContent -replace '<YOUR_JWT_SECRET>', $jwtSecret
$templateContent = $templateContent -replace '<YOUR_WEATHER_API_KEY>', $weatherKey
$templateContent = $templateContent -replace '<YOUR_DATAGOV_API_KEY>', $dataGovKey
$templateContent = $templateContent -replace '<YOUR_RDS_ENDPOINT>', $rdsEndpoint

# Write the rendered copy (Make sure .aws-ecs-template-rendered.yml is added to .gitignore)
$renderedFile = ".aws-ecs-template-rendered.yml"
Set-Content -Path $renderedFile -Value $templateContent

Write-Host "`nSuccessfully injected local secrets into $renderedFile." -ForegroundColor Green
Write-Host "Deploying CloudFormation stack 'farmer-ecs-stack'..."

# Deploy the rendered template
aws cloudformation deploy `
    --template-file $renderedFile `
    --stack-name farmer-ecs-stack `
    --capabilities CAPABILITY_NAMED_IAM

Write-Host "`nWaiting for CloudFormation stack to reach steady state..."

if ($?) {
    Write-Host "`n[SUCCESS] CloudFormation stack deployed successfully!" -ForegroundColor Green
    
    # Optionally delete the rendered file after success
    Remove-Item $renderedFile -ErrorAction SilentlyContinue
}
else {
    Write-Host "`n[ERROR] CloudFormation deployment failed. Check AWS Console." -ForegroundColor Red
}
