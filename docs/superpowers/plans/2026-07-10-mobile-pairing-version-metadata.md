# Mobile Pairing Version Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Send the installed Android app version when pairing a device and publish the verified fix as DlaFlow Mobile Assistant 0.4.2 (16).

**Architecture:** Keep the existing API and pairing flow intact. Add the app version as explicit, testable `MobileApiClient` constructor dependencies defaulted from `BuildConfig`, serialize them into the existing pairing-complete body, and verify the released artifact and production API result.

**Tech Stack:** Kotlin, Android Gradle Plugin, JUnit, `JSONObject`, GitHub Actions, ADB.

---

## File map

- Modify `app/src/test/java/pl/dlaflow/mobile/MobileApiClientTest.kt`: prove the pairing request includes exact version metadata.
- Modify `app/src/main/java/pl/dlaflow/mobile/mobile_api.kt`: provide and serialize app version metadata.
- Modify `app/build.gradle.kts`: bump the Android release to 0.4.2 (16).
- Keep `docs/superpowers/specs/2026-07-10-mobile-pairing-version-metadata-design.md` as the approved design contract.

### Task 1: Pairing request regression test and minimal implementation

**Files:**
- Modify: `app/src/test/java/pl/dlaflow/mobile/MobileApiClientTest.kt:138-204`
- Modify: `app/src/main/java/pl/dlaflow/mobile/mobile_api.kt:424-441`

- [ ] **Step 1: Write the failing request-body assertions**

Pass deterministic version values to the existing pairing test:

```kotlin
val client = MobileApiClient(
    baseUrl = "http://127.0.0.1:${server.localPort}",
    requestSigner = signer,
    nowMillis = { 1_783_540_000_123L },
    nonceFactory = { "nonce-pairing-123" },
    appVersionCode = 16,
    appVersionName = "0.4.2",
)
```

Parse the captured body and assert exact values:

```kotlin
val pairingBody = JSONObject(firstBody.get())
assertEquals("Magazyn", pairingBody.getString("deviceName"))
assertEquals(16, pairingBody.getInt("appVersionCode"))
assertEquals("0.4.2", pairingBody.getString("appVersionName"))
assertEquals(
    "PUBLIC_KEY_BASE64_VALUE_WITH_ENOUGH_LENGTH_1234567890",
    pairingBody.getString("requestSigningPublicKey"),
)
```

- [ ] **Step 2: Run the focused test and confirm RED**

Run:

```powershell
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :app:testDebugUnitTest --tests "pl.dlaflow.mobile.MobileApiClientTest.complete pairing sends device public key and signs first session lookup" --no-daemon
```

Expected: Kotlin compilation fails because `appVersionCode` and `appVersionName` are not yet constructor parameters. This confirms the test exercises missing production behavior.

- [ ] **Step 3: Add minimal production behavior**

Extend the client constructor:

```kotlin
class MobileApiClient(
    private val baseUrl: String,
    private val requestSigner: MobileRequestSigner? = null,
    private val deviceIdProvider: () -> String = { "" },
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val nonceFactory: () -> String = { UUID.randomUUID().toString() },
    private val appVersionCode: Int = BuildConfig.VERSION_CODE,
    private val appVersionName: String = BuildConfig.VERSION_NAME,
)
```

Serialize the fields in `completePairing`:

```kotlin
val body = JSONObject()
    .put("deviceName", deviceName)
    .put("pairingCode", pairingCode)
    .put("platform", "ANDROID")
    .put("appVersionCode", appVersionCode)
    .put("appVersionName", appVersionName)
```

- [ ] **Step 4: Run the focused test and confirm GREEN**

Run the same focused Gradle command. Expected: `BUILD SUCCESSFUL` and the targeted test passes.

- [ ] **Step 5: Commit the tested hotfix**

```powershell
git add -- app/src/test/java/pl/dlaflow/mobile/MobileApiClientTest.kt app/src/main/java/pl/dlaflow/mobile/mobile_api.kt
git commit -m "fix: report Android version during pairing"
```

### Task 2: Version bump and complete local verification

**Files:**
- Modify: `app/build.gradle.kts:15-16`

- [ ] **Step 1: Set release identity**

```kotlin
versionCode = 16
versionName = "0.4.2"
```

