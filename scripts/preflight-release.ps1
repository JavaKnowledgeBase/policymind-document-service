param(
    [string]$BackendTests = "",
    [switch]$SkipBackendTests,
    [switch]$SkipFrontendBuild
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$testScript = Join-Path $PSScriptRoot "run-tests.ps1"

Write-Host ""
Write-Host "=== Local Release Preflight ===" -ForegroundColor Cyan
Write-Host "Repo: $repoRoot"

if (-not $SkipBackendTests) {
    $testArgs = @(
        "-ExecutionPolicy", "Bypass",
        "-File", $testScript
    )

    if ($BackendTests) {
        $testArgs += @("-Test", $BackendTests)
    }

    if (-not $SkipFrontendBuild) {
        $testArgs += "-FrontendBuild"
    }

    & powershell @testArgs
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
elseif (-not $SkipFrontendBuild) {
    Push-Location (Join-Path $repoRoot "frontend")
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
Write-Host "Local release preflight completed successfully." -ForegroundColor Green
