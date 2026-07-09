$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
$screenPath = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt"
$pairingScreenPath = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/feature/pairing/PairingScreen.kt"
$pairingHelpPath = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/feature/pairing/PairingHelpScreen.kt"

foreach ($path in @($pairingScreenPath, $pairingHelpPath)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required pairing feature file is missing: $path"
    }
}

$screen = Get-Content -LiteralPath $screenPath -Raw
foreach ($forbidden in @(
    "private fun PairingScreen(",
    "private fun PairingHelpScreen(",
    "private fun PairingQrCard(",
    "private fun PairingCodeBoxes(",
    "private fun normalizePairingCodeInput(",
    "private fun formatPairingCodeInput("
)) {
    if ($screen.Contains($forbidden)) {
        throw "Pairing UI remains in MobileAssistantScreen.kt: $forbidden"
    }
}

$activityPath = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/MainActivity.kt"
$featureRoot = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/feature/pairing"
$activity = Get-Content -LiteralPath $activityPath -Raw

foreach ($forbidden in @(
    'completePairing(code, "Telefon DlaFlow")',
    "private fun pairDevice(",
    "private fun extractPairingCodeFromQr(",
    "private fun renderPairingCard("
)) {
    if ($activity.Contains($forbidden)) {
        throw "Pairing ownership remains in MainActivity.kt: $forbidden"
    }
}

$featureSource = (Get-ChildItem -LiteralPath $featureRoot -Filter "*.kt" | Get-Content -Raw) -join [Environment]::NewLine
foreach ($forbidden in @("MobileApiClient(", "MobileSessionStore(", "AndroidKeyStore", "DlaFlowBackgroundSyncService.start")) {
    if ($featureSource.Contains($forbidden)) {
        throw "Pairing feature crossed a forbidden dependency boundary: $forbidden"
    }
}

Write-Host "Mobile pairing feature boundary: OK"
