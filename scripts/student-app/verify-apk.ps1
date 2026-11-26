param([Parameter(Mandatory=$true)][string]$AppRoot)
$ErrorActionPreference = 'Stop'
$appPath = [IO.Path]::GetFullPath($AppRoot)
$artifactDir = Join-Path $appPath 'artifacts'
$info = Get-Content -LiteralPath (Join-Path $artifactDir 'latest-android.json') -Raw -Encoding utf8 | ConvertFrom-Json
$apkPath = [IO.Path]::GetFullPath((Join-Path $artifactDir $info.file))
if (-not $apkPath.StartsWith($artifactDir + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'APK path outside artifacts' }
$digest = (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash.ToLowerInvariant()
if ($digest -ne $info.sha256) { throw 'APK digest does not match build manifest' }
$sdkRoot = $env:ANDROID_HOME
if (-not $sdkRoot) { $sdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$tools = Get-ChildItem -LiteralPath (Join-Path $sdkRoot 'build-tools') -Directory | Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
$signer = Join-Path $tools.FullName 'apksigner.bat'
$signing = @(& $signer verify --verbose --print-certs $apkPath 2>&1)
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed' }
$certificateMatch = [regex]::Match(($signing -join "`n"), 'certificate SHA-256 digest:\s*([0-9a-fA-F]{64})')
if (-not $certificateMatch.Success) { throw 'Cannot read signing certificate digest' }
$certificate = $certificateMatch.Groups[1].Value.ToLowerInvariant()
$previousApk = Get-ChildItem -LiteralPath $artifactDir -Filter '*.apk' -File | Where-Object { $_.FullName -ne $apkPath } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$upgradeSigningMatch = $null
if ($previousApk) {
  $previousSigning = @(& $signer verify --print-certs $previousApk.FullName 2>&1)
  if ($LASTEXITCODE -ne 0) { throw 'Previous APK signature verification failed' }
  $previousMatch = [regex]::Match(($previousSigning -join "`n"), 'certificate SHA-256 digest:\s*([0-9a-fA-F]{64})')
  $upgradeSigningMatch = $previousMatch.Success -and $previousMatch.Groups[1].Value.ToLowerInvariant() -eq $certificate
  if (-not $upgradeSigningMatch) { throw 'Signing key does not match the previous APK' }
}
$badging = @(& (Join-Path $tools.FullName 'aapt.exe') dump badging $apkPath 2>&1)
if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect APK manifest' }
$packageLine = [string]($badging | Where-Object { $_ -match '^package:' } | Select-Object -First 1)
if ($packageLine -notmatch "name='com.languagecenter.student'" -or $packageLine -notmatch "versionName='$([regex]::Escape($info.version))'" -or $packageLine -notmatch "versionCode='$($info.versionCode)'" ) { throw 'APK package/version differs from release metadata' }
$tree = @(& (Join-Path $tools.FullName 'aapt.exe') dump xmltree $apkPath AndroidManifest.xml 2>&1)
if (($tree -join "`n") -match 'android:debuggable.*0xffffffff') { throw 'APK must not be debuggable' }
if (-not ($badging | Where-Object { $_ -match '^launchable-activity:' })) { throw 'APK has no launchable activity' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead($apkPath)
try {
  $configEntry = $zip.GetEntry('assets/app.config')
  $reader = [IO.StreamReader]::new($configEntry.Open())
  try { $runtime = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
  if ($runtime.extra.apiBaseUrl -ne $info.apiBaseUrl) { throw 'Embedded API URL differs from release metadata' }
  if ($runtime.version -ne $info.version) { throw 'Embedded Expo version differs from release metadata' }
  $entry = $zip.GetEntry('assets/index.android.bundle')
  $memory = [IO.MemoryStream]::new()
  $stream = $entry.Open()
  try { $stream.CopyTo($memory); $bundle = [Text.Encoding]::UTF8.GetString($memory.ToArray()) } finally { $stream.Dispose(); $memory.Dispose() }
  if (-not $bundle.Contains($info.apiBaseUrl)) { throw 'Expected public API URL not found in embedded JS bundle' }
  foreach ($banned in @('127.0.0.1:18765', 'http://192.168.1.2:8080', 'local-synthetic-session', 'synthetic-session-901')) {
    if ($bundle.Contains($banned)) { throw 'APK contains a local verification endpoint or fixture' }
  }
  $abis = @($zip.Entries | Where-Object { $_.FullName -match '^lib/[^/]+/libreactnative.so$' } | ForEach-Object { $_.FullName.Split('/')[1] })
  if ($abis -notcontains 'arm64-v8a') { throw 'APK does not contain the intended ARM64 native library' }
} finally { $zip.Dispose() }
$sourceLines = foreach ($file in Get-ChildItem -LiteralPath (Join-Path $appPath 'src') -Recurse -File | Sort-Object FullName) {
  $file.FullName.Substring($appPath.Length + 1).Replace('\','/') + ':' + (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
}
$sha = [Security.Cryptography.SHA256]::Create()
try { $sourceDigest = [BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes(($sourceLines -join "`n")))).Replace('-','').ToLowerInvariant() } finally { $sha.Dispose() }
$gatewayStatus = $null; $gatewayError = $null
try {
  $response = Invoke-WebRequest -Uri ($info.apiBaseUrl + '/auth/me') -UseBasicParsing -TimeoutSec 5 -Headers @{'ngrok-skip-browser-warning'='true'}
  $gatewayStatus = [int]$response.StatusCode
} catch {
  if ($_.Exception.Response) { $gatewayStatus = [int]$_.Exception.Response.StatusCode; $gatewayError = $_.Exception.Response.Headers['ngrok-error-code'] }
}
$gatewayReachable = $gatewayStatus -in @(200, 401, 403) -and -not $gatewayError
$report = [ordered]@{
  file = $info.file; version = $info.version; versionCode = $info.versionCode; packageName = $info.packageName
  bytes = (Get-Item -LiteralPath $apkPath).Length; sha256 = $digest; sourceSha256 = $sourceDigest; apiBaseUrl = $info.apiBaseUrl
  architecture = $abis; signatureVerified = $true; debuggable = $false; nativeVersionVerified = $true
  embeddedConfigVerified = $true; fixtureEndpointAbsent = $true
  certificateSha256 = $certificate; upgradeSigningMatch = $upgradeSigningMatch
  distribution = 'Internal only; local debug keystore signing'
  deviceAcceptance = 'PENDING'; gatewayReachable = $gatewayReachable; gatewayHttpStatus = $gatewayStatus; gatewayError = $gatewayError
  checkedAt = (Get-Date).ToUniversalTime().ToString('o')
}
$json = $report | ConvertTo-Json -Depth 5
[IO.File]::WriteAllText((Join-Path $artifactDir 'latest-android-verification.json'), $json, [Text.UTF8Encoding]::new($false))
$json
