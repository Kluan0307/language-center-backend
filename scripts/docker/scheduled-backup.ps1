param([string]$CopyTo)
$ErrorActionPreference='Stop'
$backendRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$taskMutex=New-Object System.Threading.Mutex($false,'Local\LanguageCenterScheduledBackup')
if(-not $taskMutex.WaitOne(0)){throw 'A scheduled backup is already running.'}
try{
  $logFolder=Join-Path $backendRoot 'storage/docker-backups/logs'
  New-Item -ItemType Directory -Path $logFolder -Force | Out-Null
  $logFile=Join-Path $logFolder ('backup-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.log')
  & node (Join-Path $PSScriptRoot 'ops.cjs') backup 2>&1 | Tee-Object -FilePath $logFile -Variable taskOutput
  if($LASTEXITCODE -ne 0){throw 'Backup failed; no external copy attempted.'}
  if($CopyTo){
    $backupRoot=Join-Path $backendRoot 'storage/docker-backups'
    $match=[regex]::Match(($taskOutput -join "`n"),'"backupId"\s*:\s*"(ops-\d+-[a-f0-9]{6})"')
    if(-not $match.Success){throw 'Completed backup ID not found.'}
    $latest=Get-Item -LiteralPath (Join-Path $backupRoot $match.Groups[1].Value)
    if(-not (Test-Path -LiteralPath (Join-Path $latest.FullName 'backup.json'))){throw 'Completed backup not found.'}
    $destination=[IO.Path]::GetFullPath($CopyTo)
    if($destination.StartsWith([IO.Path]::GetFullPath($backupRoot),[StringComparison]::OrdinalIgnoreCase)){throw 'Choose a backup destination outside the primary backup folder.'}
    New-Item -ItemType Directory -Path $destination -Force | Out-Null
    $copy=Join-Path $destination $latest.Name
    if(Test-Path -LiteralPath $copy){throw 'Destination backup already exists.'}
    Copy-Item -LiteralPath $latest.FullName -Destination $copy -Recurse
    foreach($name in @('database.sql','documents.tar','backup.json')){
      if((Get-FileHash -LiteralPath (Join-Path $latest.FullName $name)).Hash -ne (Get-FileHash -LiteralPath (Join-Path $copy $name)).Hash){throw ('External backup copy mismatch: '+$name)}
    }
    Write-Output ('Verified backup copy: '+$copy)
  }
}finally{$taskMutex.ReleaseMutex();$taskMutex.Dispose()}
