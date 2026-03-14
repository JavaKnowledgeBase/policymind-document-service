param(
    [string]$RemoteHost = "ec2-user@3.128.197.215",
    [string]$KeyPath = "C:\Users\rkafl\Documents\Projects\policymind-key.pem",
    [string]$RepoPath = "/home/ec2-user/policymind-src",
    [string]$Branch = "main",
    [string]$ComposeFile = "docker-compose.aws.yml",
    [switch]$SkipGitSync,
    [switch]$FollowLogs,
    [int]$TailLines = 120
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

function Invoke-RemoteCommand {
    param([string]$RemoteCommand)

    & ssh -i $KeyPath $RemoteHost $RemoteCommand
    if ($LASTEXITCODE -ne 0) {
        throw "Remote command failed with exit code $LASTEXITCODE"
    }
}

Write-Section "EC2 Deploy"
Write-Host "Host: $RemoteHost"
Write-Host "Repo: $RepoPath"
Write-Host "Branch: $Branch"
Write-Host "Compose file: $ComposeFile"

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
docker-compose --env-file .env.production -f COMPOSE_FILE config 2>&1 | tee -a "$deploy_log"
docker-compose --env-file .env.production -f COMPOSE_FILE pull 2>&1 | tee -a "$deploy_log"
docker-compose --env-file .env.production -f COMPOSE_FILE up -d --remove-orphans 2>&1 | tee -a "$deploy_log"
docker-compose --env-file .env.production -f COMPOSE_FILE ps 2>&1 | tee -a "$deploy_log"
echo "DEPLOY_LOG=$deploy_log"
'@
$deployCommand = $deployCommand.Replace("REPO_PATH", $RepoPath).Replace("COMPOSE_FILE", $ComposeFile)
Invoke-RemoteCommand $deployCommand

if ($FollowLogs) {
    Write-Section "Follow Logs"
    $logCommand = @'
cd REPO_PATH
docker-compose --env-file .env.production -f COMPOSE_FILE logs --no-color --tail=TAIL_LINES -f
'@
    $logCommand = $logCommand.Replace("REPO_PATH", $RepoPath).Replace("COMPOSE_FILE", $ComposeFile).Replace("TAIL_LINES", [string]$TailLines)
    Invoke-RemoteCommand $logCommand
}
