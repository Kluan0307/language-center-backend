param([ValidateSet('start','stop')][string]$Action='start')
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$runDir=(Get-Content (Join-Path $root 'storage/demo-phase4-latest.txt') -Raw).Trim()
$runtime=Get-Content (Join-Path $runDir 'runtime.json') -Raw | ConvertFrom-Json
if($runtime.database -notmatch '^lc_rehearsal_demo_g4_\w+$'){throw 'Only phase4 demo schemas allowed'}
& "$PSScriptRoot/../acceptance/runtime.ps1" -Action $Action -Service all -RunDir $runDir
if($Action -eq 'start'){
 Write-Output "Demo: http://127.0.0.1:$($runtime.frontendPort)"
 Write-Output "Private demo credentials: $runDir/private-session.json (Super Admin bootstrap password: bootstrap.private.json)"
}
