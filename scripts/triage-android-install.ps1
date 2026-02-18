param(
    [Parameter(Mandatory = $true)]
    [string]$ApkPath,

    [string]$AdbPath,

    [switch]$ApplyFix,

    [int[]]$UsersToCheck = @(0, 10),

    [string]$ReleasePackage = 'com.android.ai.mcp',

    [string]$DebugPackage = 'com.android.ai.mcp.debug'
)

$ErrorActionPreference = 'Stop'

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

function Find-Adb {
    param([string]$ExplicitPath)

    if ($ExplicitPath) {
        if (Test-Path $ExplicitPath) {
            return (Resolve-Path $ExplicitPath).Path
        }
        throw "ADB not found at explicit path: $ExplicitPath"
    }

    $adbCmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCmd) {
        return $adbCmd.Source
    }

    $repoRoot = Split-Path -Parent $PSScriptRoot
    $common = @(
        (Join-Path $repoRoot 'tools\platform-tools\adb.exe'),
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "C:\Android\platform-tools\adb.exe"
    )

    foreach ($candidate in $common) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    throw "adb not found. Install Android platform-tools or pass -AdbPath."
}

function Run-External {
    param(
        [Parameter(Mandatory = $true)][string]$Exe,
        [Parameter(Mandatory = $true)][string[]]$Args
    )

    $oldPreference = $ErrorActionPreference
    try {
        # adb writes daemon startup logs to stderr even when command succeeds.
        # Keep processing output and let callers decide based on command text.
        $ErrorActionPreference = 'SilentlyContinue'
        $output = & $Exe @Args 2>&1
    }
    finally {
        $ErrorActionPreference = $oldPreference
    }

    $text = ($output | Out-String).Trim()
    return $text
}

function Write-PackagePresenceForUser {
    param(
        [Parameter(Mandatory = $true)][string]$AdbExe,
        [Parameter(Mandatory = $true)][int]$UserId,
        [Parameter(Mandatory = $true)][string[]]$Packages
    )

    Write-Host "User ${UserId}:"
    $packagesOutput = Run-External -Exe $AdbExe -Args @('shell', 'pm', 'list', 'packages', '--user', "$UserId")
    $packageLines = $packagesOutput -split '\r?\n'
    $found = @()

    foreach ($pkg in $Packages) {
        $needle = "package:$pkg"
        if ($packageLines -contains $needle) {
            $found += $needle
        }
    }

    if ($found.Count -eq 0) {
        Write-Host '  (none)'
        return
    }

    $found | ForEach-Object { Write-Host "  $_" }
}

function Find-InstallErrorCode {
    param([string]$InstallOutput)

    if ($InstallOutput -match 'Failure \[(?<code>[^\]]+)\]') {
        return $Matches.code
    }

    if ($InstallOutput -match '(INSTALL_FAILED_[A-Z_]+|INSTALL_PARSE_FAILED_[A-Z_]+)') {
        return $Matches[1]
    }

    return $null
}

function Find-Apksigner {
    $sdkRoot = $env:ANDROID_SDK_ROOT
    if (-not $sdkRoot) {
        $sdkRoot = $env:ANDROID_HOME
    }

    if (-not $sdkRoot) {
        $default = "$env:LOCALAPPDATA\Android\Sdk"
        if (Test-Path $default) {
            $sdkRoot = $default
        }
    }

    if (-not $sdkRoot) {
        return $null
    }

    $buildToolsRoot = Join-Path $sdkRoot 'build-tools'
    if (-not (Test-Path $buildToolsRoot)) {
        return $null
    }

    $candidate = Get-ChildItem -Path $buildToolsRoot -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        ForEach-Object {
            $tool = Join-Path $_.FullName 'apksigner.bat'
            if (Test-Path $tool) { $tool }
        } |
        Select-Object -First 1

    return $candidate
}

if (-not (Test-Path $ApkPath)) {
    throw "APK not found: $ApkPath"
}

if ([IO.Path]::GetExtension($ApkPath).ToLowerInvariant() -ne '.apk') {
    throw "Expected an .apk file path. Got: $ApkPath"
}

$adb = Find-Adb -ExplicitPath $AdbPath
$resolvedApk = (Resolve-Path $ApkPath).Path

Write-Section 'Environment'
Write-Host "ADB: $adb"
Write-Host "APK: $resolvedApk"

