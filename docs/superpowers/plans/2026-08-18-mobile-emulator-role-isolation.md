# Mobile Emulator Role Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep the Operator emulator paired while all Android instrumentation runs on an isolated Gradle Managed Virtual Device.

**Architecture:** Android Gradle Plugin owns a Pixel 6/API 35 managed device named `dlaflowQaApi35`; the repository test command targets its generated Gradle task instead of connected devices. A separate fail-closed PowerShell installer resolves the Operator by AVD name and performs only `adb install -r`.

**Tech Stack:** Android Gradle Plugin 8.13, Kotlin DSL, Gradle Managed Devices, PowerShell 7/Windows PowerShell, ADB, Android API 35.

---

### Task 1: Add A Failing Repository Contract

**Files:**
- Modify: `scripts/verify-repository-contract.ps1`

- [ ] **Step 1: Require the emulator-isolation artifacts and markers**

Add requirements for `scripts/run-qa-emulator-tests.ps1`, `scripts/install-operator-apk.ps1`, `dlaflowQaApi35`, `ManagedVirtualDevice`, `:app:dlaflowQaApi35DebugAndroidTest`, `DlaFlow_Task6_Dashboard_API35_20260717`, and `install -r`. Reject a raw `connectedDebugAndroidTest` command in `README.md`.

```powershell
function Reject-Text([string]$relativePath, [string]$needle) {
    $content = Get-Content -LiteralPath (Join-Path $root $relativePath) -Raw
    if ($content.Contains($needle)) {
        throw "Forbidden text '$needle' remains in $relativePath"
    }
}

Require-File "scripts/run-qa-emulator-tests.ps1"
Require-File "scripts/install-operator-apk.ps1"
Require-Text "app/build.gradle.kts" '"dlaflowQaApi35"'
Require-Text "app/build.gradle.kts" "ManagedVirtualDevice"
Require-Text "scripts/run-qa-emulator-tests.ps1" ":app:dlaflowQaApi35DebugAndroidTest"
Require-Text "scripts/install-operator-apk.ps1" "DlaFlow_Task6_Dashboard_API35_20260717"
Require-Text "scripts/install-operator-apk.ps1" '"install", "-r"'
Reject-Text "README.md" '.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon'
```

- [ ] **Step 2: Run the contract and verify RED**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-repository-contract.ps1`

Expected: FAIL because `scripts/run-qa-emulator-tests.ps1` does not exist.

- [ ] **Step 3: Commit the RED contract**

```powershell
git add scripts/verify-repository-contract.ps1
git commit -m "test: require isolated Android emulator roles"
```

### Task 2: Define And Run The Managed QA Device

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `scripts/run-qa-emulator-tests.ps1`

- [ ] **Step 1: Add the managed Pixel 6/API 35 device**

Add below `buildFeatures` inside `android`:

```kotlin
testOptions {
    managedDevices {
        devices {
            maybeCreate<com.android.build.api.dsl.ManagedVirtualDevice>("dlaflowQaApi35").apply {
                device = "Pixel 6"
                apiLevel = 35
                systemImageSource = "google"
            }
        }
    }
}
```

- [ ] **Step 2: Create the exact QA runner**

```powershell
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
```

- [ ] **Step 3: Confirm Gradle exposes only the expected managed task**

Run: `.\gradlew.bat :app:tasks --all --no-daemon | Select-String "dlaflowQaApi35DebugAndroidTest"`

Expected: exactly the managed QA test task is listed.

- [ ] **Step 4: Commit the managed-device boundary**

```powershell
git add app/build.gradle.kts scripts/run-qa-emulator-tests.ps1
git commit -m "feat: isolate instrumentation on managed emulator"
```

### Task 3: Add The Fail-Closed Operator Installer

**Files:**
- Create: `scripts/install-operator-apk.ps1`

- [ ] **Step 1: Implement SDK and AVD resolution**

The script accepts `-ApkPath` and `-OperatorAvdName`, resolves SDK from `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or ignored `local.properties`, lists only `emulator-*` devices, and queries every candidate with `adb -s <serial> emu avd name`.

```powershell
param(
    [string] $ApkPath = "app/build/outputs/apk/debug/app-debug.apk",
    [string] $OperatorAvdName = "DlaFlow_Task6_Dashboard_API35_20260717"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot

function Resolve-AndroidSdkRoot {
    foreach ($candidate in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path -LiteralPath $candidate -PathType Container)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    $localProperties = Join-Path $root "local.properties"
    if (Test-Path -LiteralPath $localProperties -PathType Leaf) {
        $sdkLine = Get-Content -LiteralPath $localProperties | Where-Object { $_ -like "sdk.dir=*" } | Select-Object -First 1
        if ($sdkLine) {
            $decoded = $sdkLine.Substring("sdk.dir=".Length).Replace('\:', ':').Replace('\\', '\')
            if (Test-Path -LiteralPath $decoded -PathType Container) {
                return (Resolve-Path -LiteralPath $decoded).Path
            }
        }
    }

    throw "Android SDK was not found. Set ANDROID_HOME or keep sdk.dir in local.properties."
}

$sdkRoot = Resolve-AndroidSdkRoot
$adbPath = Join-Path $sdkRoot "platform-tools/adb.exe"
if (-not (Test-Path -LiteralPath $adbPath -PathType Leaf)) {
    throw "ADB is missing: $adbPath"
}

$runningAvds = @()
foreach ($line in (& $adbPath "devices")) {
    if ($line -notmatch '^(emulator-\d+)\s+device\b') {
        continue
    }
    $serial = $Matches[1]
    $name = (& $adbPath "-s" $serial "emu" "avd" "name" | Where-Object {
        -not [string]::IsNullOrWhiteSpace($_) -and $_.Trim() -ne "OK"
    } | Select-Object -First 1).Trim()
    if (-not [string]::IsNullOrWhiteSpace($name)) {
        $runningAvds += [pscustomobject]@{ Name = $name; Serial = $serial }
    }
}

$matches = @($runningAvds | Where-Object { $_.Name -eq $OperatorAvdName })
if ($matches.Count -ne 1) {
    throw "Expected exactly one running Operator AVD '$OperatorAvdName'; found $($matches.Count)."
}

$resolvedApkPath = if ([System.IO.Path]::IsPathRooted($ApkPath)) { $ApkPath } else { Join-Path $root $ApkPath }
if (-not (Test-Path -LiteralPath $resolvedApkPath -PathType Leaf)) {
    throw "APK is missing: $resolvedApkPath"
}
if ((Get-Item -LiteralPath $resolvedApkPath).Length -le 0) {
    throw "APK is empty: $resolvedApkPath"
}

$installArgs = @("-s", $matches[0].Serial, "install", "-r", $resolvedApkPath)
& $adbPath @installArgs
if ($LASTEXITCODE -ne 0) {
    throw "Operator APK update failed; existing application data was left untouched."
}

Write-Host "Operator APK updated with application data preserved."
```

