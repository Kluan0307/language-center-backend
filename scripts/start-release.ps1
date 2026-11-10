param(
 [Parameter(Mandatory=$true)][string]$Release,
 [Parameter(Mandatory=$true)][ValidateSet('auth-service','academic-service','teaching-service','finance-service','api-gateway')][string]$Service,
 [Parameter(Mandatory=$true)][string]$EnvFile,
 [ValidatePattern('^[A-Za-z0-9-]{1,80}$')][string]$RunTag
)
$ErrorActionPreference='Stop'
$releaseRoot=(Resolve-Path -LiteralPath $Release).Path
$envPath=(Resolve-Path -LiteralPath $EnvFile).Path
foreach($line in Get-Content -LiteralPath $envPath -Encoding UTF8){
 if($line -match '^([A-Z][A-Z0-9_]*)=(.*)$'){
  [Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process')
 }
}
if($env:SPRING_PROFILES_ACTIVE -ne 'prod'){throw 'Release requires SPRING_PROFILES_ACTIVE=prod'}
if(!$env:SPRING_CONFIG_IMPORT){throw 'Set SPRING_CONFIG_IMPORT explicitly to avoid importing developer .env.local'}
if(!$env:CORS_ALLOWED_ORIGINS){throw 'Set explicit CORS_ALLOWED_ORIGINS'}
if($Service -ne 'api-gateway' -and (!$env:DB_PASSWORD -or !$env:JWT_SECRET)){throw 'DB_PASSWORD and JWT_SECRET required'}
if($Service -eq 'teaching-service' -and ![IO.Path]::IsPathRooted($env:DOCUMENT_STORAGE)){throw 'DOCUMENT_STORAGE must be absolute'}
if(!$env:LOGGING_FILE_NAME){if($env:LOG_FILE){$env:LOGGING_FILE_NAME=$env:LOG_FILE}else{$env:LOGGING_FILE_NAME=Join-Path (Split-Path -Parent $envPath) "logs/$Service.log"}}
if(![IO.Path]::IsPathRooted($env:LOGGING_FILE_NAME)){throw 'LOGGING_FILE_NAME must be absolute and outside release'}
$logPath=[IO.Path]::GetFullPath($env:LOGGING_FILE_NAME)
if($logPath.StartsWith($releaseRoot+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Runtime logs must stay outside immutable release'}
$env:LOG_FILE=$logPath
$jar=Join-Path $releaseRoot "backend/$Service.jar"
if(!(Test-Path -LiteralPath $jar -PathType Leaf)){throw 'Release JAR missing'}
$manifest=Get-Content -LiteralPath (Join-Path $releaseRoot 'release.json') -Raw -Encoding UTF8|ConvertFrom-Json
$entry=@($manifest.files|Where-Object{$_.path -eq "backend/$Service.jar"})
if($entry.Count -ne 1 -or (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry[0].sha256){throw 'Release JAR checksum mismatch'}
Push-Location $releaseRoot
try {
 $javaArgs=@()
 if($RunTag){$javaArgs+=@('-Xmx256m',"-Dlc.rehearsal=$RunTag")}
 $javaArgs+=@('-jar',$jar)
 & java @javaArgs
 if($LASTEXITCODE -ne 0){throw "Service exited with code $LASTEXITCODE"}
} finally {Pop-Location}
