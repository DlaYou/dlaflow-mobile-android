$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
$gradle = Join-Path $root "gradlew.bat"

if (-not (Test-Path -LiteralPath $gradle -PathType Leaf)) {
    throw "Gradle wrapper is missing: $gradle"
}

& $gradle ":app:dlaflowQaApi35DebugAndroidTest" "--no-daemon"
if ($LASTEXITCODE -ne 0) {
    throw "Managed QA emulator tests failed with exit code $LASTEXITCODE."
}

Write-Host "Managed QA emulator tests: OK"
