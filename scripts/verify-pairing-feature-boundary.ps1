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

Write-Host "Mobile pairing feature boundary: OK"
