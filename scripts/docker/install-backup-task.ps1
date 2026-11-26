param([ValidatePattern('^\d{2}:\d{2}$')][string]$At='02:00',[string]$CopyTo,[switch]$Remove)
$ErrorActionPreference='Stop'
$name='LanguageCenter-DailyBackup'
if($Remove){Unregister-ScheduledTask -TaskName $name -Confirm:$false;return}
$time=[DateTime]::ParseExact($At,'HH:mm',[Globalization.CultureInfo]::InvariantCulture)
$script=Join-Path $PSScriptRoot 'scheduled-backup.ps1'
if($CopyTo -match '["\r\n]'){throw 'Invalid backup destination.'}
$arguments='-NoProfile -NonInteractive -File "'+$script+'"'
if($CopyTo){$arguments+=' -CopyTo "'+[IO.Path]::GetFullPath($CopyTo)+'"'}
$action=New-ScheduledTaskAction -Execute 'powershell.exe' -Argument $arguments
$trigger=New-ScheduledTaskTrigger -Daily -At $time
$identity=[Security.Principal.WindowsIdentity]::GetCurrent().Name
$principal=New-ScheduledTaskPrincipal -UserId $identity -LogonType Interactive -RunLevel Limited
$settings=New-ScheduledTaskSettingsSet -StartWhenAvailable -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Hours 1)
Register-ScheduledTask -TaskName $name -Action $action -Trigger $trigger -Principal $principal -Settings $settings -Force | Select-Object TaskName,State
