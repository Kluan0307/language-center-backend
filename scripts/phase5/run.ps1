param([Parameter(Mandatory=$true)][string[]]$Documents)
# Continue allows native JVM/npm warnings on stderr; every native exit is checked explicitly.
$ErrorActionPreference='Continue'
$root=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Push-Location $root
$runDir=$null;$restoredDir=$null;$previousRunDir=$env:ACCEPTANCE_RUN_DIR
try {
 & mvn "-Dmaven.repo.local=$(Join-Path $env:USERPROFILE '.m2/repository')" verify *> 'storage/phase5-backend.log'
 if($LASTEXITCODE -ne 0){throw 'Backend verify failed'}
 & node scripts/phase5/backend-summary.cjs
 if($LASTEXITCODE -ne 0){throw 'Surefire summary failed'}
 $frontend=(Resolve-Path ../../language-center-web-frontend).Path
 & npm --prefix $frontend run lint *> 'storage/phase5-frontend.log';if($LASTEXITCODE -ne 0){throw 'FE lint failed'}
 & npm --prefix $frontend run build >> 'storage/phase5-frontend.log' 2>&1;if($LASTEXITCODE -ne 0){throw 'FE build failed'}
 & npm --prefix $frontend test >> 'storage/phase5-frontend.log' 2>&1;if($LASTEXITCODE -ne 0){throw 'FE tests failed'}
 & node --test scripts/operations.test.cjs scripts/release-integrity.test.cjs *> 'storage/phase5-operations-tests.log';if($LASTEXITCODE -ne 0){throw 'Operations tests failed'}
 & "$PSScriptRoot/../acceptance/run.ps1" -Documents $Documents
 if($LASTEXITCODE -ne 0){throw 'Baseline acceptance failed'}
 $runDir=(Get-Content 'storage/acceptance/latest.txt' -Raw).Trim();Set-Content 'storage/phase5-latest.txt' $runDir
 if(-not (Test-Path (Join-Path $runDir 'acceptance-summary.json'))){throw 'Baseline summary missing'}
 $env:ACCEPTANCE_RUN_DIR=$runDir
 & "$PSScriptRoot/../acceptance/runtime.ps1" -Action start -Service all -RunDir $runDir
 & node -e "import('./scripts/acceptance/client.mjs').then(m=>m.ready())";if($LASTEXITCODE -ne 0){throw 'UI runtime not ready'}
 & node scripts/phase5/live-ui.mjs *> (Join-Path $runDir 'phase5-live-ui.console.log');if($LASTEXITCODE -ne 0){throw 'New UI regression failed'}
 & "$PSScriptRoot/../acceptance/runtime.ps1" -Action stop -Service all -RunDir $runDir
 & node scripts/acceptance/summarize.mjs *> (Join-Path $runDir 'summarize.console.log');if($LASTEXITCODE -ne 0){throw 'Baseline summary failed'}
 & node scripts/phase5/apply-source-v011.cjs *> (Join-Path $runDir 'source-v011.console.log');if($LASTEXITCODE -ne 0){throw 'Source V011 check/migration failed'}
 & node scripts/phase5/recover.cjs *> (Join-Path $runDir 'restore.console.log');if($LASTEXITCODE -ne 0){throw 'Recovery failed'}
 $restoredDir=(Get-Content 'storage/phase5-restored-run.txt' -Raw).Trim();$env:ACCEPTANCE_RUN_DIR=$restoredDir
 & "$PSScriptRoot/../acceptance/runtime.ps1" -Action start -Service all -RunDir $restoredDir
 & node -e "import('./scripts/acceptance/client.mjs').then(m=>m.ready())";if($LASTEXITCODE -ne 0){throw 'Restored runtime not ready'}
 & node scripts/operations.cjs smoke --base-port 28080 *> (Join-Path $restoredDir 'smoke.json');if($LASTEXITCODE -ne 0){throw 'Restored smoke failed'}
 & node scripts/phase5/live-ui.mjs *> (Join-Path $restoredDir 'phase5-live-ui.console.log');if($LASTEXITCODE -ne 0){throw 'Restored UI regression failed'}
 & node scripts/phase5/summarize.cjs
 if($LASTEXITCODE -ne 0){throw 'Final evidence incomplete'}
 Write-Output "Phase5 passed. Evidence: $runDir/phase5-summary.json"
} finally {
 if($runDir){& "$PSScriptRoot/../acceptance/runtime.ps1" -Action stop -Service all -RunDir $runDir}
 if($restoredDir){& "$PSScriptRoot/../acceptance/runtime.ps1" -Action stop -Service all -RunDir $restoredDir}
 $env:ACCEPTANCE_RUN_DIR=$previousRunDir
 Pop-Location
}
