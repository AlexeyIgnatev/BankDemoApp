[CmdletBinding()]
param(
    [string]$PackageName = "com.esom.bank",
    [string]$ActivityComponent = "com.esom.bank/.activities.MainActivity",
    [string]$Serial = "",
    [string]$ReportDir = "build/reports/long-idle-crash-check",
    [int]$MonkeyEvents = 1000,
    [int]$MonkeyThrottleMs = 150,
    [int]$DozeSeconds = 60,
    [switch]$SkipDontKeepActivities,
    [switch]$SkipDoze,
    [switch]$SkipMonkey,
    [switch]$NoInteractivePause
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$ReportPath = Join-Path $RepoRoot $ReportDir
$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$RuntimeLogPath = Join-Path $ReportPath "android-runtime-$Timestamp.log"
$SummaryPath = Join-Path $ReportPath "summary-$Timestamp.txt"
$script:OriginalAlwaysFinishActivities = $null

function Write-Step {
    param([string]$Message)

    $line = "[$(Get-Date -Format "HH:mm:ss")] $Message"
    Write-Host $line
    Add-Content -Path $SummaryPath -Value $line
}

function ConvertTo-CommandLineArgument {
    param([string]$Argument)

    if ($Argument -match '^[A-Za-z0-9_./:=+*,@-]+$') {
        return $Argument
    }

    return '"' + $Argument.Replace('\', '\\').Replace('"', '\"') + '"'
}

function Invoke-Adb {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$AdbArgs,
        [switch]$AllowFailure
    )

    $fullArgs = @()
    if (-not [string]::IsNullOrWhiteSpace($Serial)) {
        $fullArgs += @("-s", $Serial)
    }
    $fullArgs += $AdbArgs

    Write-Host "adb $($fullArgs -join ' ')"
    $stdoutPath = Join-Path $ReportPath "adb-stdout-$([guid]::NewGuid()).tmp"
    $stderrPath = Join-Path $ReportPath "adb-stderr-$([guid]::NewGuid()).tmp"
    $argumentList = ($fullArgs | ForEach-Object { ConvertTo-CommandLineArgument $_ }) -join " "

    $process = Start-Process `
        -FilePath "adb" `
        -ArgumentList $argumentList `
        -NoNewWindow `
        -Wait `
        -PassThru `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath

    $exitCode = $process.ExitCode
    $stdout = if (Test-Path $stdoutPath) { Get-Content -LiteralPath $stdoutPath } else { @() }
    $stderr = if (Test-Path $stderrPath) { Get-Content -LiteralPath $stderrPath } else { @() }
    $output = @($stdout) + @($stderr)

    Remove-Item -LiteralPath $stdoutPath, $stderrPath -Force -ErrorAction SilentlyContinue

    if ($output) {
        $output | ForEach-Object { Write-Host $_ }
    }

    if ($exitCode -ne 0 -and -not $AllowFailure) {
        throw "adb command failed with exit code ${exitCode}: adb $($fullArgs -join ' ')"
    }

    [PSCustomObject]@{
        ExitCode = $exitCode
        Output = ($output -join [Environment]::NewLine)
    }
}

function Assert-DeviceConnected {
    if (-not [string]::IsNullOrWhiteSpace($Serial)) {
        $state = (Invoke-Adb -AdbArgs @("get-state") -AllowFailure).Output.Trim()
        if ($state -ne "device") {
            throw "ADB device '$Serial' is not online. Current state: '$state'."
        }
        return
    }

    $output = & adb devices 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "adb devices failed: $($output -join [Environment]::NewLine)"
    }

    $onlineDevices = @(
        $output |
            Where-Object { $_ -match "\sdevice$" } |
            ForEach-Object { ($_ -split "\s+")[0] }
    )

    if ($onlineDevices.Count -eq 0) {
        throw "No online adb device found. Connect a device/emulator or pass -Serial."
    }

    if ($onlineDevices.Count -gt 1) {
        throw "Multiple adb devices found: $($onlineDevices -join ', '). Pass -Serial <device-id>."
    }
}

function Wait-ForManualCheckpoint {
    param([string]$Message)

    Write-Step $Message
    if (-not $NoInteractivePause) {
        Read-Host "Press Enter after you finish this checkpoint" | Out-Null
    }
}

function Start-App {
    Write-Step "Starting app: $ActivityComponent"
    Invoke-Adb -AdbArgs @("shell", "am", "start", "-W", "-n", $ActivityComponent) | Out-Null
    Start-Sleep -Seconds 3
}

function Press-Home {
    Write-Step "Sending app to background"
    Invoke-Adb -AdbArgs @("shell", "input", "keyevent", "HOME") | Out-Null
    Start-Sleep -Seconds 2
}

