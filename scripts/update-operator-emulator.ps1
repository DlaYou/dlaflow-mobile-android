[CmdletBinding()]
param(
    [string] $OperatorAvdName = "DlaFlow_CallerId_API35",
    [string] $OperatorSerial
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$root = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $root
$pwsh = (Get-Command pwsh -ErrorAction SilentlyContinue).Source
if ([string]::IsNullOrWhiteSpace($pwsh)) {
    throw "PowerShell 7 (pwsh) is required for the manifest and installer helpers."
}

if ([string]::IsNullOrWhiteSpace($env:ANDROID_HOME) -and $env:LOCALAPPDATA) {
    $candidateSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path -LiteralPath $candidateSdk -PathType Container) {
        $env:ANDROID_HOME = $candidateSdk
        $env:ANDROID_SDK_ROOT = $candidateSdk
    }
}

if (-not (Test-Path -LiteralPath (Join-Path $root "gradlew.bat") -PathType Leaf)) {
    throw "Gradle wrapper was not found in the Android repository root."
}

Write-Host "Building the current Android checkout for the developer emulator..."
& (Join-Path $root "gradlew.bat") ":app:assembleDebug" "--no-daemon"
if ($LASTEXITCODE -ne 0) {
    throw "Debug APK build failed. The emulator was not changed."
}

$releaseDir = Join-Path $root "mobile-release\operator"
$manifestPath = Join-Path $releaseDir "latest.json"
$apkPath = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
$previousManifestPath = Join-Path ([System.IO.Path]::GetTempPath()) ("dlaflow-mobile-no-previous-" + [Guid]::NewGuid().ToString("N") + ".json")

& $pwsh -NoProfile -ExecutionPolicy Bypass -File (Join-Path $root "scripts\generate-mobile-release-manifest.ps1") `
    -ApkPath $apkPath `
    -OutputDir $releaseDir `
    -Channel "test" `
    -ReleaseTitle "Najnowszy build developerski" `
    -PreviousManifestPath $previousManifestPath
if ($LASTEXITCODE -ne 0) {
    throw "Test release manifest generation failed. The emulator was not changed."
}

$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$exactApkPath = Join-Path $releaseDir ([string] $manifest.fileName)
$installerArgs = @{
    ApkPath = $exactApkPath
    ManifestPath = $manifestPath
    OperatorAvdName = $OperatorAvdName
}
if (-not [string]::IsNullOrWhiteSpace($OperatorSerial)) { $installerArgs.OperatorSerial = $OperatorSerial }
& $pwsh -NoProfile -ExecutionPolicy Bypass -File (Join-Path $root "scripts\install-operator-apk.ps1") @installerArgs

Write-Host "Developer emulator now uses the newest verified build from this checkout."
