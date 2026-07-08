[CmdletBinding()]
param(
  [string] $ApkPath = "app/build/outputs/apk/debug/app-debug.apk",
  [string] $AndroidBuildFile = "app/build.gradle.kts",
  [string] $OutputDir = "mobile-release",
  [string] $Channel = "test",
  [int] $MinSupportedVersionCode = 1,
  [ValidateSet("normal", "important", "required")]
  [string] $UpdatePriority = "normal",
  [string] $ReleaseTitle = "Nowa wersja DlaFlow",
  [string] $ExpectedPackageName = "pl.dlaflow.mobile",
  [string] $ExpectedCertificateSha256 = $env:ANDROID_SIGNING_CERT_SHA256,
  [string] $PreviousManifestPath = ""
)

$ErrorActionPreference = "Stop"

function Resolve-ProjectPath([string] $PathValue) {
  if ([System.IO.Path]::IsPathRooted($PathValue)) {
    return [System.IO.Path]::GetFullPath($PathValue)
  }

  return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $PathValue))
}

function Find-AndroidBuildTool([string] $ToolName) {
  $toolFileNames = if ($IsWindows -or $env:OS -eq "Windows_NT") {
    @("$ToolName.bat", "$ToolName.exe", $ToolName)
  } else {
    @($ToolName)
  }

  foreach ($toolFileName in $toolFileNames) {
    $command = Get-Command $toolFileName -ErrorAction SilentlyContinue
    if ($command) {
      return $command.Source
    }
  }

  $sdkRoots = @(
    $env:ANDROID_HOME,
    $env:ANDROID_SDK_ROOT,
    $(if ($env:LOCALAPPDATA) { Join-Path $env:LOCALAPPDATA "Android\Sdk" }),
    $(if ($env:HOME) { Join-Path $env:HOME "Android/Sdk" }),
    $(if ($env:HOME) { Join-Path $env:HOME "Library/Android/sdk" })
  ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique

  foreach ($sdkRoot in $sdkRoots) {
    $buildToolsDir = Join-Path $sdkRoot "build-tools"
    if (-not (Test-Path -LiteralPath $buildToolsDir -PathType Container)) {
      continue
    }

    foreach ($toolFileName in $toolFileNames) {
      $candidate = Get-ChildItem -LiteralPath $buildToolsDir -Directory |
        Sort-Object Name -Descending |
        ForEach-Object { Join-Path $_.FullName $toolFileName } |
        Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } |
        Select-Object -First 1

      if ($candidate) {
        return $candidate
      }
    }
  }

  throw "Could not find $ToolName. Install Android SDK Build Tools or set ANDROID_HOME."
}

function Read-ApkSignerValue([object[]] $Output, [string] $Label) {
  $lines = @()
  foreach ($item in $Output) {
    $text = ([string] $item) -replace "`e\[[0-9;]*[A-Za-z]", ""
    $lines += $text -split "\r?\n"
  }

  $labelPattern = [regex]::Escape($Label)
  $linePattern = "^\s*$labelPattern\s*:\s*(.+)$"
  $line = $lines | Where-Object { [string] $_ -match $linePattern } | Select-Object -First 1
  if (-not $line -and $Label -match "SHA-256") {
    $line = $lines | Where-Object { [string] $_ -match "^\s*Signer\s+#\d+\s+certificate\s+SHA[- ]?256\s+digest\s*:\s*(.+)$" } | Select-Object -First 1
  }
  if (-not $line) {
    throw "Could not read '$Label' from apksigner output."
  }

  return ([regex]::Match([string] $line, ":\s*(.+)$")).Groups[1].Value.Trim()
}

function Find-JavaTool([string] $ToolName) {
  $toolFileNames = if ($IsWindows -or $env:OS -eq "Windows_NT") {
    @("$ToolName.exe", "$ToolName.bat", $ToolName)
  } else {
    @($ToolName)
  }

  foreach ($toolFileName in $toolFileNames) {
    $command = Get-Command $toolFileName -ErrorAction SilentlyContinue
    if ($command) {
      return $command.Source
    }
  }

  if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    foreach ($toolFileName in $toolFileNames) {
      $candidate = Join-Path (Join-Path $env:JAVA_HOME "bin") $toolFileName
      if (Test-Path -LiteralPath $candidate -PathType Leaf) {
        return $candidate
      }
    }
  }

  throw "Could not find $ToolName. Install JDK or set JAVA_HOME."
}

function Read-ApkCertificateSha256FromPem([string] $ApkFile, [string] $ApkSignerPath) {
  $pemOutput = & $ApkSignerPath verify --print-certs-pem $ApkFile 2>&1
  if ($LASTEXITCODE -ne 0) {
    throw "Could not read APK signing certificate PEM with apksigner."
  }

  $pemText = ($pemOutput | ForEach-Object { [string] $_ }) -join "`n"
  $pemMatch = [regex]::Match(
    $pemText,
    "-----BEGIN CERTIFICATE-----\s*(?<body>[A-Za-z0-9+/=\s]+?)\s*-----END CERTIFICATE-----",
    [System.Text.RegularExpressions.RegexOptions]::Singleline
  )
  if (-not $pemMatch.Success) {
    throw "Could not parse APK signing certificate PEM from apksigner output."
  }

  $certificateBytes = [Convert]::FromBase64String(($pemMatch.Groups["body"].Value -replace "\s", ""))
  $sha256Algorithm = [System.Security.Cryptography.SHA256]::Create()
  try {
    $hashBytes = $sha256Algorithm.ComputeHash($certificateBytes)
  } finally {
    $sha256Algorithm.Dispose()
  }

  return -join ($hashBytes | ForEach-Object { $_.ToString("x2") })
}

