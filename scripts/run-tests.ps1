param(
    [string]$Test = "",
    [switch]$SkipFrontend,
    [switch]$FrontendBuild
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$mavenRepo = Join-Path $repoRoot ".m2\repository"
$pdfBoxCache = Join-Path $repoRoot ".pdfbox-cache"
$frontendRoot = Join-Path $repoRoot "frontend"

function Resolve-MavenCommand {
    $globalMaven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($globalMaven) {
        return $globalMaven.Source
    }

    $repoMavenCandidates = @(
        (Join-Path $repoRoot ".tools\apache-maven-3.9.14\bin\mvn.cmd"),
        (Join-Path $repoRoot ".tools\apache-maven-3.9.9\bin\mvn.cmd")
    )

    foreach ($candidate in $repoMavenCandidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    throw "Maven not found. Install Maven on PATH or place it under .tools\\apache-maven-3.9.14 or .tools\\apache-maven-3.9.9."
}

function Invoke-FrontendBuild {
    param([string]$FrontendPath)

    if (-not (Test-Path (Join-Path $FrontendPath "package.json"))) {
        Write-Host "Frontend package.json not found. Skipping frontend build." -ForegroundColor Yellow
        return
    }

    Write-Host ""
    Write-Host "Running frontend production build..." -ForegroundColor Cyan
    Push-Location $FrontendPath
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

function ConvertTo-CmdArgument {
    param([string]$Value)

    if ($null -eq $Value) {
        return '""'
    }

    if ($Value -notmatch '[\s"]') {
        return $Value
    }

    return '"' + ($Value -replace '"', '""') + '"'
}

$mavenCmd = Resolve-MavenCommand

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

$mavenStderrPath = Join-Path $repoRoot (".tmp_maven_stderr_{0}.log" -f ([guid]::NewGuid().ToString("N")))
$mavenArgLine = (($mavenArgs | ForEach-Object { ConvertTo-CmdArgument $_ }) -join " ")
$mavenCmdLine = '"' + $mavenCmd + '" ' + $mavenArgLine + ' 2> "' + $mavenStderrPath + '"'

try {
    & cmd /d /c $mavenCmdLine
    $mavenExitCode = $LASTEXITCODE

    $mavenStderr = ""
    if (Test-Path $mavenStderrPath) {
        $mavenStderr = (Get-Content $mavenStderrPath -Raw).Trim()
    }

    if ($mavenExitCode -ne 0) {
        if ($mavenStderr) {
            Write-Host $mavenStderr
        }
        exit $mavenExitCode
    }

    if ($mavenStderr -and $mavenStderr -ne "Access is denied.") {
        Write-Host $mavenStderr -ForegroundColor Yellow
    }
}
finally {
    Remove-Item $mavenStderrPath -ErrorAction SilentlyContinue
}

if (-not $SkipFrontend -and $FrontendBuild) {
    Invoke-FrontendBuild -FrontendPath $frontendRoot
}
elseif (-not $SkipFrontend -and (Test-Path (Join-Path $repoRoot "frontend\package.json"))) {
    Write-Host ""
    Write-Host "Frontend tests are not configured in this helper yet." -ForegroundColor Yellow
    Write-Host "Use -FrontendBuild to run a local production build as a preflight check." -ForegroundColor Yellow
    Write-Host "Backend Maven tests completed successfully." -ForegroundColor Green
}
