$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
$screenPath = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt"
$componentsPath = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/core/designsystem/DlaFlowComponents.kt"
$sourceRoot = Join-Path $root "app/src/main/java"
$designSystemRoot = Join-Path $sourceRoot "pl/dlaflow/mobile/core/designsystem"
$screen = Get-Content -LiteralPath $screenPath -Raw
$legacyColorLimits = @{
    "app/src/main/java/pl/dlaflow/mobile/CallerIdActivity.kt" = 1
    "app/src/main/java/pl/dlaflow/mobile/DlaFlowSessionTransitionOverlay.kt" = 30
    "app/src/main/java/pl/dlaflow/mobile/MainActivity.kt" = 27
}

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

foreach ($sourceFile in Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter "*.kt") {
    if ($sourceFile.FullName.StartsWith($designSystemRoot, [StringComparison]::OrdinalIgnoreCase)) {
        continue
    }
    $source = Get-Content -LiteralPath $sourceFile.FullName -Raw
    $hardcodedColorCount = ([regex]::Matches(
        $source,
        'Color\(0x[0-9A-Fa-f]+\)',
        [Text.RegularExpressions.RegexOptions]::IgnoreCase
    )).Count
    if ($hardcodedColorCount -eq 0) {
        continue
    }
    $relativePath = $sourceFile.FullName.Substring($root.Length).TrimStart([char[]]@('\', '/')).Replace('\', '/')
    if (-not $legacyColorLimits.ContainsKey($relativePath)) {
        throw "Hardcoded hex color outside core/designsystem: $relativePath"
    }
    if ($hardcodedColorCount -gt $legacyColorLimits[$relativePath]) {
        throw "Legacy hardcoded color limit exceeded in ${relativePath}: $hardcodedColorCount"
    }
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
