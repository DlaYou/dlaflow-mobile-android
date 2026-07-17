$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
$activityPath = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/MainActivity.kt"
$legacyScreenPath = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt"
$featureRoot = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/feature/dashboard"
$requiredFeatureFiles = @(
    "DashboardContract.kt",
    "DashboardCoordinator.kt",
    "DashboardFailure.kt",
    "DashboardGateway.kt",
    "DashboardMapper.kt",
    "DashboardScreen.kt",
    "DashboardStateHolder.kt"
)

foreach ($fileName in $requiredFeatureFiles) {
    $path = Join-Path $featureRoot $fileName
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required dashboard feature file is missing: $fileName"
    }
}

foreach ($path in @($activityPath, $legacyScreenPath)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required dashboard host file is missing: $path"
    }
}

$featureSource = (
    Get-ChildItem -LiteralPath $featureRoot -Recurse -File -Filter "*.kt" |
        ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw }
) -join [Environment]::NewLine
$activitySource = Get-Content -LiteralPath $activityPath -Raw
$legacyScreenSource = Get-Content -LiteralPath $legacyScreenPath -Raw

$forbiddenFeatureDependencies = @(
    "MainActivity",
    "MobileSessionStore(",
    "DlaFlowBackgroundSyncService",
    "IntentIntegrator",
    "startActivityForResult"
)
foreach ($forbidden in $forbiddenFeatureDependencies) {
    if ($featureSource.Contains($forbidden)) {
        throw "Dashboard feature crossed a forbidden dependency boundary: $forbidden"
    }
}

$forbiddenActivityOwnership = @(
    "private var assistantDashboard",
    "private fun refreshAssistantDashboard(",
    "MobileAssistantQuickAction"
)
foreach ($forbidden in $forbiddenActivityOwnership) {
    if ($activitySource.Contains($forbidden)) {
        throw "Dashboard ownership remains in MainActivity.kt: $forbidden"
    }
}

$forbiddenLegacyScreenSymbols = @(
    "private fun DashboardTab(",
    "private fun RevenueCard(",
    "private fun KpiGrid(",
    "private fun QuickActions("
)
foreach ($forbidden in $forbiddenLegacyScreenSymbols) {
    if ($legacyScreenSource.Contains($forbidden)) {
        throw "Dashboard UI remains in MobileAssistantScreen.kt: $forbidden"
    }
}

$requiredFeatureSymbols = @(
    "DashboardStateHolder",
    "DashboardCoordinator",
    "DashboardGateway",
    "DashboardFeatureScreen",
    "DlaFlowUiState",
    "activeRequestId",
    "contentOrNull"
)
foreach ($required in $requiredFeatureSymbols) {
    if (-not $featureSource.Contains($required)) {
        throw "Required dashboard feature symbol is missing: $required"
    }
}

Write-Host "Mobile dashboard feature boundary: OK"
