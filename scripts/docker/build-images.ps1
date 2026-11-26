param(
  [ValidateSet('all','auth-service','academic-service','teaching-service','finance-service','api-gateway','assistant-service')]
  [string[]]$Service=@('all'),
  [ValidatePattern('^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$')]
  [string]$Tag='local',
  [switch]$RunTests,
  [switch]$DryRun
)
$ErrorActionPreference='Stop'
$backendRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$catalog=Get-Content -LiteralPath (Join-Path $backendRoot 'deployment/docker/services.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$selected=@($catalog | Where-Object { $Service -contains 'all' -or $Service -contains $_.name })
if(-not $DryRun){
  & docker info --format '{{.OSType}}' | Out-Null
  if($LASTEXITCODE -ne 0){throw 'Docker Engine is not available. Start Docker Desktop with Linux containers.'}
}
foreach($item in $selected){
  $buildArgs=@('build','--progress','plain','--file',(Join-Path $backendRoot 'Dockerfile'),'--tag',('language-center/'+$item.name+':'+$Tag),'--build-arg',('SERVICE='+$item.name),'--build-arg',('SERVICE_PORT='+$item.port),'--build-arg',('SKIP_TESTS='+(-not $RunTests.IsPresent).ToString().ToLowerInvariant()),$backendRoot)
  if($DryRun){Write-Output ('docker '+(($buildArgs | ForEach-Object { if($_ -match '\s'){ '"'+$_+'"' }else{$_} }) -join ' '));continue}
  Write-Host ('Building '+$item.name)
  & docker @buildArgs
  if($LASTEXITCODE -ne 0){throw ('Image build failed: '+$item.name)}
}