function Read-ApkCertificateSha256([string] $ApkFile, [object[]] $ApkSignerOutput, [string] $ApkSignerPath) {
  try {
    return Read-ApkSignerValue $ApkSignerOutput "Signer #1 certificate SHA-256 digest"
  } catch {
    try {
      return Read-ApkCertificateSha256FromPem $ApkFile $ApkSignerPath
    } catch {
      # Some APKs are signed only with newer APK signing schemes, so keytool is a last-resort fallback.
    }

    $keytoolPath = Find-JavaTool "keytool"
    $keytoolOutput = & $keytoolPath -printcert -jarfile $ApkFile 2>&1
    if ($LASTEXITCODE -ne 0) {
      throw "Could not read APK signing certificate from apksigner or keytool."
    }

    $line = $keytoolOutput | Where-Object { [string] $_ -match "^\s*SHA256\s*:\s*(.+)$" } | Select-Object -First 1
    if (-not $line) {
      throw "Could not read APK signing certificate SHA-256 digest from apksigner or keytool output."
    }

    return ([regex]::Match([string] $line, ":\s*(.+)$")).Groups[1].Value.Trim()
  }
}

function Normalize-CertificateFingerprint([string] $Value) {
  if ([string]::IsNullOrWhiteSpace($Value)) {
    return ""
  }

  return ($Value -replace "[:\s-]", "").ToLowerInvariant()
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

if ([string]::IsNullOrWhiteSpace($ExpectedPackageName) -or $ExpectedPackageName -notmatch "^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$") {
  throw "ExpectedPackageName must be a valid Android package name."
}

if ($Channel -eq "production" -and [string]::IsNullOrWhiteSpace($ExpectedCertificateSha256)) {
  throw "ExpectedCertificateSha256 is required for production releases."
}

$apkFullPath = Resolve-ProjectPath $ApkPath
$buildFileFullPath = Resolve-ProjectPath $AndroidBuildFile
$outputFullPath = Resolve-ProjectPath $OutputDir
$previousManifestFullPath = if ([string]::IsNullOrWhiteSpace($PreviousManifestPath)) {
  Join-Path $outputFullPath "latest.json"
} else {
  Resolve-ProjectPath $PreviousManifestPath
}

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

$apkSignerPath = Find-AndroidBuildTool "apksigner"
$apkSignerOutput = & $apkSignerPath verify --print-certs $apkFullPath 2>&1
if ($LASTEXITCODE -ne 0) {
  throw "apksigner verification failed for $apkFullPath.`n$($apkSignerOutput -join "`n")"
}

$expectedCertificateSha256 = if ($Channel -eq "production") {
  Normalize-CertificateFingerprint $ExpectedCertificateSha256
} else {
  ""
}
$certificateSha256 = ""
if ($expectedCertificateSha256 -or $Channel -eq "production") {
  $certificateSha256 = Normalize-CertificateFingerprint (Read-ApkCertificateSha256 $apkFullPath $apkSignerOutput $apkSignerPath)
} else {
  try {
    $certificateSha256 = Normalize-CertificateFingerprint (Read-ApkCertificateSha256 $apkFullPath $apkSignerOutput $apkSignerPath)
  } catch {
    Write-Host "APK signing certificate fingerprint was not available for non-production manifest validation."
  }
}
if ($expectedCertificateSha256 -and $certificateSha256 -ne $expectedCertificateSha256) {
  throw "APK signing certificate does not match the expected DlaFlow release certificate."
}

$aaptPath = Find-AndroidBuildTool "aapt"
$aaptOutput = & $aaptPath dump badging $apkFullPath 2>&1
if ($LASTEXITCODE -ne 0) {
  throw "Could not inspect APK package metadata with aapt.`n$($aaptOutput -join "`n")"
}

$packageLine = $aaptOutput | Where-Object { $_ -match "^package:\s" } | Select-Object -First 1
if (-not $packageLine) {
  throw "Could not read APK package metadata."
}

$packageMatch = [regex]::Match([string] $packageLine, "name='([^']+)'\s+versionCode='(\d+)'\s+versionName='([^']*)'")
if (-not $packageMatch.Success) {
  throw "Could not parse APK package metadata: $packageLine"
}

$apkPackageName = $packageMatch.Groups[1].Value
$apkVersionCode = [int] $packageMatch.Groups[2].Value
$apkVersionName = $packageMatch.Groups[3].Value

if ($apkPackageName -ne $ExpectedPackageName) {
  throw "APK package '$apkPackageName' does not match expected package '$ExpectedPackageName'."
}

if ($apkVersionCode -ne $versionCode -or $apkVersionName -ne $versionName) {
  throw "APK version $apkVersionName ($apkVersionCode) does not match $buildFileFullPath version $versionName ($versionCode)."
}

if (Test-Path -LiteralPath $previousManifestFullPath -PathType Leaf) {
  $previousManifest = Get-Content -LiteralPath $previousManifestFullPath -Raw | ConvertFrom-Json
  $previousVersionCode = if ($null -ne $previousManifest.versionCode) { [int] $previousManifest.versionCode } else { 0 }
  if ($previousManifest.channel -eq $Channel -and $versionCode -le $previousVersionCode) {
    throw "versionCode must be greater than the previous $Channel release. Current: $versionCode, previous: $previousVersionCode."
  }
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