Write-Section 'ADB status'
Write-Host (Run-External -Exe $adb -Args @('version'))
Write-Host (Run-External -Exe $adb -Args @('devices', '-l'))

Write-Section 'Pre-install package visibility'
foreach ($userId in $UsersToCheck) {
    Write-PackagePresenceForUser -AdbExe $adb -UserId $userId -Packages @($ReleasePackage, $DebugPackage)
}

Write-Section 'Primary install attempt'
$installOutput = Run-External -Exe $adb -Args @('install', '-r', $resolvedApk)
Write-Host $installOutput

if ($installOutput -match '^Success' -or $installOutput -match '\bSuccess\b') {
    Write-Section 'Install result'
    Write-Host 'Install succeeded.'
    $packageDump = Run-External -Exe $adb -Args @('shell', 'dumpsys', 'package', $ReleasePackage)
    $packageDump -split '\r?\n' |
        Where-Object { $_ -match 'versionCode=|versionName=' } |
        ForEach-Object { Write-Host $_ }
    exit 0
}

$errorCode = Find-InstallErrorCode -InstallOutput $installOutput
if (-not $errorCode) {
    Write-Section 'Verbose install attempt'
    $verboseOutput = Run-External -Exe $adb -Args @('install', '-r', '-d', '-g', $resolvedApk)
    Write-Host $verboseOutput
    $errorCode = Find-InstallErrorCode -InstallOutput $verboseOutput
}

Write-Section 'Detected error code'
if ($errorCode) {
    Write-Host $errorCode
} else {
    Write-Host 'Could not determine INSTALL_FAILED_* code. Check full output above.'
    exit 1
}

switch ($errorCode) {
    'INSTALL_FAILED_UPDATE_INCOMPATIBLE' {
        Write-Section 'Remediation'
        Write-Host 'Signature mismatch conflict. Check all users/profiles and uninstall old copies.'

        $usersOutput = Run-External -Exe $adb -Args @('shell', 'pm', 'list', 'users')
        Write-Host $usersOutput

        foreach ($userId in $UsersToCheck) {
            Write-PackagePresenceForUser -AdbExe $adb -UserId $userId -Packages @($ReleasePackage, $DebugPackage)
        }

        if ($ApplyFix) {
            Write-Host 'ApplyFix enabled: uninstalling conflicting packages for configured users.'
            foreach ($userId in $UsersToCheck) {
                Write-Host (Run-External -Exe $adb -Args @('shell', 'pm', 'uninstall', '--user', "$userId", $ReleasePackage))
                Write-Host (Run-External -Exe $adb -Args @('shell', 'pm', 'uninstall', '--user', "$userId", $DebugPackage))
            }

            Write-Section 'Reinstall after fix'
            $retry = Run-External -Exe $adb -Args @('install', '-r', $resolvedApk)
            Write-Host $retry
        }
    }

    'INSTALL_FAILED_VERSION_DOWNGRADE' {
        Write-Section 'Remediation'
        Write-Host 'Installed app has a higher versionCode than this APK.'
        Write-Host 'Use latest workflow artifact or uninstall release package then reinstall.'
        if ($ApplyFix) {
            Write-Host (Run-External -Exe $adb -Args @('uninstall', $ReleasePackage))
            Write-Host (Run-External -Exe $adb -Args @('install', '-r', $resolvedApk))
        }
    }

    'INSTALL_PARSE_FAILED_NO_CERTIFICATES' {
        Write-Section 'Remediation'
        Write-Host 'APK appears corrupt/invalid signature payload. Re-download artifact and verify.'

        $apksigner = Find-Apksigner
        if ($apksigner) {
            Write-Section 'apksigner verify'
            Write-Host (Run-External -Exe $apksigner -Args @('verify', '--print-certs', $resolvedApk))
        } else {
            Write-Host 'apksigner not found on this machine.'
        }
    }

    'INSTALL_FAILED_USER_RESTRICTED' {
        Write-Section 'Remediation'
        Write-Host 'Enable Install unknown apps for the installer app on device (Files/Chrome/My Files), then retry.'
    }

    default {
        Write-Section 'Remediation'
        Write-Host 'Unhandled error code. Use the printed code and full adb output for targeted fix.'
    }
}

Write-Section 'Post-check package state'
foreach ($userId in $UsersToCheck) {
    Write-PackagePresenceForUser -AdbExe $adb -UserId $userId -Packages @($ReleasePackage, $DebugPackage)
}
