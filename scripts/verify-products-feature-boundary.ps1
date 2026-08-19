$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$root = Split-Path -Parent $PSScriptRoot
$feature = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/feature/products"
$activity = Get-Content (Join-Path $root "app/src/main/java/pl/dlaflow/mobile/MainActivity.kt") -Raw
$source = (Get-ChildItem $feature -Filter *.kt -File | ForEach-Object { Get-Content $_.FullName -Raw }) -join "`n"
foreach ($file in @("ProductsContract.kt","ProductsCoordinator.kt","ProductsStateHolder.kt","PhotoTasksContract.kt","PhotoTasksCoordinator.kt","PhotoTasksStateHolder.kt","PhotoCaptureStateHolder.kt")) {
    if (-not (Test-Path (Join-Path $feature $file))) { throw "Missing products feature file: $file" }
}
foreach ($forbidden in @("MobileSessionStore(","AndroidKeyStore","IntentIntegrator","startActivityForResult","MainActivity")) {
    if ($source.Contains($forbidden)) { throw "Products feature crossed forbidden boundary: $forbidden" }
}
foreach ($forbidden in @(
    "private fun refreshMobileProducts(",
    "private fun refreshPhotoTasks(",
    "mobileApiClientForSession(sessionStore).listProducts(",
    "mobileApiClientForSession(sessionStore).listProductVariants(",
    "mobileApiClientForSession(sessionStore).quickEditProduct(",
    "mobileApiClientForSession(sessionStore).quickEditProductVariant(",
    "mobileApiClientForSession(sessionStore).listActivePhotoTasks(",
    "mobileApiClientForSession(sessionStore).uploadPhotoTaskMedia(",
    "mobileApiClientForSession(sessionStore).completePhotoTask(",
    "mobileApiClientForSession(sessionStore).getPhotoTaskDispatch("
)) {
    if ($activity.Contains($forbidden)) { throw "Legacy products ownership remains in MainActivity: $forbidden" }
}
if (-not $activity.Contains("ProductsCoordinator") -or -not $activity.Contains("PhotoTasksCoordinator")) { throw "Products coordinators are not wired into host" }
Write-Host "Mobile products feature boundary: OK"