function Capture-RuntimeLog {
    Write-Step "Capturing AndroidRuntime log to $RuntimeLogPath"
    $result = Invoke-Adb -AdbArgs @("logcat", "-d", "AndroidRuntime:E", "*:S") -AllowFailure
    Set-Content -Path $RuntimeLogPath -Value $result.Output -Encoding UTF8
}

function Restore-AlwaysFinishActivities {
    if ($null -eq $script:OriginalAlwaysFinishActivities) {
        return
    }

    $value = $script:OriginalAlwaysFinishActivities.Trim()
    if ([string]::IsNullOrWhiteSpace($value) -or $value -eq "null") {
        Invoke-Adb -AdbArgs @("shell", "settings", "delete", "global", "always_finish_activities") -AllowFailure | Out-Null
    } else {
        Invoke-Adb -AdbArgs @("shell", "settings", "put", "global", "always_finish_activities", $value) -AllowFailure | Out-Null
    }
}

function Test-ProcessDeathScenario {
    Write-Step "Scenario 1: process death after background"
    Start-App
    Wait-ForManualCheckpoint "Manual checkpoint: log in if needed and navigate to the main screen."
    Press-Home
    Invoke-Adb -AdbArgs @("shell", "am", "kill", $PackageName) | Out-Null
    Start-Sleep -Seconds 2
    Start-App
    Wait-ForManualCheckpoint "Manual checkpoint: open Wallet, History, Settings, and Chat. Verify no crash or stuck loader."
}

function Test-DontKeepActivitiesScenario {
    if ($SkipDontKeepActivities) {
        Write-Step "Scenario 2 skipped: Don't keep activities"
        return
    }

    Write-Step "Scenario 2: Don't keep activities"
    $script:OriginalAlwaysFinishActivities = (Invoke-Adb -AdbArgs @("shell", "settings", "get", "global", "always_finish_activities") -AllowFailure).Output
    Invoke-Adb -AdbArgs @("shell", "settings", "put", "global", "always_finish_activities", "1") | Out-Null

    Start-App
    Wait-ForManualCheckpoint "Manual checkpoint: visit Wallet, History, Settings, Chat. Background and return from each tab if possible."
    Press-Home
    Start-App
    Wait-ForManualCheckpoint "Manual checkpoint: verify the restored screen still works."
}

function Test-DozeScenario {
    if ($SkipDoze) {
        Write-Step "Scenario 3 skipped: Doze"
        return
    }

    Write-Step "Scenario 3: Doze/background restrictions"
    Start-App
    Press-Home
    Invoke-Adb -AdbArgs @("shell", "dumpsys", "deviceidle", "force-idle") -AllowFailure | Out-Null
    Write-Step "Device is in forced idle for $DozeSeconds seconds"
    Start-Sleep -Seconds $DozeSeconds
    Invoke-Adb -AdbArgs @("shell", "dumpsys", "deviceidle", "unforce") -AllowFailure | Out-Null
    Start-App
    Wait-ForManualCheckpoint "Manual checkpoint: verify tabs, Chat polling, and data-only push if you can send one."
}

function Test-MonkeyScenario {
    if ($SkipMonkey) {
        Write-Step "Scenario 4 skipped: monkey"
        return
    }

    Write-Step "Scenario 4: monkey stress smoke test"
    Start-App
    Invoke-Adb -AdbArgs @(
        "shell",
        "monkey",
        "-p",
        $PackageName,
        "--throttle",
        "$MonkeyThrottleMs",
        "-v",
        "$MonkeyEvents"
    ) -AllowFailure | Out-Null
}

function Assert-NoAndroidRuntimeCrash {
    Capture-RuntimeLog

    $hasCrash = $false
    if ((Test-Path $RuntimeLogPath) -and ((Get-Item $RuntimeLogPath).Length -gt 0)) {
        $hasCrash = $true
    }

    if ($hasCrash) {
        Write-Step "FAILED: AndroidRuntime output was found. Inspect $RuntimeLogPath"
        exit 1
    }

    Write-Step "PASSED: no AndroidRuntime crash output found."
}

New-Item -ItemType Directory -Force -Path $ReportPath | Out-Null
Set-Content -Path $SummaryPath -Value "Long idle crash check report: $Timestamp" -Encoding UTF8

try {
    if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
        throw "adb was not found in PATH."
    }

    Write-Step "Checking connected device"
    Assert-DeviceConnected
    Invoke-Adb -AdbArgs @("devices") | Out-Null
    Write-Step "Clearing logcat"
    Invoke-Adb -AdbArgs @("logcat", "-c") | Out-Null

    Test-ProcessDeathScenario
    Test-DontKeepActivitiesScenario
    Test-DozeScenario
    Test-MonkeyScenario
    Assert-NoAndroidRuntimeCrash
} finally {
    Write-Step "Restoring device settings"
    Restore-AlwaysFinishActivities
    Invoke-Adb -AdbArgs @("shell", "dumpsys", "deviceidle", "unforce") -AllowFailure | Out-Null
    Write-Step "Report: $SummaryPath"
}
