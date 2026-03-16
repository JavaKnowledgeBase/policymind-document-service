param(
    [string]$RemoteHost = "ec2-user@3.128.197.215",
    [string]$KeyPath = "C:\Users\rkafl\Documents\Projects\policymind-key.pem",
    [string]$RepoPath = "/home/ec2-user/policymind-src",
    [string]$Branch = "main",
    [string]$ComposeFile = "docker-compose.aws.yml",
    [switch]$SkipGitSync,
    [switch]$SkipLocalPreflight,
    [string]$PreflightBackendTests = "",
    [switch]$FollowLogs,
    [int]$TailLines = 120,
    [int]$HealthRetries = 8,
    [int]$HealthDelaySeconds = 5
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$preflightScript = Join-Path $PSScriptRoot "preflight-release.ps1"
$sshPathCandidates = @(
    (Get-Command ssh.exe -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue),
    "C:\Windows\System32\OpenSSH\ssh.exe",
    "C:\Program Files\Git\usr\bin\ssh.exe",
    "C:\Program Files\Git\bin\ssh.exe"
) | Where-Object { $_ }
$sshOptions = @(
    "-o", "BatchMode=yes",
    "-o", "ConnectTimeout=15",
    "-o", "ServerAliveInterval=15",
    "-o", "ServerAliveCountMax=3"
)

$sshCommand = $sshPathCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $sshCommand) {
    throw "ssh.exe was not found. Install the Windows OpenSSH Client or add ssh.exe to PATH."
}

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

function Invoke-RemoteCommand {
    param([string]$RemoteCommand)

    & $sshCommand @sshOptions -i $KeyPath $RemoteHost $RemoteCommand
    if ($LASTEXITCODE -ne 0) {
        throw "Remote command failed with exit code $LASTEXITCODE"
    }
}

function Invoke-LocalPreflight {
    Write-Section "Local Preflight"
    $preflightArgs = @(
        "-ExecutionPolicy", "Bypass",
        "-File", $preflightScript
    )

    if ($PreflightBackendTests) {
        $preflightArgs += @("-BackendTests", $PreflightBackendTests)
    }

    & powershell @preflightArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Local preflight failed with exit code $LASTEXITCODE"
    }
}

Write-Section "EC2 Deploy"
Write-Host "Host: $RemoteHost"
Write-Host "Repo: $RepoPath"
Write-Host "Branch: $Branch"
Write-Host "Compose file: $ComposeFile"

if (-not $SkipLocalPreflight) {
    Invoke-LocalPreflight
}

if (-not $SkipGitSync) {
    Write-Section "Git Sync"
    $gitSyncCommand = @'
set -e
cd REPO_PATH
backup_dir=/home/ec2-user/policymind-backup-$(date +%Y%m%d-%H%M%S)
mkdir -p "$backup_dir"
if [ -f .env.production ]; then cp .env.production "$backup_dir/.env.production"; fi
if [ -d secrets ]; then cp -r secrets "$backup_dir/secrets"; fi
echo "Backup created at $backup_dir"
git fetch origin
git reset --hard origin/BRANCH_NAME
git clean -fd -e .env.production -e secrets/
git log --oneline -1
'@
    $gitSyncCommand = $gitSyncCommand.Replace("REPO_PATH", $RepoPath).Replace("BRANCH_NAME", $Branch)
    Invoke-RemoteCommand $gitSyncCommand
}

Write-Section "Build And Deploy"
$deployCommand = @'
set -e
cd REPO_PATH
deploy_log=/home/ec2-user/policymind-deploy-$(date +%Y%m%d-%H%M%S).log
echo "Deploy log: $deploy_log"
docker-compose --env-file .env.production -f COMPOSE_FILE pull 2>&1 | tee -a "$deploy_log"
docker-compose --env-file .env.production -f COMPOSE_FILE up -d --remove-orphans 2>&1 | tee -a "$deploy_log"
docker-compose --env-file .env.production -f COMPOSE_FILE ps 2>&1 | tee -a "$deploy_log"
echo "DEPLOY_LOG=$deploy_log"
'@
$deployCommand = $deployCommand.Replace("REPO_PATH", $RepoPath).Replace("COMPOSE_FILE", $ComposeFile)
Invoke-RemoteCommand $deployCommand

Write-Section "Remote Health Check"
$healthCommand = @'
set -e
cd REPO_PATH
for attempt in $(seq 1 HEALTH_RETRIES); do
  status=$(docker ps --filter "name=^/policymind_ai$" --format "{{.Status}}" | head -n 1)
  frontend=$(docker ps --filter "name=^/policymind_frontend$" --format "{{.Status}}" | head -n 1)
  if [ -z "$status" ]; then status="missing"; fi
  if [ -z "$frontend" ]; then frontend="missing"; fi
  echo "Attempt $attempt: backend=$status frontend=$frontend"
  if [[ "$status" == Up* ]] && [[ "$frontend" == Up* ]]; then
    exit 0
  fi
  sleep HEALTH_DELAY
done
echo "Remote containers did not report healthy/running state in time."
docker-compose --env-file .env.production -f COMPOSE_FILE ps
exit 1
'@
$healthCommand = $healthCommand.Replace("REPO_PATH", $RepoPath)
$healthCommand = $healthCommand.Replace("COMPOSE_FILE", $ComposeFile)
$healthCommand = $healthCommand.Replace("HEALTH_RETRIES", [string]$HealthRetries)
$healthCommand = $healthCommand.Replace("HEALTH_DELAY", [string]$HealthDelaySeconds)
Invoke-RemoteCommand $healthCommand

if ($FollowLogs) {
    Write-Section "Follow Logs"
    $logCommand = @'
cd REPO_PATH
docker-compose --env-file .env.production -f COMPOSE_FILE logs --no-color --tail=TAIL_LINES -f
'@
    $logCommand = $logCommand.Replace("REPO_PATH", $RepoPath).Replace("COMPOSE_FILE", $ComposeFile).Replace("TAIL_LINES", [string]$TailLines)
    Invoke-RemoteCommand $logCommand
}