- [ ] **Step 2: Validate the APK and preserve application data**

Run `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/install-operator-apk.ps1 -ApkPath missing.apk`.

Expected: FAIL before ADB mutation with `APK is missing` when Operator is available, or fail earlier with the exact missing Operator role. Inspect the source and require no destructive fallback.

The script must not contain `uninstall`, `pm clear`, `clear data`, or automatic fallback after signing mismatch.

- [ ] **Step 3: Run the repository contract and verify GREEN**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-repository-contract.ps1`

Expected: still FAIL only because README contains the raw connected command; the two scripts and Gradle markers pass.

- [ ] **Step 4: Commit the Operator installer**

```powershell
git add scripts/install-operator-apk.ps1
git commit -m "feat: preserve paired Operator emulator data"
```

### Task 4: Make The Safe Workflow Canonical

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Replace the connected-device command**

Replace `.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon` with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-qa-emulator-tests.ps1
```

- [ ] **Step 2: Document the role contract and Operator update**

Add a short `Local emulator roles` section naming the Operator AVD, the managed QA device, and the preserving command:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/install-operator-apk.ps1
```

State that instrumentation, package clearing, uninstall, and test APK installation are forbidden on Operator.

- [ ] **Step 3: Run the repository contract and verify GREEN**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-repository-contract.ps1`

Expected: `Mobile repository contract: OK`.

- [ ] **Step 4: Commit documentation**

```powershell
git add README.md scripts/verify-repository-contract.ps1
git commit -m "docs: enforce safe emulator verification workflow"
```

### Task 5: Prove Isolation End To End

**Files:**
- Modify after verification: `D:/ECOM/Maciek/PANEL DLAYOU/PROJECT_TODO.md`

- [ ] **Step 1: Record a harmless Operator sentinel**

With the Operator running, create a synthetic file through `run-as pl.dlaflow.mobile` under `files/qa-operator-preservation.txt`, then record only its SHA-256 hash. Do not read or export shared preferences, tokens, keys, or customer data.

```powershell
$adb = "C:\Users\Maciej\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$serial = ((& $adb devices) | Where-Object { $_ -match '^(emulator-\d+)\s+device\b' } | ForEach-Object { $Matches[1] } | Select-Object -First 1)
$avdName = (& $adb -s $serial emu avd name | Where-Object { $_.Trim() -ne "OK" } | Select-Object -First 1).Trim()
if ($avdName -ne "DlaFlow_Task6_Dashboard_API35_20260717") { throw "Operator AVD was not resolved." }
& $adb -s $serial shell "run-as pl.dlaflow.mobile sh -c 'printf dlaflow-operator-preservation-v1 > files/qa-operator-preservation.txt'"
$beforeHash = (& $adb -s $serial shell "run-as pl.dlaflow.mobile sha256sum files/qa-operator-preservation.txt").Split(' ')[0]
```

- [ ] **Step 2: Run the managed QA suite**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-qa-emulator-tests.ps1`

Expected: the managed API 35 suite passes and the Operator remains connected to ADB.

- [ ] **Step 3: Prove Operator data preservation**

Recalculate the synthetic sentinel hash on Operator and require it to match. Verify the package remains installed and launch `MainActivity`; do not inspect private application data.

```powershell
$afterHash = (& $adb -s $serial shell "run-as pl.dlaflow.mobile sha256sum files/qa-operator-preservation.txt").Split(' ')[0]
if ($afterHash -ne $beforeHash) { throw "Operator sentinel changed during QA tests." }
& $adb -s $serial shell pm path pl.dlaflow.mobile
& $adb -s $serial shell am start -n pl.dlaflow.mobile/.MainActivity
```

- [ ] **Step 4: Run all repository gates**

Run the five boundary scripts followed by:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

Expected: all guards report `OK`; Gradle reports `BUILD SUCCESSFUL`.

- [ ] **Step 5: Install the current APK on Operator safely**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/install-operator-apk.ps1`

Expected: `adb install -r` succeeds and the sentinel hash still matches.

- [ ] **Step 6: Update project memory without VPS changes**

Append a concise entry to panel `PROJECT_TODO.md` containing the branch/commit, managed QA proof, Operator sentinel preservation, guard/build results, and explicit absence of release/push/deploy. Do not modify `VPS_TODO.md`.

- [ ] **Step 7: Commit the verified memory-independent Android scope**

```powershell
git status --short --branch
git diff --check
```

Expected: Android worktree is clean after commits; panel TODO contains only the local memory addition alongside preserved existing changes.