- [ ] **Step 2: Run repository and pairing boundary checks**

```powershell
.\scripts\verify-repository-contract.ps1
.\scripts\verify-design-system-boundary.ps1
.\scripts\verify-pairing-feature-boundary.ps1
```

Expected: every script exits 0.

- [ ] **Step 3: Run the complete Android gate**

```powershell
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease --no-daemon
```

Expected: `BUILD SUCCESSFUL`; all tests, lint, debug build and unsigned-or-locally-configured release build complete.

- [ ] **Step 4: Verify APK package and version metadata**

```powershell
$aapt=Get-ChildItem "$env:ANDROID_HOME\build-tools" -Recurse -Filter aapt.exe | Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
& $aapt dump badging app\build\outputs\apk\debug\app-debug.apk | Select-String "package:"
```

Expected: package `pl.dlaflow.mobile`, `versionCode='16'`, `versionName='0.4.2'`.

- [ ] **Step 5: Commit the release identity**

```powershell
git add -- app/build.gradle.kts
git commit -m "release: prepare Android Mobile Assistant 0.4.2"
```

### Task 3: Review, publish and verify production

**Files:**
- Update after validated release: `D:/ECOM/Maciek/PANEL DLAYOU/PROJECT_TODO.md`
- Update after validated release: `D:/ECOM/Maciek/PANEL DLAYOU/VPS_TODO.md`

- [ ] **Step 1: Review the exact diff and rerun completion checks**

```powershell
git diff --check origin/main...HEAD
git diff --stat origin/main...HEAD
git log --oneline origin/main..HEAD
```

Expected: only the approved spec/plan, two Kotlin files and Gradle version file differ.

- [ ] **Step 2: Push, open PR and wait for exact-SHA checks**

```powershell
git push -u origin codex/mobile-pairing-version-042-20260710
$prUrl=gh pr create --base main --head codex/mobile-pairing-version-042-20260710 --title "Release Android Mobile Assistant 0.4.2" --body "## Zakres`n- przekazywanie wersji aplikacji podczas parowania`n- wydanie 0.4.2 (16)`n`n## Weryfikacja`n- testy jednostkowe`n- lint`n- debug i release build`n- boundary checks"
$prNumber=($prUrl | Select-String -Pattern '/pull/(\d+)$').Matches.Groups[1].Value
gh pr checks $prNumber --watch
```

Expected: required PR checks pass for the branch head SHA.

- [ ] **Step 3: Merge and tag the exact merged main commit**

```powershell
gh pr merge $prNumber --squash --delete-branch
git fetch origin --prune --tags
git tag mobile-v0.4.2 origin/main
git push origin mobile-v0.4.2
```

Expected: `mobile-v0.4.2` points to the merge commit containing both version fields and version 0.4.2 (16).

- [ ] **Step 4: Verify GitHub release workflow and VPS manifest**

Watch the tag-triggered `mobile-release.yml` run to success, then retrieve the public mobile release manifest through the production API. Verify `channel=production`, `versionCode=16`, `versionName=0.4.2`, normal update priority, APK size and SHA-256; download the APK and independently compare its SHA-256 and signer certificate with the previous trusted release.

- [ ] **Step 5: Run isolated production pairing smoke**

Use the Android emulator and a temporary production account/tenant. Pair with an explicit required device name and verify the panel API reports:

```json
{
  "appVersion": {
    "code": 16,
    "name": "0.4.2"
  }
}
```

Also verify signed-request status, revoke/reconnect behavior, final revoke and zero remaining temporary users, tenants, devices and pairing codes. Never print credentials, pairing codes, tokens or device identifiers.

- [ ] **Step 6: Update the physical phone without destroying its session**

Compare the downloaded APK signing certificate with the installed application, run `adb install -r`, and verify package version 0.4.2 (16), preserved install identity, preserved session and dashboard access. Do not revoke or re-pair the real phone solely for this smoke.

- [ ] **Step 7: Record durable evidence**

Append a concise dated entry to `PROJECT_TODO.md` and `VPS_TODO.md` with PR, merge SHA, tag, workflow result, manifest/checksum proof, emulator pairing metadata proof, cleanup result and physical-update result. Do not record secrets or customer/device identifiers.
