$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$root = Split-Path -Parent $PSScriptRoot
$feature = Join-Path $root "app/src/main/java/pl/dlaflow/mobile/feature/notifications"
$service = Get-Content (Join-Path $root "app/src/main/java/pl/dlaflow/mobile/DlaFlowBackgroundSyncService.kt") -Raw
$jobService = Get-Content (Join-Path $root "app/src/main/java/pl/dlaflow/mobile/DlaFlowDispatchJobService.kt") -Raw
$activity = Get-Content (Join-Path $root "app/src/main/java/pl/dlaflow/mobile/MainActivity.kt") -Raw
$source = (Get-ChildItem $feature -Filter *.kt -File | ForEach-Object { Get-Content $_.FullName -Raw }) -join "`n"
foreach ($file in @("NotificationsContract.kt","NotificationsCoordinator.kt","NotificationsStateHolder.kt","NotificationsBackgroundCoordinator.kt")) {
    if (-not (Test-Path (Join-Path $feature $file))) { throw "Missing notifications feature file: $file" }
}
foreach ($forbidden in @("AndroidKeyStore","FirebaseMessagingService","startActivityForResult")) {
    if ($source.Contains($forbidden)) { throw "Notifications feature crossed forbidden boundary: $forbidden" }
}
foreach ($required in @("NotificationsBackgroundRuntime","NotificationSessionKey")) {
    if (-not $service.Contains($required)) { throw "Missing notification contract marker: $required" }
    if (-not $jobService.Contains($required)) { throw "Missing job notification contract marker: $required" }
}
if (-not $activity.Contains("NotificationsCoordinator")) { throw "MainActivity does not use NotificationsCoordinator" }
foreach ($forbidden in @("private fun refreshMobileNotifications", "pollUnreadPanelAlertNotifications")) {
    if ($activity.Contains($forbidden) -or $service.Contains($forbidden) -or $jobService.Contains($forbidden)) {
        throw "Legacy notification ownership remains: $forbidden"
    }
}
Write-Host "Mobile notifications feature boundary: OK"
