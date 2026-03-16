param(
    [string]$DockerUser = "7nepalithito",
    [string]$BackendImage = "policymind-document-service",
    [string]$FrontendImage = "policymind-document-service-frontend",
    [string]$Tag = "latest",
    [switch]$SkipFrontendBuild,
    [string]$Platform = "linux/amd64",
    [string]$BuilderName = "policymindbuilder"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$frontendRoot = Join-Path $repoRoot "frontend"
$backendRef = "${DockerUser}/${BackendImage}:${Tag}"
$frontendRef = "${DockerUser}/${FrontendImage}:${Tag}"

function Ensure-BuildxBuilder {
    param(
        [string]$Name
    )

    cmd /c "docker buildx inspect $Name 1>nul 2>nul"
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "Creating buildx builder '$Name'..." -ForegroundColor Cyan
        & docker buildx create --use --name $Name
        if ($LASTEXITCODE -ne 0) {
            exit $LASTEXITCODE
        }
    }
    else {
        & docker buildx use $Name
        if ($LASTEXITCODE -ne 0) {
            exit $LASTEXITCODE
        }
    }

    & docker buildx inspect --bootstrap
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

Write-Host ""
Write-Host "=== Build And Push Images ===" -ForegroundColor Cyan
Write-Host "Backend image: $backendRef"
Write-Host "Frontend image: $frontendRef"
Write-Host "Platform: $Platform"

Ensure-BuildxBuilder -Name $BuilderName

if (-not $SkipFrontendBuild) {
    Write-Host ""
    Write-Host "Building frontend dist..." -ForegroundColor Cyan
    Push-Location $frontendRoot
    try {
        & cmd /c npm run build
        if ($LASTEXITCODE -ne 0) {
            exit $LASTEXITCODE
        }
    }
    finally {
        Pop-Location
    }
}

Write-Host ""
Write-Host "Building and pushing backend image..." -ForegroundColor Cyan
& docker buildx build --platform $Platform -t $backendRef --push .
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "Building and pushing frontend image..." -ForegroundColor Cyan
& docker buildx build --platform $Platform -f frontend\Dockerfile.release -t $frontendRef --push .
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "Images pushed successfully." -ForegroundColor Green
