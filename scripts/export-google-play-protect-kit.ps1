[CmdletBinding()]
param(
  [string] $ApkPath = "",
  [string] $OutputDir = "mobile-release/google-play-protect",
  [string] $AndroidBuildFile = "app/build.gradle.kts",
  [string] $LatestManifestPath = "mobile-release/latest.json"
)

$ErrorActionPreference = "Stop"

function Resolve-ProjectPath([string] $PathValue) {
  if ([System.IO.Path]::IsPathRooted($PathValue)) {
    return [System.IO.Path]::GetFullPath($PathValue)
  }

  return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $PathValue))
}

function Read-LocalPropertiesSdkDir() {
  $localPropertiesPath = Resolve-ProjectPath "local.properties"
  if (-not (Test-Path -LiteralPath $localPropertiesPath -PathType Leaf)) {
    return $null
  }

  foreach ($line in Get-Content -LiteralPath $localPropertiesPath) {
    if ($line -match "^\s*sdk\.dir\s*=\s*(.+?)\s*$") {
      return (($Matches[1] -replace "\\:", ":") -replace "\\\\", "\")
    }
  }

  return $null
}

function Find-ApkSigner() {
  $sdkCandidates = @(
    $env:ANDROID_HOME,
    $env:ANDROID_SDK_ROOT,
    (Read-LocalPropertiesSdkDir)
  ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique

  foreach ($sdkPath in $sdkCandidates) {
    $buildToolsPath = Join-Path $sdkPath "build-tools"
    if (-not (Test-Path -LiteralPath $buildToolsPath -PathType Container)) {
      continue
    }

    $candidate = Get-ChildItem -LiteralPath $buildToolsPath -Directory |
      Sort-Object Name -Descending |
      ForEach-Object { Join-Path $_.FullName "apksigner.bat" } |
      Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } |
      Select-Object -First 1

    if ($candidate) {
      return $candidate
    }
  }

  $fromPath = Get-Command "apksigner.bat" -ErrorAction SilentlyContinue
  if ($fromPath) {
    return $fromPath.Source
  }

  throw "Could not find apksigner.bat. Install Android SDK Build Tools or set ANDROID_HOME."
}

function Read-RequiredRegex([string] $Content, [string] $Pattern, [string] $Name) {
  $match = [regex]::Match($Content, $Pattern)
  if (-not $match.Success) {
    throw "Could not read $Name from Android build file."
  }

  return $match.Groups[1].Value
}

function Read-ApkSignerValue([string[]] $Lines, [string] $Label) {
  $escapedLabel = [regex]::Escape($Label)
  foreach ($line in $Lines) {
    if ($line -match "${escapedLabel}:\s*(.+)$") {
      return $Matches[1].Trim()
    }
  }

  throw "Could not read '$Label' from apksigner output."
}

$buildFileFullPath = Resolve-ProjectPath $AndroidBuildFile
if (-not (Test-Path -LiteralPath $buildFileFullPath -PathType Leaf)) {
  throw "Android build file was not found: $buildFileFullPath"
}

$buildFileContent = Get-Content -LiteralPath $buildFileFullPath -Raw
$applicationId = Read-RequiredRegex $buildFileContent 'applicationId\s*=\s*"([^"]+)"' "applicationId"
$versionName = Read-RequiredRegex $buildFileContent 'versionName\s*=\s*"([^"]+)"' "versionName"
$versionCode = [int](Read-RequiredRegex $buildFileContent 'versionCode\s*=\s*(\d+)' "versionCode")
$releaseManifest = $null
$manifestFullPath = $null

if (-not [string]::IsNullOrWhiteSpace($LatestManifestPath)) {
  $candidateManifestPath = Resolve-ProjectPath $LatestManifestPath
  if (Test-Path -LiteralPath $candidateManifestPath -PathType Leaf) {
    $releaseManifest = Get-Content -LiteralPath $candidateManifestPath -Raw | ConvertFrom-Json
    $manifestFullPath = $candidateManifestPath
  }
}

if ([string]::IsNullOrWhiteSpace($ApkPath)) {
  if (-not $releaseManifest) {
    throw "Provide -ApkPath or generate $LatestManifestPath first."
  }

  if ([string]::IsNullOrWhiteSpace($releaseManifest.fileName)) {
    throw "Release manifest does not contain fileName."
  }

  $ApkPath = Join-Path (Split-Path -Parent $manifestFullPath) $releaseManifest.fileName
}

if ($releaseManifest) {
  if (-not [string]::IsNullOrWhiteSpace($releaseManifest.versionName)) {
    $versionName = [string] $releaseManifest.versionName
  }

  if ($releaseManifest.versionCode) {
    $versionCode = [int] $releaseManifest.versionCode
  }
}

$apkFullPath = Resolve-ProjectPath $ApkPath
if (-not (Test-Path -LiteralPath $apkFullPath -PathType Leaf)) {
  throw "APK file was not found: $apkFullPath"
}

$apkSignerPath = Find-ApkSigner
$apkSignerOutput = & $apkSignerPath verify --print-certs $apkFullPath 2>&1
if ($LASTEXITCODE -ne 0) {
  throw "apksigner verification failed for $apkFullPath.`n$($apkSignerOutput -join "`n")"
}

$apkSha256 = (Get-FileHash -LiteralPath $apkFullPath -Algorithm SHA256).Hash.ToLowerInvariant()
$certSha256 = Read-ApkSignerValue $apkSignerOutput "Signer #1 certificate SHA-256 digest"
$certSha1 = Read-ApkSignerValue $apkSignerOutput "Signer #1 certificate SHA-1 digest"
$certMd5 = Read-ApkSignerValue $apkSignerOutput "Signer #1 certificate MD5 digest"
$certDn = Read-ApkSignerValue $apkSignerOutput "Signer #1 certificate DN"
$apkInfo = Get-Item -LiteralPath $apkFullPath
$outputFullPath = Resolve-ProjectPath $OutputDir
New-Item -ItemType Directory -Force -Path $outputFullPath | Out-Null

$generatedAt = (Get-Date).ToUniversalTime().ToString("o")
$kit = [ordered] @{
  generatedAt = $generatedAt
  packageName = $applicationId
  versionName = $versionName
  versionCode = $versionCode
  apkPath = $apkFullPath
  releaseManifestPath = $manifestFullPath
  apkSizeBytes = [int64] $apkInfo.Length
  apkSha256 = $apkSha256
  signerCertificate = [ordered] @{
    sha256 = $certSha256
    sha1 = $certSha1
    md5 = $certMd5
    distinguishedName = $certDn
  }
  googleConsoleChecklist = @(
    "Complete developer identity verification.",
    "Register package name $applicationId.",
    "Use this signed production APK as package ownership proof.",
    "Confirm package registration status is registered or verified.",
    "Install the same production APK from the DlaFlow panel and confirm Play Protect does not block installation."
  )
}

$jsonPath = Join-Path $outputFullPath "android-developer-verification-kit.json"
$markdownPath = Join-Path $outputFullPath "android-developer-verification-kit.md"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText($jsonPath, ($kit | ConvertTo-Json -Depth 5), $utf8NoBom)

$markdown = @"
# Android Developer Verification Kit

Generated: $generatedAt

## App

- Package name: ``$applicationId``
- Version: ``$versionName`` (``$versionCode``)
- APK path: ``$apkFullPath``
- APK size: ``$($apkInfo.Length)`` bytes
- APK SHA-256: ``$apkSha256``

## Signing Certificate

- SHA-256: ``$certSha256``
- SHA-1: ``$certSha1``
- MD5: ``$certMd5``
- DN: ``$certDn``

## Google Console Checklist

1. Complete developer identity verification.
2. Register package name ``$applicationId``.
3. Use this signed production APK as package ownership proof.
4. Confirm package registration status is registered or verified.
5. Install the same production APK from the DlaFlow panel and confirm Play Protect does not block installation.

This file contains public APK and certificate metadata only. It does not contain the private signing key or any DlaFlow secret.
"@

[System.IO.File]::WriteAllText($markdownPath, $markdown, $utf8NoBom)

Write-Host "Generated Android Developer Verification kit:"
Write-Host "  JSON: $jsonPath"
Write-Host "  Markdown: $markdownPath"
Write-Host "  Package: $applicationId"
Write-Host "  Version: $versionName ($versionCode)"
Write-Host "  APK SHA-256: $apkSha256"
Write-Host "  Certificate SHA-256: $certSha256"
