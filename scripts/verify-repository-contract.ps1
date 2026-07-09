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

Require-File "AGENTS.md"
Require-File "gradlew"
Require-File "gradlew.bat"
Require-File "gradle/wrapper/gradle-wrapper.jar"
Require-Text "gradle/wrapper/gradle-wrapper.properties" "distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip"
Require-Text "gradle/wrapper/gradle-wrapper.properties" "distributionSha256Sum=20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78"
Require-Text ".github/workflows/mobile-release.yml" "./gradlew :app:testDebugUnitTest"
Require-Text ".github/workflows/mobile-release.yml" ":app:lintDebug"
Require-Text ".github/workflows/mobile-release.yml" "./gradlew :app:assembleDebug"
Require-Text ".github/workflows/mobile-release.yml" "./gradlew :app:assembleRelease"
Require-Text ".github/workflows/mobile-release.yml" "./scripts/verify-repository-contract.ps1"
Require-Text ".github/workflows/mobile-release.yml" "gradle/actions/wrapper-validation@748248ddd2a24f49513d8f472f81c3a07d4d50e1"

Write-Host "Mobile repository contract: OK"
