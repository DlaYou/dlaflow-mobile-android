$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot

function Require-File([string]$relativePath) {
    $path = Join-Path $root $relativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required repository file is missing: $relativePath"
    }
}

function Require-Text([string]$relativePath, [string]$needle) {
    $path = Join-Path $root $relativePath
    Require-File $relativePath
    $content = Get-Content -LiteralPath $path -Raw
    if (-not $content.Contains($needle)) {
        throw "Required text '$needle' is missing from $relativePath"
    }
}

function Reject-Text([string]$relativePath, [string]$needle) {
    $path = Join-Path $root $relativePath
    Require-File $relativePath
    $content = Get-Content -LiteralPath $path -Raw
    if ($content.Contains($needle)) {
        throw "Forbidden text '$needle' remains in $relativePath"
    }
}

function Reject-Pattern([string]$relativePath, [string]$pattern) {
    $path = Join-Path $root $relativePath
    Require-File $relativePath
    $content = Get-Content -LiteralPath $path -Raw
    if ($content -match $pattern) {
        throw "Forbidden pattern '$pattern' remains in $relativePath"
    }
}

Require-File "AGENTS.md"
Require-File "gradlew"
Require-File "gradlew.bat"
Require-File "gradle/wrapper/gradle-wrapper.jar"
Require-File "scripts/verify-design-system-boundary.ps1"
Require-File "scripts/verify-pairing-feature-boundary.ps1"
Require-File "scripts/verify-products-feature-boundary.ps1"
Require-File "scripts/verify-notifications-feature-boundary.ps1"
Require-File "scripts/verify-settings-feature-boundary.ps1"
Require-File "scripts/run-qa-emulator-tests.ps1"
Require-File "scripts/install-operator-apk.ps1"
Require-Text "gradle/wrapper/gradle-wrapper.properties" "distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip"
Require-Text "gradle/wrapper/gradle-wrapper.properties" "distributionSha256Sum=20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78"
Require-Text ".github/workflows/mobile-release.yml" "./gradlew :app:testDebugUnitTest"
Require-Text ".github/workflows/mobile-release.yml" ":app:lintDebug"
Require-Text ".github/workflows/mobile-release.yml" "./gradlew :app:assembleDebug"
Require-Text ".github/workflows/mobile-release.yml" "./gradlew :app:assembleRelease"
Require-Text ".github/workflows/mobile-release.yml" "./scripts/verify-repository-contract.ps1"
Require-Text ".github/workflows/mobile-release.yml" "./scripts/verify-design-system-boundary.ps1"
Require-Text ".github/workflows/mobile-release.yml" "./scripts/verify-pairing-feature-boundary.ps1"
Require-Text ".github/workflows/mobile-release.yml" "./scripts/verify-products-feature-boundary.ps1"
Require-Text ".github/workflows/mobile-release.yml" "./scripts/verify-notifications-feature-boundary.ps1"
Require-Text ".github/workflows/mobile-release.yml" "./scripts/verify-settings-feature-boundary.ps1"
Require-Text ".github/workflows/mobile-release.yml" "gradle/actions/wrapper-validation@748248ddd2a24f49513d8f472f81c3a07d4d50e1"
Require-Text ".github/workflows/mobile-release.yml" "Materialize Firebase Google Services configuration"
Require-Text ".github/workflows/mobile-release.yml" 'GOOGLE_SERVICES_JSON_BASE64: ${{ secrets.GOOGLE_SERVICES_JSON_BASE64 }}'
Require-Text ".github/workflows/mobile-release.yml" "base64 --decode > app/google-services.json"
Require-Text ".github/workflows/mobile-release.yml" "Remove Firebase Google Services configuration"
Require-Text "app/build.gradle.kts" '"dlaflowQaApi35"'
Require-Text "app/build.gradle.kts" "ManagedVirtualDevice"
Require-Text "app/build.gradle.kts" 'testedAbi = "x86_64"'
Require-Text "app/build.gradle.kts" "allDevices {"
Require-Text "scripts/run-qa-emulator-tests.ps1" ":app:dlaflowQaApi35DebugAndroidTest"
Require-Text "scripts/install-operator-apk.ps1" "DlaFlow_Task6_Dashboard_API35_20260717"
Require-Text "scripts/install-operator-apk.ps1" '"install", "-r"'
Reject-Text "scripts/run-qa-emulator-tests.ps1" ":app:connectedDebugAndroidTest"
Reject-Pattern "app/build.gradle.kts" '(?m)^\s*devices\s*\{'
Reject-Pattern "scripts/install-operator-apk.ps1" '(?i)\b(?:uninstall|pm\s+clear|clear\s+data)\b'
Reject-Text "README.md" '.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon'

Write-Host "Mobile repository contract: OK"
