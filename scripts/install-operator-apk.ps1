param(
    [string] $ApkPath = "app/build/outputs/apk/debug/app-debug.apk",
    [string] $OperatorAvdName = "DlaFlow_Task6_Dashboard_API35_20260717"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot

function Resolve-AndroidSdkRoot {
    foreach ($candidate in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path -LiteralPath $candidate -PathType Container)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    $localProperties = Join-Path $root "local.properties"
    if (Test-Path -LiteralPath $localProperties -PathType Leaf) {
        $sdkLine = Get-Content -LiteralPath $localProperties |
            Where-Object { $_ -like "sdk.dir=*" } |
            Select-Object -First 1
        if ($sdkLine) {
            $decoded = $sdkLine.Substring("sdk.dir=".Length).Replace('\:', ':').Replace('\\', '\')
            if (Test-Path -LiteralPath $decoded -PathType Container) {
                return (Resolve-Path -LiteralPath $decoded).Path
            }
        }
    }

    throw "Android SDK was not found. Set ANDROID_HOME or keep sdk.dir in local.properties."
}

$sdkRoot = Resolve-AndroidSdkRoot
$adbPath = Join-Path $sdkRoot "platform-tools/adb.exe"
if (-not (Test-Path -LiteralPath $adbPath -PathType Leaf)) {
    throw "ADB is missing: $adbPath"
}

$runningAvds = @()
foreach ($line in (& $adbPath "devices")) {
    if ($line -notmatch '^(emulator-\d+)\s+device\b') {
        continue
    }

    $serial = $Matches[1]
    $nameLine = & $adbPath "-s" $serial "emu" "avd" "name" |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and $_.Trim() -ne "OK" } |
        Select-Object -First 1
    if ($null -eq $nameLine) {
        continue
    }

    $name = $nameLine.Trim()
    if (-not [string]::IsNullOrWhiteSpace($name)) {
        $runningAvds += [pscustomobject]@{ Name = $name; Serial = $serial }
    }
}

$matches = @($runningAvds | Where-Object { $_.Name -eq $OperatorAvdName })
if ($matches.Count -ne 1) {
    throw "Expected exactly one running Operator AVD '$OperatorAvdName'; found $($matches.Count)."
}

$resolvedApkPath = if ([System.IO.Path]::IsPathRooted($ApkPath)) {
    $ApkPath
} else {
    Join-Path $root $ApkPath
}
if (-not (Test-Path -LiteralPath $resolvedApkPath -PathType Leaf)) {
    throw "APK is missing: $resolvedApkPath"
}
if ((Get-Item -LiteralPath $resolvedApkPath).Length -le 0) {
    throw "APK is empty: $resolvedApkPath"
}

$installArgs = @("-s", $matches[0].Serial, "install", "-r", $resolvedApkPath)
& $adbPath @installArgs
if ($LASTEXITCODE -ne 0) {
    throw "Operator APK update failed; existing application data was left untouched."
}

Write-Host "Operator APK updated with application data preserved."
