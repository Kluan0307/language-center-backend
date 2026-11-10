param([Parameter(Mandatory=$true)][string[]]$Documents)
$ErrorActionPreference='Stop'
$backendRoot=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Push-Location $backendRoot
$runDir=$null
$previousRunDir=$env:ACCEPTANCE_RUN_DIR
$completed=$false
try {
  $prepareArgs=@('scripts/acceptance/prepare.cjs')
  foreach($directory in $Documents){$prepareArgs+=@('--documents',$directory)}
  & node @prepareArgs
  if($LASTEXITCODE -ne 0){throw 'Acceptance preparation failed; inspect prepare.log'}
  $runDir=(Get-Content 'storage/acceptance/latest.txt' -Raw).Trim()
  $env:ACCEPTANCE_RUN_DIR=$runDir
  & "$PSScriptRoot/runtime.ps1" -Action start -Service all -RunDir $runDir
  foreach($phase in @('g17','g18','g17-crud','browser','verify-db')){
    Write-Output "Running real acceptance: $phase"
    & node "scripts/acceptance/$phase.mjs" *> (Join-Path $runDir "$phase.console.log")
    if($LASTEXITCODE -ne 0){throw "Acceptance $phase failed; inspect $runDir/$phase.console.log"}
  }
  & node scripts/operations.cjs smoke --base-port 28080 *> (Join-Path $runDir 'smoke.json')
  if($LASTEXITCODE -ne 0){throw 'Acceptance runtime health failed'}
  $completed=$true
} finally {
  if($runDir){& "$PSScriptRoot/runtime.ps1" -Action stop -Service all -RunDir $runDir}
  $env:ACCEPTANCE_RUN_DIR=$previousRunDir
  Pop-Location
}
if($completed){
  # Hash lifecycle.log after cleanup has appended the final stop records.
  $env:ACCEPTANCE_RUN_DIR=$runDir
  try {
    & node (Join-Path $PSScriptRoot 'summarize.mjs') *> (Join-Path $runDir 'summarize.console.log')
    if($LASTEXITCODE -ne 0){throw 'Acceptance evidence summary failed'}
    Write-Output "PASSED: 86 checks. Evidence: $runDir/acceptance-summary.json"
  } finally {$env:ACCEPTANCE_RUN_DIR=$previousRunDir}
}
