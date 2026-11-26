param(
  [ValidateSet('up','down','stop','ps','logs','doctor','status','backup','backups','rehearse','restore','update','rollback','verify')][string]$Action='status',
  [switch]$Build,
  [switch]$BackendOnly,
  [switch]$NoAssistant,
  [ValidatePattern('^ops-\d+-[a-f0-9]{6}$')][string]$BackupId,
  [switch]$ReplaceData,
  [ValidateSet('web-admin','website','auth-service','academic-service','teaching-service','finance-service','api-gateway','assistant-service')][string[]]$Service
)
$ErrorActionPreference='Stop'
$backendRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if($Action -in @('up','down','stop','ps','logs')){
  $params=@{Action=$Action;Web=(-not $BackendOnly);Assistant=(-not $NoAssistant);Build=$Build}
  & (Join-Path $PSScriptRoot 'compose.ps1') @params
}elseif($Action -eq 'verify'){
  & node (Join-Path $PSScriptRoot 'verify-dck5.cjs')
  if($LASTEXITCODE -ne 0){throw 'Docker functional verification failed'}
}else{
  if($Action -in @('restore','rehearse','rollback') -and -not $BackupId){throw 'Supply -BackupId ops-<timestamp>-<suffix>.'}
  if($Action -eq 'restore' -and -not $ReplaceData){throw 'Restore replaces Docker data. Supply -ReplaceData explicitly.'}
  $nodeArgs=@((Join-Path $PSScriptRoot 'ops.cjs'),$Action)
  if($BackupId){$nodeArgs+=@('--backup-id',$BackupId)}
  if($ReplaceData){$nodeArgs+='--replace-data'}
  if($Service){$nodeArgs+=@('--services',($Service -join ','))}
  & node @nodeArgs
  if($LASTEXITCODE -ne 0){throw ('Docker '+$Action+' failed')}
}
