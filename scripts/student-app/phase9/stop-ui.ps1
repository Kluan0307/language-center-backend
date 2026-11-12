$ErrorActionPreference='Stop'
$root=(Resolve-Path "$PSScriptRoot/../../..").Path
$ownerFile="$root/storage/student-phase9-ui-owner.txt"
if(!(Test-Path -LiteralPath $ownerFile)){$ownerFile="$root/storage/student-phase9-latest.txt"}
$owner=(Get-Content -LiteralPath $ownerFile -Raw).Trim()
$owned=(Get-Content -LiteralPath "$owner/ui-processes.json" -Raw)|ConvertFrom-Json
foreach($key in @('expo','vite')){
 $proc=Get-CimInstance Win32_Process -Filter ("ProcessId="+$owned.$key)
 $expected=if($key -eq 'expo'){'node_modules/expo/bin/cli'}else{'node_modules/vite/bin/vite.js'}
 if($proc -and $proc.Name -eq 'node.exe' -and $proc.CommandLine.Contains($expected)){Stop-Process -Id $proc.ProcessId;Write-Output "Stopped owned $key"}
}
$edge=Get-CimInstance Win32_Process -Filter ("ProcessId="+$owned.edge)
if($edge -and $edge.CommandLine.Contains($owner+'/edge-profile')){Stop-Process -Id $edge.ProcessId;Write-Output 'Stopped owned Edge'}
