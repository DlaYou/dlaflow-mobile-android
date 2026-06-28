[CmdletBinding()]
param(
  [string] $ApkPath = "app/build/outputs/apk/debug/app-debug.apk",
  [string] $AndroidBuildFile = "app/build.gradle.kts",
  [string] $OutputDir = "mobile-release",
  [string] $Channel = "test",
  [int] $MinSupportedVersionCode = 1,
  [ValidateSet("normal", "important", "required")]
  [string] $UpdatePriority = "normal",
  [string] $ReleaseTitle = "Nowa wersja DlaFlow"
)

$ErrorActionPreference = "Stop"

function Resolve-ProjectPath([string] $PathValue) {
  if ([System.IO.Path]::IsPathRooted($PathValue)) {
    return [System.IO.Path]::GetFullPath($PathValue)
  }

  return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $PathValue))
}

if ($Channel -notmatch "^[A-Za-z0-9._-]{1,40}$") {
  throw "Channel must contain only letters, numbers, dot, underscore or dash."
}

if ($MinSupportedVersionCode -lt 1) {
  throw "MinSupportedVersionCode must be greater than zero."
}

if ([string]::IsNullOrWhiteSpace($ReleaseTitle) -or $ReleaseTitle.Length -gt 80) {
  throw "ReleaseTitle must be non-empty and at most 80 characters."
}

$apkFullPath = Resolve-ProjectPath $ApkPath
$buildFileFullPath = Resolve-ProjectPath $AndroidBuildFile
$outputFullPath = Resolve-ProjectPath $OutputDir

if (-not (Test-Path -LiteralPath $apkFullPath -PathType Leaf)) {
  throw "APK file was not found: $apkFullPath"
}

if (-not (Test-Path -LiteralPath $buildFileFullPath -PathType Leaf)) {
  throw "Android build file was not found: $buildFileFullPath"
}

$buildFileContent = Get-Content -LiteralPath $buildFileFullPath -Raw
$versionNameMatch = [regex]::Match($buildFileContent, 'versionName\s*=\s*"([^"]+)"')
$versionCodeMatch = [regex]::Match($buildFileContent, 'versionCode\s*=\s*(\d+)')

if (-not $versionNameMatch.Success) {
  throw "Could not read versionName from $buildFileFullPath"
}

if (-not $versionCodeMatch.Success) {
  throw "Could not read versionCode from $buildFileFullPath"
}

$versionName = $versionNameMatch.Groups[1].Value
$versionCode = [int] $versionCodeMatch.Groups[1].Value

if ($versionName -notmatch "^[A-Za-z0-9._-]{1,40}$") {
  throw "versionName must be safe for an APK filename. Current value: $versionName"
}

New-Item -ItemType Directory -Force -Path $outputFullPath | Out-Null

$targetFileName = "dlaflow-mobile-assistant-$versionName-$Channel.apk"
$targetApkPath = Join-Path $outputFullPath $targetFileName
Copy-Item -LiteralPath $apkFullPath -Destination $targetApkPath -Force

$sha256Algorithm = [System.Security.Cryptography.SHA256]::Create()
try {
  $stream = [System.IO.File]::OpenRead($targetApkPath)
  try {
    $hashBytes = $sha256Algorithm.ComputeHash($stream)
  } finally {
    $stream.Dispose()
  }
} finally {
  $sha256Algorithm.Dispose()
}

$sha256 = -join ($hashBytes | ForEach-Object { $_.ToString("x2") })
$apkInfo = Get-Item -LiteralPath $targetApkPath

$manifest = [ordered] @{
  channel = $Channel
  fileName = $targetFileName
  minSupportedVersionCode = $MinSupportedVersionCode
  releaseTitle = $ReleaseTitle
  releaseNotes = @(
    "Automatyczny build Android Mobile Assistant.",
    "Kanal: $Channel."
  )
  releasedAt = (Get-Date).ToUniversalTime().ToString("o")
  sha256 = $sha256
  sizeBytes = [int64] $apkInfo.Length
  updatePriority = $UpdatePriority
  versionCode = $versionCode
  versionName = $versionName
}

$manifestPath = Join-Path $outputFullPath "latest.json"
$manifestJson = $manifest | ConvertTo-Json -Depth 4
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText($manifestPath, $manifestJson, $utf8NoBom)

Write-Host "Generated Android release package:"
Write-Host "  APK: $targetApkPath"
Write-Host "  Manifest: $manifestPath"
Write-Host "  Version: $versionName ($versionCode)"
Write-Host "  SHA-256: $sha256"
