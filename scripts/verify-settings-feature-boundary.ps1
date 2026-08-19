$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$root = Split-Path -Parent $PSScriptRoot
$feature = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/feature/settings"
$hostSource = Get-Content (Join-Path $root "app/src/main/java/pl/dlaflow/mobile/MainActivity.kt") -Raw
$screen = Get-Content (Join-Path $root "app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt") -Raw
$featureSource = (Get-ChildItem $feature -Filter *.kt -File | ForEach-Object { Get-Content $_.FullName -Raw }) -join "`n"
foreach ($file in @("SettingsContract.kt","SettingsCoordinator.kt","SettingsStateHolder.kt","SettingsScreen.kt","SettingsIntentLaunchPolicy.kt")) {
    if (-not (Test-Path (Join-Path $feature $file))) { throw "Missing settings feature file: $file" }
}
foreach ($forbidden in @("AndroidKeyStore","MobileApiClient(","Throwable")) {
    if ($featureSource.Contains($forbidden)) { throw "Settings feature crossed forbidden boundary: $forbidden" }
}
if (-not $screen.Contains("SettingsFeatureScreen(")) { throw "Settings screen is not wired" }
if ($screen.Contains("MoreTab(") -or $screen.Contains("MobileMoreSettings")) { throw "Legacy settings system remains" }
if (-not $hostSource.Contains("launchFirstResolvedSettingsTarget(")) { throw "Settings launch policy is not used" }
Write-Host "Mobile settings feature boundary: OK"
