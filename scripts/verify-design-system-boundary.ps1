$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
$screenPath = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt"
$componentsPath = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/core/designsystem/DlaFlowComponents.kt"
$screen = Get-Content -LiteralPath $screenPath -Raw

$forbiddenDefinitions = @(
    "private fun ScreenHeader(",
    "private fun PanelCard(",
    "private fun DlaFlowTextField(",
    "private fun PrimaryActionButton(",
    "private fun SecondaryActionButton(",
    "private fun DlaIcon(",
    "private fun StatusPill(",
    "private fun StatusStrip(",
    "private fun KeyValue("
)

foreach ($definition in $forbiddenDefinitions) {
    if ($screen.Contains($definition)) {
        throw "Shared component is still private in MobileAssistantScreen.kt: $definition"
    }
}

if ($screen -match 'Color\(0x[0-9A-Fa-f]+\)') {
    throw "Hardcoded hex color remains in MobileAssistantScreen.kt"
}

if (-not (Test-Path -LiteralPath $componentsPath -PathType Leaf)) {
    throw "DlaFlowComponents.kt is missing"
}

$components = Get-Content -LiteralPath $componentsPath -Raw
foreach ($name in @(
    "DlaFlowScreenHeader",
    "DlaFlowCard",
    "DlaFlowTextField",
    "DlaFlowPrimaryButton",
    "DlaFlowSecondaryButton",
    "DlaFlowIcon",
    "DlaFlowStatusBadge",
    "DlaFlowStatusStrip",
    "DlaFlowKeyValue"
)) {
    if (-not $components.Contains("fun $name(")) {
        throw "Shared design-system component is missing: $name"
    }
}

Write-Host "Mobile design-system boundary: OK"
