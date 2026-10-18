[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)] [string] $ApkPath,
    [Parameter(Mandatory = $true)] [string] $ManifestPath,
    [string] $OperatorAvdName = "DlaFlow_CallerId_API35",
    [string] $OperatorSerial
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$root = Split-Path -Parent $PSScriptRoot

function Resolve-ProjectPath([string] $value) {
    if ([System.IO.Path]::IsPathRooted($value)) { return [System.IO.Path]::GetFullPath($value) }
    return [System.IO.Path]::GetFullPath((Join-Path $root $value))
}

function Resolve-SdkRoot {
    foreach ($candidate in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path -LiteralPath $candidate -PathType Container)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    $localProperties = Join-Path $root "local.properties"
    if (Test-Path -LiteralPath $localProperties -PathType Leaf) {
        $line = Get-Content $localProperties | Where-Object { $_ -like "sdk.dir=*" } | Select-Object -First 1
        if ($line) {
            $decoded = $line.Substring(8).Replace('\:', ':').Replace('\\', '\')
            if (Test-Path -LiteralPath $decoded -PathType Container) { return (Resolve-Path $decoded).Path }
        }
    }
    throw "Android SDK was not found. Set ANDROID_HOME or keep sdk.dir in local.properties."
}

function Invoke-Checked([string] $path, [string[]] $arguments) {
    $output = & $path @arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Command failed: $path $($arguments -join ' ')" }
    return @($output | ForEach-Object { [string] $_ })
}

function Read-Manifest([string] $path) {
    $manifest = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
    foreach ($key in @("channel", "fileName", "sha256", "sizeBytes", "versionCode", "versionName")) {
        if ($null -eq $manifest.$key) { throw "Release manifest is missing '$key'." }
    }
    if ($manifest.channel -notin @("test", "production")) { throw "Unsupported release channel '$($manifest.channel)'." }
    if ($manifest.fileName -notmatch '^[A-Za-z0-9._-]+\.apk$') { throw "Release manifest filename is invalid." }
    if ($manifest.sha256 -notmatch '^[a-f0-9]{64}$') { throw "Release manifest SHA-256 is invalid." }
    return $manifest
}

function Read-ApkSummary([string] $analyzer, [string] $apk) {
    $line = Invoke-Checked $analyzer @("apk", "summary", $apk) | Where-Object { $_.Trim() } | Select-Object -First 1
    $parts = ([string] $line).Trim() -split "\s+"
    if ($parts.Count -ne 3) { throw "Could not parse APK summary: $line" }
    return [pscustomobject] @{ PackageName = $parts[0]; VersionCode = [int] $parts[1]; VersionName = $parts[2] }
}

function Resolve-Operator([string] $adb) {
    $running = @()
    foreach ($line in (Invoke-Checked $adb @("devices"))) {
        if ($line -notmatch '^(emulator-\d+)\s+device\b') { continue }
        $serial = $Matches[1]
        $name = Invoke-Checked $adb @("-s", $serial, "emu", "avd", "name") |
            Where-Object { $_.Trim() -and $_.Trim() -ne "OK" } | Select-Object -First 1
        if ($name) { $running += [pscustomobject] @{ Name = $name.Trim(); Serial = $serial } }
    }
    if ($OperatorSerial) {
        $selected = @($running | Where-Object { $_.Serial -eq $OperatorSerial })
        if ($selected.Count -ne 1 -or $selected[0].Name -ne $OperatorAvdName) { throw "Operator serial/AVD identity is not verified." }
        return $selected[0]
    }
    $matches = @($running | Where-Object { $_.Name -eq $OperatorAvdName })
    if ($matches.Count -ne 1) { throw "Expected exactly one running Operator AVD '$OperatorAvdName'; found $($matches.Count)." }
    return $matches[0]
}

$apk = Resolve-ProjectPath $ApkPath
$manifestPath = Resolve-ProjectPath $ManifestPath
if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) { throw "APK is missing: $apk" }
if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) { throw "Release manifest is missing: $manifestPath" }
$manifest = Read-Manifest $manifestPath
$item = Get-Item -LiteralPath $apk
if ($item.Name -ne $manifest.fileName -or $item.Length -ne [int64] $manifest.sizeBytes) { throw "APK filename or size does not match the release manifest." }
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $apk).Hash.ToLowerInvariant()
if ($hash -ne $manifest.sha256) { throw "APK SHA-256 does not match the release manifest." }

$sdk = Resolve-SdkRoot
$adb = Join-Path $sdk "platform-tools/adb.exe"
$analyzer = Join-Path $sdk "cmdline-tools/latest/bin/apkanalyzer.bat"
if (-not (Test-Path -LiteralPath $adb -PathType Leaf)) { throw "ADB is missing: $adb" }
if (-not (Test-Path -LiteralPath $analyzer -PathType Leaf)) { throw "apkanalyzer is missing: $analyzer" }
$summary = Read-ApkSummary $analyzer $apk
if ($summary.PackageName -ne "pl.dlaflow.mobile" -or $summary.VersionCode -ne [int] $manifest.versionCode -or $summary.VersionName -ne [string] $manifest.versionName) { throw "APK package/version does not match the release manifest." }
$operator = Resolve-Operator $adb
Write-Host "Installing $($manifest.fileName) ($($manifest.versionName), $($manifest.versionCode)) on $($operator.Name) [$($operator.Serial)]"
& $adb "-s" $operator.Serial "install" "-r" $apk
if ($LASTEXITCODE -ne 0) { throw "Operator APK update failed; existing application data was left untouched." }
$installed = (Invoke-Checked $adb @("-s", $operator.Serial, "shell", "dumpsys", "package", "pl.dlaflow.mobile")) -join "`n"
if ($installed -notmatch "versionCode=$([regex]::Escape([string] $manifest.versionCode))\b" -or $installed -notmatch "versionName=$([regex]::Escape([string] $manifest.versionName))\b") { throw "Installed package metadata does not match the release manifest." }
Write-Host "Operator APK updated with application data and session preserved."
Write-Host "Verified version: $($manifest.versionName) ($($manifest.versionCode)); SHA-256: $hash"
