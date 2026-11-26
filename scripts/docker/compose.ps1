param(
  [ValidateSet('up','down','ps','logs','stop')][string]$Action='up',
  [switch]$Assistant,
  [switch]$Web,
  [switch]$Build,
  [ValidatePattern('^[a-z0-9][a-z0-9_-]*$')][string]$ProjectName='language-center',
  [string]$EnvFile
)
$ErrorActionPreference='Stop'
$backendRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if(-not $EnvFile){$EnvFile=Join-Path ([IO.Path]::GetFullPath((Join-Path $backendRoot '../..'))) '.env.docker.local'}
if(-not(Test-Path -LiteralPath $EnvFile)){throw 'Run node scripts/docker/init-env.cjs once before Compose.'}
$composeArgs=@('compose','--project-name',$ProjectName,'--file',(Join-Path $backendRoot 'compose.yaml'),'--env-file',([IO.Path]::GetFullPath($EnvFile)))
if($Web -or $Action -ne 'up'){$composeArgs+=@('--file',(Join-Path $backendRoot 'compose.web.yaml'))}
if($Assistant -or $Action -ne 'up'){$composeArgs+=@('--profile','assistant')}
switch($Action){
  'up' {$composeArgs+=@('up','--detach','--wait','--wait-timeout','240');if($Build){$composeArgs+='--build'}else{$composeArgs+='--no-build'}}
  'down' {$composeArgs+=@('down','--remove-orphans')} # Volumes are deliberately preserved.
  'ps' {$composeArgs+='ps'}
  'logs' {$composeArgs+=@('logs','--tail','100')}
  'stop' {$composeArgs+='stop'}
}
& docker @composeArgs
if($LASTEXITCODE -ne 0){throw ('Docker Compose '+$Action+' failed')}
