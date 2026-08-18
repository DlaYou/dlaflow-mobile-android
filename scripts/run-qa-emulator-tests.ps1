$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
$gradle = Join-Path $root "gradlew.bat"

if (-not (Test-Path -LiteralPath $gradle -PathType Leaf)) {
    throw "Gradle wrapper is missing: $gradle"
}

& $gradle ":app:dlaflowQaApi35DebugAndroidTest" "--no-daemon" 2>&1 |
    Tee-Object -Variable capturedGradleOutput
$gradleExitCode = $LASTEXITCODE

if ($gradleExitCode -ne 0) {
    throw "Managed QA emulator tests failed with exit code $gradleExitCode."
}

if ($capturedGradleOutput -match "unspecified testedAbi") {
    throw "Managed QA emulator ABI is not pinned in the Gradle setup task."
}

Write-Host "Managed QA emulator tests: OK"
