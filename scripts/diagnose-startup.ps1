param(
    [int]$Port = 8080,
    [string]$LogPath = "logs/policymind-document-service.log",
    [int]$TailLines = 250,
    [switch]$Watch,
    [int]$RefreshSeconds = 3
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

function Show-PortStatus {
    param([int]$PortNumber)

    Write-Section "Port $PortNumber"

    $listeners = Get-NetTCPConnection -LocalPort $PortNumber -State Listen -ErrorAction SilentlyContinue
    if (-not $listeners) {
        Write-Host "No listening process found on port $PortNumber."
        return
    }

    foreach ($listener in $listeners) {
        $process = Get-Process -Id $listener.OwningProcess -ErrorAction SilentlyContinue
        if ($process) {
            Write-Host ("PID {0} is listening on {1}. Process: {2}" -f $listener.OwningProcess, $PortNumber, $process.ProcessName)
        } else {
            Write-Host ("PID {0} is listening on {1}. Process name unavailable." -f $listener.OwningProcess, $PortNumber)
        }
    }
}

function Show-RecentLogSummary {
    param(
        [string]$Path,
        [int]$LineCount
    )

    Write-Section "Recent Log Summary"

    if (-not (Test-Path $Path)) {
        Write-Host "Log file not found: $Path"
        return
    }

    Write-Host "Log file: $Path"
    Get-Item $Path | Select-Object FullName, LastWriteTime, Length | Format-Table -AutoSize

    $recentLines = Get-Content $Path -Tail $LineCount

    Write-Host ""
    Write-Host ("Last {0} log lines containing WARN/ERROR/Exception/FAILED/timeout:" -f $LineCount)
    $matches = $recentLines | Select-String -Pattern "WARN|ERROR|Exception|FAILED|Failed|timed out|timeout|refused|already in use|starvation"

    if ($matches) {
        $matches | ForEach-Object { $_.Line }
    } else {
        Write-Host "No matching warning/error lines found in the recent log window."
    }
}

function Show-StartupMarkers {
    param([string]$Path)

    Write-Section "Startup Markers"

    if (-not (Test-Path $Path)) {
        Write-Host "Log file not found: $Path"
        return
    }

    $markers = Select-String -Path $Path -Pattern "Starting DocumentServiceApplication|Started DocumentServiceApplication|APPLICATION FAILED TO START|Port 8080 was already in use|Tomcat started on port|Thread starvation"
    if ($markers) {
        $markers | Select-Object -Last 20 | ForEach-Object { $_.Line }
    } else {
        Write-Host "No startup markers found."
    }
}

function Show-SurefireFailures {
    Write-Section "Surefire Failures"

    $reportDir = "target/surefire-reports"
    if (-not (Test-Path $reportDir)) {
        Write-Host "Surefire report directory not found: $reportDir"
        return
    }

    $matches = Select-String -Path "$reportDir/*.txt" -Pattern "<<< FAILURE!|Errors: [1-9]|Failures: [1-9]|Java 24|Could not modify all classes|BUILD FAILURE" -ErrorAction SilentlyContinue
    if ($matches) {
        $matches | ForEach-Object {
            Write-Host ("{0}:{1}: {2}" -f $_.Path, $_.LineNumber, $_.Line.Trim())
        }
    } else {
        Write-Host "No failing Surefire summaries found."
    }
}

function Get-ListeningSummary {
    param([int]$PortNumber)

    $listeners = Get-NetTCPConnection -LocalPort $PortNumber -State Listen -ErrorAction SilentlyContinue
    if (-not $listeners) {
        return "No listener on port $PortNumber."
    }

    $summaries = foreach ($listener in $listeners) {
        $process = Get-Process -Id $listener.OwningProcess -ErrorAction SilentlyContinue
        if ($process) {
            "PID $($listener.OwningProcess) ($($process.ProcessName))"
        } else {
            "PID $($listener.OwningProcess)"
        }
    }

    return ("Port {0} listeners: {1}" -f $PortNumber, ($summaries -join ", "))
}

function Start-WatchMode {
    param(
        [int]$PortNumber,
        [string]$Path,
        [int]$IntervalSeconds
    )

    Write-Section "Watch Mode"
    Write-Host "Watching port status and new matching log lines. Press Ctrl+C to stop."

    $lastSeenLineCount = 0
    if (Test-Path $Path) {
        $lastSeenLineCount = (Get-Content $Path).Count
    }

    while ($true) {
        Write-Host ""
        Write-Host ("[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), (Get-ListeningSummary -PortNumber $PortNumber))

        if (Test-Path $Path) {
            $allLines = Get-Content $Path
            $currentCount = $allLines.Count
            if ($currentCount -gt $lastSeenLineCount) {
                $newLines = $allLines[($lastSeenLineCount)..($currentCount - 1)]
                $matches = $newLines | Select-String -Pattern "WARN|ERROR|Exception|FAILED|Failed|timed out|timeout|refused|already in use|starvation|Starting DocumentServiceApplication|Started DocumentServiceApplication|Tomcat started on port"
                if ($matches) {
                    $matches | ForEach-Object { Write-Host $_.Line }
                }
                $lastSeenLineCount = $currentCount
            }
        } else {
            Write-Host "Log file not found yet: $Path"
        }

        Start-Sleep -Seconds $IntervalSeconds
    }
}

Show-PortStatus -PortNumber $Port
Show-RecentLogSummary -Path $LogPath -LineCount $TailLines
Show-StartupMarkers -Path $LogPath
Show-SurefireFailures

Write-Section "Tip"
Write-Host "Run this after a slow or failed startup to quickly spot port conflicts, recent backend errors, and test failures."

if ($Watch) {
    Start-WatchMode -PortNumber $Port -Path $LogPath -IntervalSeconds $RefreshSeconds
}
