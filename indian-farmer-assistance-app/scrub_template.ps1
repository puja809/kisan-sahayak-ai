$ErrorActionPreference = "Stop"

Write-Host "Reading local .env file to identify secrets..."
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

$dbPass = $envVars["USER_DB_PASSWORD"]
$jwtSecret = $envVars["JWT_SECRET"]
$weatherKey = $envVars["WEATHER_API_KEY"]
$dataGovKey = $envVars["MANDI_DATAGOV_PRICE_API_KEY"]

# Extract RDS endpoint from JDBC URL
$dbUrl = $envVars["USER_DB_URL"]
if ($dbUrl -match "//([^:]+):") {
    $rdsEndpoint = $matches[1]
}
else {
    Write-Host "[ERROR] Could not parse RDS endpoint from USER_DB_URL: $dbUrl" -ForegroundColor Red
    exit 1
}

$file = "aws-ecs-template.yml"
if (-not (Test-Path $file)) {
    Write-Host "[ERROR] $file not found!" -ForegroundColor Red
    exit 1
}

Write-Host "Scrubbing known secrets from $file..."
$content = Get-Content $file -Raw

# Replace real secrets with placeholders if they accidentally exist in the file
if ($dbPass -and $content.Contains($dbPass)) { $content = $content.Replace($dbPass, '<YOUR_DB_PASSWORD>') }
if ($jwtSecret -and $content.Contains($jwtSecret)) { $content = $content.Replace($jwtSecret, '<YOUR_JWT_SECRET>') }
if ($weatherKey -and $content.Contains($weatherKey)) { $content = $content.Replace($weatherKey, '<YOUR_WEATHER_API_KEY>') }
if ($dataGovKey -and $content.Contains($dataGovKey)) { $content = $content.Replace($dataGovKey, '<YOUR_DATAGOV_API_KEY>') }
if ($rdsEndpoint -and $content.Contains($rdsEndpoint)) { $content = $content.Replace($rdsEndpoint, '<YOUR_RDS_ENDPOINT>') }

Set-Content -Path $file -Value $content

Write-Host "`n[SUCCESS] Scrubbed all secrets from $file and replaced them with placeholders." -ForegroundColor Green
Write-Host "The file is now safe to commit to version control."
