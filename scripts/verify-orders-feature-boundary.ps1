$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
$activityPath = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/MainActivity.kt"
$legacyScreenPath = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt"
$mobileApiPath = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/mobile_api.kt"
$featureRoot = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/feature/orders"
$requiredFeatureFiles = @(
    "OrdersContract.kt",
    "OrdersCoordinator.kt",
    "OrdersFailure.kt",
    "OrdersGateway.kt",
    "OrdersMapper.kt",
    "OrdersScreen.kt",
    "OrderDetailPanel.kt",
    "OrdersStateHolder.kt"
)

foreach ($fileName in $requiredFeatureFiles) {
    $path = Join-Path $featureRoot $fileName
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required orders feature file is missing: $fileName"
    }
}

foreach ($path in @($activityPath, $legacyScreenPath, $mobileApiPath)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required orders host file is missing: $path"
    }
}

$featureFiles = Get-ChildItem -LiteralPath $featureRoot -Recurse -File -Filter "*.kt"
$featureSource = ($featureFiles | ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw }) -join [Environment]::NewLine
$coordinatorSource = Get-Content -LiteralPath (Join-Path $featureRoot "OrdersCoordinator.kt") -Raw
$gatewaySource = Get-Content -LiteralPath (Join-Path $featureRoot "OrdersGateway.kt") -Raw
$activitySource = Get-Content -LiteralPath $activityPath -Raw
$legacyScreenSource = Get-Content -LiteralPath $legacyScreenPath -Raw
$mobileApiSource = Get-Content -LiteralPath $mobileApiPath -Raw

foreach ($marker in @(
    'getJson("/api/mobile/orders?${buildMobileOrdersQuery(search, filter, offset)}", token)',
    'getJson("/api/mobile/orders/${encodePathSegment(orderId)}", token)'
)) {
    if (-not $mobileApiSource.Contains($marker)) {
        throw "Canonical mobile orders endpoint marker is missing: $marker"
    }
}

$forbiddenFeatureDependencies = @(
    "MainActivity",
    "MobileSessionStore(",
    "MobileApiClient(",
    "feature.dashboard",
    "DashboardContent",
    "DashboardAction",
    "feature.scanner",
    "MobilePackageScanUiState",
    "IntentIntegrator",
    "DlaFlowBackgroundSyncService",
    "AndroidKeyStore",
    "startActivityForResult"
)
foreach ($forbidden in $forbiddenFeatureDependencies) {
    if ($featureSource.Contains($forbidden)) {
        throw "Orders feature crossed a forbidden dependency boundary: $forbidden"
    }
}

foreach ($featureFile in $featureFiles) {
    if ($featureFile.Name -in @("OrdersGateway.kt", "OrdersMapper.kt")) {
        continue
    }
    $source = Get-Content -LiteralPath $featureFile.FullName -Raw
    if ($source -match '\bMobileOrder[A-Za-z0-9_]*\b') {
        throw "Legacy mobile order DTO escaped gateway/mapper boundary: $($featureFile.Name)"
    }
}

if ($coordinatorSource -match '\bMobileOrder[A-Za-z0-9_]*\b' -or $coordinatorSource.Contains("toOrdersListContent") -or $coordinatorSource.Contains("toOrderDetailContent")) {
    throw "OrdersCoordinator must consume presentation models returned by OrdersGateway, not transport DTOs."
}
foreach ($gatewayContractMarker in @(
    "): OrdersListContent",
    "fun loadDetail(token: String, orderNumber: String): OrderDetailContent"
)) {
    if (-not $gatewaySource.Contains($gatewayContractMarker)) {
        throw "OrdersGateway presentation contract marker is missing: $gatewayContractMarker"
    }
}

$forbiddenActivityOwnership = @(
    "private var mobileOrders",
    "private var selectedMobileOrder",
    "private var mobileOrdersRequestVersion",
    "private var mobileOrderDetailRequestVersion",
    "private fun refreshMobileOrders(",
    "private fun ensureMobileOrdersLoaded(",
    "private fun loadMobileOrderDetail(",
    "private fun clearMobileOrdersData(",
    "private fun clearMobileOrdersState("
)
foreach ($forbidden in $forbiddenActivityOwnership) {
    if ($activitySource.Contains($forbidden)) {
        throw "Orders ownership remains in MainActivity.kt: $forbidden"
    }
}

$forbiddenLegacyScreenSymbols = @(
    "private fun OrdersTab(",
    "private fun OrderSearchField(",
    "private fun OrderFilterChips(",
    "private fun OrderListSkeleton(",
    "private fun MobileOrderCard(",
    "private fun OrderTinyPill(",
    "private fun MobileOrderDetailPanel(",
    "private fun MobileOrderDetailSection(",
    "private fun MobileOrderDetailListRow(",
    "private fun ordersSummary(",
    "private fun orderBadgeSummary(",
    "private fun orderQuickInfo(",
    "private fun orderAddressLabel(",
    "private fun orderIcon(",
    "private fun orderToneColor("
)
foreach ($forbidden in $forbiddenLegacyScreenSymbols) {
    if ($legacyScreenSource.Contains($forbidden)) {
        throw "Orders UI remains in MobileAssistantScreen.kt: $forbidden"
    }
}

$requiredFeatureSymbols = @(
    "OrdersStateHolder",
    "OrdersCoordinator",
    "OrdersGateway",
    "OrdersFeatureScreen",
    "OrdersAction",
    "OrdersRoute",
    "DlaFlowUiState",
    "activeListRequestId",
    "activeDetailRequestId"
)
foreach ($required in $requiredFeatureSymbols) {
    if (-not $featureSource.Contains($required)) {
        throw "Required orders feature symbol is missing: $required"
    }
}

Write-Host "Mobile orders feature boundary: OK"
