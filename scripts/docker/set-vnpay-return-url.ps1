param([string]$BaseUrl)
$ErrorActionPreference='Stop'
$backendRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$envFile=Join-Path ([IO.Path]::GetFullPath((Join-Path $backendRoot '../..'))) '.env.docker.local'
& (Join-Path $backendRoot 'scripts/set-vnpay-return-url.ps1') -BaseUrl $BaseUrl -TargetPort 18080 -EnvFile $envFile
