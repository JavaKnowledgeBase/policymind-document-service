param(
    [string]$Test = "",
    [switch]$SkipFrontend
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$mavenHome = Join-Path $repoRoot ".tools\apache-maven-3.9.9"
$mavenCmd = Join-Path $mavenHome "bin\mvn.cmd"
$mavenRepo = Join-Path $repoRoot ".m2\repository"
$pdfBoxCache = Join-Path $repoRoot ".pdfbox-cache"

if (-not (Test-Path $mavenCmd)) {
    throw "Local Maven not found at '$mavenCmd'. Download Apache Maven 3.9.9 into .tools before running tests."
}

New-Item -ItemType Directory -Force -Path $mavenRepo | Out-Null
New-Item -ItemType Directory -Force -Path $pdfBoxCache | Out-Null

$mavenArgs = @(
    "-Dmaven.repo.local=$mavenRepo"
    "-Dpdfbox.fontcache=$pdfBoxCache"
)

if ($Test) {
    # Keep focused runs easy when we only want a small subset while iterating locally.
    $mavenArgs += "-Dtest=$Test"
}

$mavenArgs += "test"

Write-Host "Running backend tests with repo-local Maven cache..." -ForegroundColor Cyan
Write-Host "Maven: $mavenCmd"
Write-Host "Repo cache: $mavenRepo"
Write-Host "PDFBox cache: $pdfBoxCache"

& $mavenCmd @mavenArgs
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if (-not $SkipFrontend -and (Test-Path (Join-Path $repoRoot "frontend\package.json"))) {
    Write-Host ""
    Write-Host "Frontend tests are not configured in this helper yet." -ForegroundColor Yellow
    Write-Host "Backend Maven tests completed successfully." -ForegroundColor Green
}
